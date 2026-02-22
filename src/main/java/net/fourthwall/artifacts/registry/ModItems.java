package net.fourthwall.artifacts.registry;

import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.item.InfestedPickaxeItem;
import net.fourthwall.artifacts.item.InfestedSwordItem;
import net.fourthwall.artifacts.item.SmolderingRodItem;
import net.fourthwall.artifacts.item.VoidReaverItem;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    private static final List<Item> ARTIFACT_ITEMS = new ArrayList<>();

    public static final Item SMOLDERING_ROD = register(
            "smoldering_rod",
            new SmolderingRodItem(withItemSettings("smoldering_rod").maxDamage(128))
    );
    public static final Item VOID_REAVER = register(
            "void_reaver",
            new VoidReaverItem(ToolMaterial.NETHERITE, 8.0F, -3.0F, withItemSettings("void_reaver").fireproof())
    );
    public static final Item INFESTED_SWORD = register(
            "infested_sword",
            new InfestedSwordItem(withItemSettings("infested_sword").sword(ToolMaterial.NETHERITE, 7.0F, -2.4F).fireproof())
    );
    public static final Item INFESTED_PICKAXE = register(
            "infested_pickaxe",
            new InfestedPickaxeItem(withItemSettings("infested_pickaxe").pickaxe(ToolMaterial.NETHERITE, 2.0F, -2.8F).fireproof())
    );

    private ModItems() {
    }

    public static void init() {
        // Triggers static registration.
    }

    public static List<Item> getArtifactItems() {
        return List.copyOf(ARTIFACT_ITEMS);
    }

    private static Item register(String path, Item item) {
        Identifier id = FourthWallArtifacts.id(path);
        Item registered = Registry.register(Registries.ITEM, id, item);
        ARTIFACT_ITEMS.add(registered);
        return registered;
    }

    private static Item.Settings withItemSettings(String path) {
        Identifier id = FourthWallArtifacts.id(path);
        return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id));
    }
}
