package com.plagun.mod.managers;

import com.google.gson.JsonObject;
import com.plagun.mod.data.PlagunData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LivesManager {

    private static final Map<UUID, Integer> LIVES = new HashMap<>();
    private static int defaultLives = 3;
    private static GameMode deathGameMode = GameMode.SPECTATOR;
    private static boolean broadcastFinal = true;

    private LivesManager() {}

    public static void load() {
        LIVES.clear();
        JsonObject root = PlagunData.load();
        if (root.has("config")) {
            JsonObject cfg = root.getAsJsonObject("config");
            if (cfg.has("default-lives")) defaultLives = cfg.get("default-lives").getAsInt();
            if (cfg.has("death-gamemode")) {
                try { deathGameMode = GameMode.valueOf(cfg.get("death-gamemode").getAsString().toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            }
            if (cfg.has("broadcast-final-death")) broadcastFinal = cfg.get("broadcast-final-death").getAsBoolean();
        }
        if (root.has("lives")) {
            JsonObject lives = root.getAsJsonObject("lives");
            for (String k : lives.keySet()) {
                try { LIVES.put(UUID.fromString(k), lives.get(k).getAsInt()); }
                catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public static void save() {
        JsonObject root = PlagunData.load();
        JsonObject cfg = new JsonObject();
        cfg.addProperty("default-lives", defaultLives);
        cfg.addProperty("death-gamemode", deathGameMode.name());
        cfg.addProperty("broadcast-final-death", broadcastFinal);
        root.add("config", cfg);

        JsonObject lives = new JsonObject();
        for (Map.Entry<UUID, Integer> e : LIVES.entrySet()) lives.addProperty(e.getKey().toString(), e.getValue());
        root.add("lives", lives);
        PlagunData.save(root);
    }

    public static int getDefault() { return defaultLives; }
    public static void setDefault(int v) { defaultLives = Math.max(0, v); }
    public static GameMode deathGameMode() { return deathGameMode; }
    public static boolean broadcastFinal() { return broadcastFinal; }

    public static boolean has(UUID id) { return LIVES.containsKey(id); }
    public static int get(UUID id) { return LIVES.getOrDefault(id, 0); }

    public static void set(UUID id, int amount) { LIVES.put(id, Math.max(0, amount)); }
    public static void give(UUID id, int amount) { LIVES.put(id, get(id) + amount); }

    public static int decrement(UUID id) {
        int v = Math.max(0, get(id) - 1);
        LIVES.put(id, v);
        return v;
    }

    public static void remove(UUID id) { LIVES.remove(id); }
    public static void clearAll() { LIVES.clear(); }
    public static Map<UUID, Integer> all() { return LIVES; }

    public static boolean isPermaDead(UUID id) { return has(id) && get(id) <= 0; }

    public static void revive(ServerPlayerEntity p) {
        set(p.getUuid(), defaultLives);
        p.changeGameMode(GameMode.SURVIVAL);
        p.setHealth(p.getMaxHealth());
        p.getHungerManager().setFoodLevel(20);
        save();
    }
}
