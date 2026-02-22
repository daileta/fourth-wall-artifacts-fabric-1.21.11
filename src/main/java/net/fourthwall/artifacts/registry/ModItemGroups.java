package net.fourthwall.artifacts.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
    public static final ItemGroup ARTIFACTS = Registry.register(
            Registries.ITEM_GROUP,
            FourthWallArtifacts.id("artifacts"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.evanpack.artifacts"))
                    .icon(() -> new ItemStack(ModItems.SMOLDERING_ROD))
                    .entries((displayContext, entries) -> ModItems.getArtifactItems().forEach(entries::add))
                    .build()
    );

    private ModItemGroups() {
    }

    public static void init() {
        // Triggers static registration.
    }
}
