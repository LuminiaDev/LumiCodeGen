package com.luminiadev.lumi.codegen;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.palantir.javapoet.*;
import lombok.SneakyThrows;

import javax.lang.model.element.Modifier;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;

public class ItemTagsGen {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    public static void generate() {
        ClassName itemTagClass = ClassName.get("cn.nukkit.item.material.tags", "ItemTag");
        ClassName lazyItemTagClass = ClassName.get("cn.nukkit.item.material.tags.impl", "LazyItemTag");

        Map<String, Set<String>> vanillaItemTags = getVanillaItemTags();
        List<String> itemTags = new ArrayList<>(vanillaItemTags.keySet());
        itemTags.sort(Comparator.naturalOrder());

        TypeSpec.Builder builder = TypeSpec.classBuilder("ItemTags")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Adding NAME_2_TAG and BLOCK_2_TAGS map field
        builder.addField(FieldSpec.builder(
                                        ParameterizedTypeName.get(
                                                ClassName.get("java.util", "Map"),
                                                ClassName.get(String.class),
                                                itemTagClass
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
                                                        itemTagClass
                                                )
                                        ),
                                        "ITEM_2_TAGS",
                                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL
                                )
                                .initializer("new $T<>()", ClassName.get("java.util", "HashMap"))
                                .build()
                );

        // Adding item tags constants
        for (String itemTag : itemTags) {
            String name = itemTag.split(":")[1].toUpperCase();
            builder.addField(FieldSpec.builder(itemTagClass, name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("register($S, new $T($S))", itemTag, lazyItemTagClass, itemTag)
                    .build()
            );
        }

        // Adding register and getter methods
        builder.addMethod(MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(itemTagClass)
                        .addParameter(String.class, "tagName")
                        .addParameter(itemTagClass, "itemTag")
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
                        .build()
                )
                .addMethod(MethodSpec.methodBuilder("getTagsSet")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ParameterizedTypeName.get(
                                ClassName.get("java.util", "Set"),
                                itemTagClass
                        ))
                        .addParameter(String.class, "identifier")
                        .addStatement("return ITEM_2_TAGS.getOrDefault(identifier, $T.emptySet())",
                                ClassName.get("java.util", "Collections"))
                        .build()
                )
                .addMethod(MethodSpec.methodBuilder("getTag")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(itemTagClass)
                        .addParameter(String.class, "tagName")
                        .addStatement("return NAME_2_TAG.get(tagName)")
                        .build()
                )
                .build();

        JavaFile javaFile = JavaFile.builder("cn.nukkit.item.material.tags", builder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
    }

    @SneakyThrows
    private static Map<String, Set<String>> getVanillaItemTags() {
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
}
