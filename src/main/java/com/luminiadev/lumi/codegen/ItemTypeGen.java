package com.luminiadev.lumi.codegen;

import com.google.gson.Gson;
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

public class ItemTypeGen {

    private static final Gson GSON = new Gson();
    private static final ClassName ITEM_TYPE_CLASS = ClassName.get("cn.nukkit.item.material", "ItemType");

    @SneakyThrows
    public static void generate() {
        List<ItemEntry> itemEntries = prepareItemEntries();

        TypeSpec itemTypesClass = TypeSpec.classBuilder("ItemTypes")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(createMapFields())
                .addFields(createItemConstants(itemEntries))
                .addMethods(createUtilityMethods())
                .addType(createItemTypeImpl())
                .build();

        JavaFile javaFile = JavaFile.builder("cn.nukkit.item.material", itemTypesClass)
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
    }

    private static List<ItemEntry> prepareItemEntries() {
        Map<String, Integer> cbLegacyItemIds = getLegacyItemIds("data/cloudburst/legacy_item_ids.json");

        List<ItemEntry> itemEntries = cbLegacyItemIds.entrySet().stream()
                .map(entry -> new ItemEntry(entry.getKey(), entry.getValue()))
                .sorted()
                .collect(Collectors.toList());

        return itemEntries;
    }

    private static List<FieldSpec> createMapFields() {
        return List.of(
                FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("it.unimi.dsi.fastutil.ints", "Int2ObjectMap"),
                                        ITEM_TYPE_CLASS
                                ),
                                "LEGACY_TO_TYPE",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("it.unimi.dsi.fastutil.ints", "Int2ObjectOpenHashMap"))
                        .build(),
                FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("it.unimi.dsi.fastutil.objects", "Object2ObjectMap"),
                                        ClassName.get(String.class),
                                        ITEM_TYPE_CLASS
                                ),
                                "ID_TO_TYPE",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("it.unimi.dsi.fastutil.objects", "Object2ObjectOpenHashMap"))
                        .build()
        );
    }

    private static List<FieldSpec> createItemConstants(List<ItemEntry> itemEntries) {
        return itemEntries.stream()
                .map(entry -> {
                    String name = entry.identifier.split(":")[1].toUpperCase();
                    return FieldSpec.builder(
                                    ITEM_TYPE_CLASS,
                                    name,
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
                        .returns(ITEM_TYPE_CLASS)
                        .addParameter(String.class, "identifier")
                        .addParameter(TypeName.INT, "legacyId")
                        .addStatement("return register(new ItemTypeImpl(identifier, legacyId))")
                        .build(),
                MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ITEM_TYPE_CLASS)
                        .addParameter(ITEM_TYPE_CLASS, "itemType")
                        .addStatement("ItemType oldType = ID_TO_TYPE.get(itemType.getIdentifier())")
                        .addStatement("LEGACY_TO_TYPE.putIfAbsent(itemType.getLegacyId(), itemType)")
                        .addStatement("ID_TO_TYPE.putIfAbsent(itemType.getIdentifier(), itemType)")
                        .addStatement("return oldType != null ? oldType : itemType")
                        .build(),
                MethodSpec.methodBuilder("get")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ITEM_TYPE_CLASS)
                        .addParameter(String.class, "identifier")
                        .addStatement("return ID_TO_TYPE.get(identifier)")
                        .build(),
                MethodSpec.methodBuilder("getFromLegacy")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ITEM_TYPE_CLASS)
                        .addParameter(TypeName.INT, "legacyId")
                        .addStatement("return LEGACY_TO_TYPE.get(legacyId)")
                        .build()
        );
    }

    private static TypeSpec createItemTypeImpl() {
        return TypeSpec.classBuilder("ItemTypeImpl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addSuperinterface(ITEM_TYPE_CLASS)
                .addAnnotation(Data.class)
                .addField(FieldSpec.builder(String.class, "identifier", Modifier.PRIVATE, Modifier.FINAL).build())
                .addField(FieldSpec.builder(TypeName.INT, "legacyId", Modifier.PRIVATE, Modifier.FINAL).build())
                .build();
    }

    @SneakyThrows
    private static Map<String, Integer> getLegacyItemIds(String path) {
        var inputStream = ItemTypeGen.class.getClassLoader().getResourceAsStream(path);
        if (inputStream != null) {
            try (var reader = new InputStreamReader(inputStream)) {
                Type type = new TypeToken<Map<String, Integer>>() {
                }.getType();
                return GSON.fromJson(reader, type);
            }
        }
        return new HashMap<>();
    }

    private record ItemEntry(String identifier, int legacyId) implements Comparable<ItemEntry> {
        @Override
        public int compareTo(@NonNull ItemEntry entry) {
            return this.identifier.compareTo(entry.identifier);
        }
    }
}