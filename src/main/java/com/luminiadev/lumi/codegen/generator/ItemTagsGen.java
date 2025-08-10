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

public class ItemTagsGen {
    private static final ClassName ITEM_TAG_CLASS = ClassName.get("cn.nukkit.item.material.tags", "ItemTag");
    private static final ClassName LAZY_ITEM_TAG_CLASS = ClassName.get("cn.nukkit.item.material.tags.impl", "LazyItemTag");

    @SneakyThrows
    public static void generate() {
        List<String> itemTags = prepareItemTags();

        TypeSpec itemTagsClass = TypeSpec.classBuilder("ItemTags")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(createMapFields())
                .addFields(createTagConstants(itemTags))
                .addMethods(createUtilityMethods())
                .build();

        JavaFile javaFile = JavaFile.builder("cn.nukkit.item.material.tags", itemTagsClass)
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
    }

    private static List<String> prepareItemTags() {
        Map<String, Set<String>> vanillaItemTags = KaoootDataUtil.getVanillaItemTags();
        return vanillaItemTags.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<FieldSpec> createMapFields() {
        return List.of(
                FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get("java.util", "Map"),
                                        ClassName.get(String.class),
                                        ITEM_TAG_CLASS
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
                                                ITEM_TAG_CLASS
                                        )
                                ),
                                "ITEM_2_TAGS",
                                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T<>()", ClassName.get("java.util", "HashMap"))
                        .build()
        );
    }

    private static List<FieldSpec> createTagConstants(List<String> itemTags) {
        return itemTags.stream()
                .map(itemTag -> {
                    String name = itemTag.split(":")[1].toUpperCase();
                    return FieldSpec.builder(ITEM_TAG_CLASS, name)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("register($S, new $T($S))", itemTag, LAZY_ITEM_TAG_CLASS, itemTag)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static List<MethodSpec> createUtilityMethods() {
        return List.of(
                MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ITEM_TAG_CLASS)
                        .addParameter(String.class, "tagName")
                        .addParameter(ITEM_TAG_CLASS, "itemTag")
                        .beginControlFlow("if (NAME_2_TAG.containsKey(tagName))")
                        .addStatement("throw new IllegalArgumentException($S + tagName + $S)", "Item tag ", " is already registered")
                        .endControlFlow()
                        .addStatement("NAME_2_TAG.put(tagName, itemTag)")
                        .beginControlFlow("for ($T itemType : itemTag.getItemTypes())",
                                ClassName.get("cn.nukkit.item.material", "ItemType"))
                        .addStatement("ITEM_2_TAGS.computeIfAbsent(itemType.getIdentifier(), t -> new $T<>()).add(itemTag)",
                                ClassName.get("java.util", "HashSet"))
                        .endControlFlow()
                        .addStatement("return itemTag")
                        .build(),
                MethodSpec.methodBuilder("getTagsSet")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ParameterizedTypeName.get(
                                ClassName.get("java.util", "Set"),
                                ITEM_TAG_CLASS
                        ))
                        .addParameter(String.class, "identifier")
                        .addStatement("return ITEM_2_TAGS.getOrDefault(identifier, $T.emptySet())",
                                ClassName.get("java.util", "Collections"))
                        .build(),
                MethodSpec.methodBuilder("getTag")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ITEM_TAG_CLASS)
                        .addParameter(String.class, "tagName")
                        .addStatement("return NAME_2_TAG.get(tagName)")
                        .build()
        );
    }
}