package net.fourthwall.artifacts.item;

import net.fourthwall.artifacts.FourthWallArtifacts;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class PolymerPackAssetGuard {
    private static final Map<String, Boolean> CACHED_ITEM_DEFINITIONS = new ConcurrentHashMap<>();
    private static final Set<String> WARNED_MISSING_ENTRIES = ConcurrentHashMap.newKeySet();

    private PolymerPackAssetGuard() {
    }

    static boolean hasItemDefinition(Identifier itemId) {
        String resourcePath = "assets/%s/items/%s.json".formatted(itemId.getNamespace(), itemId.getPath());
        boolean present = CACHED_ITEM_DEFINITIONS.computeIfAbsent(
                resourcePath,
                path -> PolymerPackAssetGuard.class.getClassLoader().getResource(path) != null
        );

        if (!present && WARNED_MISSING_ENTRIES.add(resourcePath)) {
            FourthWallArtifacts.LOGGER.warn(
                    "Bundled Polymer item definition '{}' is missing for {}; using fallback item texture/model for vanilla clients.",
                    resourcePath,
                    itemId
            );
        }

        return present;
    }
}
