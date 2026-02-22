package net.fourthwall.artifacts.item;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;

public class InfestedPickaxeItem extends Item {
    private static final float BASE_MINING_SPEED = 26.0F;

    public InfestedPickaxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
            return BASE_MINING_SPEED;
        }
        return super.getMiningSpeed(stack, state);
    }
}
