package net.fourthwall.artifacts.item;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fourthwall.artifacts.FourthWallArtifacts;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class PolymerPackAssetGuard {
    private static final Object LOCK = new Object();

    private static Path cachedPackPath;
    private static long cachedPackSize = -1L;
    private static Set<String> cachedEntries = Set.of();
    private static boolean warnedReadFailure;
    private static final Set<String> warnedMissingEntries = new HashSet<>();

    private PolymerPackAssetGuard() {
    }

    static boolean hasItemDefinition(Identifier itemId) {
        Path packPath = PolymerResourcePackUtils.getMainPath();
        if (packPath == null || !Files.exists(packPath)) {
            return false;
        }

        String entryPath = "assets/%s/items/%s.json".formatted(itemId.getNamespace(), itemId.getPath());

        synchronized (LOCK) {
            if (!refreshCacheIfNeeded(packPath)) {
                return false;
            }

            boolean present = cachedEntries.contains(entryPath);
            if (!present && warnedMissingEntries.add(entryPath)) {
                FourthWallArtifacts.LOGGER.warn(
                        "Polymer pack '{}' is missing '{}' for {}; using fallback item texture/model for vanilla clients.",
                        packPath,
                        entryPath,
                        itemId
                );
            }
            return present;
        }
    }

    private static boolean refreshCacheIfNeeded(Path packPath) {
        long size;
        try {
            size = Files.size(packPath);
        } catch (IOException exception) {
            if (!warnedReadFailure) {
                warnedReadFailure = true;
                FourthWallArtifacts.LOGGER.warn("Could not inspect Polymer pack '{}': {}", packPath, exception.getMessage());
            }
            return false;
        }

        if (packPath.equals(cachedPackPath) && size == cachedPackSize) {
            return true;
        }

        try (ZipFile zipFile = new ZipFile(packPath.toFile())) {
            Set<String> entries = new HashSet<>();
            zipFile.stream()
                    .map(ZipEntry::getName)
                    .forEach(entries::add);

            cachedPackPath = packPath;
            cachedPackSize = size;
            cachedEntries = Set.copyOf(entries);
            warnedReadFailure = false;
            warnedMissingEntries.clear();
            return true;
        } catch (IOException exception) {
            if (!warnedReadFailure) {
                warnedReadFailure = true;
                FourthWallArtifacts.LOGGER.warn("Could not read Polymer pack '{}': {}", packPath, exception.getMessage());
            }
            return false;
        }
    }
}
