package com.qis.ammo;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class PackFinders {

    private PackFinders() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        Path base = FMLPaths.CONFIGDIR.get().resolve("qisammo");
        Path dir = base.resolve("datapack");
        if (ensurePackFolder(dir, "datapack")) {
            addFolderPack(event, dir, "qisammo_generated_recipes", "qisammo generated recipes");
        }
    }

    private static boolean ensurePackFolder(Path dir, String what) {
        try {
            Files.createDirectories(dir);
            if (!Files.isRegularFile(dir.resolve("pack.mcmeta"))) {
                QISAmmoMod.LOGGER.warn("[qisammo] {} pack folder has no pack.mcmeta yet: {}", what, dir);
                return false;
            }
            return true;
        } catch (java.io.IOException e) {
            QISAmmoMod.LOGGER.error("[qisammo] failed to create {} pack folder {}", what, dir, e);
            return false;
        }
    }

    private static void addFolderPack(AddPackFindersEvent event, Path folder, String id, String displayName) {
        PackLocationInfo info = new PackLocationInfo(id, Component.literal(displayName), PackSource.BUILT_IN,
                Optional.of(new KnownPack("qisammo", id, "1.0.0")));
        Pack.ResourcesSupplier resources = new PathPackResources.PathResourcesSupplier(folder);
        PackSelectionConfig config = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);
        Pack pack = Pack.readMetaAndCreate(info, resources, event.getPackType(), config);
        if (pack != null) {
            event.addRepositorySource(packConsumer -> packConsumer.accept(pack));
        }
    }
}
