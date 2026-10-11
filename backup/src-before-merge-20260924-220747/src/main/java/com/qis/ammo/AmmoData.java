package com.qis.ammo;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * The recipe data for one TACZ ammo type, read from a gun pack's
 * {@code data/<pack>/recipe/ammo/*.json} file.
 *
 * @param id            full ammo id, e.g. "tacz:556x45"
 * @param namespace     namespace part, e.g. "tacz"
 * @param path          path part, e.g. "556x45"
 * @param resultCount   how many rounds the original gun-smith-table craft produces
 * @param materials     list of material entries (item/tag spec + count)
 */
public record AmmoData(String id, String namespace, String path, int resultCount, List<Material> materials) {

    public record Material(JsonObject item, int count) {

        /** True if this material is the copper casing (already covered by the copper plate). */
        public boolean isCopperCasing() {
            if (item == null) {
                return false;
            }
            if (item.has("tag")) {
                return "c:ingots/copper".equals(item.get("tag").getAsString());
            }
            if (item.has("item")) {
                String s = item.get("item").getAsString();
                return "minecraft:copper_ingot".equals(s) || "create:copper_ingot".equals(s);
            }
            return false;
        }

        /** Deep copy of the original item/tag spec object (for reuse in generated recipes). */
        public JsonObject itemSpec() {
            return item.deepCopy();
        }
    }

    /**
     * Make a resource-location-safe path. Ammo ids are normally already valid
     * (a-z0-9._-), this is just a safety net for unusual custom packs.
     */
    public static String sanitize(String s) {
        return s.replaceAll("[^a-z0-9/._-]", "_");
    }

    public static JsonObject ammoStackJson(String ammoId, int count) {
        JsonObject result = new JsonObject();
        result.addProperty("id", "tacz:ammo");
        result.addProperty("count", count);
        JsonObject components = new JsonObject();
        JsonObject customData = new JsonObject();
        customData.addProperty("AmmoId", ammoId);
        components.add("minecraft:custom_data", customData);
        result.add("components", components);
        return result;
    }
}
