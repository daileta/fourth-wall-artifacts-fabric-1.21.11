package net.fourthwall.artifacts;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

public class SmolderingRod extends FishingRodItem {

    public SmolderingRod(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        boolean hadBobber = user.fishHook != null;
        ActionResult result = super.use(world, user, hand);
        if (!world.isClient()) {
            if (!hadBobber && user.fishHook != null) {
                SmolderingRodManager.onBobberCast(user, user.fishHook);
            } else if (hadBobber && user.fishHook == null) {
                SmolderingRodManager.onBobberRemoved(user, world);
            }
        }
        return result;
    }
}
