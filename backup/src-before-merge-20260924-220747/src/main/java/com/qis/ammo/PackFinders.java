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

/**
 * Registers the two generated folders (data pack + resource pack) so that the
 * server loads the generated Create recipes and clients load the generated
 * item models/lang for the incomplete ammo items.
 */
public final class PackFinders {

    private PackFinders() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        Path base = FMLPaths.CONFIGDIR.get().resolve("qisammo");
        if (event.getPackType() == PackType.SERVER_DATA) {
            Path dir = base.resolve("datapack");
            if (Files.isDirectory(dir)) {
                addFolderPack(event, dir, "qisammo_generated_recipes", "qisammo generated recipes");
            }
        } else if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path dir = base.resolve("resourcepack");
            if (Files.isDirectory(dir)) {
                addFolderPack(event, dir, "qisammo_generated_assets", "qisammo generated assets");
            }
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
