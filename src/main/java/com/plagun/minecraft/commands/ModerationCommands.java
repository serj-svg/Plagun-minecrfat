package com.plagun.minecraft.commands;

import com.plagun.minecraft.PlagunPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModerationCommands implements CommandExecutor, TabCompleter {

    private final PlagunPlugin plugin;

    public ModerationCommands(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] args) {
        if (!s.hasPermission("plagun.admin")) {
            s.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }
        switch (c.getName().toLowerCase()) {
            case "pfreeze" -> {
                if (args.length < 1) { s.sendMessage(Component.text("Usage: /pfreeze <player>").color(NamedTextColor.YELLOW)); return true; }
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                plugin.moderation().freeze(t.getUniqueId());
                t.sendMessage(Component.text("You have been frozen.").color(NamedTextColor.AQUA));
                s.sendMessage(Component.text("Froze " + t.getName() + ".").color(NamedTextColor.GREEN));
            }
            case "punfreeze" -> {
                if (args.length < 1) { s.sendMessage(Component.text("Usage: /punfreeze <player>").color(NamedTextColor.YELLOW)); return true; }
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                plugin.moderation().unfreeze(t.getUniqueId());
                t.sendMessage(Component.text("You have been unfrozen.").color(NamedTextColor.AQUA));
                s.sendMessage(Component.text("Unfroze " + t.getName() + ".").color(NamedTextColor.GREEN));
            }
            case "pheal" -> {
                Player t = resolveTarget(s, args);
                if (t == null) return true;
                t.setHealth(t.getMaxHealth());
                t.setFireTicks(0);
                s.sendMessage(Component.text("Healed " + t.getName() + ".").color(NamedTextColor.GREEN));
            }
            case "pfeed" -> {
                Player t = resolveTarget(s, args);
                if (t == null) return true;
                t.setFoodLevel(20);
                t.setSaturation(20f);
                s.sendMessage(Component.text("Fed " + t.getName() + ".").color(NamedTextColor.GREEN));
            }
            case "pvanish" -> {
                if (!(s instanceof Player p)) { s.sendMessage(Component.text("Players only.").color(NamedTextColor.RED)); return true; }
                boolean v = plugin.moderation().toggleVanish(p);
                p.sendMessage(Component.text(v ? "You are now vanished." : "You are no longer vanished.").color(NamedTextColor.AQUA));
            }
            case "ptp" -> {
                if (!(s instanceof Player p)) { s.sendMessage(Component.text("Players only.").color(NamedTextColor.RED)); return true; }
                if (args.length < 1) { s.sendMessage(Component.text("Usage: /ptp <player>").color(NamedTextColor.YELLOW)); return true; }
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                p.teleport(t.getLocation());
                p.sendMessage(Component.text("Teleported to " + t.getName() + ".").color(NamedTextColor.GREEN));
            }
            case "pbroadcast" -> {
                if (args.length < 1) { s.sendMessage(Component.text("Usage: /pbroadcast <message>").color(NamedTextColor.YELLOW)); return true; }
                String msg = String.join(" ", args);
                Bukkit.broadcast(Component.text("[Broadcast] " + msg).color(NamedTextColor.GOLD));
            }
            default -> s.sendMessage(Component.text("Unknown moderation command.").color(NamedTextColor.RED));
        }
        return true;
    }

    private Player resolveTarget(CommandSender s, String[] args) {
        if (args.length >= 1) {
            Player t = Bukkit.getPlayerExact(args[0]);
            if (t == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return null; }
            return t;
        }
        if (s instanceof Player p) return p;
        s.sendMessage(Component.text("Specify a player.").color(NamedTextColor.YELLOW));
        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (args.length == 1 && !c.getName().equalsIgnoreCase("pvanish") && !c.getName().equalsIgnoreCase("pbroadcast")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
