package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.config.ArtifactsConfig;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.fourthwall.artifacts.integration.EmptyEmbraceArtifactSuppression;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public class EmperorsCrownItem extends Item implements PolymerFallbackItem {
    private static final int DEFAULT_ARMOR_VALUE = 5;
    private static final float DEFAULT_TOUGHNESS = 0.0F;
    private static final double DEFAULT_MOVEMENT_SPEED = 0.2D;
    private static final double DEFAULT_JUMP_STRENGTH = 0.18D;
    private static final double DEFAULT_ATTACK_DAMAGE = 3.0D;
    private static final double DEFAULT_ENTITY_INTERACTION_RANGE = 1.5D;
    private static final double DEFAULT_BLOCK_INTERACTION_RANGE = 2.0D;
    private static final String CUSTOM_MODEL_TAG = "Alexandria's Artifact";

    public EmperorsCrownItem(Settings settings) {
        super(settings);
    }

    public static Item.Settings applyHelmetSettings(Item.Settings settings) {
        ArmorMaterial material = createCrownMaterial(DEFAULT_ARMOR_VALUE, DEFAULT_TOUGHNESS);
        return settings
                .maxCount(1)
                .armor(material, EquipmentType.HELMET)
                .attributeModifiers(createCrownAttributeModifiers(
                        material,
                        DEFAULT_MOVEMENT_SPEED,
                        DEFAULT_JUMP_STRENGTH,
                        DEFAULT_ATTACK_DAMAGE,
                        DEFAULT_ENTITY_INTERACTION_RANGE,
                        DEFAULT_BLOCK_INTERACTION_RANGE
                ));
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.GOLDEN_HELMET;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        if (entity instanceof PlayerEntity player
                && slot == EquipmentSlot.HEAD
                && EmptyEmbraceArtifactSuppression.isArtifactArmorSuppressed(player)) {
            refreshSuppressedStack(stack, Items.GOLDEN_HELMET.getDefaultStack());
            return;
        }

        refreshConfiguredStack(stack, world);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("T").styled(s -> s.withColor(0xFFB914).withBold(true)))
            .append(Text.literal("h").styled(s -> s.withColor(0xFFBC17).withBold(true)))
            .append(Text.literal("e ").styled(s -> s.withColor(0xFFC019).withBold(true)))
            .append(Text.literal("E").styled(s -> s.withColor(0xFFC31C).withBold(true)))
            .append(Text.literal("m").styled(s -> s.withColor(0xFFC61F).withBold(true)))
            .append(Text.literal("p").styled(s -> s.withColor(0xFFCA22).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xFFCD24).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xFFD027).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xFFD42A).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xFFD72C).withBold(true)))
            .append(Text.literal("'").styled(s -> s.withColor(0xFFDB2F).withBold(true)))
            .append(Text.literal("s ").styled(s -> s.withColor(0xFFDE32).withBold(true)))
            .append(Text.literal("C").styled(s -> s.withColor(0xFFE134).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xFFE537).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xFFE83A).withBold(true)))
            .append(Text.literal("w").styled(s -> s.withColor(0xFFEB3D).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0xFFF242).withBold(true)));
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        boolean changed = false;
        changed |= ensureTrim(stack, world);

        ArtifactsConfig.EmperorsCrownSection config = cfg();
        AttributeModifiersComponent desiredAttributes = createCrownAttributeModifiers(
                createCrownMaterial(config.armorValue, config.toughness),
                config.movementSpeed,
                config.jumpStrength,
                config.attackDamage,
                config.entityInteractionRange,
                config.blockInteractionRange
        );
        AttributeModifiersComponent currentAttributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (!desiredAttributes.equals(currentAttributes)) {
            stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, desiredAttributes);
            changed = true;
        }

        if (!stack.contains(DataComponentTypes.UNBREAKABLE)) {
            stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
            changed = true;
        }
        
        LoreComponent desiredLore = createLore();
        LoreComponent currentLore = stack.get(DataComponentTypes.LORE);
        if (!Objects.equals(desiredLore, currentLore)) {
            stack.set(DataComponentTypes.LORE, desiredLore);
            changed = true;
        }

        CustomModelDataComponent desiredCustomModelData = new CustomModelDataComponent(List.of(), List.of(), List.of(CUSTOM_MODEL_TAG), List.of());
        CustomModelDataComponent currentCustomModelData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (!Objects.equals(desiredCustomModelData, currentCustomModelData)) {
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, desiredCustomModelData);
            changed = true;
        }

        return ArtifactEnchantments.refreshConfiguredStack(stack, world) || changed;
    }

    private static boolean refreshSuppressedStack(ItemStack stack, ItemStack fallbackStack) {
        boolean changed = false;

        AttributeModifiersComponent fallbackAttributes = fallbackStack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        AttributeModifiersComponent currentAttributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (!Objects.equals(fallbackAttributes, currentAttributes)) {
            if (fallbackAttributes == null) {
                stack.remove(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            } else {
                stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, fallbackAttributes);
            }
            changed = true;
        }

        var fallbackEnchantments = fallbackStack.get(DataComponentTypes.ENCHANTMENTS);
        var currentEnchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (!Objects.equals(fallbackEnchantments, currentEnchantments)) {
            if (fallbackEnchantments == null) {
                stack.remove(DataComponentTypes.ENCHANTMENTS);
            } else {
                stack.set(DataComponentTypes.ENCHANTMENTS, fallbackEnchantments);
            }
            changed = true;
        }

        return changed;
    }

    private static ArmorMaterial createCrownMaterial(int helmetArmorValue, float toughness) {
        ArmorMaterial gold = ArmorMaterials.GOLD;
        EnumMap<EquipmentType, Integer> defense = new EnumMap<>(gold.defense());
        defense.put(EquipmentType.HELMET, helmetArmorValue);

        return new ArmorMaterial(
                gold.durability(),
                defense,
                gold.enchantmentValue(),
                gold.equipSound(),
                toughness,
                gold.knockbackResistance(),
                gold.repairIngredient(),
                RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, FourthWallArtifacts.id("emperors_crown"))

        );
    }

    private static AttributeModifiersComponent createCrownAttributeModifiers(
            ArmorMaterial material,
            double movementSpeed,
            double jumpStrength,
            double attackDamage,
            double entityInteractionRange,
            double blockInteractionRange
    ) {
        return material.createAttributeModifiers(EquipmentType.HELMET)
                .with(
                        EntityAttributes.MOVEMENT_SPEED,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_movement_speed"),
                                movementSpeed,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                )
                .with(
                        EntityAttributes.JUMP_STRENGTH,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_jump_strength"),
                                jumpStrength,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                )
                .with(
                        EntityAttributes.ATTACK_DAMAGE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_attack_damage"),
                                attackDamage,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                )
                .with(
                        EntityAttributes.ENTITY_INTERACTION_RANGE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_entity_interaction_range"),
                                entityInteractionRange,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                )
                .with(
                        EntityAttributes.BLOCK_INTERACTION_RANGE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_block_interaction_range"),
                                blockInteractionRange,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                );
    }

    private static LoreComponent createLore() {
        return new LoreComponent(List.of(
                Text.literal("Formerly Emperor Dominus' Crown, it was passed down"),
                Text.literal("to the previous Empress of Alexandria, Anastasia,"),
                Text.literal("after her father's death")
        ));
    }

    private static boolean ensureTrim(ItemStack stack, ServerWorld world) {
        var patterns = world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_PATTERN);
        var materials = world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_MATERIAL);

        var patternKey = net.minecraft.registry.RegistryKey.of(RegistryKeys.TRIM_PATTERN, Identifier.of("evanpack", "rib"));
        var materialKey = net.minecraft.registry.RegistryKey.of(RegistryKeys.TRIM_MATERIAL, Identifier.of("minecraft", "gold"));

        var patternEntry = patterns.getOptional(patternKey);
        var materialEntry = materials.getOptional(materialKey);

        if (patternEntry.isEmpty() || materialEntry.isEmpty()) {
            return false;
        }

        var trim = new ArmorTrim(materialEntry.get(), patternEntry.get());
        var current = stack.get(DataComponentTypes.TRIM);
        if (!trim.equals(current)) {
            stack.set(DataComponentTypes.TRIM, trim);
            return true;
        }
        return false;
    }

    private static ArtifactsConfig.EmperorsCrownSection cfg() {
        return ArtifactsConfigManager.get().emperorsCrown;
    }
}
