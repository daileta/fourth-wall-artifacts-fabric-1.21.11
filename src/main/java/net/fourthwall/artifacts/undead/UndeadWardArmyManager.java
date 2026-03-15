package net.fourthwall.artifacts.undead;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.config.ArtifactsConfig;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.integration.EmptyEmbraceArtifactSuppression;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.Unit;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class UndeadWardArmyManager {
    private static final double SUMMON_EMERGE_DEPTH = 1.8D;
    private static final int DEPUTY_RISE_TICKS = 38;
    private static final int COMMANDER_RISE_TICKS = 56;
    private static final int WARDEN_RISE_TICKS = 88;
    private static final int DEPUTY_PAUSE_TICKS = 8;
    private static final int COMMANDER_PAUSE_TICKS = 10;
    private static final int WARDEN_PAUSE_TICKS = 16;

    private static final int COMMANDER_PROTECTION_LEVEL = 4;
    private static final int COMMANDER_PROJECTILE_PROTECTION_LEVEL = 4;
    private static final int COMMANDER_CROSSBOW_QUICK_CHARGE = 3;
    private static final int COMMANDER_CROSSBOW_PIERCING = 4;
    private static final int COMMANDER_SHOT_COOLDOWN_TICKS = 26;
    private static final double COMMANDER_STOP_RANGE_SQUARED = 12.0D * 12.0D;
    private static final double COMMANDER_ARROW_DAMAGE = 6.0D;

    private static final int WARDEN_PROTECTION_LEVEL = 4;
    private static final int WARDEN_PROJECTILE_PROTECTION_LEVEL = 4;
    private static final int WARDEN_FIRE_PROTECTION_LEVEL = 4;
    private static final int WARDEN_BLAST_PROTECTION_LEVEL = 4;
    private static final int WARDEN_AXE_SHARPNESS = 5;
    private static final int WARDEN_AXE_FIRE_ASPECT = 2;
    private static final int DEPUTY_SHIELD_BLOCK_TICKS = 3 * 20;
    private static final Text DEPUTY_NAME = Text.literal("Undead Deputy")
            .styled(style -> style.withColor(Formatting.BLUE).withBold(true));
    private static final Text COMMANDER_NAME = Text.literal("Undead Commander")
            .styled(style -> style.withColor(Formatting.AQUA).withBold(true));
    private static final Text WARDEN_NAME = Text.literal("Undead Warden")
            .styled(style -> style.withColor(Formatting.YELLOW).withBold(true));

    private static final RegistryKey<EquipmentAsset> NETHERWALKER_DEPUTY_EQUIPMENT_ASSET =
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, FourthWallArtifacts.id("netherwalker_deputy"));
    private static final RegistryKey<EquipmentAsset> NETHERWALKER_COMMANDER_EQUIPMENT_ASSET =
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, FourthWallArtifacts.id("netherwalker_commander"));
    private static final RegistryKey<EquipmentAsset> NETHERWALKER_WARDEN_EQUIPMENT_ASSET =
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, FourthWallArtifacts.id("netherwalker_warden"));
    private static final String SUMMON_ENTITY_TAG = "artifacts_undead_summon";
    private static final String SUMMON_OWNER_TAG_PREFIX = "artifacts_undead_owner:";
    private static final String SUMMON_TYPE_TAG_PREFIX = "artifacts_undead_type:";
    private static final String SUMMON_SPAWN_TICK_TAG_PREFIX = "artifacts_undead_spawn:";
    private static final String SUMMON_NAMEPLATE_TAG = "artifacts_undead_nameplate";
    private static final String SUMMON_NAMEPLATE_SUMMON_TAG_PREFIX = "artifacts_undead_nameplate_summon:";

    private static final Map<UUID, SummonData> SUMMONS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> SUMMONS_BY_OWNER = new HashMap<>();
    private static final Map<UUID, SummonAnimationState> SUMMON_ANIMATIONS = new HashMap<>();
    private static final Map<UUID, CooldownState> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, DeputyDefenseState> DEPUTY_DEFENSES = new HashMap<>();
    private static final Map<UUID, Long> COMMANDER_SHOT_READY_TICKS = new HashMap<>();
    private static final Map<UUID, UUID> NAMEPLATES_BY_SUMMON = new HashMap<>();
    private static final Map<UUID, UUID> SUMMONS_BY_NAMEPLATE = new HashMap<>();
    private static List<ResolvedStatusEffect> commanderArrowEffects = List.of();

    private UndeadWardArmyManager() {
    }

    public static void init() {
        reloadConfig(null);
        ServerTickEvents.END_SERVER_TICK.register(UndeadWardArmyManager::onEndServerTick);
        ServerEntityEvents.ENTITY_LOAD.register(UndeadWardArmyManager::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(UndeadWardArmyManager::onEntityUnload);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(UndeadWardArmyManager::onAllowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(UndeadWardArmyManager::onAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> onEntityDeath(entity.getUuid(), entity.getEntityWorld().getServer()));
    }

    public static void reloadConfig(MinecraftServer server) {
        commanderArrowEffects = resolveConfiguredEffects(
                cfg().commanders.arrowHitEffects,
                "undeadWardArmy.commanders.arrowHitEffects"
        );

        if (server == null) {
            return;
        }

        for (Map.Entry<UUID, SummonData> entry : List.copyOf(SUMMONS.entrySet())) {
            Entity entity = findEntity(server, entry.getKey());
            if (!(entity instanceof MobEntity summon) || !(entity.getEntityWorld() instanceof ServerWorld world)) {
                continue;
            }
            refreshSummonConfiguredStats(summon, entry.getValue().type());
            equipSummon(summon, world, entry.getValue().type());
            syncSummonNameplate(summon, entry.getValue().type());
        }
    }

    public static int summonDeputies(ServerPlayerEntity summoner) {
        return summon(summoner, SummonType.DEPUTY);
    }

    public static int dismissDeputies(ServerPlayerEntity summoner) {
        return dismiss(summoner, SummonType.DEPUTY);
    }

    public static int summonCommanders(ServerPlayerEntity summoner) {
        return summon(summoner, SummonType.COMMANDER);
    }

    public static int dismissCommanders(ServerPlayerEntity summoner) {
        return dismiss(summoner, SummonType.COMMANDER);
    }

    public static int summonWarden(ServerPlayerEntity summoner) {
        return summon(summoner, SummonType.WARDEN);
    }

    public static int dismissWarden(ServerPlayerEntity summoner) {
        return dismiss(summoner, SummonType.WARDEN);
    }

    private static int summon(ServerPlayerEntity summoner, SummonType type) {
        if (!playerHasUndeadWardArmy(summoner)) {
            sendActionBar(summoner, Text.literal("You must have The Undead Ward Army in your inventory to command summons."));
            return 0;
        }

        ServerWorld world = (ServerWorld) summoner.getEntityWorld();
        MinecraftServer server = world.getServer();
        long now = server.getTicks();
        long cooldownRemaining = getCooldownRemaining(summoner.getUuid(), type, now);
        if (cooldownRemaining > 0L) {
            sendCooldownMessage(summoner, type, cooldownRemaining);
            return 0;
        }

        int summonCount = summonCount(type);
        if (summonCount <= 0) {
            sendActionBar(summoner, Text.literal(type.displayNamePlural + " are disabled in config (summonCount = 0)."));
            return 0;
        }

        int spawned = 0;
        for (int index = 0; index < summonCount; index++) {
            MobEntity summon = createSummonEntity(type, world);
            if (summon == null) {
                continue;
            }

            Vec3d summonPos = calculateSummonPosition(type, summoner, index, summonCount);
            Vec3d buriedPos = summonPos.add(0.0D, -SUMMON_EMERGE_DEPTH, 0.0D);
            summon.refreshPositionAndAngles(buriedPos.x, buriedPos.y, buriedPos.z, summoner.getYaw(), 0.0F);
            configureSummon(summon, world, type);
            tagSummonEntity(summon, summoner.getUuid(), type, now);

            if (!world.spawnEntity(summon)) {
                FourthWallArtifacts.LOGGER.warn("Failed to spawn {} for {}", type.displayNameSingular, summoner.getName().getString());
                continue;
            }

            trackSummon(summoner.getUuid(), summon.getUuid(), type, now);
            ensureSummonNameplate(summon, type);
            startSummonAnimation(summon, summonPos, type);
            spawned++;
        }

        if (spawned > 0) {
            setCooldownReadyTick(summoner.getUuid(), type, now + summonCooldownTicks(type));
            sendActionBar(summoner, Text.literal("Summoned " + spawned + " " + type.displayNamePlural + "."));
        } else {
            sendActionBar(summoner, Text.literal("No " + type.displayNamePlural + " could be summoned."));
        }

        return spawned;
    }

    private static int dismiss(ServerPlayerEntity summoner, SummonType type) {
        if (!playerHasUndeadWardArmy(summoner)) {
            sendActionBar(summoner, Text.literal("You must have The Undead Ward Army in your inventory to command summons."));
            return 0;
        }

        int removed = dismissSummonsForOwner(((ServerWorld) summoner.getEntityWorld()).getServer(), summoner.getUuid(), type);
        if (removed > 0) {
            sendActionBar(summoner, Text.literal("Dismissed " + removed + " " + type.displayNamePlural + "."));
        } else {
            sendActionBar(summoner, Text.literal("No active " + type.displayNamePlural + " to dismiss."));
        }
        return removed;
    }

    private static void onEndServerTick(MinecraftServer server) {
        tickCooldowns(server);

        for (Map.Entry<UUID, SummonData> entry : List.copyOf(SUMMONS.entrySet())) {
            UUID summonId = entry.getKey();
            SummonData data = entry.getValue();
            Entity entity = findEntity(server, summonId);
            if (entity == null) {
                continue;
            }
            if (!(entity instanceof MobEntity summon) || !summon.isAlive()) {
                removeSummonTracking(server, summonId);
                continue;
            }

            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(data.ownerId());
            if (owner == null) {
                parkOfflineSummon(server, summon, data);
                continue;
            }
            if (owner != null && (EmptyEmbraceArtifactSuppression.areArtifactPowersSuppressed(owner)
                    || EmptyEmbraceArtifactSuppression.areArtifactSummonsSuppressed(owner))) {
                dismissSummonsForOwner(server, data.ownerId(), null);
                continue;
            }

            if (expireSummonIfNeeded(server, summon, data)) {
                continue;
            }

            if (tickSummonAnimationIfActive(summon)) {
                continue;
            }

            maintainSummon(server, summon, data);
        }
    }

    private static boolean onAllowDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target instanceof ServerPlayerEntity playerTarget)) {
            return true;
        }

        SummonData attackerData = resolveAttackingSummon(source);
        if (attackerData == null || !attackerData.ownerId().equals(playerTarget.getUuid())) {
            return true;
        }

        return !playerHasUndeadWardArmy(playerTarget);
    }

    private static void onAfterDamage(LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F) {
            return;
        }

        SummonData targetData = SUMMONS.get(target.getUuid());
        if (targetData != null && targetData.type() == SummonType.DEPUTY && target instanceof MobEntity deputy) {
            activateDeputyDefense(deputy, source);
        }

        SummonData attackerData = resolveAttackingSummon(source);
        if (attackerData == null) {
            return;
        }

        if (attackerData.type() == SummonType.COMMANDER && source.getSource() instanceof ProjectileEntity) {
            if (target instanceof ServerPlayerEntity playerTarget) {
                applyCommanderArrowEffects(playerTarget);
                emitHitParticles(playerTarget, SummonType.COMMANDER);
            }
            return;
        }

        if ((attackerData.type() == SummonType.DEPUTY || attackerData.type() == SummonType.WARDEN) && target instanceof ServerPlayerEntity playerTarget) {
            emitHitParticles(playerTarget, attackerData.type());
        }
    }

    private static void onEntityDeath(UUID entityId, MinecraftServer server) {
        removeSummonTracking(server, entityId);
    }

    private static void maintainSummon(MinecraftServer server, MobEntity summon, SummonData data) {
        refreshCoreSummonState(summon, data.type());
        refreshSummonConfiguredStats(summon, data.type());
        syncSummonNameplate(summon, data.type());

        if (data.type() == SummonType.DEPUTY && maintainDeputyDefense(server, summon)) {
            return;
        }

        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(data.ownerId());
        boolean ownerHasBook = owner != null && owner.isAlive() && playerHasUndeadWardArmy(owner);
        ServerPlayerEntity target = findNearestTarget(summon, data.ownerId(), ownerHasBook, aggroRadius(data.type()));
        if (target != null) {
            if (data.type() == SummonType.COMMANDER) {
                maintainCommanderSummon(server, summon, target);
                return;
            }
            if (summon.getTarget() != target) {
                summon.setTarget(target);
            }
            summon.getNavigation().startMovingTo(target, movementNavigationSpeed(data.type()));
            return;
        }

        if (summon.getTarget() != null) {
            summon.setTarget(null);
        }
        summon.getNavigation().stop();
    }

    private static void parkOfflineSummon(MinecraftServer server, MobEntity summon, SummonData data) {
        refreshCoreSummonState(summon, data.type());
        refreshSummonConfiguredStats(summon, data.type());
        summon.setTarget(null);
        summon.getNavigation().stop();
        if (summon.isUsingItem()) {
            summon.clearActiveItem();
        }
        discardNameplateForSummon(server, summon.getUuid());
        DEPUTY_DEFENSES.remove(summon.getUuid());
    }

    private static void configureSummon(MobEntity summon, ServerWorld world, SummonType type) {
        summon.setPersistent();
        summon.setCanPickUpLoot(false);
        summon.setSilent(true);
        summon.setInvisible(false);
        summon.setAiDisabled(true);
        summon.setNoGravity(true);
        summon.setInvulnerable(true);
        if (summon instanceof ZombieEntity zombie) {
            zombie.setBaby(false);
            zombie.setCanBreakDoors(false);
        }

        refreshCoreSummonState(summon, type);
        equipSummon(summon, world, type);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            summon.setEquipmentDropChance(slot, 0.0F);
        }

        refreshSummonConfiguredStats(summon, type);
        summon.setHealth(summon.getMaxHealth());
    }

    private static void refreshCoreSummonState(MobEntity summon, SummonType type) {
        summon.setSilent(true);
        summon.setFireTicks(0);
        summon.setCustomName(summonName(type));
        summon.setCustomNameVisible(false);
        summon.setInvisible(true);
        summon.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, true, false, false));
    }

    private static void ensureSummonNameplate(MobEntity summon, SummonType type) {
        if (!(summon.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        UUID summonId = summon.getUuid();
        UUID nameplateId = NAMEPLATES_BY_SUMMON.get(summonId);
        Entity existing = nameplateId == null ? null : world.getEntity(nameplateId);
        if (existing instanceof ArmorStandEntity armorStand && armorStand.isAlive()) {
            syncSummonNameplate(summon, type);
            return;
        }

        if (nameplateId != null) {
            SUMMONS_BY_NAMEPLATE.remove(nameplateId);
        }

        ArmorStandEntity nameplate = new ArmorStandEntity(world, summon.getX(), summon.getY(), summon.getZ());
        configureNameplate(nameplate, summonName(type));
        tagSummonNameplate(nameplate, summonId);
        positionSummonNameplate(nameplate, summon);

        if (world.spawnEntity(nameplate)) {
            NAMEPLATES_BY_SUMMON.put(summonId, nameplate.getUuid());
            SUMMONS_BY_NAMEPLATE.put(nameplate.getUuid(), summonId);
        }
    }

    private static void syncSummonNameplate(MobEntity summon, SummonType type) {
        if (!(summon.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        UUID nameplateId = NAMEPLATES_BY_SUMMON.get(summon.getUuid());
        Entity entity = nameplateId == null ? null : world.getEntity(nameplateId);
        if (!(entity instanceof ArmorStandEntity armorStand) || !armorStand.isAlive()) {
            ensureSummonNameplate(summon, type);
            return;
        }

        configureNameplate(armorStand, summonName(type));
        positionSummonNameplate(armorStand, summon);
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

    private static void positionSummonNameplate(ArmorStandEntity nameplate, MobEntity summon) {
        double y = summon.getBoundingBox().maxY + 0.15D;
        nameplate.refreshPositionAndAngles(summon.getX(), y, summon.getZ(), summon.getYaw(), 0.0F);
        nameplate.setVelocity(Vec3d.ZERO);
    }

    private static void refreshSummonConfiguredStats(MobEntity summon, SummonType type) {
        setBaseAttribute(summon, EntityAttributes.FOLLOW_RANGE, aggroRadius(type));

        if (type == SummonType.DEPUTY) {
            setBaseAttribute(summon, EntityAttributes.ATTACK_DAMAGE, cfg().deputies.swordAttackDamage);
        } else if (type == SummonType.WARDEN) {
            setBaseAttribute(summon, EntityAttributes.ATTACK_DAMAGE, cfg().warden.baseAttackDamage);
            setBaseAttribute(summon, EntityAttributes.MOVEMENT_SPEED, cfg().warden.movementSpeed);
        }
    }

    private static void equipSummon(MobEntity summon, ServerWorld world, SummonType type) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        ArtifactsConfig.UndeadArmorProfile armor = armorProfile(type);

        ItemStack helmet = createArmorPiece(type, "helmet", Items.NETHERITE_HELMET, AttributeModifierSlot.HEAD, armor.helmetArmorPoints, armor.armorToughnessPerPiece);
        ItemStack chestplate = createArmorPiece(type, "chestplate", Items.NETHERITE_CHESTPLATE, AttributeModifierSlot.CHEST, armor.chestplateArmorPoints, armor.armorToughnessPerPiece);
        ItemStack leggings = createArmorPiece(type, "leggings", Items.NETHERITE_LEGGINGS, AttributeModifierSlot.LEGS, armor.leggingsArmorPoints, armor.armorToughnessPerPiece);
        ItemStack boots = createArmorPiece(type, "boots", Items.NETHERITE_BOOTS, AttributeModifierSlot.FEET, armor.bootsArmorPoints, armor.armorToughnessPerPiece);

        if (type == SummonType.COMMANDER) {
            helmet.addEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), COMMANDER_PROTECTION_LEVEL);
            helmet.addEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), COMMANDER_PROJECTILE_PROTECTION_LEVEL);
            chestplate.addEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), COMMANDER_PROTECTION_LEVEL);
            chestplate.addEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), COMMANDER_PROJECTILE_PROTECTION_LEVEL);
            leggings.addEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), COMMANDER_PROTECTION_LEVEL);
            leggings.addEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), COMMANDER_PROJECTILE_PROTECTION_LEVEL);
            boots.addEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), COMMANDER_PROTECTION_LEVEL);
            boots.addEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), COMMANDER_PROJECTILE_PROTECTION_LEVEL);
        }

        if (type == SummonType.WARDEN) {
            addWardenArmorEnchants(helmet, enchantments);
            addWardenArmorEnchants(chestplate, enchantments);
            addWardenArmorEnchants(leggings, enchantments);
            addWardenArmorEnchants(boots, enchantments);
        }

        summon.equipStack(EquipmentSlot.HEAD, helmet);
        summon.equipStack(EquipmentSlot.CHEST, chestplate);
        summon.equipStack(EquipmentSlot.LEGS, leggings);
        summon.equipStack(EquipmentSlot.FEET, boots);

        switch (type) {
            case DEPUTY -> {
                ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
                sword.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("dark_master_sword_sharpness"));
                makeUnbreakable(sword);
                ItemStack shield = new ItemStack(Items.SHIELD);
                shield.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("shield"));
                makeUnbreakable(shield);
                summon.equipStack(EquipmentSlot.MAINHAND, sword);
                summon.equipStack(EquipmentSlot.OFFHAND, shield);
            }
            case COMMANDER -> {
                ItemStack crossbow = new ItemStack(Items.CROSSBOW);
                crossbow.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("soul_hunter_crossbow"));
                crossbow.addEnchantment(enchantments.getOrThrow(Enchantments.QUICK_CHARGE), COMMANDER_CROSSBOW_QUICK_CHARGE);
                crossbow.addEnchantment(enchantments.getOrThrow(Enchantments.PIERCING), COMMANDER_CROSSBOW_PIERCING);
                makeUnbreakable(crossbow);
                summon.equipStack(EquipmentSlot.MAINHAND, crossbow);
                summon.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            case WARDEN -> {
                ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
                axe.set(DataComponentTypes.ITEM_MODEL, FourthWallArtifacts.id("mechanical_breaker_sword"));
                axe.addEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS), WARDEN_AXE_SHARPNESS);
                axe.addEnchantment(enchantments.getOrThrow(Enchantments.FIRE_ASPECT), WARDEN_AXE_FIRE_ASPECT);
                makeUnbreakable(axe);
                summon.equipStack(EquipmentSlot.MAINHAND, axe);
                summon.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
        }
    }

    private static void addWardenArmorEnchants(ItemStack stack, net.minecraft.registry.RegistryWrapper.Impl<net.minecraft.enchantment.Enchantment> enchantments) {
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.PROTECTION), WARDEN_PROTECTION_LEVEL);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), WARDEN_PROJECTILE_PROTECTION_LEVEL);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), WARDEN_FIRE_PROTECTION_LEVEL);
        stack.addEnchantment(enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), WARDEN_BLAST_PROTECTION_LEVEL);
    }

    private static ItemStack createArmorPiece(
            SummonType type,
            String pieceName,
            Item baseItem,
            AttributeModifierSlot slot,
            double armor,
            double toughness
    ) {
        ItemStack stack = new ItemStack(baseItem);
        AttributeModifiersComponent attributes = AttributeModifiersComponent.builder()
                .add(
                        EntityAttributes.ARMOR,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("undead_ward_" + type.id + "_" + pieceName + "_armor"),
                                armor,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        slot
                )
                .add(
                        EntityAttributes.ARMOR_TOUGHNESS,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("undead_ward_" + type.id + "_" + pieceName + "_toughness"),
                                toughness,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        slot
                )
                .build();
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, attributes);
        applySummonEquipmentAsset(type, stack);
        makeUnbreakable(stack);
        return stack;
    }

    private static void makeUnbreakable(ItemStack stack) {
        if (!stack.contains(DataComponentTypes.UNBREAKABLE)) {
            stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        }
    }

    private static MobEntity createSummonEntity(SummonType type, ServerWorld world) {
        return switch (type) {
            case DEPUTY, WARDEN -> new ZombieEntity(EntityType.ZOMBIE, world);
            case COMMANDER -> new SkeletonEntity(EntityType.SKELETON, world);
        };
    }

    private static Vec3d calculateSummonPosition(SummonType type, ServerPlayerEntity summoner, int index, int total) {
        return switch (type) {
            case DEPUTY -> randomAroundSummoner(summoner, cfg().deputies.summonRadius);
            case COMMANDER -> randomAroundSummoner(summoner, cfg().commanders.summonRadius);
            case WARDEN -> inFrontOfSummoner(summoner, cfg().warden.summonForwardDistance, cfg().warden.summonSpreadRadius, index, total);
        };
    }

    private static Vec3d randomAroundSummoner(ServerPlayerEntity summoner, double radius) {
        double clampedRadius = Math.max(0.0D, radius);
        double angle = summoner.getRandom().nextDouble() * 6.283185307179586D;
        double distance = Math.sqrt(summoner.getRandom().nextDouble()) * clampedRadius;
        return new Vec3d(
                summoner.getX() + (Math.cos(angle) * distance),
                summoner.getY() + 0.1D,
                summoner.getZ() + (Math.sin(angle) * distance)
        );
    }

    private static Vec3d inFrontOfSummoner(ServerPlayerEntity summoner, double forwardDistance, double spreadRadius, int index, int total) {
        Vec3d look = summoner.getRotationVec(1.0F);
        Vec3d flatLook = new Vec3d(look.x, 0.0D, look.z);
        if (flatLook.lengthSquared() < 0.0001D) {
            flatLook = new Vec3d(0.0D, 0.0D, 1.0D);
        } else {
            flatLook = flatLook.normalize();
        }

        Vec3d base = new Vec3d(summoner.getX(), summoner.getY(), summoner.getZ())
                .add(flatLook.multiply(Math.max(0.0D, forwardDistance)))
                .add(0.0D, 0.1D, 0.0D);

        if (total <= 1 || spreadRadius <= 0.0D) {
            return base;
        }

        double angle = (6.283185307179586D * index) / (double) total;
        double offsetScale = Math.min(1.0D, spreadRadius);
        return base.add(Math.cos(angle) * offsetScale, 0.0D, Math.sin(angle) * offsetScale);
    }

    private static void startSummonAnimation(MobEntity summon, Vec3d finalPos, SummonType type) {
        int riseTicks = switch (type) {
            case DEPUTY -> DEPUTY_RISE_TICKS;
            case COMMANDER -> COMMANDER_RISE_TICKS;
            case WARDEN -> WARDEN_RISE_TICKS;
        };
        int pauseTicks = switch (type) {
            case DEPUTY -> DEPUTY_PAUSE_TICKS;
            case COMMANDER -> COMMANDER_PAUSE_TICKS;
            case WARDEN -> WARDEN_PAUSE_TICKS;
        };
        int totalTicks = riseTicks + pauseTicks;

        SUMMON_ANIMATIONS.put(summon.getUuid(), new SummonAnimationState(
                type,
                new Vec3d(summon.getX(), summon.getY(), summon.getZ()),
                finalPos,
                riseTicks,
                totalTicks,
                0
        ));

        if (summon.getEntityWorld() instanceof ServerWorld world) {
            world.playSound(
                    null,
                    summon.getX(),
                    summon.getY(),
                    summon.getZ(),
                    SoundEvents.ENTITY_WARDEN_NEARBY_CLOSER,
                    SoundCategory.HOSTILE,
                    summonSoundVolume(type),
                    1.05F,
                    world.getRandom().nextLong()
            );
        }
    }

    private static boolean tickSummonAnimationIfActive(MobEntity summon) {
        SummonAnimationState state = SUMMON_ANIMATIONS.get(summon.getUuid());
        if (state == null) {
            return false;
        }
        if (!(summon.getEntityWorld() instanceof ServerWorld world)) {
            SUMMON_ANIMATIONS.remove(summon.getUuid());
            return false;
        }

        state.elapsedTicks++;
        boolean inPause = state.elapsedTicks > state.riseTicks;
        int riseElapsed = Math.min(state.elapsedTicks, state.riseTicks);
        double progress = Math.clamp((double) riseElapsed / (double) state.riseTicks, 0.0D, 1.0D);
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);

        Vec3d currentPos = inPause ? state.endPos : state.startPos.lerp(state.endPos, eased);
        summon.refreshPositionAndAngles(currentPos.x, currentPos.y, currentPos.z, summon.getYaw(), summon.getPitch());
        summon.setVelocity(Vec3d.ZERO);
        summon.getNavigation().stop();
        refreshCoreSummonState(summon, state.type);
        syncSummonNameplate(summon, state.type);

        emitSummonAnimationParticles(world, summon, state, inPause, progress);
        emitSummonAnimationSounds(world, summon, state);

        if (state.elapsedTicks >= state.totalTicks) {
            SUMMON_ANIMATIONS.remove(summon.getUuid());
            summon.setAiDisabled(false);
            summon.setNoGravity(false);
            summon.setInvulnerable(false);
            emitSummonFinalBurst(world, summon, state.type);
            world.playSound(
                    null,
                    summon.getX(),
                    summon.getY(),
                    summon.getZ(),
                    SoundEvents.ENTITY_WARDEN_NEARBY_CLOSEST,
                    SoundCategory.HOSTILE,
                    summonSoundVolume(state.type),
                    1.0F,
                    world.getRandom().nextLong()
            );
            return false;
        }

        return true;
    }

    private static void emitSummonAnimationParticles(ServerWorld world, MobEntity summon, SummonAnimationState state, boolean inPause, double progress) {
        if (!summonParticlesEnabled(state.type)) {
            return;
        }

        double x = summon.getX();
        double y = summon.getY() + 0.2D;
        double z = summon.getZ();
        double intensity = particleIntensityScale(state.type);
        int scaledDust = Math.max(1, (int) Math.round((4.0D + (6.0D * progress)) * intensity));
        int scaledSoul = Math.max(1, (int) Math.round((3.0D + (8.0D * progress)) * intensity));

        if (state.elapsedTicks == 1) {
            world.spawnParticles(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SCULK.getDefaultState()),
                    x, y, z,
                    Math.max(4, (int) Math.round(16.0D * intensity)),
                    0.45D, 0.10D, 0.45D, 0.03D
            );
        }

        if (state.elapsedTicks % 2 == 0) {
            world.spawnParticles(
                    ParticleTypes.SCULK_SOUL,
                    x, y + 0.35D, z,
                    scaledSoul,
                    0.20D, 0.16D, 0.20D, 0.012D
            );
            world.spawnParticles(
                    ParticleTypes.SOUL,
                    x, y + 0.40D, z,
                    scaledDust,
                    0.20D, 0.16D, 0.20D, 0.01D
            );
        }

        if (state.elapsedTicks % 4 == 0) {
            world.spawnParticles(
                    ParticleTypes.POOF,
                    x, y + 0.10D, z,
                    Math.max(1, (int) Math.round(5.0D * intensity)),
                    0.24D, 0.08D, 0.24D, 0.01D
            );
        }

        if (inPause) {
            world.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    x, summon.getBodyY(0.55D), z,
                    Math.max(1, (int) Math.round(6.0D * intensity)),
                    0.18D, 0.18D, 0.18D, 0.01D
            );
        }
    }

    private static void emitSummonAnimationSounds(ServerWorld world, MobEntity summon, SummonAnimationState state) {
        int oneThird = state.totalTicks / 3;
        int twoThirds = (state.totalTicks * 2) / 3;
        if (state.elapsedTicks == oneThird || state.elapsedTicks == twoThirds) {
            world.playSound(
                    null,
                    summon.getX(),
                    summon.getY(),
                    summon.getZ(),
                    SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE,
                    SoundCategory.HOSTILE,
                    summonSoundVolume(state.type) * 0.9F,
                    1.1F,
                    world.getRandom().nextLong()
            );
        }
    }

    private static void emitSummonFinalBurst(ServerWorld world, MobEntity summon, SummonType type) {
        if (!summonParticlesEnabled(type)) {
            return;
        }

        double x = summon.getX();
        double y = summon.getBodyY(0.50D);
        double z = summon.getZ();
        double intensity = particleIntensityScale(type);

        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                x, y, z,
                Math.max(2, (int) Math.round(12.0D * intensity)),
                0.40D, 0.25D, 0.40D, 0.02D
        );
        world.spawnParticles(
                ParticleTypes.SOUL,
                x, y, z,
                Math.max(3, (int) Math.round(14.0D * intensity)),
                0.45D, 0.30D, 0.45D, 0.02D
        );
        world.spawnParticles(
                ParticleTypes.SCULK_SOUL,
                x, y, z,
                Math.max(2, (int) Math.round(10.0D * intensity)),
                0.35D, 0.25D, 0.35D, 0.02D
        );
    }

    private static void emitHitParticles(ServerPlayerEntity target, SummonType type) {
        if (!hitParticlesEnabled(type) || !(target.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(),
                target.getBodyY(0.50D),
                target.getZ(),
                5,
                0.18D, 0.14D, 0.18D, 0.01D
        );
        world.spawnParticles(
                ParticleTypes.SOUL,
                target.getX(),
                target.getBodyY(0.55D),
                target.getZ(),
                7,
                0.18D, 0.14D, 0.18D, 0.01D
        );
    }

    private static void applyCommanderArrowEffects(ServerPlayerEntity target) {
        for (ResolvedStatusEffect effect : commanderArrowEffects) {
            target.addStatusEffect(new StatusEffectInstance(effect.effect(), effect.durationTicks(), effect.amplifier(), true, false));
        }
    }

    private static void maintainCommanderSummon(MinecraftServer server, MobEntity summon, ServerPlayerEntity target) {
        summon.setTarget(target);
        summon.getLookControl().lookAt(target, 30.0F, 30.0F);
        summon.lookAtEntity(target, 30.0F, 30.0F);
        summon.setBodyYaw(summon.getHeadYaw());

        boolean canShoot = summon.squaredDistanceTo(target) <= COMMANDER_STOP_RANGE_SQUARED
                && summon.getVisibilityCache().canSee(target);
        if (!canShoot) {
            summon.getNavigation().startMovingTo(target, movementNavigationSpeed(SummonType.COMMANDER));
            return;
        }

        summon.getNavigation().stop();
        long now = server.getTicks();
        long readyTick = COMMANDER_SHOT_READY_TICKS.getOrDefault(summon.getUuid(), 0L);
        if (readyTick > now) {
            return;
        }

        shootCommanderArrow(summon, target);
        COMMANDER_SHOT_READY_TICKS.put(summon.getUuid(), now + COMMANDER_SHOT_COOLDOWN_TICKS);
    }

    private static void shootCommanderArrow(MobEntity summon, LivingEntity target) {
        if (!(summon.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        ItemStack projectileStack = new ItemStack(Items.ARROW);
        ItemStack weaponStack = summon.getMainHandStack().copy();
        if (!weaponStack.isOf(Items.CROSSBOW)) {
            weaponStack = new ItemStack(Items.CROSSBOW);
        }
        ArrowEntity arrow = new ArrowEntity(world, summon, projectileStack, weaponStack);
        Vec3d origin = summon.getEyePos();
        Vec3d aimPoint = new Vec3d(target.getX(), target.getBodyY(0.3333333333333333D), target.getZ());
        double deltaX = aimPoint.x - origin.x;
        double deltaY = aimPoint.y - origin.y;
        double deltaZ = aimPoint.z - origin.z;
        double horizontalDistance = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));

        arrow.setPosition(origin.x, origin.y - 0.1D, origin.z);
        arrow.setOwner(summon);
        arrow.setDamage(COMMANDER_ARROW_DAMAGE);
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        arrow.setVelocity(deltaX, deltaY + (horizontalDistance * 0.2D), deltaZ, 1.8F, 3.0F);

        if (world.spawnEntity(arrow)) {
            world.playSound(
                    null,
                    summon.getX(),
                    summon.getY(),
                    summon.getZ(),
                    SoundEvents.ITEM_CROSSBOW_SHOOT,
                    SoundCategory.HOSTILE,
                    1.0F,
                    0.95F + (world.getRandom().nextFloat() * 0.1F),
                    world.getRandom().nextLong()
            );
        }
    }

    private static void activateDeputyDefense(MobEntity deputy, DamageSource source) {
        if (!(deputy.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        UUID focusTargetId = resolveLivingAttackerId(source);
        if (focusTargetId == null) {
            return;
        }

        DEPUTY_DEFENSES.put(
                deputy.getUuid(),
                new DeputyDefenseState(world.getServer().getTicks() + DEPUTY_SHIELD_BLOCK_TICKS, focusTargetId)
        );
        if (!deputy.isUsingItem()) {
            deputy.setCurrentHand(Hand.OFF_HAND);
        }
    }

    private static boolean maintainDeputyDefense(MinecraftServer server, MobEntity deputy) {
        DeputyDefenseState state = DEPUTY_DEFENSES.get(deputy.getUuid());
        if (state == null) {
            return false;
        }

        if (state.defendUntilTick() <= server.getTicks()) {
            stopDeputyDefense(deputy);
            return false;
        }

        deputy.setTarget(null);
        deputy.getNavigation().stop();

        Entity focusEntity = findEntity(server, state.focusTargetId());
        if (focusEntity instanceof LivingEntity living && living.isAlive() && living.getEntityWorld() == deputy.getEntityWorld()) {
            deputy.getLookControl().lookAt(living, 30.0F, 30.0F);
        }

        if (!deputy.isUsingItem()) {
            deputy.setCurrentHand(Hand.OFF_HAND);
        }

        return true;
    }

    private static void stopDeputyDefense(MobEntity deputy) {
        DEPUTY_DEFENSES.remove(deputy.getUuid());
        if (deputy.isUsingItem()) {
            deputy.clearActiveItem();
        }
    }

    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (entity instanceof ArmorStandEntity armorStand) {
            UUID summonId = summonIdFromNameplateTags(armorStand);
            if (summonId == null) {
                return;
            }

            UUID existingNameplateId = NAMEPLATES_BY_SUMMON.get(summonId);
            if (existingNameplateId != null && !existingNameplateId.equals(armorStand.getUuid())) {
                Entity existing = findEntity(world.getServer(), existingNameplateId);
                if (existing instanceof ArmorStandEntity existingArmorStand && existingArmorStand.isAlive()) {
                    armorStand.discard();
                    return;
                }
            }

            NAMEPLATES_BY_SUMMON.put(summonId, armorStand.getUuid());
            SUMMONS_BY_NAMEPLATE.put(armorStand.getUuid(), summonId);
            return;
        }

        if (!(entity instanceof MobEntity summon)) {
            return;
        }

        StoredSummonData stored = storedSummonData(summon);
        if (stored == null) {
            return;
        }

        trackSummon(stored.ownerId(), summon.getUuid(), stored.type(), stored.spawnTick());
        refreshCoreSummonState(summon, stored.type());
        refreshSummonConfiguredStats(summon, stored.type());
    }

    private static void onEntityUnload(Entity entity, ServerWorld world) {
        UUID entityId = entity.getUuid();
        SUMMON_ANIMATIONS.remove(entityId);
        DEPUTY_DEFENSES.remove(entityId);
        COMMANDER_SHOT_READY_TICKS.remove(entityId);

        if (entity.getCommandTags().contains(SUMMON_NAMEPLATE_TAG)) {
            UUID summonId = SUMMONS_BY_NAMEPLATE.remove(entityId);
            if (summonId != null && entityId.equals(NAMEPLATES_BY_SUMMON.get(summonId))) {
                NAMEPLATES_BY_SUMMON.remove(summonId);
            }
            return;
        }

        if (!entity.getCommandTags().contains(SUMMON_ENTITY_TAG)) {
            return;
        }

        UUID nameplateId = NAMEPLATES_BY_SUMMON.remove(entityId);
        if (nameplateId != null) {
            SUMMONS_BY_NAMEPLATE.remove(nameplateId);
        }
    }

    private static UUID resolveLivingAttackerId(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity) {
            return attacker.getUuid();
        }

        Entity sourceEntity = source.getSource();
        if (sourceEntity instanceof ProjectileEntity projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner.getUuid();
        }

        return null;
    }

    private static void applySummonEquipmentAsset(SummonType type, ItemStack stack) {
        EquippableComponent existing = stack.get(DataComponentTypes.EQUIPPABLE);
        if (existing == null) {
            return;
        }

        RegistryKey<EquipmentAsset> assetKey = summonEquipmentAsset(type);
        if (assetKey == null) {
            return;
        }

        stack.set(DataComponentTypes.EQUIPPABLE, new EquippableComponent(
                existing.slot(),
                existing.equipSound(),
                Optional.of(assetKey),
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

    private static RegistryKey<EquipmentAsset> summonEquipmentAsset(SummonType type) {
        return switch (type) {
            case DEPUTY -> NETHERWALKER_DEPUTY_EQUIPMENT_ASSET;
            case COMMANDER -> NETHERWALKER_COMMANDER_EQUIPMENT_ASSET;
            case WARDEN -> NETHERWALKER_WARDEN_EQUIPMENT_ASSET;
        };
    }

    private static SummonData resolveAttackingSummon(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker != null) {
            SummonData direct = SUMMONS.get(attacker.getUuid());
            if (direct != null) {
                return direct;
            }
        }

        Entity sourceEntity = source.getSource();
        if (sourceEntity instanceof ProjectileEntity projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null) {
                return SUMMONS.get(owner.getUuid());
            }
        }

        return null;
    }

    private static boolean expireSummonIfNeeded(MinecraftServer server, MobEntity summon, SummonData data) {
        long lifespan = lifespanTicks(data.type());
        if (lifespan <= 0L) {
            return false;
        }
        if ((server.getTicks() - data.spawnTick()) < lifespan) {
            return false;
        }

        summon.discard();
        removeSummonTracking(server, summon.getUuid());
        return true;
    }

    private static ServerPlayerEntity findNearestTarget(MobEntity summon, UUID ownerId, boolean ownerFriendly, double radius) {
        if (!(summon.getEntityWorld() instanceof ServerWorld world)) {
            return null;
        }
        double radiusSquared = radius * radius;

        return world.getPlayers(player -> player.isAlive() && !player.isSpectator())
                .stream()
                .filter(player -> summon.squaredDistanceTo(player) <= radiusSquared)
                .filter(player -> !ownerFriendly || !player.getUuid().equals(ownerId))
                .min(Comparator.comparingDouble(summon::squaredDistanceTo))
                .orElse(null);
    }

    private static void trackSummon(UUID ownerId, UUID summonId, SummonType type, long spawnTick) {
        SUMMONS.put(summonId, new SummonData(ownerId, type, spawnTick));
        SUMMONS_BY_OWNER.computeIfAbsent(ownerId, ignored -> new HashSet<>()).add(summonId);
    }

    private static void tagSummonEntity(Entity entity, UUID ownerId, SummonType type, long spawnTick) {
        entity.addCommandTag(SUMMON_ENTITY_TAG);
        replacePrefixedCommandTag(entity, SUMMON_OWNER_TAG_PREFIX, ownerId.toString());
        replacePrefixedCommandTag(entity, SUMMON_TYPE_TAG_PREFIX, type.id);
        replacePrefixedCommandTag(entity, SUMMON_SPAWN_TICK_TAG_PREFIX, Long.toString(spawnTick));
    }

    private static void tagSummonNameplate(Entity entity, UUID summonId) {
        entity.addCommandTag(SUMMON_NAMEPLATE_TAG);
        replacePrefixedCommandTag(entity, SUMMON_NAMEPLATE_SUMMON_TAG_PREFIX, summonId.toString());
    }

    private static void replacePrefixedCommandTag(Entity entity, String prefix, String value) {
        for (String tag : List.copyOf(entity.getCommandTags())) {
            if (tag.startsWith(prefix)) {
                entity.removeCommandTag(tag);
            }
        }
        entity.addCommandTag(prefix + value);
    }

    private static StoredSummonData storedSummonData(Entity entity) {
        if (!entity.getCommandTags().contains(SUMMON_ENTITY_TAG)) {
            return null;
        }

        UUID ownerId = parseUuidCommandTag(entity, SUMMON_OWNER_TAG_PREFIX);
        SummonType type = parseSummonTypeCommandTag(entity, SUMMON_TYPE_TAG_PREFIX);
        Long spawnTick = parseLongCommandTag(entity, SUMMON_SPAWN_TICK_TAG_PREFIX);
        if (ownerId == null || type == null || spawnTick == null) {
            return null;
        }

        return new StoredSummonData(ownerId, type, spawnTick);
    }

    private static UUID summonIdFromNameplateTags(Entity entity) {
        if (!entity.getCommandTags().contains(SUMMON_NAMEPLATE_TAG)) {
            return null;
        }

        return parseUuidCommandTag(entity, SUMMON_NAMEPLATE_SUMMON_TAG_PREFIX);
    }

    private static UUID parseUuidCommandTag(Entity entity, String prefix) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(prefix)) {
                try {
                    return UUID.fromString(tag.substring(prefix.length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Long parseLongCommandTag(Entity entity, String prefix) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(prefix)) {
                try {
                    return Long.parseLong(tag.substring(prefix.length()));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static SummonType parseSummonTypeCommandTag(Entity entity, String prefix) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(prefix)) {
                return SummonType.fromId(tag.substring(prefix.length()));
            }
        }
        return null;
    }

    private static void discardNameplateForSummon(MinecraftServer server, UUID summonId) {
        UUID nameplateId = NAMEPLATES_BY_SUMMON.remove(summonId);
        if (nameplateId == null) {
            return;
        }

        SUMMONS_BY_NAMEPLATE.remove(nameplateId);
        if (server == null) {
            return;
        }

        Entity nameplate = findEntity(server, nameplateId);
        if (nameplate != null) {
            nameplate.discard();
        }
    }

    private static void removeSummonTracking(MinecraftServer server, UUID summonId) {
        SUMMON_ANIMATIONS.remove(summonId);
        DEPUTY_DEFENSES.remove(summonId);
        COMMANDER_SHOT_READY_TICKS.remove(summonId);

        UUID summonFromNameplate = SUMMONS_BY_NAMEPLATE.remove(summonId);
        if (summonFromNameplate != null) {
            NAMEPLATES_BY_SUMMON.remove(summonFromNameplate);
            return;
        }

        discardNameplateForSummon(server, summonId);
        SummonData removed = SUMMONS.remove(summonId);
        if (removed == null) {
            return;
        }

        Set<UUID> ownerSummons = SUMMONS_BY_OWNER.get(removed.ownerId());
        if (ownerSummons == null) {
            return;
        }
        ownerSummons.remove(summonId);
        if (ownerSummons.isEmpty()) {
            SUMMONS_BY_OWNER.remove(removed.ownerId());
        }
    }

    private static int dismissSummonsForOwner(MinecraftServer server, UUID ownerId, SummonType type) {
        Set<UUID> ownerSummons = SUMMONS_BY_OWNER.get(ownerId);
        if (ownerSummons == null || ownerSummons.isEmpty()) {
            return 0;
        }

        int removed = 0;
        for (UUID summonId : List.copyOf(ownerSummons)) {
            SummonData data = SUMMONS.get(summonId);
            if (data == null || (type != null && data.type() != type)) {
                continue;
            }

            Entity entity = findEntity(server, summonId);
            if (entity != null) {
                entity.discard();
            }
            removeSummonTracking(server, summonId);
            removed++;
        }
        return removed;
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void setBaseAttribute(LivingEntity entity, RegistryEntry<EntityAttribute> attribute, double value) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static boolean playerHasUndeadWardArmy(ServerPlayerEntity player) {
        if (EmptyEmbraceArtifactSuppression.areArtifactPowersSuppressed(player)) {
            return false;
        }
        if (player.getMainHandStack().isOf(ModItems.UNDEAD_WARD_ARMY) || player.getOffHandStack().isOf(ModItems.UNDEAD_WARD_ARMY)) {
            return true;
        }
        return player.getInventory().contains(stack -> stack.isOf(ModItems.UNDEAD_WARD_ARMY));
    }

    private static void sendCooldownMessage(ServerPlayerEntity player, SummonType type, long ticksRemaining) {
        sendActionBar(player, Text.literal(type.displayNameSingular + " ready in " + formatDuration(ticksRemaining) + "."));
    }

    private static String formatDuration(long ticks) {
        long totalSeconds = Math.max(1L, (ticks + 19L) / 20L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes <= 0L) {
            return totalSeconds + "s";
        }
        return minutes + "m " + seconds + "s";
    }

    private static void sendActionBar(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, true);
    }

    private static long getCooldownRemaining(UUID ownerId, SummonType type, long nowTick) {
        CooldownState state = COOLDOWNS.get(ownerId);
        if (state == null) {
            return 0L;
        }
        long readyTick = state.readyTick(type);
        return Math.max(0L, readyTick - nowTick);
    }

    private static void setCooldownReadyTick(UUID ownerId, SummonType type, long readyTick) {
        CooldownState state = COOLDOWNS.computeIfAbsent(ownerId, ignored -> new CooldownState());
        state.setReadyTick(type, readyTick);
    }

    private static void tickCooldowns(MinecraftServer server) {
        long now = server.getTicks();
        for (Map.Entry<UUID, CooldownState> entry : List.copyOf(COOLDOWNS.entrySet())) {
            CooldownState state = entry.getValue();
            if (state.deputiesReadyTick <= now && state.commandersReadyTick <= now && state.wardenReadyTick <= now) {
                COOLDOWNS.remove(entry.getKey());
            }
        }
    }

    private static int summonCount(SummonType type) {
        return switch (type) {
            case DEPUTY -> cfg().deputies.summonCount;
            case COMMANDER -> cfg().commanders.summonCount;
            case WARDEN -> cfg().warden.summonCount;
        };
    }

    private static int summonCooldownTicks(SummonType type) {
        return switch (type) {
            case DEPUTY -> cfg().deputies.summonCooldownTicks;
            case COMMANDER -> cfg().commanders.summonCooldownTicks;
            case WARDEN -> cfg().warden.summonCooldownTicks;
        };
    }

    private static int lifespanTicks(SummonType type) {
        return switch (type) {
            case DEPUTY -> cfg().deputies.lifespanTicks;
            case COMMANDER -> cfg().commanders.lifespanTicks;
            case WARDEN -> cfg().warden.lifespanTicks;
        };
    }

    private static double aggroRadius(SummonType type) {
        return switch (type) {
            case DEPUTY -> cfg().deputies.aggroRadius;
            case COMMANDER -> cfg().commanders.aggroRadius;
            case WARDEN -> cfg().warden.aggroRadius;
        };
    }

    private static double movementNavigationSpeed(SummonType type) {
        return switch (type) {
            case DEPUTY -> 1.35D;
            case COMMANDER -> 1.25D;
            case WARDEN -> 1.45D;
        };
    }

    private static float summonSoundVolume(SummonType type) {
        return switch (type) {
            case DEPUTY -> 0.70F;
            case COMMANDER -> 0.85F;
            case WARDEN -> 1.00F;
        };
    }

    private static double particleIntensityScale(SummonType type) {
        return switch (type) {
            case DEPUTY -> 0.42D;
            case COMMANDER -> 0.62D;
            case WARDEN -> 1.0D;
        };
    }

    private static boolean summonParticlesEnabled(SummonType type) {
        return switch (type) {
            case DEPUTY -> cfg().deputies.enableSummonParticles;
            case COMMANDER -> cfg().commanders.enableSummonParticles;
            case WARDEN -> cfg().warden.enableSummonParticles;
        };
    }

    private static boolean hitParticlesEnabled(SummonType type) {
        return switch (type) {
            case DEPUTY -> cfg().deputies.enableHitParticles;
            case COMMANDER -> cfg().commanders.enableHitParticles;
            case WARDEN -> cfg().warden.enableHitParticles;
        };
    }

    private static Text summonName(SummonType type) {
        return switch (type) {
            case DEPUTY -> DEPUTY_NAME;
            case COMMANDER -> COMMANDER_NAME;
            case WARDEN -> WARDEN_NAME;
        };
    }

    private static ArtifactsConfig.UndeadArmorProfile armorProfile(SummonType type) {
        return switch (type) {
            case DEPUTY -> cfg().deputies.armor;
            case COMMANDER -> cfg().commanders.armor;
            case WARDEN -> cfg().warden.armor;
        };
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

    private static ArtifactsConfig.UndeadWardArmySection cfg() {
        return ArtifactsConfigManager.get().undeadWardArmy;
    }

    private enum SummonType {
        DEPUTY("deputy", "Undead Deputy", "Undead Deputies"),
        COMMANDER("commander", "Undead Commander", "Undead Commanders"),
        WARDEN("warden", "Undead Warden", "Undead Wardens");

        private final String id;
        private final String displayNameSingular;
        private final String displayNamePlural;

        SummonType(String id, String displayNameSingular, String displayNamePlural) {
            this.id = id;
            this.displayNameSingular = displayNameSingular;
            this.displayNamePlural = displayNamePlural;
        }

        private static SummonType fromId(String id) {
            for (SummonType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return null;
        }
    }

    private record SummonData(UUID ownerId, SummonType type, long spawnTick) {
    }

    private record StoredSummonData(UUID ownerId, SummonType type, long spawnTick) {
    }

    private static final class SummonAnimationState {
        private final SummonType type;
        private final Vec3d startPos;
        private final Vec3d endPos;
        private final int riseTicks;
        private final int totalTicks;
        private int elapsedTicks;

        private SummonAnimationState(SummonType type, Vec3d startPos, Vec3d endPos, int riseTicks, int totalTicks, int elapsedTicks) {
            this.type = type;
            this.startPos = startPos;
            this.endPos = endPos;
            this.riseTicks = riseTicks;
            this.totalTicks = totalTicks;
            this.elapsedTicks = elapsedTicks;
        }
    }

    private static final class CooldownState {
        private long deputiesReadyTick;
        private long commandersReadyTick;
        private long wardenReadyTick;

        private long readyTick(SummonType type) {
            return switch (type) {
                case DEPUTY -> deputiesReadyTick;
                case COMMANDER -> commandersReadyTick;
                case WARDEN -> wardenReadyTick;
            };
        }

        private void setReadyTick(SummonType type, long readyTick) {
            switch (type) {
                case DEPUTY -> deputiesReadyTick = readyTick;
                case COMMANDER -> commandersReadyTick = readyTick;
                case WARDEN -> wardenReadyTick = readyTick;
            }
        }
    }

    private record DeputyDefenseState(long defendUntilTick, UUID focusTargetId) {
    }

    private record ResolvedStatusEffect(RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int durationTicks, int amplifier) {
    }
}
