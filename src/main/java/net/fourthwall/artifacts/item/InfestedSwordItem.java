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
        ensureEnchantments(stack, world);
    }

    private static void ensureEnchantments(ItemStack stack, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        var fireAspect = enchantments.getOrThrow(Enchantments.FIRE_ASPECT);
        var sweepingEdge = enchantments.getOrThrow(Enchantments.SWEEPING_EDGE);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);
        var looting = enchantments.getOrThrow(Enchantments.LOOTING);

        if (EnchantmentHelper.getLevel(sharpness, stack) < SHARPNESS_LEVEL) {
            stack.addEnchantment(sharpness, SHARPNESS_LEVEL);
        }
        if (EnchantmentHelper.getLevel(fireAspect, stack) < FIRE_ASPECT_LEVEL) {
            stack.addEnchantment(fireAspect, FIRE_ASPECT_LEVEL);
        }
        if (EnchantmentHelper.getLevel(sweepingEdge, stack) < SWEEPING_EDGE_LEVEL) {
            stack.addEnchantment(sweepingEdge, SWEEPING_EDGE_LEVEL);
        }
        if (EnchantmentHelper.getLevel(unbreaking, stack) < UNBREAKING_LEVEL) {
            stack.addEnchantment(unbreaking, UNBREAKING_LEVEL);
        }
        if (EnchantmentHelper.getLevel(mending, stack) < MENDING_LEVEL) {
            stack.addEnchantment(mending, MENDING_LEVEL);
        }
        if (EnchantmentHelper.getLevel(looting, stack) < LOOTING_LEVEL) {
            stack.addEnchantment(looting, LOOTING_LEVEL);
        }
    }
}
