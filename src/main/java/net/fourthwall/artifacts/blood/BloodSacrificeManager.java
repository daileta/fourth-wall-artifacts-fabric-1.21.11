package net.fourthwall.artifacts.blood;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.config.ArtifactsConfig;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SculkChargeParticleEffect;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BloodSacrificeManager {
    private static final double FOLLOW_DISTANCE_SQUARED = 16.0D;
    private static final int BURST_DURATION_TICKS = 20;
    private static final int WITHER_ON_HIT_DURATION_TICKS = 60;
    private static final int WITHER_ON_HIT_AMPLIFIER = 0;
    private static final int SUMMON_RISE_TICKS = 88;
    private static final int SUMMON_FINAL_PAUSE_TICKS = 16;
    private static final int SUMMON_ANIMATION_TICKS = SUMMON_RISE_TICKS + SUMMON_FINAL_PAUSE_TICKS;
    private static final double SUMMON_EMERGE_DEPTH = 1.8D;
    private static final int LARGE_BLOOD_COLOR = 0x8C0012;
    private static final int SMALL_BLOOD_COLOR = 0xA60F1F;
    private static final Text GUARDIAN_NAME = Text.literal("Blood Guardian")
            .styled(style -> style.withColor(Formatting.RED).withBold(true));
    private static final RegistryKey<EquipmentAsset> GRIM_EQUIPMENT_ASSET =
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, FourthWallArtifacts.id("grim"));

    private static final Map<UUID, UUID> GUARDIANS_BY_OWNER = new HashMap<>();
    private static final Map<UUID, UUID> OWNERS_BY_GUARDIAN = new HashMap<>();
    private static final Map<UUID, Long> GUARDIAN_SPAWN_TICKS = new HashMap<>();
    private static final Map<UUID, BloodBurst> ACTIVE_BURSTS = new HashMap<>();
    private static final Map<UUID, SummonAnimationState> SUMMON_ANIMATIONS = new HashMap<>();
    private static final Map<UUID, SummonerLockState> SUMMONER_LOCKS = new HashMap<>();
    private static final Set<UUID> UNTETHERED_GUARDIANS = new HashSet<>();
    private static final Map<UUID, UUID> NAMEPLATES_BY_GUARDIAN = new HashMap<>();
    private static final Map<UUID, UUID> GUARDIANS_BY_NAMEPLATE = new HashMap<>();
    private BloodSacrificeManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BloodSacrificeManager::onEndServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            removeGuardianForOwner(server, handler.player.getUuid());
            ACTIVE_BURSTS.remove(handler.player.getUuid());
            SUMMONER_LOCKS.remove(handler.player.getUuid());
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity playerTarget)) {
                return true;
            }
            if (SUMMONER_LOCKS.containsKey(playerTarget.getUuid())) {
                return false;
            }

            Entity attacker = source.getAttacker();
            if (attacker == null) {
                return true;
            }

            UUID ownerId = OWNERS_BY_GUARDIAN.get(attacker.getUuid());
            if (ownerId == null || !ownerId.equals(playerTarget.getUuid())) {
                return true;
            }

            // Guardian can hurt the original summoner once they no longer possess the artifact.
            return !playerHasBloodSacrifice(playerTarget);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(BloodSacrificeManager::onAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            onAfterDamage(entity, source, 0.0F, 0.0F, false);
            onEntityDeath(entity.getUuid(), entity.getEntityWorld().getServer());
        });
    }

    public static void reloadConfig(MinecraftServer server) {
        if (server == null) {
            return;
        }

        for (UUID guardianId : List.copyOf(OWNERS_BY_GUARDIAN.keySet())) {
            Entity entity = findEntity(server, guardianId);
            if (!(entity instanceof WitherSkeletonEntity guardian) || !(guardian.getEntityWorld() instanceof ServerWorld world)) {
                continue;
            }

            refreshGuardianConfiguredStats(guardian);
            equipGuardian(guardian, world);
            syncGuardianNameplate(guardian);
        }
    }

    public static void activate(ServerPlayerEntity summoner) {
        ServerWorld world = (ServerWorld) summoner.getEntityWorld();
        MinecraftServer server = world.getServer();

        removeGuardianForOwner(server, summoner.getUuid());

        summoner.setHealth(Math.min(bloodCfg().healthAfterUse, summoner.getMaxHealth()));
        startBloodBurst(summoner, false);

        WitherSkeletonEntity guardian = new WitherSkeletonEntity(net.minecraft.entity.EntityType.WITHER_SKELETON, world);
        Vec3d spawnPos = getSpawnPosition(summoner);
        Vec3d buriedPos = spawnPos.add(0.0D, -SUMMON_EMERGE_DEPTH, 0.0D);
        guardian.refreshPositionAndAngles(buriedPos.x, buriedPos.y, buriedPos.z, summoner.getYaw(), 0.0F);
        configureGuardian(guardian, summoner);

        if (!world.spawnEntity(guardian)) {
            FourthWallArtifacts.LOGGER.warn("Failed to spawn Blood Guardian for {}", summoner.getName().getString());
            return;
        }

        GUARDIANS_BY_OWNER.put(summoner.getUuid(), guardian.getUuid());
        OWNERS_BY_GUARDIAN.put(guardian.getUuid(), summoner.getUuid());
        GUARDIAN_SPAWN_TICKS.put(guardian.getUuid(), (long) server.getTicks());
        UNTETHERED_GUARDIANS.remove(guardian.getUuid());
        ensureGuardianNameplate(guardian);
        lockSummonerForSummonSequence(summoner);
        startSummonAnimation(guardian, spawnPos);
    }

    private static void onEndServerTick(MinecraftServer server) {
        tickSummonerLocks(server);

        for (Map.Entry<UUID, UUID> entry : List.copyOf(GUARDIANS_BY_OWNER.entrySet())) {
            UUID ownerId = entry.getKey();
            UUID guardianId = entry.getValue();

            ServerPlayerEntity summoner = server.getPlayerManager().getPlayer(ownerId);
            Entity entity = findEntity(server, guardianId);
            if (!(entity instanceof WitherSkeletonEntity guardian) || !guardian.isAlive()) {
                SUMMON_ANIMATIONS.remove(guardianId);
                UNTETHERED_GUARDIANS.remove(guardianId);
                GUARDIAN_SPAWN_TICKS.remove(guardianId);
                discardNameplateForGuardian(server, guardianId);
                unlinkGuardian(ownerId, guardianId);
                continue;
            }

            if (expireGuardianIfNeeded(server, guardian, ownerId)) {
                continue;
            }

            if (summoner == null) {
                discardGuardianById(server, guardianId);
                unlinkGuardian(ownerId, guardianId);
                continue;
            }

            if (!summoner.isAlive()) {
                unlinkGuardian(ownerId, guardianId);
                UNTETHERED_GUARDIANS.add(guardianId);
                continue;
            }

            if (!playerHasBloodSacrifice(summoner)) {
                unlinkGuardian(ownerId, guardianId);
                UNTETHERED_GUARDIANS.add(guardianId);
                continue;
            }

            if (guardian.getEntityWorld() != summoner.getEntityWorld()) {
                guardian.discard();
                SUMMON_ANIMATIONS.remove(guardianId);
                UNTETHERED_GUARDIANS.remove(guardianId);
                discardNameplateForGuardian(server, guardianId);
                unlinkGuardian(ownerId, guardianId);
                continue;
            }

            if (tickSummonAnimationIfActive(guardian)) {
                continue;
            }

            maintainGuardian(summoner, guardian);
        }

        for (UUID guardianId : List.copyOf(UNTETHERED_GUARDIANS)) {
            Entity entity = findEntity(server, guardianId);
            if (!(entity instanceof WitherSkeletonEntity guardian) || !guardian.isAlive()) {
                UNTETHERED_GUARDIANS.remove(guardianId);
                SUMMON_ANIMATIONS.remove(guardianId);
                OWNERS_BY_GUARDIAN.remove(guardianId);
                GUARDIAN_SPAWN_TICKS.remove(guardianId);
                discardNameplateForGuardian(server, guardianId);
                continue;
            }

            if (expireGuardianIfNeeded(server, guardian, null)) {
                continue;
            }

            if (tickSummonAnimationIfActive(guardian)) {
                continue;
            }

            maintainUntetheredGuardian(guardian);
        }

        tickBursts(server);
    }

    private static void onAfterDamage(LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F) {
            return;
        }

        Entity attacker = source.getAttacker();
        if (attacker == null || source.getSource() != attacker) {
            return;
        }

        boolean guardianAttack = OWNERS_BY_GUARDIAN.containsKey(attacker.getUuid());
        if (!guardianAttack) {
            return;
        }

        if (guardianCfg().inflictsWither) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, WITHER_ON_HIT_DURATION_TICKS, WITHER_ON_HIT_AMPLIFIER));
        }

        if (!(target instanceof ServerPlayerEntity playerTarget)) {
            return;
        }

        startBloodBurst(playerTarget, true);
    }

    private static void onEntityDeath(UUID entityId, MinecraftServer server) {
        ACTIVE_BURSTS.remove(entityId);
        SUMMON_ANIMATIONS.remove(entityId);
        SUMMONER_LOCKS.remove(entityId);
        UNTETHERED_GUARDIANS.remove(entityId);
        GUARDIAN_SPAWN_TICKS.remove(entityId);

        UUID guardianFromNameplate = GUARDIANS_BY_NAMEPLATE.remove(entityId);
        if (guardianFromNameplate != null) {
            NAMEPLATES_BY_GUARDIAN.remove(guardianFromNameplate);
            return;
        }

        UUID ownerId = OWNERS_BY_GUARDIAN.remove(entityId);
        if (ownerId != null) {
            GUARDIANS_BY_OWNER.remove(ownerId);
            discardNameplateForGuardian(server, entityId);
            return;
        }

        if (server == null) {
            return;
        }

        UUID guardianId = GUARDIANS_BY_OWNER.remove(entityId);
        if (guardianId != null) {
            OWNERS_BY_GUARDIAN.remove(guardianId);
            Entity guardianEntity = findEntity(server, guardianId);
            if (guardianEntity instanceof WitherSkeletonEntity guardian && guardian.isAlive()) {
                UNTETHERED_GUARDIANS.add(guardianId);
            } else {
                discardNameplateForGuardian(server, guardianId);
                discardGuardianById(server, guardianId);
            }
        }
    }

    private static void maintainGuardian(ServerPlayerEntity summoner, WitherSkeletonEntity guardian) {
        ensureGuardianInvisibility(guardian);
        guardian.setCustomName(GUARDIAN_NAME);
        guardian.setCustomNameVisible(false);

        if (guardian.squaredDistanceTo(summoner) > guardianTetherRadiusSquared()) {
            teleportNearSummoner(guardian, summoner);
        }

        syncGuardianNameplate(guardian);

        ServerPlayerEntity target = findTargetForSummoner(summoner);
        if (target != null) {
            if (guardian.getTarget() != target) {
                guardian.setTarget(target);
            }
            guardian.getNavigation().startMovingTo(target, 1.5D);
            return;
        }

        if (guardian.getTarget() != null) {
            guardian.setTarget(null);
        }
        if (guardian.squaredDistanceTo(summoner) > FOLLOW_DISTANCE_SQUARED) {
            guardian.getNavigation().startMovingTo(summoner, 1.35D);
        } else {
            guardian.getNavigation().stop();
        }
    }

    private static void maintainUntetheredGuardian(WitherSkeletonEntity guardian) {
        ensureGuardianInvisibility(guardian);
        guardian.setCustomName(GUARDIAN_NAME);
        guardian.setCustomNameVisible(false);
        syncGuardianNameplate(guardian);

        ServerPlayerEntity target = findClosestTargetInWorld(guardian);
        if (target != null) {
            if (guardian.getTarget() != target) {
                guardian.setTarget(target);
            }
            guardian.getNavigation().startMovingTo(target, 1.5D);
            return;
        }

        if (guardian.getTarget() != null) {
            guardian.setTarget(null);
        }
        guardian.getNavigation().stop();
    }

    private static ServerPlayerEntity findTargetForSummoner(ServerPlayerEntity summoner) {
        MinecraftServer server = ((ServerWorld) summoner.getEntityWorld()).getServer();
        if (server == null) {
            return null;
        }

        if (guardianCfg().targetClosestPlayerOnly) {
            return server.getPlayerManager().getPlayerList().stream()
                    .filter(player ->
                            player.isAlive()
                                    && player != summoner
                                    && !player.isSpectator()
                                    && player.getEntityWorld() == summoner.getEntityWorld()
                                    && player.squaredDistanceTo(summoner.getX(), summoner.getY(), summoner.getZ()) <= guardianTetherRadiusSquared())
                    .min(Comparator.comparingDouble(player -> player.squaredDistanceTo(summoner)))
                    .orElse(null);
        }

        return server.getPlayerManager().getPlayerList().stream()
                .filter(player ->
                        player.isAlive()
                                && player != summoner
                                && !player.isSpectator()
                                && player.getEntityWorld() == summoner.getEntityWorld()
                                && player.squaredDistanceTo(summoner.getX(), summoner.getY(), summoner.getZ()) <= guardianTetherRadiusSquared())
                .findFirst()
                .orElse(null);
    }

    private static ServerPlayerEntity findClosestTargetInWorld(WitherSkeletonEntity guardian) {
        if (!(guardian.getEntityWorld() instanceof ServerWorld world)) {
            return null;
        }

        return world.getPlayers(player -> player.isAlive() && !player.isSpectator())
                .stream()
                .min(Comparator.comparingDouble(guardian::squaredDistanceTo))
                .orElse(null);
    }

    private static void teleportNearSummoner(WitherSkeletonEntity guardian, ServerPlayerEntity summoner) {
        Vec3d spawnPos = getSpawnPosition(summoner);
        guardian.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, guardian.getYaw(), guardian.getPitch());
        guardian.setVelocity(Vec3d.ZERO);
        guardian.getNavigation().stop();
    }

    private static Vec3d getSpawnPosition(ServerPlayerEntity summoner) {
        Vec3d look = summoner.getRotationVec(1.0F);
        Vec3d flatLook = new Vec3d(look.x, 0.0D, look.z);
        if (flatLook.lengthSquared() < 0.0001D) {
            flatLook = new Vec3d(0.0D, 0.0D, 1.0D);
        } else {
            flatLook = flatLook.normalize();
        }

        return new Vec3d(summoner.getX(), summoner.getY(), summoner.getZ())
                .add(flatLook.multiply(2.0D))
                .add(0.0D, 0.1D, 0.0D);
    }

    private static void configureGuardian(WitherSkeletonEntity guardian, ServerPlayerEntity summoner) {
        ServerWorld world = (ServerWorld) summoner.getEntityWorld();

        guardian.setPersistent();
        guardian.setCanPickUpLoot(false);
        guardian.setCustomName(GUARDIAN_NAME);
        guardian.setCustomNameVisible(false);
        guardian.setInvisible(false);
        ensureGuardianInvisibility(guardian);
        guardian.setAiDisabled(true);
        guardian.setNoGravity(true);
        guardian.setInvulnerable(true);

        equipGuardian(guardian, world);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            guardian.setEquipmentDropChance(slot, 0.0F);
        }

        refreshGuardianConfiguredStats(guardian);
        guardian.setHealth(guardianCfg().maxHealth);
    }

    private static void equipGuardian(WitherSkeletonEntity guardian, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        ArtifactsConfig.BloodGuardianEnchantLevels enchantLevels = guardianCfg().enchantLevels;

        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        helmet.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("template_helmet"));
        applyGrimEquipmentAsset(helmet);
        addProtectionEnchantments(helmet, enchantments, enchantLevels);

        ItemStack chestplate = new ItemStack(Items.NETHERITE_CHESTPLATE);
        chestplate.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("template_chestplate"));
        applyGrimEquipmentAsset(chestplate);
        addProtectionEnchantments(chestplate, enchantments, enchantLevels);

        ItemStack leggings = new ItemStack(Items.NETHERITE_LEGGINGS);
        leggings.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("template_leggings"));
        applyGrimEquipmentAsset(leggings);
        addProtectionEnchantments(leggings, enchantments, enchantLevels);

        ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
        boots.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("template_boots"));
        applyGrimEquipmentAsset(boots);
        addProtectionEnchantments(boots, enchantments, enchantLevels);
        boots.addEnchantment(enchantments.getOrThrow(Enchantments.FEATHER_FALLING), enchantLevels.boots.featherFalling);
        boots.addEnchantment(enchantments.getOrThrow(Enchantments.FROST_WALKER), enchantLevels.boots.frostWalker);
        boots.addEnchantment(enchantments.getOrThrow(Enchantments.SOUL_SPEED), enchantLevels.boots.soulSpeed);

        ItemStack soulScythe = new ItemStack(Items.NETHERITE_AXE);
        soulScythe.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("soul_scythe"));
        soulScythe.addEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS), enchantLevels.weapon.sharpness);

        guardian.equipStack(EquipmentSlot.HEAD, helmet);
        guardian.equipStack(EquipmentSlot.CHEST, chestplate);
        guardian.equipStack(EquipmentSlot.LEGS, leggings);
        guardian.equipStack(EquipmentSlot.FEET, boots);
        guardian.equipStack(EquipmentSlot.MAINHAND, soulScythe);
    }

    private static void applyGrimEquipmentAsset(ItemStack stack) {
        EquippableComponent existing = stack.get(DataComponentTypes.EQUIPPABLE);
        if (existing == null) {
            return;
        }

        stack.set(DataComponentTypes.EQUIPPABLE, new EquippableComponent(
                existing.slot(),
                existing.equipSound(),
                Optional.of(GRIM_EQUIPMENT_ASSET),
                existing.cameraOverlay(),
                existing.allowedEntities(),
                existing.dispensable(),
                existing.swappable(),
                existing.damageOnHurt(),
                existing.equipOnInteract(),
                existing.canBeSheared(),
                existing.shearingSound()
        ));
    }

    private static void addProtectionEnchantments(ItemStack stack, net.minecraft.registry.RegistryWrapper.Impl<net.minecraft.enchantment.Enchantment> enchantments, ArtifactsConfig.BloodGuardianEnchantLevels enchantLevels) {
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), enchantLevels.armor.protection);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), enchantLevels.armor.projectileProtection);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), enchantLevels.armor.fireProtection);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), enchantLevels.armor.blastProtection);
    }

    private static void setBaseAttribute(LivingEntity entity, RegistryEntry<EntityAttribute> attribute, double value) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static void refreshGuardianConfiguredStats(WitherSkeletonEntity guardian) {
        ArtifactsConfig.BloodGuardianSection config = guardianCfg();
        setBaseAttribute(guardian, EntityAttributes.MAX_HEALTH, config.maxHealth);
        setBaseAttribute(guardian, EntityAttributes.MOVEMENT_SPEED, config.movementSpeed);
        setBaseAttribute(guardian, EntityAttributes.ATTACK_DAMAGE, config.baseAttackDamage);
        setBaseAttribute(guardian, EntityAttributes.KNOCKBACK_RESISTANCE, config.knockbackResistance);
        setBaseAttribute(guardian, EntityAttributes.FOLLOW_RANGE, 32.0D);
        // Netheritelike equipment contributes most of the requested armor/toughness. These bases close the gap.
        setBaseAttribute(guardian, EntityAttributes.ARMOR, 6.0D);
        setBaseAttribute(guardian, EntityAttributes.ARMOR_TOUGHNESS, 4.0D);
        if (guardian.getHealth() > guardian.getMaxHealth()) {
            guardian.setHealth(guardian.getMaxHealth());
        }
    }

    private static void startBloodBurst(LivingEntity entity, boolean mini) {
        ACTIVE_BURSTS.put(entity.getUuid(), new BloodBurst(mini, BURST_DURATION_TICKS));
    }

    private static void tickBursts(MinecraftServer server) {
        for (Map.Entry<UUID, BloodBurst> entry : List.copyOf(ACTIVE_BURSTS.entrySet())) {
            UUID entityId = entry.getKey();
            BloodBurst burst = entry.getValue();
            Entity entity = findEntity(server, entityId);

            if (!(entity instanceof LivingEntity living) || !living.isAlive() || !(living.getEntityWorld() instanceof ServerWorld world)) {
                ACTIVE_BURSTS.remove(entityId);
                continue;
            }

            if (burst.ticksRemaining <= 0) {
                ACTIVE_BURSTS.remove(entityId);
                continue;
            }

            emitBloodParticles(world, living, burst.mini);
            burst.ticksRemaining--;
            if (burst.ticksRemaining <= 0) {
                ACTIVE_BURSTS.remove(entityId);
            }
        }
    }

    private static void emitBloodParticles(ServerWorld world, LivingEntity target, boolean mini) {
        if (mini && world.getTime() % 2L != 0L) {
            return;
        }

        int dustCount = mini ? 6 : 18;
        int critCount = mini ? 2 : 8;
        int damageCount = mini ? 2 : 10;
        double spread = mini ? 0.20D : 0.45D;
        double ySpread = mini ? 0.15D : 0.55D;
        int color = mini ? SMALL_BLOOD_COLOR : LARGE_BLOOD_COLOR;

        world.spawnParticles(new DustParticleEffect(color, mini ? 0.9F : 1.25F),
                target.getX(),
                target.getBodyY(0.5D),
                target.getZ(),
                dustCount,
                spread,
                ySpread,
                spread,
                0.01D);
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                target.getX(),
                target.getBodyY(0.55D),
                target.getZ(),
                damageCount,
                spread,
                ySpread,
                spread,
                0.05D);
        world.spawnParticles(ParticleTypes.CRIT,
                target.getX(),
                target.getBodyY(0.45D),
                target.getZ(),
                critCount,
                spread,
                ySpread,
                spread,
                0.03D);
    }

    private static void ensureGuardianInvisibility(WitherSkeletonEntity guardian) {
        guardian.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, true, false, false));
    }

    private static void startSummonAnimation(WitherSkeletonEntity guardian, Vec3d finalPos) {
        SUMMON_ANIMATIONS.put(guardian.getUuid(), new SummonAnimationState(
                new Vec3d(guardian.getX(), guardian.getY(), guardian.getZ()),
                finalPos,
                0,
                SUMMON_ANIMATION_TICKS,
                false
        ));

        if (guardian.getEntityWorld() instanceof ServerWorld world) {
            world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                    SoundEvents.ENTITY_WARDEN_NEARBY_CLOSER, SoundCategory.HOSTILE, 1.15F, 1.0F, world.getRandom().nextLong());
        }
    }

    private static boolean tickSummonAnimationIfActive(WitherSkeletonEntity guardian) {
        SummonAnimationState state = SUMMON_ANIMATIONS.get(guardian.getUuid());
        if (state == null) {
            return false;
        }

        if (!(guardian.getEntityWorld() instanceof ServerWorld world)) {
            SUMMON_ANIMATIONS.remove(guardian.getUuid());
            return false;
        }

        state.elapsedTicks++;
        boolean inPause = state.elapsedTicks > SUMMON_RISE_TICKS;
        int riseElapsed = Math.min(state.elapsedTicks, SUMMON_RISE_TICKS);
        double progress = Math.clamp((double) riseElapsed / (double) SUMMON_RISE_TICKS, 0.0D, 1.0D);
        // Ease-out rise to feel more like the Warden emerging.
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        Vec3d current = inPause ? state.endPos : state.startPos.lerp(state.endPos, eased);
        guardian.refreshPositionAndAngles(current.x, current.y, current.z, guardian.getYaw(), guardian.getPitch());
        guardian.setVelocity(Vec3d.ZERO);
        guardian.getNavigation().stop();
        ensureGuardianInvisibility(guardian);
        guardian.setCustomName(GUARDIAN_NAME);
        guardian.setCustomNameVisible(false);
        syncGuardianNameplate(guardian);

        emitSummonAnimationParticles(world, guardian, state.elapsedTicks, state.totalTicks);
        emitSummonAnimationSounds(world, guardian, state.elapsedTicks, state.totalTicks);
        if (inPause && !state.pauseShriekPlayed) {
            state.pauseShriekPlayed = true;
            world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                    SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 2.25F, 0.85F, world.getRandom().nextLong());
        }

        if (state.elapsedTicks >= state.totalTicks) {
            SUMMON_ANIMATIONS.remove(guardian.getUuid());
            guardian.setAiDisabled(false);
            guardian.setNoGravity(false);
            guardian.setInvulnerable(false);
            emitSummonFinalBurst(world, guardian);
            world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                    SoundEvents.ENTITY_WARDEN_NEARBY_CLOSEST, SoundCategory.HOSTILE, 1.05F, 0.95F, world.getRandom().nextLong());
            world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                    SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 0.75F, 1.35F, world.getRandom().nextLong());
            return false;
        }

        return true;
    }

    private static void emitSummonAnimationParticles(ServerWorld world, WitherSkeletonEntity guardian, int elapsedTicks, int totalTicks) {
        double x = guardian.getX();
        double y = guardian.getY() + 0.2D;
        double z = guardian.getZ();
        BlockState sculk = Blocks.SCULK.getDefaultState();
        boolean inPause = elapsedTicks > SUMMON_RISE_TICKS;
        int riseElapsed = Math.min(elapsedTicks, SUMMON_RISE_TICKS);
        double progress = (double) riseElapsed / (double) SUMMON_RISE_TICKS;
        double intensity = 0.35D + (0.65D * progress);

        if (elapsedTicks == 1) {
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, sculk), x, y, z, 26, 0.6D, 0.12D, 0.6D, 0.035D);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, x, y + 0.2D, z, 18, 0.45D, 0.16D, 0.45D, 0.02D);
        }

        if (elapsedTicks % 2 == 0) {
            int dustCount = 6 + (int) Math.round(8.0D * progress);
            int soulCount = 4 + (int) Math.round(6.0D * progress);
            int chargeCount = elapsedTicks % 4 == 0 ? 3 : 2;
            if (inPause) {
                dustCount += 4;
                soulCount += 8;
                chargeCount += 2;
            }
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, sculk), x, y, z, dustCount, 0.22D + (0.18D * intensity), 0.05D, 0.22D + (0.18D * intensity), 0.012D);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, x, y + 0.45D, z, soulCount, 0.16D + (0.14D * intensity), 0.18D, 0.16D + (0.14D * intensity), 0.012D);
            world.spawnParticles(new SculkChargeParticleEffect(world.getRandom().nextFloat() * 6.2831855F), x, y + 0.35D, z, chargeCount, 0.2D, 0.14D, 0.2D, 0.01D);
        }

        if (elapsedTicks % 4 == 0) {
            world.spawnParticles(ParticleTypes.POOF, x, y + 0.15D, z, 4 + (int) Math.round(4.0D * progress), 0.22D + (0.12D * intensity), 0.1D, 0.22D + (0.12D * intensity), 0.012D);
        }

        if (elapsedTicks % 6 == 0) {
            world.spawnParticles(new DustParticleEffect(0x0A2A24, 1.15F), x, y + 0.05D, z, 8 + (int) Math.round(10.0D * progress), 0.35D + (0.25D * intensity), 0.06D, 0.35D + (0.25D * intensity), 0.01D);
        }

        if (inPause) {
            world.spawnParticles(ParticleTypes.SCULK_SOUL, x, guardian.getBodyY(0.55D), z, 10, 0.28D, 0.28D, 0.28D, 0.02D);
            world.spawnParticles(ParticleTypes.SOUL, x, guardian.getBodyY(0.7D), z, 6, 0.22D, 0.25D, 0.22D, 0.015D);
        }

        if (elapsedTicks > totalTicks - 16) {
            int endPhaseTicks = elapsedTicks - (totalTicks - 16);
            double endIntensity = endPhaseTicks / 16.0D;
            world.spawnParticles(ParticleTypes.SOUL, x, guardian.getBodyY(0.65D), z, 5 + (int) Math.round(10.0D * endIntensity), 0.2D + (0.25D * endIntensity), 0.25D, 0.2D + (0.25D * endIntensity), 0.015D);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, x, guardian.getBodyY(0.55D), z, 8 + (int) Math.round(14.0D * endIntensity), 0.22D + (0.28D * endIntensity), 0.22D, 0.22D + (0.28D * endIntensity), 0.015D);
        }
    }

    private static void emitSummonAnimationSounds(ServerWorld world, WitherSkeletonEntity guardian, int elapsedTicks, int totalTicks) {
        if (elapsedTicks == totalTicks / 3) {
            world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                    SoundEvents.ENTITY_WARDEN_NEARBY_CLOSER, SoundCategory.HOSTILE, 0.7F, 1.15F, world.getRandom().nextLong());
        }

        if (elapsedTicks == (totalTicks * 2) / 3) {
            world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                    SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE, SoundCategory.HOSTILE, 0.9F, 1.05F, world.getRandom().nextLong());
        }

        if (elapsedTicks > totalTicks - 14 && elapsedTicks % 4 == 0) {
            world.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                    SoundEvents.ENTITY_WARDEN_NEARBY_CLOSEST, SoundCategory.HOSTILE, 0.38F, 1.45F, world.getRandom().nextLong());
        }
    }

    private static void emitSummonFinalBurst(ServerWorld world, WitherSkeletonEntity guardian) {
        double x = guardian.getX();
        double y = guardian.getBodyY(0.45D);
        double z = guardian.getZ();
        BlockState sculk = Blocks.SCULK.getDefaultState();

        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, sculk), x, y - 0.2D, z, 36, 0.9D, 0.2D, 0.9D, 0.05D);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, sculk), x, y - 0.1D, z, 30, 0.8D, 0.18D, 0.8D, 0.02D);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, x, y + 0.2D, z, 40, 0.75D, 0.45D, 0.75D, 0.03D);
        world.spawnParticles(ParticleTypes.SOUL, x, y + 0.35D, z, 20, 0.6D, 0.35D, 0.6D, 0.02D);
        world.spawnParticles(ParticleTypes.POOF, x, y + 0.1D, z, 20, 0.75D, 0.25D, 0.75D, 0.025D);
        world.spawnParticles(new DustParticleEffect(0x0A2A24, 1.6F), x, y, z, 28, 0.8D, 0.25D, 0.8D, 0.02D);
    }

    private static void lockSummonerForSummonSequence(ServerPlayerEntity summoner) {
        float healthAfterUse = Math.min(bloodCfg().healthAfterUse, summoner.getMaxHealth());
        SUMMONER_LOCKS.put(summoner.getUuid(), new SummonerLockState(
                summoner.getX(),
                summoner.getY(),
                summoner.getZ(),
                Math.min(healthAfterUse, summoner.getHealth()),
                SUMMON_ANIMATION_TICKS
        ));
    }

    private static boolean playerHasBloodSacrifice(ServerPlayerEntity player) {
        if (player.getMainHandStack().isOf(ModItems.BLOOD_SACRIFICE) || player.getOffHandStack().isOf(ModItems.BLOOD_SACRIFICE)) {
            return true;
        }

        return player.getInventory().contains(stack -> stack.isOf(ModItems.BLOOD_SACRIFICE));
    }

    private static void ensureGuardianNameplate(WitherSkeletonEntity guardian) {
        if (!(guardian.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        UUID guardianId = guardian.getUuid();
        UUID nameplateId = NAMEPLATES_BY_GUARDIAN.get(guardianId);
        Entity existing = nameplateId == null ? null : world.getEntity(nameplateId);
        if (existing instanceof ArmorStandEntity armorStand && armorStand.isAlive()) {
            syncGuardianNameplate(guardian);
            return;
        }

        if (nameplateId != null) {
            GUARDIANS_BY_NAMEPLATE.remove(nameplateId);
        }

        ArmorStandEntity nameplate = new ArmorStandEntity(world, guardian.getX(), guardian.getY(), guardian.getZ());
        configureNameplate(nameplate, GUARDIAN_NAME);
        positionGuardianNameplate(nameplate, guardian);

        if (world.spawnEntity(nameplate)) {
            NAMEPLATES_BY_GUARDIAN.put(guardianId, nameplate.getUuid());
            GUARDIANS_BY_NAMEPLATE.put(nameplate.getUuid(), guardianId);
        }
    }

    private static void syncGuardianNameplate(WitherSkeletonEntity guardian) {
        if (!(guardian.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        UUID nameplateId = NAMEPLATES_BY_GUARDIAN.get(guardian.getUuid());
        Entity entity = nameplateId == null ? null : world.getEntity(nameplateId);
        if (!(entity instanceof ArmorStandEntity armorStand) || !armorStand.isAlive()) {
            ensureGuardianNameplate(guardian);
            return;
        }

        configureNameplate(armorStand, GUARDIAN_NAME);
        positionGuardianNameplate(armorStand, guardian);
    }

    private static void configureNameplate(ArmorStandEntity nameplate, Text name) {
        nameplate.setCustomName(name);
        nameplate.setCustomNameVisible(true);
        nameplate.setInvisible(true);
        nameplate.setNoGravity(true);
        nameplate.setInvulnerable(true);
        nameplate.setSilent(true);
        nameplate.setSmall(true);
        nameplate.setMarker(true);
    }

    private static void positionGuardianNameplate(ArmorStandEntity nameplate, WitherSkeletonEntity guardian) {
        double y = guardian.getBoundingBox().maxY + 0.15D;
        nameplate.refreshPositionAndAngles(guardian.getX(), y, guardian.getZ(), guardian.getYaw(), 0.0F);
        nameplate.setVelocity(Vec3d.ZERO);
    }

    private static void tickSummonerLocks(MinecraftServer server) {
        for (Map.Entry<UUID, SummonerLockState> entry : List.copyOf(SUMMONER_LOCKS.entrySet())) {
            UUID playerId = entry.getKey();
            SummonerLockState lock = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);

            if (player == null || !player.isAlive()) {
                SUMMONER_LOCKS.remove(playerId);
                continue;
            }

            player.requestTeleport(lock.x, lock.y, lock.z);
            player.setVelocity(Vec3d.ZERO);
            if (player.getHealth() > lock.lockedHealth) {
                player.setHealth(lock.lockedHealth);
            }

            lock.remainingTicks--;
            if (lock.remainingTicks <= 0) {
                SUMMONER_LOCKS.remove(playerId);
            }
        }
    }

    private static void removeGuardianForOwner(MinecraftServer server, UUID ownerId) {
        UUID guardianId = GUARDIANS_BY_OWNER.get(ownerId);
        if (guardianId == null) {
            return;
        }

        discardGuardianById(server, guardianId);
        unlinkGuardian(ownerId, guardianId);
    }

    private static void discardGuardianById(MinecraftServer server, UUID guardianId) {
        discardNameplateForGuardian(server, guardianId);
        GUARDIAN_SPAWN_TICKS.remove(guardianId);
        Entity entity = findEntity(server, guardianId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void discardNameplateForGuardian(MinecraftServer server, UUID guardianId) {
        UUID nameplateId = NAMEPLATES_BY_GUARDIAN.remove(guardianId);
        if (nameplateId == null) {
            return;
        }

        GUARDIANS_BY_NAMEPLATE.remove(nameplateId);
        Entity entity = findEntity(server, nameplateId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void unlinkGuardian(UUID ownerId, UUID guardianId) {
        GUARDIANS_BY_OWNER.remove(ownerId);
        OWNERS_BY_GUARDIAN.remove(guardianId);
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerWorld world : server.getWorlds()) {
            Entity found = world.getEntity(entityId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean expireGuardianIfNeeded(MinecraftServer server, WitherSkeletonEntity guardian, UUID ownerId) {
        int lifespanTicks = guardianCfg().lifespanTicks;
        if (lifespanTicks <= 0) {
            return false;
        }

        long spawnTick = GUARDIAN_SPAWN_TICKS.computeIfAbsent(guardian.getUuid(), ignored -> (long) server.getTicks());
        if ((server.getTicks() - spawnTick) < lifespanTicks) {
            return false;
        }

        UUID guardianId = guardian.getUuid();
        discardGuardianById(server, guardianId);
        SUMMON_ANIMATIONS.remove(guardianId);
        UNTETHERED_GUARDIANS.remove(guardianId);
        if (ownerId != null) {
            unlinkGuardian(ownerId, guardianId);
        } else {
            OWNERS_BY_GUARDIAN.remove(guardianId);
        }
        return true;
    }

    private static ArtifactsConfig.BloodSacrificeSection bloodCfg() {
        return ArtifactsConfigManager.get().bloodSacrifice;
    }

    private static ArtifactsConfig.BloodGuardianSection guardianCfg() {
        return bloodCfg().summon;
    }

    private static double guardianTetherRadiusSquared() {
        double radius = guardianCfg().tetherRadius;
        return radius * radius;
    }

    private static final class BloodBurst {
        private final boolean mini;
        private int ticksRemaining;

        private BloodBurst(boolean mini, int ticksRemaining) {
            this.mini = mini;
            this.ticksRemaining = ticksRemaining;
        }
    }

    private static final class SummonAnimationState {
        private final Vec3d startPos;
        private final Vec3d endPos;
        private int elapsedTicks;
        private final int totalTicks;
        private boolean pauseShriekPlayed;

        private SummonAnimationState(Vec3d startPos, Vec3d endPos, int elapsedTicks, int totalTicks, boolean pauseShriekPlayed) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.elapsedTicks = elapsedTicks;
            this.totalTicks = totalTicks;
            this.pauseShriekPlayed = pauseShriekPlayed;
        }
    }

    private static final class SummonerLockState {
        private final double x;
        private final double y;
        private final double z;
        private final float lockedHealth;
        private int remainingTicks;

        private SummonerLockState(double x, double y, double z, float lockedHealth, int remainingTicks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lockedHealth = lockedHealth;
            this.remainingTicks = remainingTicks;
        }
    }
}
