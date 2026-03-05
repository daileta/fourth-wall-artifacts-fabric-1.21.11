package net.fourthwall.artifacts.poseidon;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.config.ArtifactsConfig;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PoseidonTridentManager {
    private static final int BEAM_VISUAL_INTERVAL = 2;
    private static final int BEAM_DAMAGE_INTERVAL = 10;
    private static final float RETALIATION_DAMAGE = 8.0F;
    private static final Identifier CHANNELING_MOVEMENT_MODIFIER_ID = FourthWallArtifacts.id("poseidon_channeling_speed");
    private static final Map<UUID, Integer> SPECIAL_COOLDOWNS = new HashMap<>();
    private static final Set<UUID> CHANNELING_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> CHANNELING_DURATIONS = new HashMap<>();
    private static final Map<UUID, Vec3d> CHANNELING_LOCK_POSITIONS = new HashMap<>();
    private static List<ResolvedStatusEffect> holdingBuffs = List.of();
    private static EntityAttributeModifier channelingMovementModifier = new EntityAttributeModifier(
            CHANNELING_MOVEMENT_MODIFIER_ID,
            -1.0D,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
    private static int retaliationDepth = 0;

    private PoseidonTridentManager() {
    }

    public static void init() {
        reloadConfig(null);
        ServerTickEvents.END_SERVER_TICK.register(PoseidonTridentManager::onEndServerTick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(PoseidonTridentManager::onAfterDamage);
    }

    public static void reloadConfig(MinecraftServer server) {
        holdingBuffs = resolveConfiguredEffects(ArtifactsConfigManager.get().tridentOfPoseidon.holdingEffects, "tridentOfPoseidon.holdingEffects");
        channelingMovementModifier = createChannelingMovementModifier();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                removeChannelingMovementModifier(player);
            }
        }
    }

    private static void onEndServerTick(MinecraftServer server) {
        long tick = server.getTicks();
        tickCooldowns();
        Set<UUID> channelingThisTick = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!isHoldingPoseidonTrident(player)) {
                removeChannelingMovementModifier(player);
                continue;
            }

            applyHoldingBuffs(player);

            if (!canUsePoseidonSpecial(player)) {
                removeChannelingMovementModifier(player);
                continue;
            }

            UUID uuid = player.getUuid();
            int nextChannelTicks = CHANNELING_DURATIONS.getOrDefault(uuid, 0) + 1;
            if (nextChannelTicks > ArtifactsConfigManager.get().tridentOfPoseidon.beamDurationTicks) {
                SPECIAL_COOLDOWNS.put(uuid, ArtifactsConfigManager.get().tridentOfPoseidon.beamCooldownTicks);
                CHANNELING_DURATIONS.remove(uuid);
                CHANNELING_LOCK_POSITIONS.remove(uuid);
                removeChannelingMovementModifier(player);
                continue;
            }

            CHANNELING_DURATIONS.put(uuid, nextChannelTicks);
            channelingThisTick.add(uuid);
            CHANNELING_LOCK_POSITIONS.putIfAbsent(uuid, new Vec3d(player.getX(), player.getY(), player.getZ()));
            applyChannelingMovementControl(player);
            spawnStanceParticles(player, tick);

            List<LivingEntity> targets = getBeamTargets(player);
            if (targets.isEmpty()) {
                continue;
            }

            if (tick % BEAM_VISUAL_INTERVAL == 0) {
                spawnBeamVisuals(player, targets);
            }

            if (tick % BEAM_DAMAGE_INTERVAL == 0) {
                fireBeams(player, targets);
            }
        }

        for (UUID uuid : Set.copyOf(CHANNELING_PLAYERS)) {
            if (!channelingThisTick.contains(uuid)) {
                SPECIAL_COOLDOWNS.put(uuid, ArtifactsConfigManager.get().tridentOfPoseidon.beamCooldownTicks);
                CHANNELING_DURATIONS.remove(uuid);
                CHANNELING_LOCK_POSITIONS.remove(uuid);
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null) {
                    removeChannelingMovementModifier(player);
                }
            }
        }

        CHANNELING_PLAYERS.clear();
        CHANNELING_PLAYERS.addAll(channelingThisTick);
    }

    private static void onAfterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        spawnPoseidonHitParticles(entity, source, damageTaken, blocked);

        if (retaliationDepth > 0 || blocked || damageTaken <= 0.0F) {
            return;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!canUsePoseidonSpecial(player)) {
            return;
        }
        if (!(source.getAttacker() instanceof LivingEntity attacker) || !attacker.isAlive() || attacker == player) {
            return;
        }

        retaliationDepth++;
        try {
            ServerWorld world = (ServerWorld) player.getEntityWorld();
            attacker.damage(world, attacker.getDamageSources().playerAttack(player), RETALIATION_DAMAGE);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getBodyY(0.5), attacker.getZ(), 10, 0.25, 0.25, 0.25, 0.02);
            world.spawnParticles(ParticleTypes.SPLASH, attacker.getX(), attacker.getBodyY(0.5), attacker.getZ(), 8, 0.25, 0.2, 0.25, 0.03);
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.8F, 1.25F);
        } finally {
            retaliationDepth--;
        }
    }

    private static void spawnPoseidonHitParticles(LivingEntity target, DamageSource source, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F || !ArtifactsConfigManager.get().tridentOfPoseidon.enableParticles) {
            return;
        }
        if (!(target.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        boolean poseidonStrike = false;
        ItemStack weapon = source.getWeaponStack();
        if (weapon != null && weapon.isOf(ModItems.TRIDENT_OF_POSEIDON)) {
            poseidonStrike = true;
            if (source.getAttacker() instanceof ServerPlayerEntity attacker && CHANNELING_PLAYERS.contains(attacker.getUuid())) {
                poseidonStrike = false;
            }
        }

        if (source.getSource() instanceof TridentEntity trident && trident.getWeaponStack().isOf(ModItems.TRIDENT_OF_POSEIDON)) {
            poseidonStrike = true;
        }

        if (!poseidonStrike) {
            return;
        }

        double x = target.getX();
        double y = target.getBodyY(0.55D);
        double z = target.getZ();
        world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP, x, y, z, 18, 0.45D, 0.35D, 0.45D, 0.025D);
        world.spawnParticles(ParticleTypes.BUBBLE_POP, x, y, z, 16, 0.45D, 0.35D, 0.45D, 0.02D);
    }

    private static void applyHoldingBuffs(ServerPlayerEntity player) {
        for (ResolvedStatusEffect effect : holdingBuffs) {
            player.addStatusEffect(new StatusEffectInstance(effect.effect(), effect.durationTicks(), effect.amplifier(), true, false));
        }
    }

    private static List<LivingEntity> getBeamTargets(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        return world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(ArtifactsConfigManager.get().tridentOfPoseidon.beamRange),
                target -> isValidBeamTarget(player, target)
        );
    }

    private static boolean isValidBeamTarget(ServerPlayerEntity player, LivingEntity target) {
        if (!target.isAlive() || target == player) {
            return false;
        }
        if (target instanceof PlayerEntity otherPlayer && (otherPlayer.isSpectator() || otherPlayer.isCreative())) {
            return false;
        }
        return hasLineOfSight(player, target);
    }

    private static void fireBeams(ServerPlayerEntity player, List<LivingEntity> targets) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.7F, 1.0F);

        for (LivingEntity target : targets) {
            target.damage(world, target.getDamageSources().playerAttack(player), ArtifactsConfigManager.get().tridentOfPoseidon.beamDamage);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            world.spawnParticles(ParticleTypes.BUBBLE, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.25, 0.25, 0.25, 0.02);
        }
    }

    private static void spawnBeamVisuals(ServerPlayerEntity player, List<LivingEntity> targets) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d origin = player.getEyePos();

        for (LivingEntity target : targets) {
            Vec3d end = new Vec3d(target.getX(), target.getBodyY(0.55), target.getZ());
            Vec3d delta = end.subtract(origin);
            int steps = Math.max(8, (int) (delta.length() * 4.0));
            Vec3d step = delta.multiply(1.0 / steps);

            for (int i = 0; i <= steps; i++) {
                Vec3d point = origin.add(step.multiply(i));
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
                if (i % 3 == 0) {
                    world.spawnParticles(ParticleTypes.NAUTILUS, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
    }

    private static void spawnStanceParticles(ServerPlayerEntity player, long tick) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        double x = player.getX();
        double y = player.getY() + 0.9;
        double z = player.getZ();

        world.spawnParticles(ParticleTypes.NAUTILUS, x, y, z, 12, 0.6, 0.7, 0.6, 0.01);
        world.spawnParticles(ParticleTypes.BUBBLE, x, y, z, 10, 0.5, 0.6, 0.5, 0.02);
        if (tick % 4 == 0) {
            world.spawnParticles(ParticleTypes.SPLASH, x, player.getY() + 0.1, z, 8, 0.45, 0.1, 0.45, 0.03);
        }
    }

    private static boolean hasLineOfSight(ServerPlayerEntity player, LivingEntity target) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d start = player.getEyePos();
        Vec3d end = new Vec3d(target.getX(), target.getBodyY(0.55), target.getZ());

        HitResult hitResult = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        return hitResult.getType() == HitResult.Type.MISS;
    }

    private static void applyChannelingMovementControl(ServerPlayerEntity player) {
        double speedMultiplier = ArtifactsConfigManager.get().tridentOfPoseidon.beamMovementSpeedMultiplierWhileChanneling;
        if (speedMultiplier <= 0.0D) {
            removeChannelingMovementModifier(player);
            lockPlayerMovement(player);
            return;
        }

        EntityAttributeInstance movementSpeed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (movementSpeed != null && !movementSpeed.hasModifier(CHANNELING_MOVEMENT_MODIFIER_ID)) {
            movementSpeed.addTemporaryModifier(channelingMovementModifier);
        }
        player.setVelocity(player.getVelocity().x, 0.0D, player.getVelocity().z);
    }

    private static void lockPlayerMovement(ServerPlayerEntity player) {
        Vec3d lockPos = CHANNELING_LOCK_POSITIONS.get(player.getUuid());
        if (lockPos == null) {
            return;
        }

        player.setVelocity(0.0, 0.0, 0.0);
        if (player.squaredDistanceTo(lockPos.x, lockPos.y, lockPos.z) > 0.0001D) {
            player.refreshPositionAfterTeleport(lockPos.x, lockPos.y, lockPos.z);
        }
    }

    private static void removeChannelingMovementModifier(ServerPlayerEntity player) {
        EntityAttributeInstance movementSpeed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        movementSpeed.removeModifier(CHANNELING_MOVEMENT_MODIFIER_ID);
    }

    private static boolean isHoldingPoseidonTrident(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.TRIDENT_OF_POSEIDON) || player.getOffHandStack().isOf(ModItems.TRIDENT_OF_POSEIDON);
    }

    private static boolean canUsePoseidonSpecial(ServerPlayerEntity player) {
        return isPoseidonStanceConditionsMet(player) && !isHoldingShield(player) && !isOnCooldown(player);
    }

    private static boolean isPoseidonStanceConditionsMet(ServerPlayerEntity player) {
        boolean wetCondition = ArtifactsConfigManager.get().tridentOfPoseidon.allowBeamInRain
                ? player.isTouchingWaterOrRain()
                : player.isTouchingWater();
        return isHoldingPoseidonTrident(player) && player.isSneaking() && wetCondition;
    }

    private static boolean isHoldingShield(PlayerEntity player) {
        return player.getMainHandStack().isOf(Items.SHIELD) || player.getOffHandStack().isOf(Items.SHIELD);
    }

    private static boolean isOnCooldown(PlayerEntity player) {
        return SPECIAL_COOLDOWNS.getOrDefault(player.getUuid(), 0) > 0;
    }

    private static void tickCooldowns() {
        for (UUID uuid : List.copyOf(SPECIAL_COOLDOWNS.keySet())) {
            int next = SPECIAL_COOLDOWNS.get(uuid) - 1;
            if (next <= 0) {
                SPECIAL_COOLDOWNS.remove(uuid);
            } else {
                SPECIAL_COOLDOWNS.put(uuid, next);
            }
        }
    }

    private static EntityAttributeModifier createChannelingMovementModifier() {
        double amount = ArtifactsConfigManager.get().tridentOfPoseidon.beamMovementSpeedMultiplierWhileChanneling - 1.0D;
        return new EntityAttributeModifier(CHANNELING_MOVEMENT_MODIFIER_ID, amount, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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
