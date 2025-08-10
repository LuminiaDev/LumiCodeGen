package com.luminiadev.lumi.codegen.generator;

import com.luminiadev.lumi.codegen.data.KaoootDataUtil;
import com.palantir.javapoet.*;
import lombok.SneakyThrows;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockTagsGen {
    private static final ClassName BLOCK_TAG_CLASS = ClassName.get("cn.nukkit.block.material.tags", "BlockTag");
    private static final ClassName LAZY_BLOCK_TAG_CLASS = ClassName.get("cn.nukkit.block.material.tags.impl", "LazyBlockTag");

    @SneakyThrows
    public static void generate() {
        List<String> blockTags = prepareBlockTags();

        TypeSpec blockTagsClass = TypeSpec.classBuilder("BlockTags")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(createMapFields())
                .addFields(createTagConstants(blockTags))
                .addMethods(createUtilityMethods())
                .build();

        JavaFile javaFile = JavaFile.builder("cn.nukkit.block.material.tags", blockTagsClass)
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
    }

    private static List<String> prepareBlockTags() {
        Map<String, Set<String>> vanillaBlockTags = KaoootDataUtil.getVanillaBlockTags();
        return vanillaBlockTags.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<FieldSpec> createMapFields() {
        return List.of(
                FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("java.util", "Map"),
                                        ClassName.get(String.class),
                                        BLOCK_TAG_CLASS
                                ),
                                "NAME_2_TAG",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("java.util", "HashMap"))
                        .build(),
                FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("java.util", "Map"),
                                        ClassName.get(String.class),
                                        ParameterizedTypeName.get(
                                                ClassName.get("java.util", "Set"),
                                                BLOCK_TAG_CLASS
                                        )
                                ),
                                "BLOCK_2_TAGS",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("java.util", "HashMap"))
                        .build()
        );
    }

    private static List<FieldSpec> createTagConstants(List<String> blockTags) {
        return blockTags.stream()
                .map(blockTag -> {
                    String name = blockTag.split(":")[1].toUpperCase();
                    return FieldSpec.builder(BLOCK_TAG_CLASS, name)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("register($S, new $T($S))", blockTag, LAZY_BLOCK_TAG_CLASS, blockTag)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static List<MethodSpec> createUtilityMethods() {
        return List.of(
                MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(BLOCK_TAG_CLASS)
                        .addParameter(String.class, "tagName")
                        .addParameter(BLOCK_TAG_CLASS, "blockTag")
                        .beginControlFlow("if (NAME_2_TAG.containsKey(tagName))")
                        .addStatement("throw new IllegalArgumentException($S + tagName + $S)", "Block tag ", " is already registered")
                        .endControlFlow()
                        .addStatement("NAME_2_TAG.put(tagName, blockTag)")
                        .beginControlFlow("for ($T blockType : blockTag.getBlockTypes())",
                                ClassName.get("cn.nukkit.block.material", "BlockType"))
                        .addStatement("BLOCK_2_TAGS.computeIfAbsent(blockType.getIdentifier(), t -> new $T<>()).add(blockTag)",
                                ClassName.get("java.util", "HashSet"))
                        .endControlFlow()
                        .addStatement("return blockTag")
                        .build(),
                MethodSpec.methodBuilder("getTagsSet")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ParameterizedTypeName.get(
                                ClassName.get("java.util", "Set"),
                                BLOCK_TAG_CLASS
                        ))
                        .addParameter(String.class, "identifier")
                        .addStatement("return BLOCK_2_TAGS.getOrDefault(identifier, $T.emptySet())",
                                ClassName.get("java.util", "Collections"))
                        .build(),
                MethodSpec.methodBuilder("getTag")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(BLOCK_TAG_CLASS)
                        .addParameter(String.class, "tagName")
                        .addStatement("return NAME_2_TAG.get(tagName)")
                        .build()
        );
    }
}