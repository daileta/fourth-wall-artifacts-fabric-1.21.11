package net.fourthwall.artifacts.registry;

import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.item.PolymerFallbackItem;
import net.fourthwall.artifacts.item.BloodSacrificeItem;
import net.fourthwall.artifacts.item.BeaconAnchorItem;
import net.fourthwall.artifacts.item.BeaconCoreItem;
import net.fourthwall.artifacts.item.EarthsplitterItem;
import net.fourthwall.artifacts.item.EmperorsCrownItem;
import net.fourthwall.artifacts.item.InfestedPickaxeItem;
import net.fourthwall.artifacts.item.InfestedSwordItem;
import net.fourthwall.artifacts.item.LionsHeartItem;
import net.fourthwall.artifacts.item.RepeaterCrossbowItem;
import net.fourthwall.artifacts.item.ExcaliburItem;
import net.fourthwall.artifacts.item.SmolderingRodItem;
import net.fourthwall.artifacts.item.TridentOfPoseidonItem;
import net.fourthwall.artifacts.item.VoidReaverItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.TridentItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ModItems {
    private static final List<Item> ARTIFACT_ITEMS = new ArrayList<>();

    public static final Item SMOLDERING_ROD = register(
            "smoldering_rod",
            new SmolderingRodItem(withItemSettings("smoldering_rod").maxDamage(128))
    );
    public static final Item BLOOD_SACRIFICE = register(
            "blood_sacrifice",
            new BloodSacrificeItem(withItemSettings("blood_sacrifice").maxCount(1))
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
    public static final Item LIONS_HEART = register(
            "lions_heart",
            new LionsHeartItem(LionsHeartItem.applyChestplateSettings(withItemSettings("lions_heart")))
    );
    public static final Item REPEATER = register(
            "repeater",
            new RepeaterCrossbowItem(withItemSettings("repeater").maxDamage(465))
    );
    public static final Item TRIDENT_OF_POSEIDON = register(
            "trident_of_poseidon",
            new TridentOfPoseidonItem(
                withItemSettings("trident_of_poseidon")
                        .maxDamage(250)
                        .attributeModifiers(TridentOfPoseidonItem.createPoseidonAttributeModifiers())
                        .component(DataComponentTypes.TOOL, TridentItem.createToolComponent())
                        .fireproof()
            )
    );
        public static final Item EARTHSPLITTER = register(
                "earthsplitter",
                new EarthsplitterItem(
                        withItemSettings("earthsplitter")
                                .maxDamage(500)
                                .attributeModifiers(EarthsplitterItem.createEarthsplitterAttributeModifiers())
                                .fireproof()
                )
        );
        public static final Item EXCALIBUR = register(
                "excalibur",
                new ExcaliburItem(
                        withItemSettings("excalibur")
                                .sword(ToolMaterial.IRON, 3.0F, -2.4F)
                                .attributeModifiers(ExcaliburItem.createExcaliburAttributeModifiers())
                )
        );
    public static final Item EMPERORS_CROWN = register(
            "emperors_crown",
            new EmperorsCrownItem(EmperorsCrownItem.applyHelmetSettings(withItemSettings("emperors_crown")))
    );
    public static final Item BEACON_CORE = register(
            "beacon_core",
            new BeaconCoreItem(withItemSettings("beacon_core").maxCount(1))
    );
    public static final Item BEACON_ANCHOR = register(
            "beacon_anchor",
            new BeaconAnchorItem(ModBlocks.BEACON_ANCHOR, withItemSettings("beacon_anchor").maxCount(1))
    );

    private ModItems() {
    }

    public static void init() {
        // Triggers static registration.
    }

    public static List<Item> getArtifactItems() {
        return List.copyOf(ARTIFACT_ITEMS);
    }

    public static String describeRegistrations() {
        return ARTIFACT_ITEMS.stream()
                .map(item -> {
                    Identifier id = Registries.ITEM.getId(item);
                    String polymerInfo = item instanceof PolymerFallbackItem polymerFallbackItem
                            ? "polymerFallback=" + Registries.ITEM.getId(polymerFallbackItem.getFallbackItem(item.getDefaultStack()))
                            : "polymerFallback=<none>";
                    return id + "(" + item.getClass().getSimpleName() + "," + polymerInfo + ")";
                })
                .collect(Collectors.joining(", "));
    }

    private static Item register(String path, Item item) {
        Identifier id = FourthWallArtifacts.id(path);
        Item registered = Registry.register(Registries.ITEM, id, item);
        ARTIFACT_ITEMS.add(registered);
        FourthWallArtifacts.LOGGER.info("Registered item {} as {} (polymer={})",
                id,
                registered.getClass().getSimpleName(),
                registered instanceof PolymerFallbackItem);
        return registered;
    }

    private static Item.Settings withItemSettings(String path) {
        Identifier id = FourthWallArtifacts.id(path);
        return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id));
    }
}
