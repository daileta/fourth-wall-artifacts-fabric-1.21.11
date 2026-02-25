package net.fourthwall.artifacts.blood;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BloodSacrificeManager {
    private static final double GUARDIAN_RADIUS = 15.0D;
    private static final double GUARDIAN_RADIUS_SQUARED = GUARDIAN_RADIUS * GUARDIAN_RADIUS;
    private static final double FOLLOW_DISTANCE_SQUARED = 16.0D;
    private static final int BURST_DURATION_TICKS = 20;
    private static final int LARGE_BLOOD_COLOR = 0x8C0012;
    private static final int SMALL_BLOOD_COLOR = 0xA60F1F;
    private static final Text GUARDIAN_NAME = Text.literal("Blood Guardian");
    private static final RegistryKey<EquipmentAsset> GRIM_EQUIPMENT_ASSET =
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, FourthWallArtifacts.id("grim"));

    private static final Map<UUID, UUID> GUARDIANS_BY_OWNER = new HashMap<>();
    private static final Map<UUID, UUID> OWNERS_BY_GUARDIAN = new HashMap<>();
    private static final Map<UUID, BloodBurst> ACTIVE_BURSTS = new HashMap<>();

    private BloodSacrificeManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BloodSacrificeManager::onEndServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            removeGuardianForOwner(server, handler.player.getUuid());
            ACTIVE_BURSTS.remove(handler.player.getUuid());
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity playerTarget)) {
                return true;
            }

            Entity attacker = source.getAttacker();
            if (attacker == null) {
                return true;
            }

            UUID ownerId = OWNERS_BY_GUARDIAN.get(attacker.getUuid());
            return ownerId == null || !ownerId.equals(playerTarget.getUuid());
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(BloodSacrificeManager::onAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            onAfterDamage(entity, source, 0.0F, 0.0F, false);
            onEntityDeath(entity.getUuid(), entity.getEntityWorld().getServer());
        });
    }

    public static void activate(ServerPlayerEntity summoner) {
        ServerWorld world = (ServerWorld) summoner.getEntityWorld();
        MinecraftServer server = world.getServer();

        removeGuardianForOwner(server, summoner.getUuid());

        summoner.setHealth(Math.min(1.0F, summoner.getMaxHealth()));
        startBloodBurst(summoner, false);

        WitherSkeletonEntity guardian = new WitherSkeletonEntity(net.minecraft.entity.EntityType.WITHER_SKELETON, world);
        Vec3d spawnPos = getSpawnPosition(summoner);
        guardian.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, summoner.getYaw(), 0.0F);
        configureGuardian(guardian, summoner);

        if (!world.spawnEntity(guardian)) {
            FourthWallArtifacts.LOGGER.warn("Failed to spawn Blood Guardian for {}", summoner.getName().getString());
            return;
        }

        GUARDIANS_BY_OWNER.put(summoner.getUuid(), guardian.getUuid());
        OWNERS_BY_GUARDIAN.put(guardian.getUuid(), summoner.getUuid());
        maintainGuardian(summoner, guardian);
    }

    private static void onEndServerTick(MinecraftServer server) {
        for (Map.Entry<UUID, UUID> entry : List.copyOf(GUARDIANS_BY_OWNER.entrySet())) {
            UUID ownerId = entry.getKey();
            UUID guardianId = entry.getValue();

            ServerPlayerEntity summoner = server.getPlayerManager().getPlayer(ownerId);
            if (summoner == null || !summoner.isAlive()) {
                discardGuardianById(server, guardianId);
                unlinkGuardian(ownerId, guardianId);
                continue;
            }

            Entity entity = findEntity(server, guardianId);
            if (!(entity instanceof WitherSkeletonEntity guardian) || !guardian.isAlive()) {
                unlinkGuardian(ownerId, guardianId);
                continue;
            }

            if (guardian.getEntityWorld() != summoner.getEntityWorld()) {
                guardian.discard();
                unlinkGuardian(ownerId, guardianId);
                continue;
            }

            maintainGuardian(summoner, guardian);
        }

        tickBursts(server);
    }

    private static void onAfterDamage(LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(target instanceof ServerPlayerEntity playerTarget)) {
            return;
        }
        if (blocked) {
            return;
        }

        Entity attacker = source.getAttacker();
        if (attacker == null || source.getSource() != attacker) {
            return;
        }
        if (!OWNERS_BY_GUARDIAN.containsKey(attacker.getUuid())) {
            return;
        }

        startBloodBurst(playerTarget, true);
    }

    private static void onEntityDeath(UUID entityId, MinecraftServer server) {
        ACTIVE_BURSTS.remove(entityId);

        UUID ownerId = OWNERS_BY_GUARDIAN.remove(entityId);
        if (ownerId != null) {
            GUARDIANS_BY_OWNER.remove(ownerId);
            return;
        }

        if (server == null) {
            return;
        }

        UUID guardianId = GUARDIANS_BY_OWNER.remove(entityId);
        if (guardianId != null) {
            OWNERS_BY_GUARDIAN.remove(guardianId);
            discardGuardianById(server, guardianId);
        }
    }

    private static void maintainGuardian(ServerPlayerEntity summoner, WitherSkeletonEntity guardian) {
        ensureGuardianInvisibility(guardian);
        guardian.setCustomName(GUARDIAN_NAME);
        guardian.setCustomNameVisible(true);

        if (guardian.squaredDistanceTo(summoner) > GUARDIAN_RADIUS_SQUARED) {
            teleportNearSummoner(guardian, summoner);
        }

        ServerPlayerEntity target = findClosestTarget(summoner);
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

    private static ServerPlayerEntity findClosestTarget(ServerPlayerEntity summoner) {
        MinecraftServer server = ((ServerWorld) summoner.getEntityWorld()).getServer();
        if (server == null) {
            return null;
        }

        return server.getPlayerManager().getPlayerList().stream()
                .filter(player ->
                        player.isAlive()
                                && player != summoner
                                && !player.isSpectator()
                                && player.getEntityWorld() == summoner.getEntityWorld()
                                && player.squaredDistanceTo(summoner.getX(), summoner.getY(), summoner.getZ()) <= GUARDIAN_RADIUS_SQUARED)
                .min(Comparator.comparingDouble(player -> player.squaredDistanceTo(summoner)))
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
        guardian.setCustomNameVisible(true);
        guardian.setInvisible(false);
        ensureGuardianInvisibility(guardian);

        equipGuardian(guardian, world);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            guardian.setEquipmentDropChance(slot, 0.0F);
        }

        setBaseAttribute(guardian, EntityAttributes.MAX_HEALTH, 50.0D);
        setBaseAttribute(guardian, EntityAttributes.MOVEMENT_SPEED, 0.50D);
        setBaseAttribute(guardian, EntityAttributes.ATTACK_DAMAGE, 12.0D);
        setBaseAttribute(guardian, EntityAttributes.KNOCKBACK_RESISTANCE, 1.0D);
        setBaseAttribute(guardian, EntityAttributes.FOLLOW_RANGE, 32.0D);
        // Netheritelike equipment contributes most of the requested armor/toughness. These bases close the gap.
        setBaseAttribute(guardian, EntityAttributes.ARMOR, 6.0D);
        setBaseAttribute(guardian, EntityAttributes.ARMOR_TOUGHNESS, 4.0D);
        guardian.setHealth(50.0F);
    }

    private static void equipGuardian(WitherSkeletonEntity guardian, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        helmet.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("template_helmet"));
        applyGrimEquipmentAsset(helmet);
        addProtectionEnchantments(helmet, enchantments);

        ItemStack chestplate = new ItemStack(Items.NETHERITE_CHESTPLATE);
        chestplate.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("template_chestplate"));
        applyGrimEquipmentAsset(chestplate);
        addProtectionEnchantments(chestplate, enchantments);

        ItemStack leggings = new ItemStack(Items.NETHERITE_LEGGINGS);
        leggings.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("template_leggings"));
        applyGrimEquipmentAsset(leggings);
        addProtectionEnchantments(leggings, enchantments);

        ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
        boots.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("template_boots"));
        applyGrimEquipmentAsset(boots);
        addProtectionEnchantments(boots, enchantments);
        boots.addEnchantment(enchantments.getOrThrow(Enchantments.FEATHER_FALLING), 4);
        boots.addEnchantment(enchantments.getOrThrow(Enchantments.FROST_WALKER), 2);
        boots.addEnchantment(enchantments.getOrThrow(Enchantments.SOUL_SPEED), 3);

        ItemStack soulScythe = new ItemStack(Items.NETHERITE_AXE);
        soulScythe.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("soul_scythe"));
        soulScythe.addEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS), 5);

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

    private static void addProtectionEnchantments(ItemStack stack, net.minecraft.registry.RegistryWrapper.Impl<net.minecraft.enchantment.Enchantment> enchantments) {
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), 4);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), 4);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), 4);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), 4);
    }

    private static void setBaseAttribute(LivingEntity entity, RegistryEntry<EntityAttribute> attribute, double value) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
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

    private static void removeGuardianForOwner(MinecraftServer server, UUID ownerId) {
        UUID guardianId = GUARDIANS_BY_OWNER.get(ownerId);
        if (guardianId == null) {
            return;
        }

        discardGuardianById(server, guardianId);
        unlinkGuardian(ownerId, guardianId);
    }

    private static void discardGuardianById(MinecraftServer server, UUID guardianId) {
        Entity entity = findEntity(server, guardianId);
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

    private static final class BloodBurst {
        private final boolean mini;
        private int ticksRemaining;

        private BloodBurst(boolean mini, int ticksRemaining) {
            this.mini = mini;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
