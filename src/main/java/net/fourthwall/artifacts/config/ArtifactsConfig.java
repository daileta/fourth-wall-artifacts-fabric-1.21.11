package net.fourthwall.artifacts.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class ArtifactsConfig {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public SmolderingRodSection smolderingRod = new SmolderingRodSection();
    public InfestedSwordSection infestedSword = new InfestedSwordSection();
    public InfestedPickaxeSection infestedPickaxe = new InfestedPickaxeSection();
    public RepeaterSection repeater = new RepeaterSection();
    public BloodSacrificeSection bloodSacrifice = new BloodSacrificeSection();
    public BeaconCoreSection beaconCore = new BeaconCoreSection();
    public TridentOfPoseidonSection tridentOfPoseidon = new TridentOfPoseidonSection();
    public LionsHeartChestplateSection lionsHeartChestplate = new LionsHeartChestplateSection();

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
        if (beaconCore == null) {
            beaconCore = new BeaconCoreSection();
        }
        if (tridentOfPoseidon == null) {
            tridentOfPoseidon = new TridentOfPoseidonSection();
        }
        if (lionsHeartChestplate == null) {
            lionsHeartChestplate = new LionsHeartChestplateSection();
        }

        smolderingRod.sanitize();
        infestedSword.sanitize();
        infestedPickaxe.sanitize();
        repeater.sanitize();
        bloodSacrifice.sanitize();
        beaconCore.sanitize();
        tridentOfPoseidon.sanitize();
        lionsHeartChestplate.sanitize();
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
        root.add("beaconCore", buildBeaconCoreJson(this.beaconCore, defaults.beaconCore));
        root.add("tridentOfPoseidon", buildTridentJson(this.tridentOfPoseidon, defaults.tridentOfPoseidon));
        root.add("lionHeartChestplate", buildLionHeartJson(this.lionsHeartChestplate, defaults.lionsHeartChestplate));
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

    private static JsonObject buildBeaconCoreJson(BeaconCoreSection current, BeaconCoreSection defaults) {
        JsonObject obj = new JsonObject();
        addDoc(obj, "anchorProtectionRadius", current.anchorProtectionRadius, defaults.anchorProtectionRadius);
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
        obj.addProperty("interactionRange_note", "These are raw attribute modifier amounts using ADD_MULTIPLIED_TOTAL (2.0 = +200% range).");
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
        public int lifespanTicks = 0;
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

    public static final class BeaconCoreSection {
        public double anchorProtectionRadius = 75.0D;

        private void sanitize() {
            anchorProtectionRadius = nonNegative(anchorProtectionRadius);
        }
    }

    public static final class TridentOfPoseidonSection {
        public double beamRange = 6.0D;
        public float beamDamage = 6.0F;
        public int beamDurationTicks = 100;
        public double beamMovementSpeedMultiplierWhileChanneling = 0.0D;
        public boolean allowBeamInRain = true;
        public int beamCooldownTicks = 300;
        public List<StatusEffectEntry> holdingEffects = new ArrayList<>(List.of(
                new StatusEffectEntry("minecraft:dolphins_grace", 10, 0),
                new StatusEffectEntry("minecraft:night_vision", 50, 0),
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
