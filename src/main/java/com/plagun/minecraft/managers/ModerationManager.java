package com.plagun.minecraft.managers;

import com.plagun.minecraft.PlagunPlugin;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModerationManager {

    private final PlagunPlugin plugin;
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();

    public ModerationManager(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isFrozen(UUID id) { return frozen.contains(id); }
    public void freeze(UUID id) { frozen.add(id); }
    public void unfreeze(UUID id) { frozen.remove(id); }

    public boolean toggleVanish(Player player) {
        UUID id = player.getUniqueId();
        if (vanished.contains(id)) {
            vanished.remove(id);
            for (Player viewer : plugin.getServer().getOnlinePlayers()) {
                viewer.showPlayer(plugin, player);
            }
            return false;
        }
        vanished.add(id);
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            if (!viewer.hasPermission("plagun.admin")) {
                viewer.hidePlayer(plugin, player);
            }
        }
        return true;
    }

    public boolean isVanished(UUID id) { return vanished.contains(id); }
}
