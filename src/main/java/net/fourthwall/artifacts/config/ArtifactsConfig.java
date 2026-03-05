package net.fourthwall.artifacts.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArtifactsConfig {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public SmolderingRodSection smolderingRod = new SmolderingRodSection();
    public InfestedSwordSection infestedSword = new InfestedSwordSection();
    public InfestedPickaxeSection infestedPickaxe = new InfestedPickaxeSection();
    public RepeaterSection repeater = new RepeaterSection();
    public BloodSacrificeSection bloodSacrifice = new BloodSacrificeSection();
    public UndeadWardArmySection undeadWardArmy = new UndeadWardArmySection();
    public BeaconCoreSection beaconCore = new BeaconCoreSection();
    public EarthsplitterSection earthsplitter = new EarthsplitterSection();
    public ExcaliburSection excalibur = new ExcaliburSection();
    public EmperorsCrownSection emperorsCrown = new EmperorsCrownSection();
    public VoidReaverSection voidReaver = new VoidReaverSection();
    public TridentOfPoseidonSection tridentOfPoseidon = new TridentOfPoseidonSection();
    public LionsHeartChestplateSection lionsHeartChestplate = new LionsHeartChestplateSection();
    public ArtifactEnchantSection artifactEnchants = new ArtifactEnchantSection();

    public void sanitize() {
        if (smolderingRod == null) {
            smolderingRod = new SmolderingRodSection();
        }
        if (infestedSword == null) {
            infestedSword = new InfestedSwordSection();
        }
        if (infestedPickaxe == null) {
            infestedPickaxe = new InfestedPickaxeSection();
        }
        if (repeater == null) {
            repeater = new RepeaterSection();
        }
        if (bloodSacrifice == null) {
            bloodSacrifice = new BloodSacrificeSection();
        }
        if (undeadWardArmy == null) {
            undeadWardArmy = new UndeadWardArmySection();
        }
        if (beaconCore == null) {
            beaconCore = new BeaconCoreSection();
        }
        if (earthsplitter == null) {
            earthsplitter = new EarthsplitterSection();
        }
        if (excalibur == null) {
            excalibur = new ExcaliburSection();
        }
        if (emperorsCrown == null) {
            emperorsCrown = new EmperorsCrownSection();
        }
        if (voidReaver == null) {
            voidReaver = new VoidReaverSection();
        }
        if (tridentOfPoseidon == null) {
            tridentOfPoseidon = new TridentOfPoseidonSection();
        }
        if (lionsHeartChestplate == null) {
            lionsHeartChestplate = new LionsHeartChestplateSection();
        }
        if (artifactEnchants == null) {
            artifactEnchants = new ArtifactEnchantSection();
        }

        smolderingRod.sanitize();
        infestedSword.sanitize();
        infestedPickaxe.sanitize();
        repeater.sanitize();
        bloodSacrifice.sanitize();
        undeadWardArmy.sanitize();
        beaconCore.sanitize();
        earthsplitter.sanitize();
        excalibur.sanitize();
        emperorsCrown.sanitize();
        voidReaver.sanitize();
        tridentOfPoseidon.sanitize();
        lionsHeartChestplate.sanitize();
        artifactEnchants.sanitize();
    }

    JsonObject toDocumentedJson() {
        ArtifactsConfig defaults = new ArtifactsConfig();
        defaults.sanitize();

        JsonObject root = new JsonObject();
        root.addProperty("_edit_note", "Edit the setting values. Keys ending in _default are documentation and will be rewritten on startup.");

        root.add("smolderingRod", buildSmolderingRodJson(this.smolderingRod, defaults.smolderingRod));
        root.add("infestedSword", buildInfestedSwordJson(this.infestedSword, defaults.infestedSword));
        root.add("infestedPickaxe", buildInfestedPickaxeJson(this.infestedPickaxe, defaults.infestedPickaxe));
        root.add("repeater", buildRepeaterJson(this.repeater, defaults.repeater));
        root.add("bloodSacrifice", buildBloodSacrificeJson(this.bloodSacrifice, defaults.bloodSacrifice));
        root.add("undeadWardArmy", buildUndeadWardArmyJson(this.undeadWardArmy, defaults.undeadWardArmy));
        root.add("beaconCore", buildBeaconCoreJson(this.beaconCore, defaults.beaconCore));
        root.add("earthsplitter", buildEarthsplitterJson(this.earthsplitter, defaults.earthsplitter));
        root.add("excalibur", buildExcaliburJson(this.excalibur, defaults.excalibur));
        root.add("emperorsCrown", buildEmperorsCrownJson(this.emperorsCrown, defaults.emperorsCrown));
        root.add("voidReaver", buildVoidReaverJson(this.voidReaver, defaults.voidReaver));
        root.add("tridentOfPoseidon", buildTridentJson(this.tridentOfPoseidon, defaults.tridentOfPoseidon));
        root.add("lionHeartChestplate", buildLionHeartJson(this.lionsHeartChestplate, defaults.lionsHeartChestplate));
        root.add("artifactEnchants", buildArtifactEnchantsJson(this.artifactEnchants, defaults.artifactEnchants));
        return root;
    }

    private static JsonObject buildSmolderingRodJson(SmolderingRodSection current, SmolderingRodSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "detonationDamage", current.detonationDamage, defaults.detonationDamage);
        addDoc(obj, "primeFireTicks", current.primeFireTicks, defaults.primeFireTicks);
        addDoc(obj, "maxPrimedTargetsPerPlayer", current.maxPrimedTargetsPerPlayer, defaults.maxPrimedTargetsPerPlayer);
        addDoc(obj, "friendlyFireRules", current.friendlyFireRules, defaults.friendlyFireRules);
        addDoc(obj, "lastOwnerWins", current.lastOwnerWins, defaults.lastOwnerWins);
        return obj;
    }

    private static JsonObject buildInfestedSwordJson(InfestedSwordSection current, InfestedSwordSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "silverfishAuraBuffs", current.silverfishAuraBuffs, defaults.silverfishAuraBuffs);
        addDoc(obj, "silverfishAuraRadius", current.silverfishAuraRadius, defaults.silverfishAuraRadius);
        addDoc(obj, "spawnGuaranteedInfestedChance", current.spawnGuaranteedInfestedChance, defaults.spawnGuaranteedInfestedChance);
        addDoc(obj, "enableTargetingCommand", current.enableTargetingCommand, defaults.enableTargetingCommand);
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        obj.addProperty("spawnGuaranteedInfestedChance_note", "0.0 to 1.0 chance for silverfish spawned by Guaranteed Infested to receive the effect.");
        return obj;
    }

    private static JsonObject buildInfestedPickaxeJson(InfestedPickaxeSection current, InfestedPickaxeSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "enableAuraInfestation", current.enableAuraInfestation, defaults.enableAuraInfestation);
        return obj;
    }

    private static JsonObject buildRepeaterJson(RepeaterSection current, RepeaterSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "postFiringCooldownTicks", current.postFiringCooldownTicks, defaults.postFiringCooldownTicks);
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        return obj;
    }

    private static JsonObject buildBloodSacrificeJson(BloodSacrificeSection current, BloodSacrificeSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "healthAfterUse", current.healthAfterUse, defaults.healthAfterUse);
        addDoc(obj, "itemCooldownTicks", current.itemCooldownTicks, defaults.itemCooldownTicks);

        JsonObject summon = new JsonObject();
        addDoc(summon, "maxHealth", current.summon.maxHealth, defaults.summon.maxHealth);
        addDoc(summon, "movementSpeed", current.summon.movementSpeed, defaults.summon.movementSpeed);
        addDoc(summon, "baseAttackDamage", current.summon.baseAttackDamage, defaults.summon.baseAttackDamage);
        addDoc(summon, "lifespanTicks", current.summon.lifespanTicks, defaults.summon.lifespanTicks);
        addDoc(summon, "inflictsWither", current.summon.inflictsWither, defaults.summon.inflictsWither);
        addDoc(summon, "tetherRadius", current.summon.tetherRadius, defaults.summon.tetherRadius);
        addDoc(summon, "targetClosestPlayerOnly", current.summon.targetClosestPlayerOnly, defaults.summon.targetClosestPlayerOnly);
        addDoc(summon, "knockbackResistance", current.summon.knockbackResistance, defaults.summon.knockbackResistance);
        summon.addProperty("lifespanTicks_note", "0 means infinite lifespan (current default behavior).");
        summon.addProperty("knockbackResistance_note", "Higher values mean the summon takes less knockback. 1.0 = full resistance.");

        JsonObject armorEnchants = new JsonObject();
        addDoc(armorEnchants, "protection", current.summon.enchantLevels.armor.protection, defaults.summon.enchantLevels.armor.protection);
        addDoc(armorEnchants, "projectileProtection", current.summon.enchantLevels.armor.projectileProtection, defaults.summon.enchantLevels.armor.projectileProtection);
        addDoc(armorEnchants, "fireProtection", current.summon.enchantLevels.armor.fireProtection, defaults.summon.enchantLevels.armor.fireProtection);
        addDoc(armorEnchants, "blastProtection", current.summon.enchantLevels.armor.blastProtection, defaults.summon.enchantLevels.armor.blastProtection);

        JsonObject bootsEnchants = new JsonObject();
        addDoc(bootsEnchants, "featherFalling", current.summon.enchantLevels.boots.featherFalling, defaults.summon.enchantLevels.boots.featherFalling);
        addDoc(bootsEnchants, "frostWalker", current.summon.enchantLevels.boots.frostWalker, defaults.summon.enchantLevels.boots.frostWalker);
        addDoc(bootsEnchants, "soulSpeed", current.summon.enchantLevels.boots.soulSpeed, defaults.summon.enchantLevels.boots.soulSpeed);

        JsonObject weaponEnchants = new JsonObject();
        addDoc(weaponEnchants, "sharpness", current.summon.enchantLevels.weapon.sharpness, defaults.summon.enchantLevels.weapon.sharpness);

        JsonObject enchantLevels = new JsonObject();
        enchantLevels.add("armor", armorEnchants);
        enchantLevels.add("boots", bootsEnchants);
        enchantLevels.add("weapon", weaponEnchants);
        summon.add("enchantLevels", enchantLevels);

        obj.add("summon", summon);
        return obj;
    }

    private static JsonObject buildUndeadWardArmyJson(UndeadWardArmySection current, UndeadWardArmySection defaults) {
        JsonObject obj = new JsonObject();
        obj.add("deputies", buildUndeadDeputiesJson(current.deputies, defaults.deputies));
        obj.add("commanders", buildUndeadCommandersJson(current.commanders, defaults.commanders));
        obj.add("warden", buildUndeadWardenJson(current.warden, defaults.warden));
        return obj;
    }

    private static JsonObject buildUndeadDeputiesJson(UndeadDeputySection current, UndeadDeputySection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "summonCount", current.summonCount, defaults.summonCount);
        addDoc(obj, "summonCooldownTicks", current.summonCooldownTicks, defaults.summonCooldownTicks);
        addDoc(obj, "lifespanTicks", current.lifespanTicks, defaults.lifespanTicks);
        addDoc(obj, "summonRadius", current.summonRadius, defaults.summonRadius);
        addDoc(obj, "aggroRadius", current.aggroRadius, defaults.aggroRadius);
        addDoc(obj, "swordAttackDamage", current.swordAttackDamage, defaults.swordAttackDamage);
        addDoc(obj, "enableSummonParticles", current.enableSummonParticles, defaults.enableSummonParticles);
        addDoc(obj, "enableHitParticles", current.enableHitParticles, defaults.enableHitParticles);
        obj.add("armor", buildUndeadArmorProfileJson(current.armor, defaults.armor));
        return obj;
    }

    private static JsonObject buildUndeadCommandersJson(UndeadCommanderSection current, UndeadCommanderSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "summonCount", current.summonCount, defaults.summonCount);
        addDoc(obj, "summonCooldownTicks", current.summonCooldownTicks, defaults.summonCooldownTicks);
        addDoc(obj, "lifespanTicks", current.lifespanTicks, defaults.lifespanTicks);
        addDoc(obj, "summonRadius", current.summonRadius, defaults.summonRadius);
        addDoc(obj, "aggroRadius", current.aggroRadius, defaults.aggroRadius);
        addDoc(obj, "enableSummonParticles", current.enableSummonParticles, defaults.enableSummonParticles);
        addDoc(obj, "enableHitParticles", current.enableHitParticles, defaults.enableHitParticles);
        addDoc(obj, "arrowHitEffects", current.arrowHitEffects, defaults.arrowHitEffects);
        obj.addProperty("arrowHitEffects_note", "Status effects applied by commander arrows. Amplifier is zero-based (1 = level II).");
        obj.add("armor", buildUndeadArmorProfileJson(current.armor, defaults.armor));
        return obj;
    }

    private static JsonObject buildUndeadWardenJson(UndeadWardenSection current, UndeadWardenSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "summonCount", current.summonCount, defaults.summonCount);
        addDoc(obj, "summonCooldownTicks", current.summonCooldownTicks, defaults.summonCooldownTicks);
        addDoc(obj, "lifespanTicks", current.lifespanTicks, defaults.lifespanTicks);
        addDoc(obj, "aggroRadius", current.aggroRadius, defaults.aggroRadius);
        addDoc(obj, "summonForwardDistance", current.summonForwardDistance, defaults.summonForwardDistance);
        addDoc(obj, "summonSpreadRadius", current.summonSpreadRadius, defaults.summonSpreadRadius);
        addDoc(obj, "movementSpeed", current.movementSpeed, defaults.movementSpeed);
        addDoc(obj, "baseAttackDamage", current.baseAttackDamage, defaults.baseAttackDamage);
        addDoc(obj, "enableSummonParticles", current.enableSummonParticles, defaults.enableSummonParticles);
        addDoc(obj, "enableHitParticles", current.enableHitParticles, defaults.enableHitParticles);
        obj.add("armor", buildUndeadArmorProfileJson(current.armor, defaults.armor));
        return obj;
    }

    private static JsonObject buildUndeadArmorProfileJson(UndeadArmorProfile current, UndeadArmorProfile defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "helmetArmorPoints", current.helmetArmorPoints, defaults.helmetArmorPoints);
        addDoc(obj, "chestplateArmorPoints", current.chestplateArmorPoints, defaults.chestplateArmorPoints);
        addDoc(obj, "leggingsArmorPoints", current.leggingsArmorPoints, defaults.leggingsArmorPoints);
        addDoc(obj, "bootsArmorPoints", current.bootsArmorPoints, defaults.bootsArmorPoints);
        addDoc(obj, "armorToughnessPerPiece", current.armorToughnessPerPiece, defaults.armorToughnessPerPiece);
        return obj;
    }

    private static JsonObject buildBeaconCoreJson(BeaconCoreSection current, BeaconCoreSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "anchorProtectionRadius", current.anchorProtectionRadius, defaults.anchorProtectionRadius);
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        return obj;
    }

    private static JsonObject buildEarthsplitterJson(EarthsplitterSection current, EarthsplitterSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        obj.addProperty("enchants_note", "Configure all enchantments for evanpack:earthsplitter in artifactEnchants.levels.");
        return obj;
    }

    private static JsonObject buildExcaliburJson(ExcaliburSection current, ExcaliburSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "attackDamage", current.attackDamage, defaults.attackDamage);
        addDoc(obj, "attackSpeed", current.attackSpeed, defaults.attackSpeed);
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        addDoc(obj, "enableSounds", current.enableSounds, defaults.enableSounds);
        obj.addProperty("enchants_note", "Configure all enchantments for evanpack:excalibur in artifactEnchants.levels.");
        return obj;
    }

    private static JsonObject buildEmperorsCrownJson(EmperorsCrownSection current, EmperorsCrownSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "jumpStrength", current.jumpStrength, defaults.jumpStrength);
        addDoc(obj, "armorValue", current.armorValue, defaults.armorValue);
        addDoc(obj, "toughness", current.toughness, defaults.toughness);
        addDoc(obj, "movementSpeed", current.movementSpeed, defaults.movementSpeed);
        addDoc(obj, "attackDamage", current.attackDamage, defaults.attackDamage);
        addDoc(obj, "entityInteractionRange", current.entityInteractionRange, defaults.entityInteractionRange);
        addDoc(obj, "blockInteractionRange", current.blockInteractionRange, defaults.blockInteractionRange);
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        obj.addProperty("enchants_note", "Configure all enchantments for evanpack:emperors_crown in artifactEnchants.levels.");
        return obj;
    }

    private static JsonObject buildVoidReaverJson(VoidReaverSection current, VoidReaverSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        return obj;
    }

    private static JsonObject buildTridentJson(TridentOfPoseidonSection current, TridentOfPoseidonSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "beamRange", current.beamRange, defaults.beamRange);
        addDoc(obj, "beamDamage", current.beamDamage, defaults.beamDamage);
        addDoc(obj, "beamDurationTicks", current.beamDurationTicks, defaults.beamDurationTicks);
        addDoc(obj, "beamMovementSpeedMultiplierWhileChanneling", current.beamMovementSpeedMultiplierWhileChanneling, defaults.beamMovementSpeedMultiplierWhileChanneling);
        addDoc(obj, "allowBeamInRain", current.allowBeamInRain, defaults.allowBeamInRain);
        addDoc(obj, "beamCooldownTicks", current.beamCooldownTicks, defaults.beamCooldownTicks);
        addDoc(obj, "holdingEffects", current.holdingEffects, defaults.holdingEffects);
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        obj.addProperty("beamMovementSpeedMultiplierWhileChanneling_note", "0.0 = immobile while channeling (current default). 1.0 = normal speed.");
        return obj;
    }

    private static JsonObject buildLionHeartJson(LionsHeartChestplateSection current, LionsHeartChestplateSection defaults) {
        JsonObject obj = new JsonObject();

        JsonObject enchants = new JsonObject();
        addDoc(enchants, "protection", current.enchants.protection, defaults.enchants.protection);
        addDoc(enchants, "projectileProtection", current.enchants.projectileProtection, defaults.enchants.projectileProtection);
        addDoc(enchants, "fireProtection", current.enchants.fireProtection, defaults.enchants.fireProtection);
        addDoc(enchants, "blastProtection", current.enchants.blastProtection, defaults.enchants.blastProtection);
        addDoc(enchants, "unbreaking", current.enchants.unbreaking, defaults.enchants.unbreaking);
        addDoc(enchants, "mending", current.enchants.mending, defaults.enchants.mending);
        obj.add("enchants", enchants);

        addDoc(obj, "entityInteractionRangeModifierAmount", current.entityInteractionRangeModifierAmount, defaults.entityInteractionRangeModifierAmount);
        addDoc(obj, "blockInteractionRangeModifierAmount", current.blockInteractionRangeModifierAmount, defaults.blockInteractionRangeModifierAmount);
        addDoc(obj, "sizeIncrease", current.sizeIncrease, defaults.sizeIncrease);
        addDoc(obj, "maxHealthIncrease", current.maxHealthIncrease, defaults.maxHealthIncrease);
        addDoc(obj, "armorLevel", current.armorLevel, defaults.armorLevel);
        addDoc(obj, "armorToughness", current.armorToughness, defaults.armorToughness);
        addDoc(obj, "enableParticles", current.enableParticles, defaults.enableParticles);
        obj.addProperty("interactionRange_note", "These are raw attribute modifier amounts using ADD_MULTIPLIED_TOTAL (2.0 = +200% range).");
        return obj;
    }

    private static JsonObject buildArtifactEnchantsJson(ArtifactEnchantSection current, ArtifactEnchantSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "enforceExact", current.enforceExact, defaults.enforceExact);
        addDoc(obj, "levels", current.levels, defaults.levels);
        obj.addProperty("levels_note", "Map format: <item_id>: { <enchantment_id>: <level> }. Use level 0 to remove an enchant.");
        obj.addProperty("future_artifacts_note", "Add new artifact item IDs here to configure enchantments for future artifact items without code changes.");
        return obj;
    }

    private static void addDoc(JsonObject obj, String key, int value, int defaultValue) {
        obj.addProperty(key, value);
        obj.addProperty(key + "_default", defaultValue);
    }

    private static void addDoc(JsonObject obj, String key, long value, long defaultValue) {
        obj.addProperty(key, value);
        obj.addProperty(key + "_default", defaultValue);
    }

    private static void addDoc(JsonObject obj, String key, float value, float defaultValue) {
        obj.addProperty(key, value);
        obj.addProperty(key + "_default", defaultValue);
    }

    private static void addDoc(JsonObject obj, String key, double value, double defaultValue) {
        obj.addProperty(key, value);
        obj.addProperty(key + "_default", defaultValue);
    }

    private static void addDoc(JsonObject obj, String key, boolean value, boolean defaultValue) {
        obj.addProperty(key, value);
        obj.addProperty(key + "_default", defaultValue);
    }

    private static void addDoc(JsonObject obj, String key, Object value, Object defaultValue) {
        obj.add(key, GSON.toJsonTree(value));
        obj.add(key + "_default", GSON.toJsonTree(defaultValue));
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }

    private static float nonNegative(float value) {
        return !Float.isFinite(value) ? 0.0F : Math.max(0.0F, value);
    }

    private static double nonNegative(double value) {
        return !Double.isFinite(value) ? 0.0D : Math.max(0.0D, value);
    }

    private static float chance(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.clamp(value, 0.0F, 1.0F);
    }

    private static void sanitizeEffects(List<StatusEffectEntry> effects) {
        if (effects == null) {
            return;
        }
        for (StatusEffectEntry effect : effects) {
            if (effect == null) {
                continue;
            }
            if (effect.effectId == null || effect.effectId.isBlank()) {
                effect.effectId = "minecraft:speed";
            }
            effect.durationTicks = nonNegative(effect.durationTicks);
            effect.amplifier = nonNegative(effect.amplifier);
        }
    }

    private static LinkedHashMap<String, LinkedHashMap<String, Integer>> defaultArtifactEnchantLevels() {
        LinkedHashMap<String, LinkedHashMap<String, Integer>> levels = new LinkedHashMap<>();
        levels.put("evanpack:smoldering_rod", enchantLevels(
                "minecraft:unbreaking", 3,
                "minecraft:mending", 1
        ));
        levels.put("evanpack:void_reaver", enchantLevels(
                "minecraft:unbreaking", 3,
                "minecraft:mending", 1,
                "minecraft:sharpness", 5
        ));
        levels.put("evanpack:infested_sword", enchantLevels(
                "minecraft:sharpness", 5,
                "minecraft:fire_aspect", 2,
                "minecraft:sweeping_edge", 3,
                "minecraft:unbreaking", 3,
                "minecraft:mending", 1,
                "minecraft:looting", 3
        ));
        levels.put("evanpack:infested_pickaxe", enchantLevels(
                "minecraft:unbreaking", 3,
                "minecraft:efficiency", 5,
                "minecraft:mending", 1
        ));
        levels.put("evanpack:lions_heart", enchantLevels(
                "minecraft:protection", 4,
                "minecraft:projectile_protection", 4,
                "minecraft:fire_protection", 4,
                "minecraft:blast_protection", 4,
                "minecraft:unbreaking", 3,
                "minecraft:mending", 1
        ));
        levels.put("evanpack:repeater", enchantLevels(
                "minecraft:power", 3,
                "minecraft:piercing", 4,
                "minecraft:quick_charge", 5,
                "minecraft:unbreaking", 5,
                "minecraft:mending", 1
        ));
        levels.put("evanpack:trident_of_poseidon", enchantLevels(
                "minecraft:riptide", 3,
                "minecraft:unbreaking", 3,
                "minecraft:mending", 1,
                "minecraft:impaling", 5
        ));
        levels.put("evanpack:earthsplitter", enchantLevels(
                "minecraft:density", 5,
                "minecraft:breach", 5,
                "minecraft:unbreaking", 5
        ));
        levels.put("evanpack:excalibur", enchantLevels(
                "minecraft:sharpness", 10,
                "minecraft:breach", 3,
                "minecraft:looting", 3,
                "minecraft:sweeping_edge", 3,
                "minecraft:mending", 1,
                "minecraft:unbreaking", 3
        ));
        levels.put("evanpack:emperors_crown", enchantLevels(
                "minecraft:protection", 4,
                "minecraft:aqua_affinity", 1,
                "minecraft:respiration", 3,
                "minecraft:mending", 1,
                "minecraft:unbreaking", 3
        ));
        return levels;
    }

    private static LinkedHashMap<String, Integer> enchantLevels(Object... entries) {
        LinkedHashMap<String, Integer> levels = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            String key = (String) entries[index];
            int value = (Integer) entries[index + 1];
            levels.put(key, value);
        }
        return levels;
    }

    public static final class SmolderingRodSection {
        public float detonationDamage = 30.0F;
        public int primeFireTicks = 40;
        public int maxPrimedTargetsPerPlayer = 16;
        public boolean friendlyFireRules = false;
        public boolean lastOwnerWins = false;

        private void sanitize() {
            detonationDamage = nonNegative(detonationDamage);
            primeFireTicks = nonNegative(primeFireTicks);
            maxPrimedTargetsPerPlayer = Math.max(1, maxPrimedTargetsPerPlayer);
        }
    }

    public static final class InfestedSwordSection {
        public List<StatusEffectEntry> silverfishAuraBuffs = new ArrayList<>(List.of(
                new StatusEffectEntry("minecraft:strength", 40, 2),
                new StatusEffectEntry("minecraft:resistance", 40, 2),
                new StatusEffectEntry("minecraft:regeneration", 40, 0),
                new StatusEffectEntry("minecraft:fire_resistance", 40, 0)
        ));
        public int silverfishAuraRadius = 6;
        public float spawnGuaranteedInfestedChance = 0.0F;
        public boolean enableTargetingCommand = true;
        public boolean enableParticles = true;

        private void sanitize() {
            if (silverfishAuraBuffs == null) {
                silverfishAuraBuffs = new ArrayList<>();
            }
            sanitizeEffects(silverfishAuraBuffs);
            silverfishAuraRadius = nonNegative(silverfishAuraRadius);
            spawnGuaranteedInfestedChance = chance(spawnGuaranteedInfestedChance);
        }
    }

    public static final class InfestedPickaxeSection {
        public boolean enableAuraInfestation = true;

        private void sanitize() {
        }
    }

    public static final class RepeaterSection {
        public int postFiringCooldownTicks = 0;
        public boolean enableParticles = true;

        private void sanitize() {
            postFiringCooldownTicks = nonNegative(postFiringCooldownTicks);
        }
    }

    public static final class BloodSacrificeSection {
        public float healthAfterUse = 1.0F;
        public int itemCooldownTicks = 10 * 60 * 20;
        public BloodGuardianSection summon = new BloodGuardianSection();

        private void sanitize() {
            healthAfterUse = !Float.isFinite(healthAfterUse) ? 1.0F : Math.max(0.1F, healthAfterUse);
            itemCooldownTicks = nonNegative(itemCooldownTicks);
            if (summon == null) {
                summon = new BloodGuardianSection();
            }
            summon.sanitize();
        }
    }

    public static final class BloodGuardianSection {
        public float maxHealth = 50.0F;
        public double movementSpeed = 0.50D;
        public double baseAttackDamage = 12.0D;
        public int lifespanTicks = 20 * 60 * 3;
        public boolean inflictsWither = false;
        public double tetherRadius = 15.0D;
        public boolean targetClosestPlayerOnly = true;
        public double knockbackResistance = 1.0D;
        public BloodGuardianEnchantLevels enchantLevels = new BloodGuardianEnchantLevels();

        private void sanitize() {
            maxHealth = nonNegative(maxHealth);
            movementSpeed = nonNegative(movementSpeed);
            baseAttackDamage = nonNegative(baseAttackDamage);
            lifespanTicks = nonNegative(lifespanTicks);
            tetherRadius = nonNegative(tetherRadius);
            knockbackResistance = Math.clamp(nonNegative(knockbackResistance), 0.0D, 1.0D);
            if (enchantLevels == null) {
                enchantLevels = new BloodGuardianEnchantLevels();
            }
            enchantLevels.sanitize();
        }
    }

    public static final class BloodGuardianEnchantLevels {
        public Armor armor = new Armor();
        public Boots boots = new Boots();
        public Weapon weapon = new Weapon();

        private void sanitize() {
            if (armor == null) {
                armor = new Armor();
            }
            if (boots == null) {
                boots = new Boots();
            }
            if (weapon == null) {
                weapon = new Weapon();
            }
            armor.sanitize();
            boots.sanitize();
            weapon.sanitize();
        }

        public static final class Armor {
            public int protection = 4;
            public int projectileProtection = 4;
            public int fireProtection = 4;
            public int blastProtection = 4;

            private void sanitize() {
                protection = nonNegative(protection);
                projectileProtection = nonNegative(projectileProtection);
                fireProtection = nonNegative(fireProtection);
                blastProtection = nonNegative(blastProtection);
            }
        }

        public static final class Boots {
            public int featherFalling = 4;
            public int frostWalker = 2;
            public int soulSpeed = 3;

            private void sanitize() {
                featherFalling = nonNegative(featherFalling);
                frostWalker = nonNegative(frostWalker);
                soulSpeed = nonNegative(soulSpeed);
            }
        }

        public static final class Weapon {
            public int sharpness = 5;

            private void sanitize() {
                sharpness = nonNegative(sharpness);
            }
        }
    }

    public static final class UndeadWardArmySection {
        public UndeadDeputySection deputies = new UndeadDeputySection();
        public UndeadCommanderSection commanders = new UndeadCommanderSection();
        public UndeadWardenSection warden = new UndeadWardenSection();

        private void sanitize() {
            if (deputies == null) {
                deputies = new UndeadDeputySection();
            }
            if (commanders == null) {
                commanders = new UndeadCommanderSection();
            }
            if (warden == null) {
                warden = new UndeadWardenSection();
            }

            deputies.sanitize();
            commanders.sanitize();
            warden.sanitize();
        }
    }

    public static final class UndeadDeputySection {
        public int summonCount = 10;
        public int summonCooldownTicks = 90 * 20;
        public int lifespanTicks = 60 * 20;
        public double summonRadius = 5.0D;
        public double aggroRadius = 16.0D;
        public double swordAttackDamage = 10.0D;
        public boolean enableSummonParticles = true;
        public boolean enableHitParticles = true;
        public UndeadArmorProfile armor = new UndeadArmorProfile();

        private void sanitize() {
            summonCount = nonNegative(summonCount);
            summonCooldownTicks = nonNegative(summonCooldownTicks);
            lifespanTicks = nonNegative(lifespanTicks);
            summonRadius = nonNegative(summonRadius);
            aggroRadius = nonNegative(aggroRadius);
            swordAttackDamage = nonNegative(swordAttackDamage);
            if (armor == null) {
                armor = new UndeadArmorProfile();
            }
            armor.sanitize();
        }
    }

    public static final class UndeadCommanderSection {
        public int summonCount = 3;
        public int summonCooldownTicks = 3 * 60 * 20;
        public int lifespanTicks = 90 * 20;
        public double summonRadius = 3.0D;
        public double aggroRadius = 16.0D;
        public boolean enableSummonParticles = true;
        public boolean enableHitParticles = true;
        public UndeadArmorProfile armor = new UndeadArmorProfile();
        public List<StatusEffectEntry> arrowHitEffects = new ArrayList<>(List.of(
                new StatusEffectEntry("minecraft:slowness", 80, 1),
                new StatusEffectEntry("minecraft:blindness", 80, 0),
                new StatusEffectEntry("minecraft:weakness", 80, 1),
                new StatusEffectEntry("minecraft:nausea", 80, 1)
        ));

        private void sanitize() {
            summonCount = nonNegative(summonCount);
            summonCooldownTicks = nonNegative(summonCooldownTicks);
            lifespanTicks = nonNegative(lifespanTicks);
            summonRadius = nonNegative(summonRadius);
            aggroRadius = nonNegative(aggroRadius);
            if (armor == null) {
                armor = new UndeadArmorProfile();
            }
            armor.sanitize();
            if (arrowHitEffects == null) {
                arrowHitEffects = new ArrayList<>();
            }
            sanitizeEffects(arrowHitEffects);
        }
    }

    public static final class UndeadWardenSection {
        public int summonCount = 1;
        public int summonCooldownTicks = 10 * 60 * 20;
        public int lifespanTicks = 3 * 60 * 20;
        public double aggroRadius = 16.0D;
        public double summonForwardDistance = 2.5D;
        public double summonSpreadRadius = 1.0D;
        public double movementSpeed = 0.50D;
        public double baseAttackDamage = 12.0D;
        public boolean enableSummonParticles = true;
        public boolean enableHitParticles = true;
        public UndeadArmorProfile armor = new UndeadArmorProfile();

        private void sanitize() {
            summonCount = nonNegative(summonCount);
            summonCooldownTicks = nonNegative(summonCooldownTicks);
            lifespanTicks = nonNegative(lifespanTicks);
            aggroRadius = nonNegative(aggroRadius);
            summonForwardDistance = nonNegative(summonForwardDistance);
            summonSpreadRadius = nonNegative(summonSpreadRadius);
            movementSpeed = nonNegative(movementSpeed);
            baseAttackDamage = nonNegative(baseAttackDamage);
            if (armor == null) {
                armor = new UndeadArmorProfile();
            }
            armor.sanitize();
        }
    }

    public static final class UndeadArmorProfile {
        public int helmetArmorPoints = 5;
        public int chestplateArmorPoints = 9;
        public int leggingsArmorPoints = 7;
        public int bootsArmorPoints = 5;
        public double armorToughnessPerPiece = 4.0D;

        private void sanitize() {
            helmetArmorPoints = nonNegative(helmetArmorPoints);
            chestplateArmorPoints = nonNegative(chestplateArmorPoints);
            leggingsArmorPoints = nonNegative(leggingsArmorPoints);
            bootsArmorPoints = nonNegative(bootsArmorPoints);
            armorToughnessPerPiece = nonNegative(armorToughnessPerPiece);
        }
    }

    public static final class BeaconCoreSection {
        public double anchorProtectionRadius = 75.0D;
        public boolean enableParticles = true;

        private void sanitize() {
            anchorProtectionRadius = nonNegative(anchorProtectionRadius);
        }
    }

    public static final class EarthsplitterSection {
        public boolean enableParticles = true;

        private void sanitize() {
        }
    }

    public static final class ExcaliburSection {
        public double attackDamage = 6.0D;
        public double attackSpeed = -2.4D;
        public boolean enableParticles = true;
        public boolean enableSounds = true;

        private void sanitize() {
            attackDamage = nonNegative(attackDamage);
            if (!Double.isFinite(attackSpeed)) {
                attackSpeed = -2.4D;
            }
        }
    }

    public static final class EmperorsCrownSection {
        public double jumpStrength = 0.18D;
        public int armorValue = 5;
        public float toughness = 0.0F;
        public double movementSpeed = 0.2D;
        public double attackDamage = 3.0D;
        public double entityInteractionRange = 1.5D;
        public double blockInteractionRange = 2.0D;
        public boolean enableParticles = true;

        private void sanitize() {
            jumpStrength = !Double.isFinite(jumpStrength) ? 0.18D : jumpStrength;
            armorValue = nonNegative(armorValue);
            toughness = nonNegative(toughness);
            movementSpeed = !Double.isFinite(movementSpeed) ? 0.2D : movementSpeed;
            attackDamage = !Double.isFinite(attackDamage) ? 3.0D : attackDamage;
            entityInteractionRange = !Double.isFinite(entityInteractionRange) ? 1.5D : entityInteractionRange;
            blockInteractionRange = !Double.isFinite(blockInteractionRange) ? 2.0D : blockInteractionRange;
        }
    }

    public static final class VoidReaverSection {
        public boolean enableParticles = true;

        private void sanitize() {
        }
    }

    public static final class TridentOfPoseidonSection {
        public double beamRange = 6.0D;
        public float beamDamage = 6.0F;
        public int beamDurationTicks = 100;
        public double beamMovementSpeedMultiplierWhileChanneling = 0.0D;
        public boolean allowBeamInRain = true;
        public int beamCooldownTicks = 300;
        public boolean enableParticles = true;
        public List<StatusEffectEntry> holdingEffects = new ArrayList<>(List.of(
                new StatusEffectEntry("minecraft:dolphins_grace", 10, 0),
                new StatusEffectEntry("minecraft:night_vision", 220, 0),
                new StatusEffectEntry("minecraft:conduit_power", 10, 0),
                new StatusEffectEntry("minecraft:water_breathing", 10, 0)
        ));

        private void sanitize() {
            beamRange = nonNegative(beamRange);
            beamDamage = nonNegative(beamDamage);
            beamDurationTicks = Math.max(1, nonNegative(beamDurationTicks));
            beamMovementSpeedMultiplierWhileChanneling = nonNegative(beamMovementSpeedMultiplierWhileChanneling);
            beamCooldownTicks = nonNegative(beamCooldownTicks);
            if (holdingEffects == null) {
                holdingEffects = new ArrayList<>();
            }
            sanitizeEffects(holdingEffects);
        }
    }

    public static final class LionsHeartChestplateSection {
        public LionHeartEnchantLevels enchants = new LionHeartEnchantLevels();
        public double entityInteractionRangeModifierAmount = 2.0D;
        public double blockInteractionRangeModifierAmount = 2.0D;
        public double sizeIncrease = 2.0D;
        public double maxHealthIncrease = 80.0D;
        public int armorLevel = 15;
        public float armorToughness = 5.0F;
        public boolean enableParticles = true;

        private void sanitize() {
            if (enchants == null) {
                enchants = new LionHeartEnchantLevels();
            }
            enchants.sanitize();
            entityInteractionRangeModifierAmount = !Double.isFinite(entityInteractionRangeModifierAmount) ? 2.0D : entityInteractionRangeModifierAmount;
            blockInteractionRangeModifierAmount = !Double.isFinite(blockInteractionRangeModifierAmount) ? 2.0D : blockInteractionRangeModifierAmount;
            sizeIncrease = nonNegative(sizeIncrease);
            maxHealthIncrease = nonNegative(maxHealthIncrease);
            armorLevel = nonNegative(armorLevel);
            armorToughness = nonNegative(armorToughness);
        }
    }

    public static final class ArtifactEnchantSection {
        public boolean enforceExact = true;
        public LinkedHashMap<String, LinkedHashMap<String, Integer>> levels = defaultArtifactEnchantLevels();

        private void sanitize() {
            if (levels == null) {
                levels = defaultArtifactEnchantLevels();
                return;
            }

            Iterator<Map.Entry<String, LinkedHashMap<String, Integer>>> itemIterator = levels.entrySet().iterator();
            while (itemIterator.hasNext()) {
                Map.Entry<String, LinkedHashMap<String, Integer>> itemEntry = itemIterator.next();
                String itemId = itemEntry.getKey();
                if (itemId == null || itemId.isBlank()) {
                    itemIterator.remove();
                    continue;
                }

                LinkedHashMap<String, Integer> itemEnchants = itemEntry.getValue();
                if (itemEnchants == null) {
                    itemEntry.setValue(new LinkedHashMap<>());
                    continue;
                }

                Iterator<Map.Entry<String, Integer>> enchantIterator = itemEnchants.entrySet().iterator();
                while (enchantIterator.hasNext()) {
                    Map.Entry<String, Integer> enchantEntry = enchantIterator.next();
                    String enchantId = enchantEntry.getKey();
                    if (enchantId == null || enchantId.isBlank()) {
                        enchantIterator.remove();
                        continue;
                    }

                    Integer level = enchantEntry.getValue();
                    enchantEntry.setValue(nonNegative(level == null ? 0 : level));
                }
            }
        }
    }

    public static final class LionHeartEnchantLevels {
        public int protection = 4;
        public int projectileProtection = 4;
        public int fireProtection = 4;
        public int blastProtection = 4;
        public int unbreaking = 3;
        public int mending = 1;

        private void sanitize() {
            protection = nonNegative(protection);
            projectileProtection = nonNegative(projectileProtection);
            fireProtection = nonNegative(fireProtection);
            blastProtection = nonNegative(blastProtection);
            unbreaking = nonNegative(unbreaking);
            mending = nonNegative(mending);
        }
    }

    public static final class StatusEffectEntry {
        public String effectId = "minecraft:speed";
        public int durationTicks = 20;
        public int amplifier = 0;

        public StatusEffectEntry() {
        }

        public StatusEffectEntry(String effectId, int durationTicks, int amplifier) {
            this.effectId = effectId;
            this.durationTicks = durationTicks;
            this.amplifier = amplifier;
        }
    }
}
