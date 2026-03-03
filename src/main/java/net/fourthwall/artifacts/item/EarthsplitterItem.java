package net.fourthwall.artifacts.item;

import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.util.Formatting;
import java.util.Objects;
import java.util.List;

public class EarthsplitterItem extends MaceItem implements PolymerFallbackItem {
    public EarthsplitterItem(Item.Settings settings) {
        super(settings.component(DataComponentTypes.LORE, createLore()));
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.MACE;
    }

    public static AttributeModifiersComponent createEarthsplitterAttributeModifiers() {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, 9.0D, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED, new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, -3.4D, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("E").styled(s -> s.withColor(0x1FBA07).withBold(true)))
            .append(Text.literal("a").styled(s -> s.withColor(0x2FAB0F).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0x3F9C16).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0x4F8E1E).withBold(true)))
            .append(Text.literal("h").styled(s -> s.withColor(0x5F7F25).withBold(true)))
            .append(Text.literal("s").styled(s -> s.withColor(0x6F702D).withBold(true)))
            .append(Text.literal("p").styled(s -> s.withColor(0x7F6134).withBold(true)))
            .append(Text.literal("l").styled(s -> s.withColor(0x8B5D3A).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0x93623D).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0x9B6740).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0xA26D44).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0xAA7247).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0xBA7D4E).withBold(true)));
    }

    public static boolean refreshConfiguredStack(ItemStack stack, ServerWorld world) {
        boolean changed = false;

        LoreComponent desiredLore = createLore();
        LoreComponent currentLore = stack.get(DataComponentTypes.LORE);

        if (!Objects.equals(desiredLore, currentLore)) {
            stack.set(DataComponentTypes.LORE, desiredLore);
            changed = true;
        }

        return ArtifactEnchantments.refreshConfiguredStack(stack, world) || changed;
    }

    private static LoreComponent createLore() {
    return new LoreComponent(List.of(
        Text.translatable("It is said that a mace fell down from the heavens themselves to our earth.")
            .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

        Text.translatable("When it landed, it split the earth itself and helped shaped this world.")
            .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)
        ));
    }

}
