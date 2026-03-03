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
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.util.Formatting;
import java.util.List;

public class BloodSacrificeItem extends Item implements PolymerFallbackItem {
    public BloodSacrificeItem(Settings settings) {
        super(settings.component(DataComponentTypes.LORE, createLore()));
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.STICK;
    }

    private static LoreComponent createLore() {
    return new LoreComponent(List.of(
        Text.translatable("A dagger that lights with a soul flame that can’t be extinguished.")
            .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

        Text.translatable("It begs for an equal exchange of life with blood and rewards those who give it.")
            .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)
        ));
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("B").styled(s -> s.withColor(0xE80707).withBold(true)))
            .append(Text.literal("l").styled(s -> s.withColor(0xE30808).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xDF0A0A).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xDA0B0B).withBold(true)))
            .append(Text.literal("d ").styled(s -> s.withColor(0xD50C0C).withBold(true)))
            .append(Text.literal("S").styled(s -> s.withColor(0xD00D0D).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0xCC0F0F).withBold(true)))
            .append(Text.literal("c").styled(s -> s.withColor(0xC71010).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xC21111).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0xBE1313).withBold(true)))
            .append(Text.literal("f").styled(s -> s.withColor(0xB91414).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0xB41515).withBold(true)))
            .append(Text.literal("c").styled(s -> s.withColor(0xAF1616).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xA61919).withBold(true)));
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
