package com.plagun.minecraft.commands;

import com.plagun.minecraft.PlagunPlugin;
import com.plagun.minecraft.managers.TeamManager;
import com.plagun.minecraft.managers.TeamManager.PTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final PlagunPlugin plugin;

    public TeamCommand(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] args) {
        if (!s.hasPermission("plagun.team.use")) {
            s.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) { sendHelp(s); return true; }
        TeamManager tm = plugin.teams();
        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) { s.sendMessage(Component.text("Usage: /pteam create <name> <color>").color(NamedTextColor.YELLOW)); return true; }
                PTeam t = tm.create(args[1], args[2]);
                if (t == null) {
                    s.sendMessage(Component.text("Team exists or invalid color. Colors: " + String.join(", ", TeamManager.COLORS.keySet())).color(NamedTextColor.RED));
                } else {
                    s.sendMessage(Component.text("Created team '" + t.name + "' with color " + t.colorKey + ".").color(t.color));
                }
            }
            case "delete" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /pteam delete <name>").color(NamedTextColor.YELLOW)); return true; }
                if (tm.delete(args[1])) {
                    s.sendMessage(Component.text("Deleted team '" + args[1] + "'.").color(NamedTextColor.GREEN));
                } else {
                    s.sendMessage(Component.text("Team not found.").color(NamedTextColor.RED));
                }
            }
            case "add" -> {
                if (args.length < 3) { s.sendMessage(Component.text("Usage: /pteam add <player> <team>").color(NamedTextColor.YELLOW)); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                if (!tm.addPlayer(args[2], target)) {
                    s.sendMessage(Component.text("Team not found.").color(NamedTextColor.RED));
                } else {
                    PTeam t = tm.get(args[2]);
                    s.sendMessage(Component.text("Added " + target.getName() + " to team " + t.name + ".").color(t.color));
                    target.sendMessage(Component.text("You joined team " + t.name + "!").color(t.color));
                }
            }
            case "remove" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /pteam remove <player>").color(NamedTextColor.YELLOW)); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                if (tm.removePlayer(target)) {
                    s.sendMessage(Component.text("Removed " + target.getName() + " from their team.").color(NamedTextColor.GREEN));
                } else {
                    s.sendMessage(Component.text(target.getName() + " is not in any team.").color(NamedTextColor.GRAY));
                }
            }
            case "list" -> {
                Map<String, PTeam> all = tm.getAll();
                if (all.isEmpty()) { s.sendMessage(Component.text("No teams.").color(NamedTextColor.GRAY)); return true; }
                s.sendMessage(Component.text("--- Teams ---").color(NamedTextColor.GOLD));
                for (PTeam t : all.values()) {
                    s.sendMessage(Component.text(" - " + t.name + " (" + t.colorKey + ") - " + t.members.size() + " players").color(t.color));
                }
            }
            case "info" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /pteam info <name>").color(NamedTextColor.YELLOW)); return true; }
                PTeam t = tm.get(args[1]);
                if (t == null) { s.sendMessage(Component.text("Team not found.").color(NamedTextColor.RED)); return true; }
                s.sendMessage(Component.text("Team: " + t.name + " (" + t.colorKey + ")").color(t.color));
                if (t.members.isEmpty()) {
                    s.sendMessage(Component.text("  (no players)").color(NamedTextColor.GRAY));
                } else {
                    for (UUID id : t.members) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                        String name = op.getName() != null ? op.getName() : id.toString();
                        s.sendMessage(Component.text("  - " + name).color(NamedTextColor.WHITE));
                    }
                }
            }
            case "clear" -> {
                tm.clearAllMembers();
                s.sendMessage(Component.text("Cleared all team memberships.").color(NamedTextColor.GREEN));
            }
            case "auto" -> {
                if (args.length < 3) {
                    s.sendMessage(Component.text("Usage: /pteam auto <numTeams> <playersPerTeam>").color(NamedTextColor.YELLOW));
                    return true;
                }
                int numTeams, perTeam;
                try {
                    numTeams = Integer.parseInt(args[1]);
                    perTeam = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    s.sendMessage(Component.text("Invalid numbers.").color(NamedTextColor.RED)); return true;
                }
                if (tm.getAll().size() < numTeams) {
                    s.sendMessage(Component.text("Not enough teams configured (" + tm.getAll().size() + " < " + numTeams + "). Create teams first with /pteam create.").color(NamedTextColor.RED));
                    return true;
                }
                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                int needed = numTeams * perTeam;
                if (online.size() < needed) {
                    s.sendMessage(Component.text("Warning: only " + online.size() + " players online, need " + needed + " for full balance.").color(NamedTextColor.YELLOW));
                }
                Map<String, List<Player>> result = tm.autoDistribute(online, numTeams, perTeam);
                if (result == null) { s.sendMessage(Component.text("Distribution failed.").color(NamedTextColor.RED)); return true; }
                s.sendMessage(Component.text("--- Auto distribution ---").color(NamedTextColor.GOLD));
                for (Map.Entry<String, List<Player>> e : result.entrySet()) {
                    PTeam t = tm.get(e.getKey());
                    String names = e.getValue().stream().map(Player::getName).collect(Collectors.joining(", "));
                    s.sendMessage(Component.text(t.name + " (" + e.getValue().size() + "): " + names).color(t.color));
                    for (Player p : e.getValue()) {
                        p.sendMessage(Component.text("You were assigned to team " + t.name + "!").color(t.color));
                    }
                }
            }
            default -> sendHelp(s);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(Component.text("/pteam create <name> <color>").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/pteam delete <name>").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/pteam add <player> <team>").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/pteam remove <player>").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/pteam list | info <name> | clear").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/pteam auto <numTeams> <playersPerTeam>").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("Available colors: " + String.join(", ", TeamManager.COLORS.keySet())).color(NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "delete", "add", "remove", "list", "info", "clear", "auto"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("delete", "info").contains(sub)) {
                return filter(new ArrayList<>(plugin.teams().getAll().keySet()), args[1]);
            }
            if (sub.equals("add") || sub.equals("remove")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("create")) return filter(new ArrayList<>(TeamManager.COLORS.keySet()), args[2]);
            if (sub.equals("add")) return filter(new ArrayList<>(plugin.teams().getAll().keySet()), args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> opts, String prefix) {
        String p = prefix.toLowerCase();
        return opts.stream().filter(o -> o.toLowerCase().startsWith(p)).collect(Collectors.toList());
    }
}
