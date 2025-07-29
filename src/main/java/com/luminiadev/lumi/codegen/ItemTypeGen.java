package com.luminiadev.lumi.codegen;

import cn.nukkit.item.Item;
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

public class ItemTypeGen {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    public static void generate() {
        ClassName itemTypeClass = ClassName.get("cn.nukkit.item.material", "ItemType");

        Map<String, Integer> internalLegacyItemIds = getLegacyItemIds("data/internal/legacy_item_ids.json");
        Map<String, Integer> cbLegacyItemIds = getLegacyItemIds("data/cloudburst/legacy_item_ids.json");
        Map<String, Integer> cbLegacyBlockIds = getLegacyItemIds("data/cloudburst/legacy_block_ids.json");

        List<ItemEntry> itemEntries = new ArrayList<>();
        internalLegacyItemIds.forEach((identifier, legacyId) -> {
            if (!cbLegacyItemIds.containsKey(identifier.toLowerCase())) {
                return;
            }
            // Some item ids in Lumi legacy_item_ids.json contains "minecraft:item."
            if (identifier.contains("item.")) {
                identifier = identifier.replace("item.", "");
            }
            itemEntries.add(new ItemEntry(identifier, legacyId));
        });
        cbLegacyItemIds.forEach((identifier, legacyId) -> {
            if (!cbLegacyBlockIds.containsKey(identifier) && !internalLegacyItemIds.containsKey(identifier)) {
                itemEntries.add(new ItemEntry(identifier, Item.STRING_IDENTIFIED_ITEM));
            }
        });
        itemEntries.sort(Comparator.naturalOrder());

        TypeSpec.Builder builder = TypeSpec.classBuilder("ItemTypes")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Adding ID_TO_TYPE and LEGACY_TO_TYPE maps fields
        builder.addField(FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("it.unimi.dsi.fastutil.ints", "Int2ObjectMap"),
                                        itemTypeClass
                                ),
                                "LEGACY_TO_TYPE",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("it.unimi.dsi.fastutil.ints", "Int2ObjectOpenHashMap"))
                        .build())
                .addField(FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("it.unimi.dsi.fastutil.objects", "Object2ObjectMap"),
                                        ClassName.get(String.class),
                                        itemTypeClass
                                ),
                                "ID_TO_TYPE",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("it.unimi.dsi.fastutil.objects", "Object2ObjectOpenHashMap"))
                        .build()
                );

        // Adding item type constants
        for (ItemEntry itemEntry : itemEntries) {
            String name = itemEntry.identifier.split(":")[1].toUpperCase();
            builder.addField(FieldSpec.builder(
                            itemTypeClass,
                            name,
                            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("register($S, $L)", itemEntry.identifier, itemEntry.legacyId)
                    .build()
            );
        }

        // Adding register and getter methods
        builder.addMethod(MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(itemTypeClass)
                        .addParameter(String.class, "identifier")
                        .addParameter(TypeName.INT, "legacyId")
                        .addStatement("return register(new ItemTypeImpl(identifier, legacyId))")
                        .build())
                .addMethod(MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(itemTypeClass)
                        .addParameter(itemTypeClass, "itemType")
                        .addStatement("ItemType oldType = ID_TO_TYPE.get(itemType.getIdentifier())")
                        .addStatement("LEGACY_TO_TYPE.putIfAbsent(itemType.getLegacyId(), itemType)")
                        .addStatement("ID_TO_TYPE.putIfAbsent(itemType.getIdentifier(), itemType)")
                        .addStatement("return oldType != null ? oldType : itemType")
                        .build())
                .addMethod(MethodSpec.methodBuilder("get")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(itemTypeClass)
                        .addParameter(String.class, "identifier")
                        .addStatement("return ID_TO_TYPE.get(identifier)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getFromLegacy")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(itemTypeClass)
                        .addParameter(TypeName.INT, "legacyId")
                        .addStatement("return LEGACY_TO_TYPE.get(legacyId)")
                        .build())
                .addType(TypeSpec.classBuilder("ItemTypeImpl")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addSuperinterface(itemTypeClass)
                        .addAnnotation(Data.class)
                        .addField(FieldSpec.builder(String.class, "identifier", Modifier.PRIVATE, Modifier.FINAL).build())
                        .addField(FieldSpec.builder(TypeName.INT, "legacyId", Modifier.PRIVATE, Modifier.FINAL).build())
                        .build());

        JavaFile javaFile = JavaFile.builder("cn.nukkit.item.material", builder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
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
