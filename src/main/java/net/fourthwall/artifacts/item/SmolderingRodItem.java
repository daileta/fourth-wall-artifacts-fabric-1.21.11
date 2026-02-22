package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.smoldering.SmolderingRodManager;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class SmolderingRodItem extends FishingRodItem {
    public SmolderingRodItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        boolean hadBobber = user.fishHook != null;
        Entity hookedBeforeReel = hadBobber && user.fishHook != null ? user.fishHook.getHookedEntity() : null;
        ActionResult result = super.use(world, user, hand);

        if (!world.isClient()) {
            if (!hadBobber && user.fishHook != null) {
                SmolderingRodManager.onBobberCast(user, user.fishHook);
            } else if (hadBobber) {
                if (hookedBeforeReel != null) {
                    SmolderingRodManager.onHookedBeforeReel(user, hookedBeforeReel);
                }
                if (user.fishHook == null) {
                    SmolderingRodManager.onBobberRemoved(user, world);
                }
            }
        }

        return result;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.evanpack.smoldering_rod.desc.line1"));
        textConsumer.accept(Text.translatable("item.evanpack.smoldering_rod.desc.line2"));
    }
}
