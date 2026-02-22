package net.fourthwall.artifacts.item;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class InfestedPickaxeItem extends Item implements PolymerFallbackItem {
    private static final float BASE_MINING_SPEED = 26.0F;

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
}
