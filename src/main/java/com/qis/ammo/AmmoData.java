package com.qis.ammo;

import com.google.gson.JsonObject;

import java.util.List;

public record AmmoData(String id, String namespace, String path, int resultCount, List<Material> materials) {

    public record Material(JsonObject item, int count) {

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

        public boolean isGunpowder() {
            if (item == null) {
                return false;
            }
            if (item.has("tag")) {
                return item.get("tag").getAsString().contains("gunpowder");
            }
            if (item.has("item")) {
                return "minecraft:gunpowder".equals(item.get("item").getAsString());
            }
            return false;
        }

        public JsonObject itemSpec() {
            return item.deepCopy();
        }
    }

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
