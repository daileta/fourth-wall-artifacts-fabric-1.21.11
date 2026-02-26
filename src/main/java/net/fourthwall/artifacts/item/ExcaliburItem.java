package net.fourthwall.artifacts.item;

import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.text.Text;

public class ExcaliburItem extends Item implements PolymerFallbackItem {
    private static final int SHARPNESS_LEVEL = 10;
    private static final int BREACH_LEVEL = 3;
    private static final int LOOTING_LEVEL = 3;
    private static final int SWEEPING_EDGE_LEVEL = 3;
    private static final int MENDING_LEVEL = 1;
    private static final int UNBREAKING_LEVEL = 3;

    public ExcaliburItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.IRON_SWORD;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("E").styled(s -> s.withColor(0xE81CFF).withBold(true)))
            .append(Text.literal("x").styled(s -> s.withColor(0xE21EFB).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0xDD1FF7).withBold(true)))
            .append(Text.literal("c").styled(s -> s.withColor(0xD721F2).withBold(true)))
            .append(Text.literal("l").styled(s -> s.withColor(0xD223EE).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0xCC24EA).withBold(true)))
            .append(Text.literal("b").styled(s -> s.withColor(0xC726E6).withBold(true)))
            .append(Text.literal("u").styled(s -> s.withColor(0xC128E1).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xB62BD9).withBold(true)));
    }

    public static AttributeModifiersComponent createExcaliburAttributeModifiers() {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, 6.0D, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED, new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, -2.4D, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        return ensureEnchantments(stack, world);
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        var breach = enchantments.getOrThrow(Enchantments.BREACH);
        var looting = enchantments.getOrThrow(Enchantments.LOOTING);
        var sweepingEdge = enchantments.getOrThrow(Enchantments.SWEEPING_EDGE);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        boolean changed = false;

        if (EnchantmentHelper.getLevel(sharpness, stack) < SHARPNESS_LEVEL) {
            stack.addEnchantment(sharpness, SHARPNESS_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(breach, stack) < BREACH_LEVEL) {
            stack.addEnchantment(breach, BREACH_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(looting, stack) < LOOTING_LEVEL) {
            stack.addEnchantment(looting, LOOTING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(sweepingEdge, stack) < SWEEPING_EDGE_LEVEL) {
            stack.addEnchantment(sweepingEdge, SWEEPING_EDGE_LEVEL);
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
}