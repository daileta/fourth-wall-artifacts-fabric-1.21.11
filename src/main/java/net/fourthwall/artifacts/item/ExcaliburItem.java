package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.config.ArtifactsConfig;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.util.Formatting;
import java.util.Objects;
import java.util.List;

public class ExcaliburItem extends Item implements PolymerFallbackItem {
    private static final double DEFAULT_ATTACK_DAMAGE = 6.0D;
    private static final double DEFAULT_ATTACK_SPEED = -2.4D;

    public ExcaliburItem(Item.Settings settings) {
        super(settings.component(DataComponentTypes.LORE, createLore()));
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.IRON_SWORD;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("E").styled(s -> s.withColor(0xFFFFFF).withBold(true)))
            .append(Text.literal("x").styled(s -> s.withColor(0xD6F0FF).withBold(true)))
            .append(Text.literal("c").styled(s -> s.withColor(0xADE1FF).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0x84D2FF).withBold(true)))
            .append(Text.literal("l").styled(s -> s.withColor(0x5BC3FF).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0x5BC2EC).withBold(true)))
            .append(Text.literal("b").styled(s -> s.withColor(0x84CEC5).withBold(true)))
            .append(Text.literal("u").styled(s -> s.withColor(0xADDB9F).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xFFF352).withBold(true)));
    }

    public static AttributeModifiersComponent createExcaliburAttributeModifiers() {
        return createExcaliburAttributeModifiers(DEFAULT_ATTACK_DAMAGE, DEFAULT_ATTACK_SPEED);
    }

    private static AttributeModifiersComponent createExcaliburAttributeModifiers(double attackDamage, double attackSpeed) {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, attackDamage, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED, new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        boolean changed = false;

        AttributeModifiersComponent desiredAttributes = createExcaliburAttributeModifiers(cfg().attackDamage, cfg().attackSpeed);
        AttributeModifiersComponent currentAttributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (!Objects.equals(desiredAttributes, currentAttributes)) {
            stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, desiredAttributes);
            changed = true;
        }

        LoreComponent desiredLore = createLore();
        LoreComponent currentLore = stack.get(DataComponentTypes.LORE);

        if (!Objects.equals(desiredLore, currentLore)) {
            stack.set(DataComponentTypes.LORE, desiredLore);
            changed = true;
        }

        return ArtifactEnchantments.refreshConfiguredStack(stack, world) || changed;
    }

    private static LoreComponent createLore() {
    return new LoreComponent(List.of(
        Text.translatable("The Holy Sword, forged by light and earth itself to act as a beacon of truth.")
            .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

        Text.translatable("Its presence is the only shining light in the world of darkness.")
            .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)
        ));
    }

    private static ArtifactsConfig.ExcaliburSection cfg() {
        return ArtifactsConfigManager.get().excalibur;
    }
}
