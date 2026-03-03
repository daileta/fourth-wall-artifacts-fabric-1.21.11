package net.fourthwall.artifacts.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import java.util.List;

public class UndeadWardArmyItem extends Item implements PolymerFallbackItem {
    public UndeadWardArmyItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getFallbackItem(ItemStack stack) {
        return Items.WRITTEN_BOOK;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.empty()
                .append(Text.literal("T").styled(style -> style.withColor(0xA8F6FF).withBold(true)))
                .append(Text.literal("h").styled(style -> style.withColor(0x9EF0FF).withBold(true)))
                .append(Text.literal("e ").styled(style -> style.withColor(0x94E9FF).withBold(true)))
                .append(Text.literal("U").styled(style -> style.withColor(0x89E2FF).withBold(true)))
                .append(Text.literal("n").styled(style -> style.withColor(0x7FDCFF).withBold(true)))
                .append(Text.literal("d").styled(style -> style.withColor(0x75D5FF).withBold(true)))
                .append(Text.literal("e").styled(style -> style.withColor(0x6BCFFF).withBold(true)))
                .append(Text.literal("a").styled(style -> style.withColor(0x61C8FF).withBold(true)))
                .append(Text.literal("d ").styled(style -> style.withColor(0x57C2FF).withBold(true)))
                .append(Text.literal("W").styled(style -> style.withColor(0x4CB9FF).withBold(true)))
                .append(Text.literal("a").styled(style -> style.withColor(0x42B1FF).withBold(true)))
                .append(Text.literal("r").styled(style -> style.withColor(0x38A8FF).withBold(true)))
                .append(Text.literal("d ").styled(style -> style.withColor(0x2DA0FF).withBold(true)))
                .append(Text.literal("A").styled(style -> style.withColor(0x2397FF).withBold(true)))
                .append(Text.literal("r").styled(style -> style.withColor(0x198FFF).withBold(true)))
                .append(Text.literal("m").styled(style -> style.withColor(0x0E86FF).withBold(true)))
                .append(Text.literal("y").styled(style -> style.withColor(0x047EFF).withBold(true)));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }

        ItemStack heldStack = user.getStackInHand(hand);
        if (!heldStack.isOf(this)) {
            return ActionResult.PASS;
        }

        applyCommandBookContent(heldStack, serverPlayer);
        serverPlayer.useBook(heldStack, hand);
        return ActionResult.SUCCESS;
    }

    private static void applyCommandBookContent(ItemStack stack, ServerPlayerEntity player) {
        Text title = Text.literal("The Undead Ward Army").formatted(Formatting.AQUA, Formatting.BOLD);
        Text pageOne = Text.empty()
                .append(title)
                .append(Text.literal("\n\n"))
                .append(button("Summon Undead Deputies", "/artifacts undead_ward_army summon_deputies", Formatting.GREEN))
                .append(Text.literal("\n"))
                .append(button("Dismiss Undead Deputies", "/artifacts undead_ward_army dismiss_deputies", Formatting.RED))
                .append(Text.literal("\n\n"))
                .append(button("Summon Undead Commanders", "/artifacts undead_ward_army summon_commanders", Formatting.GREEN));

        Text pageTwo = Text.empty()
                .append(title)
                .append(Text.literal("\n\n"))
                .append(button("Dismiss Undead Commanders", "/artifacts undead_ward_army dismiss_commanders", Formatting.RED))
                .append(Text.literal("\n\n"))
                .append(button("Summon Undead Warden", "/artifacts undead_ward_army summon_warden", Formatting.GREEN))
                .append(Text.literal("\n"))
                .append(button("Dismiss Undead Warden", "/artifacts undead_ward_army dismiss_warden", Formatting.RED));

        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookContentComponent(
                RawFilteredPair.of("The Undead Ward Army"),
                player.getName().getString(),
                0,
                List.of(RawFilteredPair.of(pageOne), RawFilteredPair.of(pageTwo)),
                true
        ));
    }

    private static Text button(String label, String command, Formatting color) {
        return Text.literal("[ " + label + " ]")
                .styled(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.RunCommand(command)));
    }
}
