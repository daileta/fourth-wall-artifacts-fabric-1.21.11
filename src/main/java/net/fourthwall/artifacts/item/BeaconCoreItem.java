package net.fourthwall.artifacts.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class BeaconCoreItem extends Item implements PolymerFallbackItem {
    public BeaconCoreItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.NETHER_STAR;
    }
}
