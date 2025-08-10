package com.luminiadev.lumi.codegen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import lombok.SneakyThrows;

import javax.lang.model.element.Modifier;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemNamespaceIdGen {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    public static void generate() {
        List<String> itemIds = prepareItemEntries();

        TypeSpec itemTypesClass = TypeSpec.interfaceBuilder("ItemNamespaceId")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC)
                .addFields(createItemConstants(itemIds))
                .build();

        JavaFile javaFile = JavaFile.builder("cn.nukkit.item", itemTypesClass)
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
    }

    private static List<String> prepareItemEntries() {
        List<String> itemEntries = getItemPalette().keySet().stream()
                .sorted()
                .filter(id -> !id.startsWith("minecraft:item."))
                .collect(Collectors.toList());
        return itemEntries;
    }

    private static List<FieldSpec> createItemConstants(List<String> itemIds) {
        return itemIds.stream()
                .map(id -> {
                    String name = id.split(":")[1].toUpperCase();
                    return FieldSpec.builder(
                                    String.class,
                                    name,
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$S", id)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private static Map<String, Integer> getItemPalette() {
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