package com.qis.ammo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class GeneratedPacks {

    private static final String DATAPACK_FORMAT = "48";
    private static final String RESOURCEPACK_FORMAT = "34";

    private GeneratedPacks() {
    }

    private static Path baseDir() {
        return FMLPaths.CONFIGDIR.get().resolve("qisammo");
    }

    private static Path datapackDir() {
        return baseDir().resolve("datapack");
    }

    private static Path resourcepackDir() {
        return baseDir().resolve("resourcepack");
    }

    private static Path resourcePackZip() {
        return baseDir().resolve("qisammo_resources.zip");
    }

    public static void writeAll() {
        boolean canRecipes = net.neoforged.fml.ModList.get().isLoaded("create")
                && net.neoforged.fml.ModList.get().isLoaded("tacz");
        if (!canRecipes) {
            QISAmmoMod.LOGGER.info("[qisammo] create/tacz not loaded - skipping recipe generation");
        }
        try {
            clearDir(datapackDir());
            clearDir(resourcepackDir());
            Files.createDirectories(datapackDir());
            Files.createDirectories(resourcepackDir());
            writeMeta(datapackDir(), DATAPACK_FORMAT);
            writeMeta(resourcepackDir(), RESOURCEPACK_FORMAT);
            for (AmmoData ammo : AmmoScanner.getAmmos()) {
                if (canRecipes) {
                    writeStampRecipe(ammo);
                    writeAssemblyRecipe(ammo);
                }
                writeItemModel(ammo);
            }
            writeTextures();
            writeLang();
            zipResourcePack();
            QISAmmoMod.LOGGER.info("[qisammo] generated {} ammo recipe sets; resource pack -> {}",
                    AmmoScanner.getAmmos().size(), resourcePackZip());
        } catch (IOException e) {
            QISAmmoMod.LOGGER.error("[qisammo] failed to write generated packs", e);
        }
    }

    private static void writeMeta(Path dir, String format) throws IOException {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", Integer.parseInt(format));
        pack.addProperty("description", "qisammo generated pack");
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        writeJson(dir.resolve("pack.mcmeta"), root);
    }

    private static void clearDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static void writeAssemblyRecipe(AmmoData ammo) throws IOException {
        String path = AmmoData.sanitize(ammo.path());
        String stampItem = "qisammo:stamp_" + path;
        String incompleteItem = "qisammo:incomplete_" + path;
        Path recipeDir = datapackDir().resolve("data/create/recipe/qisammo");
        Files.createDirectories(recipeDir);

        JsonObject assembly = new JsonObject();
        assembly.addProperty("type", "create:sequenced_assembly");
        JsonObject input = new JsonObject();
        input.addProperty("item", stampItem);
        assembly.add("ingredient", input);
        JsonObject transitional = new JsonObject();
        transitional.addProperty("id", incompleteItem);
        assembly.add("transitional_item", transitional);

        JsonArray sequence = new JsonArray();
        sequence.add(machineStep("create:cutting", incompleteItem));
        sequence.add(machineStep("create:pressing", incompleteItem));
        for (AmmoData.Material material : ammo.materials()) {
            if (material.isCopperCasing()) {
                continue;
            }
            for (int i = 0; i < material.count(); i++) {
                sequence.add(deployStep(incompleteItem, material.itemSpec()));
            }
        }
        assembly.add("sequence", sequence);

        JsonArray results = new JsonArray();
        results.add(AmmoData.ammoStackJson(ammo.id(), Math.min(99, Math.max(1, ammo.resultCount()))));
        assembly.add("results", results);

        writeJson(recipeDir.resolve("assemble_" + path + ".json"), assembly);
    }

    private static void writeStampRecipe(AmmoData ammo) throws IOException {
        String path = AmmoData.sanitize(ammo.path());
        Path recipeDir = datapackDir().resolve("data/minecraft/recipe/qisammo");
        Files.createDirectories(recipeDir);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:stonecutting");

        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("tag", "qisammo:casing_plate");
        recipe.add("ingredient", ingredient);

        JsonObject result = new JsonObject();
        result.addProperty("id", "qisammo:stamp_" + path);
        result.addProperty("count", 1);
        recipe.add("result", result);

        writeJson(recipeDir.resolve("stamp_" + path + ".json"), recipe);
    }

    private static JsonObject machineStep(String type, String itemName) {
        JsonObject step = new JsonObject();
        step.addProperty("type", type);
        JsonArray ingredients = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("item", itemName);
        ingredients.add(item);
        step.add("ingredients", ingredients);
        JsonArray results = new JsonArray();
        JsonObject out = new JsonObject();
        out.addProperty("id", itemName);
        results.add(out);
        step.add("results", results);
        return step;
    }

    private static JsonObject deployStep(String itemName, JsonObject appliedItem) {
        JsonObject step = new JsonObject();
        step.addProperty("type", "create:deploying");
        JsonArray ingredients = new JsonArray();
        JsonObject base = new JsonObject();
        base.addProperty("item", itemName);
        ingredients.add(base);
        ingredients.add(appliedItem);
        step.add("ingredients", ingredients);
        JsonArray results = new JsonArray();
        JsonObject out = new JsonObject();
        out.addProperty("id", itemName);
        results.add(out);
        step.add("results", results);
        return step;
    }

    private static void writeItemModel(AmmoData ammo) throws IOException {
        writeItemModelPath(AmmoData.sanitize(ammo.path()));
    }

    private static void writeItemModelPath(String path) throws IOException {
        Path modelDir = resourcepackDir().resolve("assets/qisammo/models/item");
        Files.createDirectories(modelDir);
        JsonObject model = new JsonObject();
        model.addProperty("parent", "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", QISAmmoMod.MODID + ":item/" + path);
        model.add("textures", textures);
        writeJson(modelDir.resolve("incomplete_" + path + ".json"), model);
        writeJson(modelDir.resolve("stamp_" + path + ".json"), model);
    }

    private static void writeTextures() throws IOException {
        for (AmmoData ammo : AmmoScanner.getAmmos()) {
            byte[] tex = AmmoScanner.getTexture(ammo.id());
            if (tex == null) {
                continue;
            }
            String path = AmmoData.sanitize(ammo.path());
            Path texFile = resourcepackDir().resolve(
                    "assets/" + QISAmmoMod.MODID + "/textures/item/" + path + ".png");
            Files.createDirectories(texFile.getParent());
            Files.write(texFile, tex);
        }
    }

    private static void writeLang() throws IOException {
        Path langDir = resourcepackDir().resolve("assets/qisammo/lang");
        Files.createDirectories(langDir);
        JsonObject en = new JsonObject();
        JsonObject zh = new JsonObject();
        for (AmmoData ammo : AmmoScanner.getAmmos()) {
            String path = AmmoData.sanitize(ammo.path());
            en.addProperty("item.qisammo.incomplete_" + path, "Incomplete Ammo (" + path + ")");
            en.addProperty("item.qisammo.stamp_" + path, "Bullet Stamp (" + path + ")");
            zh.addProperty("item.qisammo.incomplete_" + path, "未完成弹药 (" + path + ")");
            zh.addProperty("item.qisammo.stamp_" + path, "子弹刻印模具 (" + path + ")");
        }
        writeJson(langDir.resolve("en_us.json"), en);
        writeJson(langDir.resolve("zh_cn.json"), zh);
    }

    private static void zipResourcePack() throws IOException {
        Path zip = resourcePackZip();
        Files.deleteIfExists(zip);
        Path root = resourcepackDir();
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path p : walk.filter(Files::isRegularFile).sorted().toList()) {
                    String rel = root.relativize(p).toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(rel));
                    Files.copy(p, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private static void writeJson(Path file, JsonObject json) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(json.toString());
        }
    }
}
