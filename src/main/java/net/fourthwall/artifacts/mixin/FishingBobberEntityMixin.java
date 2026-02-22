package net.fourthwall.artifacts.mixin;

import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public class FishingBobberEntityMixin {
    @Inject(method = "removeIfInvalid", at = @At("HEAD"), cancellable = true)
    private void artifacts$allowSmolderingRod(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;

        if (player.isInteractable()) {
            ItemStack mainHand = player.getMainHandStack();
            ItemStack offHand = player.getOffHandStack();
            boolean mainHasRod = mainHand.isOf(Items.FISHING_ROD) || mainHand.isOf(ModItems.SMOLDERING_ROD);
            boolean offHasRod = offHand.isOf(Items.FISHING_ROD) || offHand.isOf(ModItems.SMOLDERING_ROD);

            if ((mainHasRod || offHasRod) && bobber.squaredDistanceTo(player) <= 1024.0) {
                cir.setReturnValue(false);
                return;
            }
        }

        bobber.discard();
        cir.setReturnValue(true);
    }
}
