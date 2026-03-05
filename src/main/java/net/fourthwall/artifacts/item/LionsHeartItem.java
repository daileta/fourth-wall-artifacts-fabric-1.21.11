package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.config.ArtifactsConfig;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
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
import net.minecraft.util.Formatting;
import java.util.List;
import java.util.Objects;
import java.util.EnumMap;

public class LionsHeartItem extends Item implements PolymerFallbackItem {
    public LionsHeartItem(Settings settings) {
        super(settings.component(DataComponentTypes.LORE, createLore()));
    }

    public static Item.Settings applyChestplateSettings(Item.Settings settings) {
        ArmorMaterial material = createLionsHeartMaterial();
        return settings
                .maxCount(1)
                .armor(material, EquipmentType.CHESTPLATE)
                .attributeModifiers(createLionsHeartAttributeModifiers(material))
                .component(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT.with(DataComponentTypes.ATTRIBUTE_MODIFIERS, true))
                .fireproof();
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.NETHERITE_CHESTPLATE;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("T").styled(s -> s.withColor(0xC69452).withBold(true)))
            .append(Text.literal("h").styled(s -> s.withColor(0xC99A5A).withBold(true)))
            .append(Text.literal("e ").styled(s -> s.withColor(0xCBA062).withBold(true)))
            .append(Text.literal("L").styled(s -> s.withColor(0xCEA76B).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0xD1AD73).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xD3B37B).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0xD6B983).withBold(true)))
            .append(Text.literal("'").styled(s -> s.withColor(0xD9C08C).withBold(true)))
            .append(Text.literal("s ").styled(s -> s.withColor(0xDECC9C).withBold(true)))
            .append(Text.literal("H").styled(s -> s.withColor(0xDE5252).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xD94747).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0xD43C3C).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xD03030).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0xC61A1A).withBold(true)));
    }

    private static ArmorMaterial createLionsHeartMaterial() {
        ArtifactsConfig.LionsHeartChestplateSection config = cfg();
        ArmorMaterial netherite = ArmorMaterials.NETHERITE;
        EnumMap<EquipmentType, Integer> defense = new EnumMap<>(netherite.defense());
        defense.put(EquipmentType.CHESTPLATE, config.armorLevel);

        return new ArmorMaterial(
                netherite.durability(),
                defense,
                netherite.enchantmentValue(),
                netherite.equipSound(),
                config.armorToughness,
                1.0F,
                netherite.repairIngredient(),
                netherite.assetId()
        );
    }

    private static AttributeModifiersComponent createLionsHeartAttributeModifiers(ArmorMaterial material) {
        ArtifactsConfig.LionsHeartChestplateSection config = cfg();
        return material.createAttributeModifiers(EquipmentType.CHESTPLATE)
                .with(
                        EntityAttributes.MAX_HEALTH,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("lions_heart_max_health"),
                                config.maxHealthIncrease,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.CHEST
                )
                .with(
                        EntityAttributes.SCALE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("lions_heart_scale"),
                                config.sizeIncrease,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.CHEST
                )
                .with(
                        EntityAttributes.BLOCK_INTERACTION_RANGE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("lions_heart_block_interaction_range"),
                                config.blockInteractionRangeModifierAmount,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        ),
                        AttributeModifierSlot.CHEST
                )
                .with(
                        EntityAttributes.ENTITY_INTERACTION_RANGE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("lions_heart_entity_interaction_range"),
                                config.entityInteractionRangeModifierAmount,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        ),
                        AttributeModifierSlot.CHEST
                )
                .with(
                        EntityAttributes.EXPLOSION_KNOCKBACK_RESISTANCE,
                        new EntityAttributeModifier(
                                FourthWallArtifacts.id("lions_heart_explosion_knockback_resistance"),
                                1.0D,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        AttributeModifierSlot.CHEST
                );
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        boolean changed = false;

        AttributeModifiersComponent desired = createLionsHeartAttributeModifiers(createLionsHeartMaterial());
        AttributeModifiersComponent existing = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (!desired.equals(existing)) {
            stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, desired);
            changed = true;
        }

        TooltipDisplayComponent tooltipDisplay = stack.getOrDefault(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT);
        TooltipDisplayComponent desiredTooltipDisplay = tooltipDisplay.with(DataComponentTypes.ATTRIBUTE_MODIFIERS, true);
        if (!desiredTooltipDisplay.equals(tooltipDisplay)) {
            stack.set(DataComponentTypes.TOOLTIP_DISPLAY, desiredTooltipDisplay);
            changed = true;
        }

        LoreComponent desiredLore = createLore();
        LoreComponent currentLore = stack.get(DataComponentTypes.LORE);

        if (!Objects.equals(desiredLore, currentLore)) {
            stack.set(DataComponentTypes.LORE, desiredLore);
            changed = true;
        }

        return ensureEnchantments(stack, world) || changed;
    }

    private static LoreComponent createLore() {
        return new LoreComponent(List.of(
            Text.translatable("A towering plate of dark alloy that grows heavier with every act of heroism.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

            Text.translatable("Only a man desperate to be enough would have fed himself to this enchantment.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)
        ));
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        ArtifactsConfig.LionHeartEnchantLevels config = cfg().enchants;
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var protection = enchantments.getOrThrow(Enchantments.PROTECTION);
        var projectileProtection = enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION);
        var fireProtection = enchantments.getOrThrow(Enchantments.FIRE_PROTECTION);
        var blastProtection = enchantments.getOrThrow(Enchantments.BLAST_PROTECTION);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);

        boolean changed = false;
        if (EnchantmentHelper.getLevel(protection, stack) < config.protection) {
            stack.addEnchantment(protection, config.protection);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(projectileProtection, stack) < config.projectileProtection) {
            stack.addEnchantment(projectileProtection, config.projectileProtection);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(fireProtection, stack) < config.fireProtection) {
            stack.addEnchantment(fireProtection, config.fireProtection);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(blastProtection, stack) < config.blastProtection) {
            stack.addEnchantment(blastProtection, config.blastProtection);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(unbreaking, stack) < config.unbreaking) {
            stack.addEnchantment(unbreaking, config.unbreaking);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(mending, stack) < config.mending) {
            stack.addEnchantment(mending, config.mending);
            changed = true;
        }
        return changed;
    }

    private static ArtifactsConfig.LionsHeartChestplateSection cfg() {
        return ArtifactsConfigManager.get().lionsHeartChestplate;
    }
}
