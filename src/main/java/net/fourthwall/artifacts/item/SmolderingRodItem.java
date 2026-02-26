package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.smoldering.SmolderingRodManager;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class SmolderingRodItem extends FishingRodItem implements PolymerFallbackItem {
    private static final int UNBREAKING_LEVEL = 3;
    private static final int MENDING_LEVEL = 1;

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
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.evanpack.smoldering_rod.desc.line1"));
        textConsumer.accept(Text.translatable("item.evanpack.smoldering_rod.desc.line2"));
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        return ensureEnchantments(stack, world);
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);
        boolean changed = false;

        if (EnchantmentHelper.getLevel(unbreaking, stack) < UNBREAKING_LEVEL) {
            stack.addEnchantment(unbreaking, UNBREAKING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(mending, stack) < MENDING_LEVEL) {
            stack.addEnchantment(mending, MENDING_LEVEL);
            changed = true;
        }
        return changed;
    }
}
