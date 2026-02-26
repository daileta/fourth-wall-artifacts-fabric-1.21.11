package net.fourthwall.artifacts.item;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class InfestedPickaxeItem extends Item implements PolymerFallbackItem {
    private static final float BASE_MINING_SPEED = 26.0F;
    private static final int UNBREAKING_LEVEL = 3;
    private static final int EFFICIENCY_LEVEL = 5;
    private static final int MENDING_LEVEL = 1;

    public InfestedPickaxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.NETHERITE_PICKAXE;
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
            return BASE_MINING_SPEED;
        }
        return super.getMiningSpeed(stack, state);
    }

    @Override
    public boolean handleMiningOnServer(ItemStack tool, BlockState targetBlock, BlockPos pos, ServerPlayerEntity player) {
        return true;
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
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        var efficiency = enchantments.getOrThrow(Enchantments.EFFICIENCY);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);
        boolean changed = false;

        if (EnchantmentHelper.getLevel(unbreaking, stack) < UNBREAKING_LEVEL) {
            stack.addEnchantment(unbreaking, UNBREAKING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(efficiency, stack) < EFFICIENCY_LEVEL) {
            stack.addEnchantment(efficiency, EFFICIENCY_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(mending, stack) < MENDING_LEVEL) {
            stack.addEnchantment(mending, MENDING_LEVEL);
            changed = true;
        }
        return changed;
    }
}
