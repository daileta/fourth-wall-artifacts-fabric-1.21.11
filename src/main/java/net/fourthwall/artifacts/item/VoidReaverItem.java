package net.fourthwall.artifacts.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class VoidReaverItem extends AxeItem {
    public VoidReaverItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("item.evanpack.void_reaver.desc.line1"));
        textConsumer.accept(Text.translatable("item.evanpack.void_reaver.desc.line2"));
    }
}
