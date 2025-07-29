package com.luminiadev.lumi.codegen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.palantir.javapoet.*;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;

import javax.lang.model.element.Modifier;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class BlockTypeGen {

    private static final Gson GSON = new Gson();
    private static final ClassName BLOCK_TYPE_CLASS = ClassName.get("cn.nukkit.block.material", "BlockType");

    @SneakyThrows
    public static void generate() {
        List<BlockEntry> blockEntries = prepareBlockEntries();

        TypeSpec blockTypesClass = TypeSpec.classBuilder("BlockTypes")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(createMapFields())
                .addFields(createBlockConstants(blockEntries))
                .addMethods(createUtilityMethods())
                .addType(createBlockTypeImpl())
                .build();

        JavaFile javaFile = JavaFile.builder("cn.nukkit.block.material", blockTypesClass)
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
    }

    private static List<BlockEntry> prepareBlockEntries() {
        Map<String, Integer> itemPalette = getItemPalette();
        Map<String, Integer> legacyBlockIds = getLegacyItemIds("data/cloudburst/legacy_block_ids.json");
        Map<String, Integer> internalItemIds = getLegacyItemIds("data/internal/legacy_item_ids.json");

        List<BlockEntry> blockEntries = itemPalette.entrySet().stream()
                .filter(entry -> legacyBlockIds.containsKey(entry.getKey()) || internalItemIds.containsKey(entry.getKey()))
                .map(entry -> new BlockEntry(entry.getKey(), entry.getValue(), entry.getKey().startsWith("minecraft:item.")))
                .collect(Collectors.toList());

        Set<String> itemIds = blockEntries.stream()
                .filter(BlockEntry::item)
                .map(entry -> entry.identifier.replaceFirst("^minecraft:item\\.", "minecraft:"))
                .collect(Collectors.toSet());

        blockEntries.removeIf(entry -> !entry.item && itemIds.contains(entry.identifier));
        blockEntries.sort(Comparator.naturalOrder());

        return blockEntries;
    }

    private static List<FieldSpec> createMapFields() {
        return List.of(
                FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("it.unimi.dsi.fastutil.ints", "Int2ObjectMap"),
                                        BLOCK_TYPE_CLASS
                                ),
                                "LEGACY_TO_TYPE",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("it.unimi.dsi.fastutil.ints", "Int2ObjectOpenHashMap"))
                        .build(),
                FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("it.unimi.dsi.fastutil.objects", "Object2ObjectMap"),
                                        ClassName.get(String.class),
                                        BLOCK_TYPE_CLASS
                                ),
                                "ID_TO_TYPE",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("it.unimi.dsi.fastutil.objects", "Object2ObjectOpenHashMap"))
                        .build()
        );
    }

    private static List<FieldSpec> createBlockConstants(List<BlockEntry> blockEntries) {
        return blockEntries.stream()
                .map(entry -> {
                    String blockName = entry.identifier.split(":")[1].toUpperCase();
                    String finalName = entry.item ? blockName.replace("ITEM.", "") : blockName;
                    return FieldSpec.builder(
                                    BLOCK_TYPE_CLASS,
                                    finalName,
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("register($S, $L)", entry.identifier, entry.legacyId)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static List<MethodSpec> createUtilityMethods() {
        return List.of(
                MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(BLOCK_TYPE_CLASS)
                        .addParameter(String.class, "identifier")
                        .addParameter(TypeName.INT, "legacyId")
                        .addStatement("return register(new BlockTypeImpl(identifier, legacyId))")
                        .build(),
                MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(BLOCK_TYPE_CLASS)
                        .addParameter(BLOCK_TYPE_CLASS, "blockType")
                        .addStatement("BlockType oldType = ID_TO_TYPE.get(blockType.getIdentifier())")
                        .addStatement("LEGACY_TO_TYPE.putIfAbsent(blockType.getLegacyId(), blockType)")
                        .addStatement("ID_TO_TYPE.putIfAbsent(blockType.getIdentifier(), blockType)")
                        .addStatement("$T.register(blockType.getIdentifier(), blockType.getLegacyId())",
                                ClassName.get("cn.nukkit.item.material", "ItemTypes"))
                        .addStatement("return oldType != null ? oldType : blockType")
                        .build(),
                MethodSpec.methodBuilder("get")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(BLOCK_TYPE_CLASS)
                        .addParameter(String.class, "identifier")
                        .addStatement("return ID_TO_TYPE.get(identifier)")
                        .build(),
                MethodSpec.methodBuilder("getFromLegacy")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(BLOCK_TYPE_CLASS)
                        .addParameter(TypeName.INT, "legacyId")
                        .addStatement("return LEGACY_TO_TYPE.get(legacyId)")
                        .build()
        );
    }

    private static TypeSpec createBlockTypeImpl() {
        return TypeSpec.classBuilder("BlockTypeImpl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addSuperinterface(BLOCK_TYPE_CLASS)
                .addAnnotation(Data.class)
                .addField(FieldSpec.builder(String.class, "identifier", Modifier.PRIVATE, Modifier.FINAL).build())
                .addField(FieldSpec.builder(TypeName.INT, "legacyId", Modifier.PRIVATE, Modifier.FINAL).build())
                .build();
    }

    @SneakyThrows
    private static Map<String, Integer> getLegacyItemIds(String path) {
        var inputStream = BlockTypeGen.class.getClassLoader().getResourceAsStream(path);
        if (inputStream != null) {
            try (var reader = new InputStreamReader(inputStream)) {
                Type type = new TypeToken<Map<String, Integer>>() {
                }.getType();
                return GSON.fromJson(reader, type);
            }
        }
        return new HashMap<>();
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

    private record BlockEntry(String identifier, int legacyId, boolean item) implements Comparable<BlockEntry> {
        @Override
        public int compareTo(@NonNull BlockEntry entry) {
            return this.identifier.compareTo(entry.identifier);
        }
    }
}