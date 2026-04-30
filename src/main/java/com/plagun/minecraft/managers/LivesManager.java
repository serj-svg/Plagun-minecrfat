package com.plagun.minecraft.managers;

import com.plagun.minecraft.PlagunPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LivesManager {

    private final PlagunPlugin plugin;
    private final Map<UUID, Integer> lives = new HashMap<>();
    private File file;
    private YamlConfiguration data;

    public LivesManager(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "lives.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create lives.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        lives.clear();
        for (String key : data.getKeys(false)) {
            try {
                lives.put(UUID.fromString(key), data.getInt(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        if (data == null) return;
        for (String key : data.getKeys(false)) {
            data.set(key, null);
        }
        for (Map.Entry<UUID, Integer> e : lives.entrySet()) {
            data.set(e.getKey().toString(), e.getValue());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save lives.yml: " + e.getMessage());
        }
    }

    public int getDefaultLives() {
        return plugin.getConfig().getInt("default-lives", 3);
    }

    public boolean hasLives(UUID id) {
        return lives.containsKey(id);
    }

    public int getLives(UUID id) {
        return lives.getOrDefault(id, 0);
    }

    public void setLives(UUID id, int amount) {
        if (amount <= 0) {
            lives.put(id, 0);
        } else {
            lives.put(id, amount);
        }
    }

    public void giveLives(UUID id, int amount) {
        lives.put(id, getLives(id) + amount);
    }

    public int decrement(UUID id) {
        int current = getLives(id) - 1;
        if (current < 0) current = 0;
        lives.put(id, current);
        return current;
    }

    public void remove(UUID id) {
        lives.remove(id);
    }

    public Map<UUID, Integer> all() {
        return lives;
    }

    public boolean isPermaDead(Player player) {
        return hasLives(player.getUniqueId()) && getLives(player.getUniqueId()) <= 0;
    }

    public void revive(Player player) {
        setLives(player.getUniqueId(), getDefaultLives());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
    }

    public GameMode deathGameMode() {
        String name = plugin.getConfig().getString("death-gamemode", "SPECTATOR");
        try {
            return GameMode.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GameMode.SPECTATOR;
        }
    }

    public String resolveName(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : id.toString();
    }
}
