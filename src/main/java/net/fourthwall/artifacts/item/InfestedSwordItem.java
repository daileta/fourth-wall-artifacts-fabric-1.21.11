package net.fourthwall.artifacts.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
import java.util.Objects;

public class InfestedSwordItem extends Item implements PolymerFallbackItem {
    public InfestedSwordItem(Settings settings) {
        super(settings.component(DataComponentTypes.LORE, createLore()));
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.NETHERITE_SWORD;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        refreshConfiguredStack(stack, world);
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
            Text.translatable("A tool that clicks faintly against stone, as if the walls beckon to the sound.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC),

            Text.translatable("Only corruption patient enough to bury its own champion could have birthed this.")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC)
        ));
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
            .append(Text.literal("I").styled(s -> s.withColor(0xA69FA3).withBold(true)))
            .append(Text.literal("n").styled(s -> s.withColor(0x9F9A9D).withBold(true)))
            .append(Text.literal("f").styled(s -> s.withColor(0x999496).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x928F90).withBold(true)))
            .append(Text.literal("s").styled(s -> s.withColor(0x8C8989).withBold(true)))
            .append(Text.literal("t").styled(s -> s.withColor(0x858483).withBold(true)))
            .append(Text.literal("e").styled(s -> s.withColor(0x7E7F7C).withBold(true)))
            .append(Text.literal("d ").styled(s -> s.withColor(0x787976).withBold(true)))
            .append(Text.literal("S").styled(s -> s.withColor(0x71746F).withBold(true)))
            .append(Text.literal("w").styled(s -> s.withColor(0x6A6F69).withBold(true)))
            .append(Text.literal("o").styled(s -> s.withColor(0x646962).withBold(true)))
            .append(Text.literal("r").styled(s -> s.withColor(0x5D645C).withBold(true)))
            .append(Text.literal("d").styled(s -> s.withColor(0x50594F).withBold(true)));
    }

    private static boolean ensureEnchantments(ItemStack stack, ServerWorld world) {
        return ArtifactEnchantments.refreshConfiguredStack(stack, world);
    }
}
