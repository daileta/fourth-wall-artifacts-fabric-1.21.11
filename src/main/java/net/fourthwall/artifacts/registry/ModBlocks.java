package net.fourthwall.artifacts.registry;

import net.fourthwall.artifacts.FourthWallArtifacts;
import net.fourthwall.artifacts.block.BeaconAnchorBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block BEACON_ANCHOR = Registry.register(
            Registries.BLOCK,
            FourthWallArtifacts.id("beacon_anchor"),
            new BeaconAnchorBlock(
                    withBlockSettings("beacon_anchor")
                            .strength(-1.0F, 3_600_000.0F)
                            .dropsNothing()
                            .noCollision()
                            .nonOpaque()
                            .luminance(state -> 15)
            )
    );

    private ModBlocks() {
    }

    public static void init() {
        // Triggers static registration.
    }

    private static AbstractBlock.Settings withBlockSettings(String path) {
        Identifier id = FourthWallArtifacts.id(path);
        return AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id));
    }
}
