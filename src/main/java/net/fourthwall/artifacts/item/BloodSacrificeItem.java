package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.blood.BloodSacrificeManager;
import net.fourthwall.artifacts.config.ArtifactsConfigManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class BloodSacrificeItem extends Item implements PolymerFallbackItem {
    public BloodSacrificeItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.STICK;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.FAIL;
        }

        if (user instanceof ServerPlayerEntity serverPlayer) {
            BloodSacrificeManager.activate(serverPlayer);
            serverPlayer.getItemCooldownManager().set(stack, ArtifactsConfigManager.get().bloodSacrifice.itemCooldownTicks);
        }

        return ActionResult.SUCCESS;
    }
}
