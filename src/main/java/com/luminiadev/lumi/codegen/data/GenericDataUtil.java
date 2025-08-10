package com.luminiadev.lumi.codegen.data;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.luminiadev.lumi.codegen.generator.BlockTypeGen;
import com.luminiadev.lumi.codegen.generator.SoundEnumGen;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class GenericDataUtil {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    public Map<String, Integer> getLegacyItemIds(String path) {
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

    @SneakyThrows
    public Set<String> getSoundNames() {
        var inputStream = SoundEnumGen.class.getClassLoader().getResourceAsStream("data/pack/sound_definitions.json");
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
    public Set<String> getMusicNames() {
        var inputStream = SoundEnumGen.class.getClassLoader().getResourceAsStream("data/pack/music_definitions.json");
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
