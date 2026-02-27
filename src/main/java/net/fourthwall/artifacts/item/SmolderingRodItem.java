package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.smoldering.SmolderingRodManager;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class SmolderingRodItem extends FishingRodItem implements PolymerFallbackItem {
    public SmolderingRodItem(Settings settings) {
        super(settings);
    }

    @Override
    public net.minecraft.item.Item getFallbackItem(ItemStack stack) {
        return Items.FISHING_ROD;
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
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("S").styled(s -> s.withColor(0xED0909).withBold(true)))
            .append(Text.literal("m").styled(s -> s.withColor(0xF01F0C).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xF3340F).withBold(true)))
            .append(Text.literal("l").styled(s -> s.withColor(0xF54A12).withBold(true)))
            .append(Text.literal("d").styled(s -> s.withColor(0xF85F15).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xFB7518).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xFE8A1B).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0xFF9C1C).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0xFFAA1C).withBold(true)))
            .append(Text.literal("g ").styled(s -> s.withColor(0xFFB81C).withBold(true)))
            .append(Text.literal("R").styled(s -> s.withColor(0xFFC61C).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xFFD41C).withBold(true)))
            .append(Text.literal("d").styled(s -> s.withColor(0xFFF01C).withBold(true)));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.evanpack.smoldering_rod.desc.line1"));
        textConsumer.accept(Text.translatable("item.evanpack.smoldering_rod.desc.line2"));
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        return ensureEnchantments(stack, world);
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        return ArtifactEnchantments.refreshConfiguredStack(stack, world);
    }
}
