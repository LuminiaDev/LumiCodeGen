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

public class BlockTypeGen {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    public static void generate() {
        ClassName blockTypeClass = ClassName.get("cn.nukkit.block.material", "BlockType");

        Map<String, Integer> internalLegacyItemIds = getLegacyItemIds("data/internal/legacy_item_ids.json");
        Map<String, Integer> cbLegacyBlockIds = getLegacyItemIds("data/cloudburst/legacy_block_ids.json");

        List<BlockEntry> blockEntries = new ArrayList<>();
        internalLegacyItemIds.forEach((identifier, legacyId) -> {
            if (!cbLegacyBlockIds.containsKey(identifier.toLowerCase())) {
                return;
            }
            blockEntries.add(new BlockEntry(identifier, legacyId));
        });
        blockEntries.sort(Comparator.naturalOrder());

        TypeSpec.Builder builder = TypeSpec.classBuilder("BlockTypes")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Adding ID_TO_TYPE and LEGACY_TO_TYPE maps fields
        builder.addField(FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("it.unimi.dsi.fastutil.ints", "Int2ObjectMap"),
                                        blockTypeClass
                                ),
                                "LEGACY_TO_TYPE",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("it.unimi.dsi.fastutil.ints", "Int2ObjectOpenHashMap"))
                        .build())
                .addField(FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("it.unimi.dsi.fastutil.objects", "Object2ObjectMap"),
                                        ClassName.get(String.class),
                                        blockTypeClass
                                ),
                                "ID_TO_TYPE",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("it.unimi.dsi.fastutil.objects", "Object2ObjectOpenHashMap"))
                        .build()
                );

        // Adding block type constants
        for (BlockEntry blockEntry : blockEntries) {
            String name = blockEntry.identifier.split(":")[1].toUpperCase();
            builder.addField(FieldSpec.builder(
                            blockTypeClass,
                            name,
                            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("register($S, $L)", blockEntry.identifier, blockEntry.legacyId)
                    .build()
            );
        }

        // Adding register and getter methods
        builder.addMethod(MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(blockTypeClass)
                        .addParameter(String.class, "identifier")
                        .addParameter(TypeName.INT, "legacyId")
                        .addStatement("return register(new BlockTypeImpl(identifier, legacyId))")
                        .build())
                .addMethod(MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(blockTypeClass)
                        .addParameter(blockTypeClass, "blockType")
                        .addStatement("BlockType oldType = ID_TO_TYPE.get(blockType.getIdentifier())")
                        .addStatement("LEGACY_TO_TYPE.putIfAbsent(blockType.getLegacyId(), blockType)")
                        .addStatement("ID_TO_TYPE.putIfAbsent(blockType.getIdentifier(), blockType)")
                        .addStatement("$T.register(blockType.getIdentifier(), blockType.getLegacyId())",
                                ClassName.get("cn.nukkit.item.material", "ItemTypes"))
                        .addStatement("return oldType != null ? oldType : blockType")
                        .build())
                .addMethod(MethodSpec.methodBuilder("get")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(blockTypeClass)
                        .addParameter(String.class, "identifier")
                        .addStatement("return ID_TO_TYPE.get(identifier)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getFromLegacy")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(blockTypeClass)
                        .addParameter(TypeName.INT, "legacyId")
                        .addStatement("return LEGACY_TO_TYPE.get(legacyId)")
                        .build())
                .addType(TypeSpec.classBuilder("BlockTypeImpl")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addSuperinterface(blockTypeClass)
                        .addAnnotation(Data.class)
                        .addField(FieldSpec.builder(String.class, "identifier", Modifier.PRIVATE, Modifier.FINAL).build())
                        .addField(FieldSpec.builder(TypeName.INT, "legacyId", Modifier.PRIVATE, Modifier.FINAL).build())
                        .build());

        JavaFile javaFile = JavaFile.builder("cn.nukkit.block.material", builder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
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

    private record BlockEntry(String identifier, int legacyId) implements Comparable<BlockEntry> {
        @Override
        public int compareTo(@NonNull BlockTypeGen.BlockEntry entry) {
            return this.identifier.compareTo(entry.identifier);
        }
    }
}
