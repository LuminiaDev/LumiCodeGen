package com.luminiadev.lumi.codegen.generator;

import com.luminiadev.lumi.codegen.data.KaoootDataUtil;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import lombok.SneakyThrows;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ItemNamespaceIdGen {

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
        List<String> itemEntries = KaoootDataUtil.getItemPalette().keySet().stream()
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
}