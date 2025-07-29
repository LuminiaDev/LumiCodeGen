package com.luminiadev.lumi.codegen;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.palantir.javapoet.*;
import lombok.SneakyThrows;

import javax.lang.model.element.Modifier;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;

public class BlockTagsGen {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    public static void generate() {
        ClassName blockTagClass = ClassName.get("cn.nukkit.block.material.tags", "BlockTag");
        ClassName lazyBlockTagClass = ClassName.get("cn.nukkit.block.material.tags.impl", "LazyBlockTag");

        Map<String, Set<String>> vanillaBlockTags = getVanillaBlockTags();
        List<String> blockTags = new ArrayList<>(vanillaBlockTags.keySet());
        blockTags.sort(Comparator.naturalOrder());

        TypeSpec.Builder builder = TypeSpec.classBuilder("BlockTags")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Adding NAME_2_TAG and BLOCK_2_TAGS map field
        builder.addField(FieldSpec.builder(
                                        ParameterizedTypeName.get(
                                                ClassName.get("java.util", "Map"),
                                                ClassName.get(String.class),
                                                blockTagClass
                                        ),
                                        "NAME_2_TAG",
                                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL
                                )
                                .initializer("new $T<>()", ClassName.get("java.util", "HashMap"))
                                .build()
                )
                .addField(FieldSpec.builder(
                                        ParameterizedTypeName.get(
                                                ClassName.get("java.util", "Map"),
                                                ClassName.get(String.class),
                                                ParameterizedTypeName.get(
                                                        ClassName.get("java.util", "Set"),
                                                        blockTagClass
                                                )
                                        ),
                                        "BLOCK_2_TAGS",
                                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL
                                )
                                .initializer("new $T<>()", ClassName.get("java.util", "HashMap"))
                                .build()
                );

        // Adding block tags constants
        for (String blockTag : blockTags) {
            String name = blockTag.split(":")[1].toUpperCase();
            builder.addField(FieldSpec.builder(blockTagClass, name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("register($S, new $T($S))", blockTag, lazyBlockTagClass, blockTag)
                    .build()
            );
        }

        // Adding register and getter methods
        builder.addMethod(MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(blockTagClass)
                        .addParameter(String.class, "tagName")
                        .addParameter(blockTagClass, "blockTag")
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
                        .build()
                )
                .addMethod(MethodSpec.methodBuilder("getTagsSet")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ParameterizedTypeName.get(
                                ClassName.get("java.util", "Set"),
                                blockTagClass
                        ))
                        .addParameter(String.class, "identifier")
                        .addStatement("return BLOCK_2_TAGS.getOrDefault(identifier, $T.emptySet())",
                                ClassName.get("java.util", "Collections"))
                        .build()
                )
                .addMethod(MethodSpec.methodBuilder("getTag")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(blockTagClass)
                        .addParameter(String.class, "tagName")
                        .addStatement("return NAME_2_TAG.get(tagName)")
                        .build()
                )
                .build();

        JavaFile javaFile = JavaFile.builder("cn.nukkit.block.material.tags", builder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
    }

    @SneakyThrows
    private static Map<String, Set<String>> getVanillaBlockTags() {
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
}
