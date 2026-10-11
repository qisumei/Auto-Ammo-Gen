package com.qis.ammo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class AmmoScanner {

    private static final Map<String, AmmoData> AMMOS = new LinkedHashMap<>();
    private static final Map<String, byte[]> TEXTURES = new LinkedHashMap<>();

    private AmmoScanner() {
    }

    public static List<AmmoData> getAmmos() {
        return new ArrayList<>(AMMOS.values());
    }

    public static byte[] getTexture(String ammoId) {
        return TEXTURES.get(ammoId);
    }

    public static Path taczDir() {
        return FMLPaths.GAMEDIR.get().resolve("tacz");
    }

    public static void scanAndGenerate() {
        scanFresh();
        QISAmmoMod.LOGGER.info("[qisammo] found {} ammo types in {}", AMMOS.size(), taczDir());
        GeneratedPacks.writeAll();
    }

    public record RescanResult(int total, int added, List<String> restartRequired) {
    }

    public static RescanResult rescanAndGenerate() {
        TEXTURES.clear();
        Map<String, AmmoData> fresh = scanDisk(taczDir());
        List<String> restartRequired = new ArrayList<>();
        int added = 0;
        for (AmmoData a : fresh.values()) {
            String name = "incomplete_" + AmmoData.sanitize(a.path());
            if (!net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(QISAmmoMod.MODID, name))) {
                QISAmmoMod.LOGGER.warn("[qisammo] ammo {} has no registered incomplete item yet - restart required for new gun packs", a.id());
                restartRequired.add(a.id());
                continue;
            }
            if (AMMOS.put(a.id(), a) == null) {
                added++;
            }
        }
        GeneratedPacks.writeAll();
        return new RescanResult(AMMOS.size(), added, restartRequired);
    }

    private static void scanFresh() {
        AMMOS.clear();
        TEXTURES.clear();
        Map<String, AmmoData> fresh = scanDisk(taczDir());
        for (AmmoData a : fresh.values()) {
            AMMOS.put(a.id(), a);
        }
    }

    private static Map<String, AmmoData> scanDisk(Path tacz) {
        Map<String, AmmoData> result = new LinkedHashMap<>();
        if (!Files.isDirectory(tacz)) {
            QISAmmoMod.LOGGER.warn("[qisammo] tacz gun pack folder not found at {}", tacz);
            return result;
        }
        List<Path> entries = new ArrayList<>();
        try (var stream = Files.list(tacz)) {
            for (Path entry : stream.toList()) {
                entries.add(entry);
            }
        } catch (IOException e) {
            QISAmmoMod.LOGGER.error("[qisammo] failed to scan tacz folder", e);
            return result;
        }
        entries.sort(java.util.Comparator
                .comparingInt(AmmoScanner::packRank)
                .thenComparing(e -> e.getFileName().toString()));
        try {
            for (Path entry : entries) {
                String fileName = entry.getFileName().toString();
                if (fileName.endsWith(".zip") && Files.isRegularFile(entry)) {
                    scanZip(entry, result);
                } else if (Files.isDirectory(entry)) {
                    scanFolder(entry, result);
                }
            }
        } catch (IOException e) {
            QISAmmoMod.LOGGER.error("[qisammo] failed to scan tacz folder", e);
        }
        return result;
    }

    private static int packRank(Path entry) {
        String name = entry.getFileName().toString();
        if (Files.isDirectory(entry) && name.equalsIgnoreCase("tacz_default_gun")) {
            return 0;
        }
        if (Files.isDirectory(entry)) {
            return 1;
        }
        return 2;
    }

    private static void scanFolder(Path folder, Map<String, AmmoData> out) throws IOException {
        try (var walk = Files.walk(folder)) {
            for (Path p : walk.filter(Files::isRegularFile).sorted().toList()) {
                String rel = folder.relativize(p).toString().replace('\\', '/');
                if (isAmmoRecipePath(rel)) {
                    AmmoData ammo;
                    try (InputStream in = Files.newInputStream(p)) {
                        ammo = parseAmmoRecipe(in);
                    }
                    QISAmmoMod.LOGGER.info("[qisammo] scan file {} -> {}", rel,
                            ammo == null ? "IGNORED" : ammo.id());
                    if (ammo != null) {
                        out.putIfAbsent(ammo.id(), ammo);
                        Path tex = folder.resolve("assets/" + ammo.namespace() + "/textures/ammo/slot/" + ammo.path() + ".png");
                        if (Files.isRegularFile(tex)) {
                            TEXTURES.putIfAbsent(ammo.id(), Files.readAllBytes(tex));
                        }
                    }
                }
            }
        }
    }

    private static void scanZip(Path zipFile, Map<String, AmmoData> out) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile.toFile(), StandardCharsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (isAmmoRecipePath(entry.getName())) {
                    AmmoData ammo;
                    try (InputStream in = zip.getInputStream(entry)) {
                        ammo = parseAmmoRecipe(in);
                    }
                    QISAmmoMod.LOGGER.info("[qisammo] scan file {} -> {}", entry.getName(),
                            ammo == null ? "IGNORED" : ammo.id());
                    if (ammo != null) {
                        out.putIfAbsent(ammo.id(), ammo);
                        String texPath = "assets/" + ammo.namespace() + "/textures/ammo/slot/" + ammo.path() + ".png";
                        ZipEntry tex = zip.getEntry(texPath);
                        if (tex != null) {
                            try (InputStream in = zip.getInputStream(tex)) {
                                TEXTURES.putIfAbsent(ammo.id(), in.readAllBytes());
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isAmmoRecipePath(String path) {
        String[] parts = path.split("/");
        return parts.length == 5
                && parts[0].equals("data")
                && parts[2].equals("recipe")
                && parts[3].equals("ammo")
                && parts[4].endsWith(".json");
    }

    private static AmmoData parseAmmoRecipe(InputStream in) {
        JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
        if (!root.has("result") || !root.get("result").isJsonObject()) {
            return null;
        }
        JsonObject result = root.getAsJsonObject("result");
        if (!result.has("type") || !"ammo".equals(result.get("type").getAsString())) {
            return null;
        }
        if (!result.has("id")) {
            return null;
        }
        String id = result.get("id").getAsString();
        int count = result.has("count") ? result.get("count").getAsInt() : 1;

        List<AmmoData.Material> materials = new ArrayList<>();
        if (root.has("materials") && root.get("materials").isJsonArray()) {
            JsonArray arr = root.getAsJsonArray("materials");
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject mat = el.getAsJsonObject();
                if (!mat.has("item") || !mat.get("item").isJsonObject()) {
                    continue;
                }
                JsonObject spec = mat.getAsJsonObject("item");
                int matCount = mat.has("count") ? mat.get("count").getAsInt() : 1;
                materials.add(new AmmoData.Material(spec, matCount));
            }
        }

        int colon = id.indexOf(':');
        String ns = colon > 0 ? id.substring(0, colon) : "tacz";
        String path = colon > 0 ? id.substring(colon + 1) : id;

        return new AmmoData(id, ns, path, count, materials);
    }
}
