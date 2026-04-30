package com.plagun.minecraft.commands;

import com.plagun.minecraft.PlagunPlugin;
import com.plagun.minecraft.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HungerGamesCommand implements CommandExecutor, TabCompleter {

    private final PlagunPlugin plugin;

    public HungerGamesCommand(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] args) {
        if (!s.hasPermission("plagun.hg.use")) {
            s.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) { sendHelp(s); return true; }
        GameManager gm = plugin.game();
        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (!gm.start()) {
                    s.sendMessage(Component.text("Cannot start: not enough players or no spawns set.").color(NamedTextColor.RED));
                } else {
                    s.sendMessage(Component.text("Game started.").color(NamedTextColor.GREEN));
                }
            }
            case "stop" -> {
                if (!gm.stop()) {
                    s.sendMessage(Component.text("Game is not running.").color(NamedTextColor.GRAY));
                }
            }
            case "setlobby" -> {
                if (!(s instanceof Player p)) { s.sendMessage(Component.text("Players only.").color(NamedTextColor.RED)); return true; }
                gm.setLobby(p.getLocation());
                s.sendMessage(Component.text("Lobby set to your current location.").color(NamedTextColor.GREEN));
            }
            case "setspawn" -> {
                if (!(s instanceof Player p)) { s.sendMessage(Component.text("Players only.").color(NamedTextColor.RED)); return true; }
                gm.addSpawn(p.getLocation());
                s.sendMessage(Component.text("Spawn #" + gm.getSpawns().size() + " added.").color(NamedTextColor.GREEN));
            }
            case "clearspawns" -> {
                gm.clearSpawns();
                s.sendMessage(Component.text("All spawns cleared.").color(NamedTextColor.GREEN));
            }
            case "tplobby" -> {
                Location lobby = gm.getLobby();
                if (lobby == null) { s.sendMessage(Component.text("Lobby not set.").color(NamedTextColor.RED)); return true; }
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                    target.teleport(lobby);
                    s.sendMessage(Component.text("Teleported " + target.getName() + " to lobby.").color(NamedTextColor.GREEN));
                } else {
                    if (!(s instanceof Player p)) { s.sendMessage(Component.text("Specify a player.").color(NamedTextColor.YELLOW)); return true; }
                    p.teleport(lobby);
                    s.sendMessage(Component.text("Teleported to lobby.").color(NamedTextColor.GREEN));
                }
            }
            case "tpall" -> {
                Location lobby = gm.getLobby();
                if (lobby == null) { s.sendMessage(Component.text("Lobby not set.").color(NamedTextColor.RED)); return true; }
                int n = 0;
                for (Player p : Bukkit.getOnlinePlayers()) { p.teleport(lobby); n++; }
                s.sendMessage(Component.text("Teleported " + n + " players to lobby.").color(NamedTextColor.GREEN));
            }
            case "spectate" -> {
                if (!(s instanceof Player p)) { s.sendMessage(Component.text("Players only.").color(NamedTextColor.RED)); return true; }
                p.setGameMode(GameMode.SPECTATOR);
                s.sendMessage(Component.text("You are now spectating.").color(NamedTextColor.AQUA));
            }
            case "info" -> {
                s.sendMessage(Component.text("Running: " + gm.isRunning()).color(NamedTextColor.AQUA));
                s.sendMessage(Component.text("Spawns: " + gm.getSpawns().size()).color(NamedTextColor.AQUA));
                s.sendMessage(Component.text("Lobby: " + (gm.getLobby() != null ? "set" : "not set")).color(NamedTextColor.AQUA));
            }
            default -> sendHelp(s);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(Component.text("/hg start | stop | info").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/hg setlobby | setspawn | clearspawns").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/hg tplobby [player] | tpall | spectate").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop", "setlobby", "setspawn", "clearspawns", "tplobby", "tpall", "spectate", "info").stream()
                    .filter(o -> o.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("tplobby")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
