package com.luminiadev.lumi.codegen;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import lombok.SneakyThrows;

import javax.lang.model.element.Modifier;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;

public class SoundEnumGen {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    public static void generate() {
        List<String> sounds = new ArrayList<>();
        sounds.addAll(getSoundNames());
        sounds.addAll(getMusicNames());
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

    @SneakyThrows
    private static Set<String> getSoundNames() {
        var inputStream = SoundEnumGen.class.getClassLoader().getResourceAsStream("data/sound_definitions.json");
        if (inputStream != null) {
            try (var reader = new InputStreamReader(inputStream)) {
                return JsonParser.parseReader(reader)
                        .getAsJsonObject()
                        .getAsJsonObject("sound_definitions")
                        .asMap()
                        .keySet();
            }
        }
        return new HashSet<>();
    }

    @SneakyThrows
    private static Set<String> getMusicNames() {
        var inputStream = SoundEnumGen.class.getClassLoader().getResourceAsStream("data/music_definitions.json");
        if (inputStream != null) {
            try (var reader = new InputStreamReader(inputStream)) {
                var musicNames = new HashSet<String>();
                JsonParser.parseReader(reader).getAsJsonObject().asMap().values().forEach(v -> {
                    musicNames.add(v.getAsJsonObject().get("event_name").getAsString());
                });
                return musicNames;
            }
        }
        return new HashSet<>();
    }
}
