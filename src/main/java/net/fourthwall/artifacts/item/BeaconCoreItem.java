package net.fourthwall.artifacts.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class BeaconCoreItem extends Item implements PolymerFallbackItem {
    public BeaconCoreItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.NETHER_STAR;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("B").styled(s -> s.withColor(0xF9B6DC).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xEFBAE3).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0xE4BEEA).withBold(true)))
            .append(Text.literal("c").styled(s -> s.withColor(0xDAC2F1).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xD2BDF3).withBold(true)))
            .append(Text.literal("n ").styled(s -> s.withColor(0xCCB4F3).withBold(true)))
            .append(Text.literal("C").styled(s -> s.withColor(0xC6AAF2).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xBBA5F3).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xA6A9F6).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x7CB1FB).withBold(true)));
    }
}
