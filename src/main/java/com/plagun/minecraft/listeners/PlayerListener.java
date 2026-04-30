package com.plagun.minecraft.listeners;

import com.plagun.minecraft.PlagunPlugin;
import com.plagun.minecraft.managers.LivesManager;
import com.plagun.minecraft.managers.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private final PlagunPlugin plugin;

    public PlayerListener(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        LivesManager lm = plugin.lives();
        if (!lm.hasLives(p.getUniqueId())) return;

        int remaining = lm.decrement(p.getUniqueId());
        lm.save();

        if (remaining <= 0) {
            event.deathMessage(Component.text(p.getName() + " has been eliminated! (Final death)").color(NamedTextColor.DARK_RED));
            if (plugin.getConfig().getBoolean("broadcast-final-death", true)) {
                Bukkit.broadcast(Component.text(p.getName() + " is permanently dead. RIP.").color(NamedTextColor.DARK_RED));
            }
        } else {
            p.sendMessage(Component.text("You died. Lives left: " + remaining).color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        LivesManager lm = plugin.lives();
        if (lm.isPermaDead(p)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.setGameMode(lm.deathGameMode());
                p.sendMessage(Component.text("You have no lives left. Wait for an admin to revive you.").color(NamedTextColor.DARK_RED));
            });
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        LivesManager lm = plugin.lives();
        if (lm.isPermaDead(p)) {
            p.setGameMode(lm.deathGameMode());
            p.sendMessage(Component.text("You are permanently dead. Awaiting revival.").color(NamedTextColor.DARK_RED));
        }
        // Re-attach to scoreboard team if assigned
        TeamManager.PTeam t = plugin.teams().findPlayerTeam(p.getUniqueId());
        if (t != null && !t.scoreboardTeam.hasEntry(p.getName())) {
            t.scoreboardTeam.addEntry(p.getName());
        }
        // Hide vanished from this player if needed
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (plugin.moderation().isVanished(other.getUniqueId()) && !p.hasPermission("plagun.admin")) {
                p.hidePlayer(plugin, other);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.lives().save();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (!plugin.moderation().isFrozen(p.getUniqueId())) return;
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
        }
    }
}
