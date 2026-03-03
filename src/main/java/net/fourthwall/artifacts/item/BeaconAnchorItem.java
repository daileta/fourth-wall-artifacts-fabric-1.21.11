package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.beacon.BeaconCoreManager;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.util.Formatting;
import java.util.List;

public class BeaconAnchorItem extends BlockItem implements PolymerFallbackItem {
    public BeaconAnchorItem(Block block, Settings settings) {
        super(block, settings.component(DataComponentTypes.LORE, createLore()));
    }

    @Override
    public net.minecraft.item.Item getFallbackItem(ItemStack stack) {
        return Items.BEACON;
    }

    @Override
    public ActionResult place(ItemPlacementContext context) {
        ActionResult result = super.place(context);
        if (result.isAccepted() && context.getWorld() instanceof ServerWorld serverWorld) {
            BeaconCoreManager.onAnchorPlaced(serverWorld, context.getBlockPos());
        }
        return result;
    }

    private static LoreComponent createLore() {
        return new LoreComponent(List.of(
            Text.translatable("Place this down where you would like your anchor to be.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

            Text.translatable("It is invisible and invincible and you can walk through it.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

            Text.translatable("Only when there is a new user of the core can a new one be placed.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)
        ));
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("B").styled(s -> s.withColor(0xF9B6DC).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xE8BDE8).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0xD6C3F3).withBold(true)))
            .append(Text.literal("c").styled(s -> s.withColor(0xCCB4F3).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xC2A4F2).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0x7CB1FB).withBold(true)));
    }
}
