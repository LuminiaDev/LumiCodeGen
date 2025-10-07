package com.luminiadev.lumi.codegen.data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.luminiadev.lumi.codegen.generator.BlockTypeGen;
import com.luminiadev.lumi.codegen.generator.SoundEnumGen;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
    public Map<String, Integer> getLegacyBlockIds(String path) {
        List<NbtMap> palette = loadVanillaPalette(path);
        Map<String, Integer> ids = new HashMap<>();

        for(NbtMap block : palette) {
            ids.put(block.getString("name"), block.getInt("block_id"));
        }

        return ids;
    }

    @SneakyThrows
    public Map<String, Integer> getRuntimeItemIds(String path) {
        var inputStream = BlockTypeGen.class.getClassLoader().getResourceAsStream(path);
        if (inputStream != null) {
            try (var reader = new InputStreamReader(inputStream)) {
                Map<String, Integer> result = new HashMap<>();
                JsonArray jsonArray = GSON.fromJson(reader, JsonArray.class);

                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    String name = obj.get("name").getAsString();
                    int id = obj.get("id").getAsInt();
                    result.put(name, id);
                }

                return result;
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

    private static List<NbtMap> loadVanillaPalette(String path) {
        var inputStream = SoundEnumGen.class.getClassLoader().getResourceAsStream(path);
        if (inputStream != null) {
            try {
                return ((NbtMap) NbtUtils.createGZIPReader(inputStream).readTag()).getList("blocks", NbtType.COMPOUND);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
