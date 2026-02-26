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
            .append(Text.literal("B").styled(s -> s.withColor(0x47BCFF).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x6CC9FF).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0x91D7FF).withBold(true)))
            .append(Text.literal("c").styled(s -> s.withColor(0xB5E4FF).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xDAF2FF).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0xFFFFFF).withBold(true)))
            .append(Text.literal(" ").styled(s -> s.withColor(0xFFFFFF).withBold(true)))
            .append(Text.literal("C").styled(s -> s.withColor(0xFFFDDC).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xFFFABA).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xFFF897).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xFFF352).withBold(true)));
    }
}
