package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.FourthWallArtifacts;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public class EmperorsCrownItem extends Item implements PolymerFallbackItem {
    private static final int PROTECTION_LEVEL = 4;
    private static final int AQUA_AFFINITY_LEVEL = 1;
    private static final int RESPIRATION_LEVEL = 3;
    private static final int MENDING_LEVEL = 1;
    private static final int UNBREAKING_LEVEL = 3;
    private static final String CUSTOM_MODEL_TAG = "Alexandria's Artifact";

    public EmperorsCrownItem(Settings settings) {
        super(settings);
    }

    public static Item.Settings applyHelmetSettings(Item.Settings settings) {
        ArmorMaterial material = createCrownMaterial();
        return settings
                .maxCount(1)
                .armor(material, EquipmentType.HELMET)
                .attributeModifiers(createCrownAttributeModifiers(material));
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.GOLDEN_HELMET;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
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

        AttributeModifiersComponent desiredAttributes = createCrownAttributeModifiers(createCrownMaterial());
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

        return ensureEnchantments(stack, world) || changed;
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var protection = enchantments.getOrThrow(Enchantments.PROTECTION);
        var aquaAffinity = enchantments.getOrThrow(Enchantments.AQUA_AFFINITY);
        var respiration = enchantments.getOrThrow(Enchantments.RESPIRATION);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        boolean changed = false;

        if (EnchantmentHelper.getLevel(protection, stack) < PROTECTION_LEVEL) {
            stack.addEnchantment(protection, PROTECTION_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(aquaAffinity, stack) < AQUA_AFFINITY_LEVEL) {
            stack.addEnchantment(aquaAffinity, AQUA_AFFINITY_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(respiration, stack) < RESPIRATION_LEVEL) {
            stack.addEnchantment(respiration, RESPIRATION_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(mending, stack) < MENDING_LEVEL) {
            stack.addEnchantment(mending, MENDING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(unbreaking, stack) < UNBREAKING_LEVEL) {
            stack.addEnchantment(unbreaking, UNBREAKING_LEVEL);
            changed = true;
        }

        return changed;
    }

    private static ArmorMaterial createCrownMaterial() {
        ArmorMaterial gold = ArmorMaterials.GOLD;
        EnumMap<EquipmentType, Integer> defense = new EnumMap<>(gold.defense());
        defense.put(EquipmentType.HELMET, 5);

        return new ArmorMaterial(
                gold.durability(),
                defense,
                gold.enchantmentValue(),
                gold.equipSound(),
                gold.toughness(),
                gold.knockbackResistance(),
                gold.repairIngredient(),
                gold.assetId()
        );
    }

    private static AttributeModifiersComponent createCrownAttributeModifiers(ArmorMaterial material) {
        return material.createAttributeModifiers(EquipmentType.HELMET)
                .with(
                        EntityAttributes.MOVEMENT_SPEED,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_movement_speed"),
                                0.2D,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                )
                .with(
                        EntityAttributes.JUMP_STRENGTH,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_jump_strength"),
                                0.18D,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                )
                .with(
                        EntityAttributes.ATTACK_DAMAGE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_attack_damage"),
                                3.0D,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                )
                .with(
                        EntityAttributes.ENTITY_INTERACTION_RANGE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_entity_interaction_range"),
                                1.5D,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.HEAD
                )
                .with(
                        EntityAttributes.BLOCK_INTERACTION_RANGE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("emperors_crown_block_interaction_range"),
                                2.0D,
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
}
