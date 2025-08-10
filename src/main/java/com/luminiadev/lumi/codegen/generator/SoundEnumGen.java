package com.luminiadev.lumi.codegen.generator;

import com.luminiadev.lumi.codegen.data.GenericDataUtil;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import lombok.SneakyThrows;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SoundEnumGen {

    @SneakyThrows
    public static void generate() {
        List<String> sounds = new ArrayList<>();
        sounds.addAll(GenericDataUtil.getSoundNames());
        sounds.addAll(GenericDataUtil.getMusicNames());
        sounds.sort(Comparator.naturalOrder());

        TypeSpec.Builder builder = TypeSpec.enumBuilder("Sound")
                .addJavadoc("This class is generated automatically, do not change it manually.")
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(String.class, "sound", Modifier.PRIVATE, Modifier.FINAL).build());

        for (var sound : sounds) {
            builder.addEnumConstant(sound.replace(".", "_").toUpperCase(), TypeSpec.anonymousClassBuilder("$S", sound).build());
        }

        builder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(String.class, "sound")
                .addStatement("this.sound = sound")
                .build());
        builder.addMethod(MethodSpec.methodBuilder("getSound")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return this.sound")
                .build());

        JavaFile javaFile = JavaFile.builder("cn.nukkit.level", builder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
        javaFile.writeTo(Path.of("generated/"));
    }
}
