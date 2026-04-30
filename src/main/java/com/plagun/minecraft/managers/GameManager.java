package com.plagun.minecraft.managers;

import com.plagun.minecraft.PlagunPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameManager {

    private final PlagunPlugin plugin;
    private Location lobby;
    private final List<Location> spawns = new ArrayList<>();
    private boolean running = false;
    private BukkitTask countdownTask;

    public GameManager(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    public void setLobby(Location loc) {
        this.lobby = loc;
    }

    public Location getLobby() {
        return lobby;
    }

    public void addSpawn(Location loc) {
        spawns.add(loc);
    }

    public void clearSpawns() {
        spawns.clear();
    }

    public List<Location> getSpawns() {
        return spawns;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean start() {
        if (running) return false;
        int min = plugin.getConfig().getInt("hungergames.min-players", 2);
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() < min) {
            return false;
        }
        if (spawns.isEmpty()) return false;

        Collections.shuffle(spawns);
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Location spawn = spawns.get(i % spawns.size());
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.getInventory().clear();
            if (plugin.getConfig().getBoolean("hungergames.invulnerable-countdown", true)) {
                p.setInvulnerable(true);
            }
        }

        running = true;
        int seconds = plugin.getConfig().getInt("hungergames.countdown-seconds", 30);
        Bukkit.broadcast(Component.text("Hunger Games starting in " + seconds + "s!").color(NamedTextColor.GOLD));

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int remaining = seconds;
            @Override public void run() {
                if (remaining <= 0) {
                    Bukkit.broadcast(Component.text("GO! May the odds be in your favor.").color(NamedTextColor.GREEN));
                    for (Player p : Bukkit.getOnlinePlayers()) p.setInvulnerable(false);
                    countdownTask.cancel();
                    return;
                }
                if (remaining <= 5 || remaining % 10 == 0) {
                    Bukkit.broadcast(Component.text("Starts in " + remaining + "s").color(NamedTextColor.YELLOW));
                }
                remaining--;
            }
        }, 0L, 20L);
        return true;
    }

    public boolean stop() {
        if (!running) return false;
        running = false;
        if (countdownTask != null) {
            try { countdownTask.cancel(); } catch (Exception ignored) {}
            countdownTask = null;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setInvulnerable(false);
        }
        Bukkit.broadcast(Component.text("Hunger Games stopped.").color(NamedTextColor.RED));
        return true;
    }
}
