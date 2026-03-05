package net.fourthwall.artifacts.item;

import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
import java.util.Objects;

public class TridentOfPoseidonItem extends TridentItem implements PolymerFallbackItem {
    public TridentOfPoseidonItem(Item.Settings settings) {
        super(settings.component(DataComponentTypes.LORE, createLore()));
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.TRIDENT;
    }

    public static AttributeModifiersComponent createPoseidonAttributeModifiers() {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, 11.0D, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.ATTACK_SPEED, new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, -2.9D, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
                .build();
    }

    @Override
    public ItemStack getDefaultStack() {
        return super.getDefaultStack();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("P").styled(s -> s.withColor(0x0A9DFF).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0x0A95F9).withBold(true)))
            .append(Text.literal("s").styled(s -> s.withColor(0x0B8DF4).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x0B85EE).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0x0B7DE9).withBold(true)))
            .append(Text.literal("d").styled(s -> s.withColor(0x0C75E3).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0x0C6DDE).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0x0C65D8).withBold(true)))
            .append(Text.literal("'").styled(s -> s.withColor(0x0D5DD3).withBold(true)))
            .append(Text.literal("s ").styled(s -> s.withColor(0x0D55CD).withBold(true)))
            .append(Text.literal("T").styled(s -> s.withColor(0x0E4DC8).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0x0E45C2).withBold(true)))
            .append(Text.literal("i").styled(s -> s.withColor(0x0E3DBD).withBold(true)))
            .append(Text.literal("d").styled(s -> s.withColor(0x0F35B7).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x0F2DB2).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0x0F25AC).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0x1015A1).withBold(true)));
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
            Text.translatable("A trident passed down from the god of the sea himself.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

            Text.translatable("Imbued with the power of the ocean, it grants the wielder aquatic abilities")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)
        ));
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        return ArtifactEnchantments.refreshConfiguredStack(stack, world);
    }
}
