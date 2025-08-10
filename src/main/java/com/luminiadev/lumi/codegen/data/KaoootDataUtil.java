package com.luminiadev.lumi.codegen.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luminiadev.lumi.codegen.generator.BlockTagsGen;
import com.luminiadev.lumi.codegen.generator.BlockTypeGen;
import com.luminiadev.lumi.codegen.generator.ItemTagsGen;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class KaoootDataUtil {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    public Map<String, Set<String>> getVanillaBlockTags() {
        var inputStream = BlockTagsGen.class.getClassLoader().getResourceAsStream("data/kaooot/block_tags.json");
        if (inputStream != null) {
            try (var reader = new InputStreamReader(inputStream)) {
                var type = new TypeToken<Map<String, Set<String>>>() {
                }.getType();
                return GSON.fromJson(reader, type);
            }
        }
        return new HashMap<>();
    }

    @SneakyThrows
    public Map<String, Set<String>> getVanillaItemTags() {
        var inputStream = ItemTagsGen.class.getClassLoader().getResourceAsStream("data/kaooot/item_tags.json");
        if (inputStream != null) {
            try (var reader = new InputStreamReader(inputStream)) {
                var type = new TypeToken<Map<String, Set<String>>>() {
                }.getType();
                return GSON.fromJson(reader, type);
            }
        }
        return new HashMap<>();
    }

    @SneakyThrows
    public Map<String, Integer> getItemPalette() {
        var inputStream = BlockTypeGen.class.getClassLoader().getResourceAsStream("data/kaooot/item_palette.json");
        if (inputStream != null) {
            try (var reader = new InputStreamReader(inputStream)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                JsonArray items = root.getAsJsonArray("items");

                Map<String, Integer> legacyItemIds = new HashMap<>();
                for (JsonElement element : items) {
                    JsonObject obj = element.getAsJsonObject();
                    String name = obj.get("name").getAsString();
                    int id = obj.get("id").getAsInt();
                    legacyItemIds.put(name, id);
                }
                return legacyItemIds;
            }
        }
        return new HashMap<>();
    }
}
