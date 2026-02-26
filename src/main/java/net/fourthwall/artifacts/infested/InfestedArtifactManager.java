package net.fourthwall.artifacts.infested;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.config.ArtifactsConfig;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.registry.ModItems;
import net.fourthwall.artifacts.registry.ModStatusEffects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class InfestedArtifactManager {
    private static final int HIT_EFFECT_DURATION_TICKS = 100;
    private static final int SELF_EFFECT_DURATION_TICKS = 10;
    private static final int COMMAND_RADIUS = 12;
    private static final int COMMAND_TIMEOUT_TICKS = 60;
    private static final int COMMAND_OUT_OF_AGGRO_RANGE = 24;
    private static final int PICKAXE_INFEST_RADIUS = 3;
    private static final int PICKAXE_PULSE_INTERVAL = 5;
    private static final int SWORD_AURA_INTERVAL = 10;
    private static final int PROTECTED_PLAYER_CLEAR_RADIUS = 32;

    private static final Map<UUID, UUID> COMMAND_TARGETS = new HashMap<>();
    private static final Map<UUID, Integer> NOT_HOLDING_SWORD_TICKS = new HashMap<>();
    private static List<ResolvedStatusEffect> swordAuraBuffs = List.of();
    private static long tickCounter = 0L;

    private InfestedArtifactManager() {
    }

    public static void init() {
        reloadConfig();
        ServerLivingEntityEvents.AFTER_DAMAGE.register(InfestedArtifactManager::onAfterDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(InfestedArtifactManager::onAllowDamage);
        PlayerBlockBreakEvents.AFTER.register(InfestedArtifactManager::onAfterBlockBreak);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearOwner(handler.player.getUuid()));
        ServerTickEvents.END_SERVER_TICK.register(InfestedArtifactManager::onEndServerTick);
    }

    public static void reloadConfig() {
        swordAuraBuffs = resolveConfiguredEffects(ArtifactsConfigManager.get().infestedSword.silverfishAuraBuffs, "infested sword silverfishAuraBuffs");
    }

    private static void onAfterDamage(LivingEntity entity, net.minecraft.entity.damage.DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F) {
            return;
        }
        if (!(source.getAttacker() instanceof PlayerEntity attacker)) {
            return;
        }
        if (!isHoldingInfestedSword(attacker)) {
            return;
        }

        applyRefreshedGuaranteedInfested(entity, HIT_EFFECT_DURATION_TICKS);
        attacker.addStatusEffect(new StatusEffectInstance(ModStatusEffects.GUARANTEED_INFESTED, SELF_EFFECT_DURATION_TICKS, 0, true, false));

        if (entity != attacker && ArtifactsConfigManager.get().infestedSword.enableTargetingCommand) {
            UUID ownerId = attacker.getUuid();
            COMMAND_TARGETS.put(ownerId, entity.getUuid());
            NOT_HOLDING_SWORD_TICKS.put(ownerId, 0);
        }
    }

    private static boolean onAllowDamage(LivingEntity entity, net.minecraft.entity.damage.DamageSource source, float amount) {
        if (entity instanceof PlayerEntity player && hasInfestedArtifact(player) && source.getAttacker() instanceof SilverfishEntity) {
            return false;
        }
        return true;
    }

    private static void onAfterBlockBreak(net.minecraft.world.World world, PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        if (player.isCreative()) {
            return;
        }
        if (!isHoldingInfestedPickaxe(player)) {
            return;
        }
        if (!isInfestedStoneVariant(state)) {
            return;
        }

        Block.dropStack(serverWorld, pos, state.getBlock().asItem().getDefaultStack());

        SilverfishEntity silverfish = EntityType.SILVERFISH.create(serverWorld, SpawnReason.TRIGGERED);
        if (silverfish != null) {
            Vec3d spawn = Vec3d.ofCenter(pos);
            silverfish.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, serverWorld.getRandom().nextFloat() * 360.0F, 0.0F);
            serverWorld.spawnEntity(silverfish);
        }
    }

    private static void onEndServerTick(MinecraftServer server) {
        tickCounter++;

        Set<UUID> protectedPlayers = new HashSet<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (hasInfestedArtifact(player)) {
                protectedPlayers.add(player.getUuid());
            }
        }

        updateHolderEffectsAndAuras(server);
        updatePickaxeInfestation(server);
        updateSilverfishCommands(server, protectedPlayers);
        clearSilverfishAggroOnProtectedPlayers(server, protectedPlayers);
    }

    private static void updateHolderEffectsAndAuras(MinecraftServer server) {
        boolean runAuraPass = tickCounter % SWORD_AURA_INTERVAL == 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!isHoldingInfestedSword(player)) {
                continue;
            }

            player.addStatusEffect(new StatusEffectInstance(ModStatusEffects.GUARANTEED_INFESTED, SELF_EFFECT_DURATION_TICKS, 0, true, false));

            if (!runAuraPass) {
                continue;
            }

            ServerWorld world = (ServerWorld) player.getEntityWorld();
            List<SilverfishEntity> silverfish = world.getEntitiesByClass(
                    SilverfishEntity.class,
                    player.getBoundingBox().expand(ArtifactsConfigManager.get().infestedSword.silverfishAuraRadius),
                    Entity::isAlive
            );

            for (SilverfishEntity fish : silverfish) {
                applyConfiguredEffects(fish, swordAuraBuffs);
            }
        }
    }

    private static void updatePickaxeInfestation(MinecraftServer server) {
        if (!ArtifactsConfigManager.get().infestedPickaxe.enableAuraInfestation) {
            return;
        }
        if (tickCounter % PICKAXE_PULSE_INTERVAL != 0) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!isHoldingInfestedPickaxe(player)) {
                continue;
            }

            ServerWorld world = (ServerWorld) player.getEntityWorld();
            BlockPos origin = BlockPos.ofFloored(player.getX(), player.getY() - 1.0, player.getZ());

            for (int dx = -PICKAXE_INFEST_RADIUS; dx <= PICKAXE_INFEST_RADIUS; dx++) {
                for (int dz = -PICKAXE_INFEST_RADIUS; dz <= PICKAXE_INFEST_RADIUS; dz++) {
                    BlockPos target = origin.add(dx, 0, dz);
                    BlockState state = world.getBlockState(target);
                    if (state.isOf(Blocks.STONE)) {
                        world.setBlockState(target, Blocks.INFESTED_STONE.getDefaultState());
                    } else if (state.isOf(Blocks.COBBLESTONE)) {
                        world.setBlockState(target, Blocks.INFESTED_COBBLESTONE.getDefaultState());
                    }
                }
            }
        }
    }

    private static void updateSilverfishCommands(MinecraftServer server, Set<UUID> protectedPlayers) {
        if (!ArtifactsConfigManager.get().infestedSword.enableTargetingCommand) {
            COMMAND_TARGETS.clear();
            NOT_HOLDING_SWORD_TICKS.clear();
            return;
        }

        for (UUID ownerId : new ArrayList<>(COMMAND_TARGETS.keySet())) {
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);
            if (owner == null || !owner.isAlive()) {
                clearOwner(ownerId);
                continue;
            }

            if (!isHoldingInfestedSword(owner)) {
                int notHoldingTicks = NOT_HOLDING_SWORD_TICKS.getOrDefault(ownerId, 0) + 1;
                if (notHoldingTicks >= COMMAND_TIMEOUT_TICKS) {
                    clearOwner(ownerId);
                    continue;
                }
                NOT_HOLDING_SWORD_TICKS.put(ownerId, notHoldingTicks);
            } else {
                NOT_HOLDING_SWORD_TICKS.put(ownerId, 0);
            }

            LivingEntity target = getLivingEntity(server, COMMAND_TARGETS.get(ownerId));
            if (target == null || !target.isAlive()) {
                clearOwner(ownerId);
                continue;
            }
            if (target.getEntityWorld() != owner.getEntityWorld()) {
                clearOwner(ownerId);
                continue;
            }
            if (owner.squaredDistanceTo(target) > (double) (COMMAND_OUT_OF_AGGRO_RANGE * COMMAND_OUT_OF_AGGRO_RANGE)) {
                clearOwner(ownerId);
                continue;
            }
            if (target instanceof PlayerEntity targetPlayer && protectedPlayers.contains(targetPlayer.getUuid())) {
                clearOwner(ownerId);
                continue;
            }

            ServerWorld world = (ServerWorld) owner.getEntityWorld();
            List<SilverfishEntity> silverfish = world.getEntitiesByClass(
                    SilverfishEntity.class,
                    owner.getBoundingBox().expand(COMMAND_RADIUS),
                    Entity::isAlive
            );

            for (SilverfishEntity fish : silverfish) {
                fish.setTarget(target);
            }
        }
    }

    private static void clearSilverfishAggroOnProtectedPlayers(MinecraftServer server, Set<UUID> protectedPlayers) {
        if (protectedPlayers.isEmpty()) {
            return;
        }

        for (ServerPlayerEntity protectedPlayer : server.getPlayerManager().getPlayerList()) {
            if (!protectedPlayers.contains(protectedPlayer.getUuid())) {
                continue;
            }

            ServerWorld world = (ServerWorld) protectedPlayer.getEntityWorld();
            List<SilverfishEntity> silverfish = world.getEntitiesByClass(
                    SilverfishEntity.class,
                    protectedPlayer.getBoundingBox().expand(PROTECTED_PLAYER_CLEAR_RADIUS),
                    Entity::isAlive
            );

            for (SilverfishEntity fish : silverfish) {
                LivingEntity target = fish.getTarget();
                if (target instanceof PlayerEntity targetPlayer && protectedPlayers.contains(targetPlayer.getUuid())) {
                    fish.setTarget(null);
                }
                LivingEntity attacker = fish.getAttacker();
                if (attacker instanceof PlayerEntity attackerPlayer && protectedPlayers.contains(attackerPlayer.getUuid())) {
                    fish.setAttacker(null);
                }
            }
        }
    }

    private static LivingEntity getLivingEntity(MinecraftServer server, UUID id) {
        if (id == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static boolean hasInfestedArtifact(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (isInfestedArtifact(inventory.getStack(slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHoldingInfestedSword(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.INFESTED_SWORD) || player.getOffHandStack().isOf(ModItems.INFESTED_SWORD);
    }

    private static boolean isHoldingInfestedPickaxe(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.INFESTED_PICKAXE) || player.getOffHandStack().isOf(ModItems.INFESTED_PICKAXE);
    }

    private static boolean isInfestedArtifact(ItemStack stack) {
        return stack.isOf(ModItems.INFESTED_SWORD) || stack.isOf(ModItems.INFESTED_PICKAXE);
    }

    private static boolean isInfestedStoneVariant(BlockState state) {
        return state.isOf(Blocks.INFESTED_STONE) || state.isOf(Blocks.INFESTED_COBBLESTONE);
    }

    private static void applyRefreshedGuaranteedInfested(LivingEntity entity, int durationTicks) {
        entity.removeStatusEffect(ModStatusEffects.GUARANTEED_INFESTED);
        entity.addStatusEffect(new StatusEffectInstance(ModStatusEffects.GUARANTEED_INFESTED, durationTicks, 0));
    }

    private static void clearOwner(UUID ownerId) {
        COMMAND_TARGETS.remove(ownerId);
        NOT_HOLDING_SWORD_TICKS.remove(ownerId);
    }

    private static void applyConfiguredEffects(LivingEntity entity, List<ResolvedStatusEffect> effects) {
        for (ResolvedStatusEffect effect : effects) {
            entity.addStatusEffect(new StatusEffectInstance(effect.effect(), effect.durationTicks(), effect.amplifier(), true, false));
        }
    }

    private static List<ResolvedStatusEffect> resolveConfiguredEffects(List<ArtifactsConfig.StatusEffectEntry> entries, String settingName) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<ResolvedStatusEffect> resolved = new ArrayList<>();
        for (ArtifactsConfig.StatusEffectEntry entry : entries) {
            if (entry == null || entry.effectId == null) {
                continue;
            }

            Identifier id = Identifier.tryParse(entry.effectId);
            if (id == null) {
                FourthWallArtifacts.LOGGER.warn("Skipping invalid status effect id '{}' in {}", entry.effectId, settingName);
                continue;
            }

            var effectEntry = Registries.STATUS_EFFECT.getEntry(id);
            if (effectEntry.isEmpty()) {
                FourthWallArtifacts.LOGGER.warn("Skipping unknown status effect id '{}' in {}", id, settingName);
                continue;
            }

            resolved.add(new ResolvedStatusEffect(
                    effectEntry.get(),
                    Math.max(0, entry.durationTicks),
                    Math.max(0, entry.amplifier)
            ));
        }
        return List.copyOf(resolved);
    }

    private record ResolvedStatusEffect(RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int durationTicks, int amplifier) {
    }
}
