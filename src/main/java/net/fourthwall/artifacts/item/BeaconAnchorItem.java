package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.beacon.BeaconCoreManager;
import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.function.Consumer;

public class BeaconAnchorItem extends BlockItem implements PolymerFallbackItem {
    public BeaconAnchorItem(Block block, Settings settings) {
        super(block, settings);
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

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.evanpack.beacon_anchor.desc.line1"));
        textConsumer.accept(Text.translatable("item.evanpack.beacon_anchor.desc.line2"));
        textConsumer.accept(Text.translatable("item.evanpack.beacon_anchor.desc.line3"));
    }
}
