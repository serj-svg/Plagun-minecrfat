package com.plagun.mod.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.plagun.mod.PlagunMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Single JSON-backed store for all Plagun runtime data:
 * lives, teams, hunger-games points, moderation flags.
 *
 * Lives in: <config>/plagun/data.json
 */
public final class PlagunData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PlagunData() {}

    public static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("plagun");
        try {
            Files.createDirectories(d);
        } catch (IOException e) {
            PlagunMod.LOGGER.warn("Could not create config dir {}: {}", d, e.getMessage());
        }
        return d;
    }

    public static Path file() {
        return dir().resolve("data.json");
    }

    public static JsonObject load() {
        Path f = file();
        if (!Files.exists(f)) return new JsonObject();
        try {
            String s = Files.readString(f);
            if (s.isBlank()) return new JsonObject();
            return JsonParser.parseString(s).getAsJsonObject();
        } catch (Exception e) {
            PlagunMod.LOGGER.warn("Could not read {}: {}", f, e.getMessage());
            return new JsonObject();
        }
    }

    public static void save(JsonObject root) {
        try {
            Files.writeString(file(), GSON.toJson(root));
        } catch (IOException e) {
            PlagunMod.LOGGER.warn("Could not save {}: {}", file(), e.getMessage());
        }
    }
}
