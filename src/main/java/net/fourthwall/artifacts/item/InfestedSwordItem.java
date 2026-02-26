package net.fourthwall.artifacts.item;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class InfestedSwordItem extends Item implements PolymerFallbackItem {
    private static final int SHARPNESS_LEVEL = 5;
    private static final int FIRE_ASPECT_LEVEL = 2;
    private static final int SWEEPING_EDGE_LEVEL = 3;
    private static final int UNBREAKING_LEVEL = 3;
    private static final int MENDING_LEVEL = 1;
    private static final int LOOTING_LEVEL = 3;

    public InfestedSwordItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.NETHERITE_SWORD;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        return ensureEnchantments(stack, world);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("I").styled(s -> s.withColor(0xA69FA3).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0x9F9A9D).withBold(true)))
            .append(Text.literal("f").styled(s -> s.withColor(0x999496).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x928F90).withBold(true)))
            .append(Text.literal("s").styled(s -> s.withColor(0x8C8989).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0x858483).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x7E7F7C).withBold(true)))
            .append(Text.literal("d ").styled(s -> s.withColor(0x787976).withBold(true)))
            .append(Text.literal("S").styled(s -> s.withColor(0x71746F).withBold(true)))
            .append(Text.literal("w").styled(s -> s.withColor(0x6A6F69).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0x646962).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0x5D645C).withBold(true)))
            .append(Text.literal("d").styled(s -> s.withColor(0x50594F).withBold(true)));
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        var fireAspect = enchantments.getOrThrow(Enchantments.FIRE_ASPECT);
        var sweepingEdge = enchantments.getOrThrow(Enchantments.SWEEPING_EDGE);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);
        var looting = enchantments.getOrThrow(Enchantments.LOOTING);
        boolean changed = false;

        if (EnchantmentHelper.getLevel(sharpness, stack) < SHARPNESS_LEVEL) {
            stack.addEnchantment(sharpness, SHARPNESS_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(fireAspect, stack) < FIRE_ASPECT_LEVEL) {
            stack.addEnchantment(fireAspect, FIRE_ASPECT_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(sweepingEdge, stack) < SWEEPING_EDGE_LEVEL) {
            stack.addEnchantment(sweepingEdge, SWEEPING_EDGE_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(unbreaking, stack) < UNBREAKING_LEVEL) {
            stack.addEnchantment(unbreaking, UNBREAKING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(mending, stack) < MENDING_LEVEL) {
            stack.addEnchantment(mending, MENDING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(looting, stack) < LOOTING_LEVEL) {
            stack.addEnchantment(looting, LOOTING_LEVEL);
            changed = true;
        }
        return changed;
    }
}
