package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.smoldering.SmolderingRodManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import java.util.List;
import java.util.Objects;

public class SmolderingRodItem extends FishingRodItem implements PolymerFallbackItem {
    public SmolderingRodItem(Settings settings) {
        super(settings.component(DataComponentTypes.LORE, createLore()));
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

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        boolean changed = false;

        LoreComponent desiredLore = createLore();
        LoreComponent currentLore = stack.get(DataComponentTypes.LORE);

        if (!Objects.equals(desiredLore, currentLore)) {
            stack.set(DataComponentTypes.LORE, desiredLore);
            changed = true;
        }

        return ensureEnchantments(stack, world) || changed;
    }

    private static LoreComponent createLore() {
        return new LoreComponent(List.of(
            Text.translatable("A magmastone-tempered rod that hums with intense heat.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

            Text.translatable("Only a deranged fool could have created this...")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)
        ));
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        return ArtifactEnchantments.refreshConfiguredStack(stack, world);
    }
}
