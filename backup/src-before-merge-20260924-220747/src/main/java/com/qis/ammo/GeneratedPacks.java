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

/**
 * Writes the generated assets into the config folder:
 * <ul>
 *   <li>{@code config/qisammo/datapack/} - Create recipes (a runtime data pack)</li>
 *   <li>{@code config/qisammo/resourcepack/} - item models + lang for the incomplete items (a runtime resource pack)</li>
 * </ul>
 * Both folders are registered as pack sources via {@link PackFinders}.
 */
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

    public static void writeAll() {
        try {
            Files.createDirectories(datapackDir());
            Files.createDirectories(resourcepackDir());
            writeMeta(datapackDir(), DATAPACK_FORMAT);
            writeMeta(resourcepackDir(), RESOURCEPACK_FORMAT);
            clearDir(datapackDir().resolve("data"));
            clearDir(resourcepackDir().resolve("assets"));
            for (AmmoData ammo : AmmoScanner.getAmmos()) {
                writeRecipes(ammo);
                writeStampRecipe(ammo);
                writeItemModel(ammo);
            }
            writeTextures();
            writeLang();
            QISAmmoMod.LOGGER.info("[qisammo] generated {} ammo recipe sets", AmmoScanner.getAmmos().size());
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

    private static void writeRecipes(AmmoData ammo) throws IOException {
        String path = AmmoData.sanitize(ammo.path());
        String incompleteItem = "qisammo:incomplete_" + path;
        String stampItem = "qisammo:stamp_" + path;
        Path recipeDir = datapackDir().resolve("data/create/recipe/qisammo");
        Files.createDirectories(recipeDir);

        // Stage 1: engrave the copper plate with the per-ammo stamp as a non-consuming die.
        // NOTE: Create's recipe ingredients match by item id/tag only (no NBT/components),
        // so we MUST use a distinct stamp item per ammo instead of the tacz:ammo bullet.
        JsonObject engrave = new JsonObject();
        engrave.addProperty("type", "create:deploying");
        JsonArray engraveIngredients = new JsonArray();
        JsonObject plate = new JsonObject();
        plate.addProperty("tag", "qisammo:casing_plate");
        engraveIngredients.add(plate);
        JsonObject stamp = new JsonObject();
        stamp.addProperty("item", stampItem);
        engraveIngredients.add(stamp);
        engrave.add("ingredients", engraveIngredients);
        JsonArray engraveResults = new JsonArray();
        JsonObject engraveResult = new JsonObject();
        engraveResult.addProperty("id", incompleteItem);
        engraveResults.add(engraveResult);
        engrave.add("results", engraveResults);
        engrave.addProperty("keep_held_item", true);
        writeJson(recipeDir.resolve("engrave_" + path + ".json"), engrave);

        // Stage 2: sequenced assembly - saw once, press three times, then one
        // deployer per full ingredient count from the original TACZ recipe.
        JsonObject assembly = new JsonObject();
        assembly.addProperty("type", "create:sequenced_assembly");
        JsonObject input = new JsonObject();
        input.addProperty("item", incompleteItem);
        assembly.add("ingredient", input);
        JsonObject transitional = new JsonObject();
        transitional.addProperty("id", incompleteItem);
        assembly.add("transitional_item", transitional);

        JsonArray sequence = new JsonArray();
        sequence.add(machineStep("create:cutting", incompleteItem));
        sequence.add(machineStep("create:pressing", incompleteItem));
        sequence.add(machineStep("create:pressing", incompleteItem));
        sequence.add(machineStep("create:pressing", incompleteItem));
        for (AmmoData.Material material : ammo.materials()) {
            if (material.isCopperCasing()) {
                continue; // the copper plate already provides the casing
            }
            for (int i = 0; i < material.count(); i++) {
                sequence.add(deployStep(incompleteItem, material.itemSpec()));
            }
        }
        assembly.add("sequence", sequence);

        // Output the real TACZ ammo with the correct AmmoId component directly.
        JsonArray results = new JsonArray();
        results.add(AmmoData.ammoStackJson(ammo.id(), Math.max(1, ammo.resultCount())));
        assembly.add("results", results);

        writeJson(recipeDir.resolve("assemble_" + path + ".json"), assembly);
    }

    /**
     * A smithing-table recipe that turns one real bullet (matched by its AmmoId
     * component) into the reusable engraving stamp. The smithing table handles
     * NBT'd inputs/outputs reliably, avoiding the "AmmoId lost in crafting grid"
     * issue. Ingredients: paper template + flint tool applied to the bullet.
     */
    private static void writeStampRecipe(AmmoData ammo) throws IOException {
        String path = AmmoData.sanitize(ammo.path());
        Path recipeDir = datapackDir().resolve("data/qisammo/recipe");
        Files.createDirectories(recipeDir);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:smithing_transform");

        JsonObject template = new JsonObject();
        template.addProperty("item", "minecraft:paper");
        recipe.add("template", template);

        JsonObject base = new JsonObject();
        base.addProperty("item", "tacz:ammo");
        JsonObject components = new JsonObject();
        JsonObject customData = new JsonObject();
        customData.addProperty("AmmoId", ammo.id());
        components.add("minecraft:custom_data", customData);
        base.add("components", components);
        recipe.add("base", base);

        JsonObject addition = new JsonObject();
        addition.addProperty("item", "minecraft:flint");
        recipe.add("addition", addition);

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
        String path = AmmoData.sanitize(ammo.path());
        Path modelDir = resourcepackDir().resolve("assets/qisammo/models/item");
        Files.createDirectories(modelDir);
        JsonObject model = new JsonObject();
        model.addProperty("parent", "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", ammo.namespace() + ":ammo/slot/" + ammo.path());
        model.add("textures", textures);
        writeJson(modelDir.resolve("incomplete_" + path + ".json"), model);
        writeJson(modelDir.resolve("stamp_" + path + ".json"), model);
    }

    /**
     * Copy each ammo's slot texture PNG into the generated resource pack so the
     * stamp/incomplete item models resolve their texture through the vanilla
     * resource system (TACZ gun pack assets are not guaranteed to be vanilla-
     * accessible, which would otherwise leave the items as black-purple blocks).
     */
    private static void writeTextures() throws IOException {
        for (AmmoData ammo : AmmoScanner.getAmmos()) {
            byte[] tex = AmmoScanner.getTexture(ammo.id());
            if (tex == null) {
                continue;
            }
            Path texFile = resourcepackDir().resolve(
                    "assets/" + ammo.namespace() + "/textures/ammo/slot/" + ammo.path() + ".png");
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

    private static void writeJson(Path file, JsonObject json) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(json.toString());
        }
    }
}
