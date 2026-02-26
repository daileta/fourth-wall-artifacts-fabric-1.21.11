package net.fourthwall.artifacts.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class VoidReaverItem extends AxeItem implements PolymerFallbackItem {
    private static final int UNBREAKING_LEVEL = 3;
    private static final int MENDING_LEVEL = 1;
    private static final int SHARPNESS_LEVEL = 5;

    public VoidReaverItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public net.minecraft.item.Item getFallbackItem(ItemStack stack) {
        return Items.NETHERITE_AXE;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.evanpack.void_reaver.desc.line1"));
        textConsumer.accept(Text.translatable("item.evanpack.void_reaver.desc.line2"));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        return ensureEnchantments(stack, world);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("☠ ").styled(s -> s.withColor(0xC745FF)))
            .append(Text.literal("V").styled(s -> s.withColor(0xB63FEA).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0xA63AD5).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0x9534BF).withBold(true)))
            .append(Text.literal("d").styled(s -> s.withColor(0x852EAA).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0x742895).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x642380).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0x531D6A).withBold(true)))
            .append(Text.literal("v").styled(s -> s.withColor(0x421755).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x321140).withBold(true)))
            .append(Text.literal("r ").styled(s -> s.withColor(0x210C2B).withBold(true)))
            .append(Text.literal("☠").styled(s -> s.withColor(0x000000)));
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        var enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        var mending = enchantments.getOrThrow(Enchantments.MENDING);
        var sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        boolean changed = false;

        if (EnchantmentHelper.getLevel(unbreaking, stack) < UNBREAKING_LEVEL) {
            stack.addEnchantment(unbreaking, UNBREAKING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(mending, stack) < MENDING_LEVEL) {
            stack.addEnchantment(mending, MENDING_LEVEL);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(sharpness, stack) < SHARPNESS_LEVEL) {
            stack.addEnchantment(sharpness, SHARPNESS_LEVEL);
            changed = true;
        }
        return changed;
    }
}
