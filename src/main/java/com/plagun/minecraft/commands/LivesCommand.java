package com.plagun.minecraft.commands;

import com.plagun.minecraft.PlagunPlugin;
import com.plagun.minecraft.managers.LivesManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

public class LivesCommand implements CommandExecutor, TabCompleter {

    private final PlagunPlugin plugin;

    public LivesCommand(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] args) {
        if (!s.hasPermission("plagun.lives.use")) {
            s.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sendHelp(s);
            return true;
        }
        LivesManager lm = plugin.lives();
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "give" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /lives give <player> [amount]").color(NamedTextColor.YELLOW)); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                int amount = lm.getDefaultLives();
                if (args.length >= 3) {
                    try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                        s.sendMessage(Component.text("Invalid amount.").color(NamedTextColor.RED)); return true;
                    }
                }
                lm.giveLives(target.getUniqueId(), amount);
                lm.save();
                s.sendMessage(Component.text("Gave " + amount + " lives to " + target.getName() + " (now " + lm.getLives(target.getUniqueId()) + ").").color(NamedTextColor.GREEN));
                target.sendMessage(Component.text("You received " + amount + " lives! Total: " + lm.getLives(target.getUniqueId())).color(NamedTextColor.GREEN));
            }
            case "set" -> {
                if (args.length < 3) { s.sendMessage(Component.text("Usage: /lives set <player> <amount>").color(NamedTextColor.YELLOW)); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                int amount;
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    s.sendMessage(Component.text("Invalid amount.").color(NamedTextColor.RED)); return true;
                }
                lm.setLives(target.getUniqueId(), amount);
                lm.save();
                s.sendMessage(Component.text("Set " + target.getName() + "'s lives to " + amount + ".").color(NamedTextColor.GREEN));
            }
            case "check" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                } else if (s instanceof Player p) {
                    target = p;
                } else {
                    s.sendMessage(Component.text("Specify a player.").color(NamedTextColor.YELLOW));
                    return true;
                }
                if (!lm.hasLives(target.getUniqueId())) {
                    s.sendMessage(Component.text(target.getName() + " has no lives entry. Use /lives give.").color(NamedTextColor.GRAY));
                } else {
                    s.sendMessage(Component.text(target.getName() + " has " + lm.getLives(target.getUniqueId()) + " lives.").color(NamedTextColor.AQUA));
                }
            }
            case "revive" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /lives revive <player>").color(NamedTextColor.YELLOW)); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                lm.revive(target);
                lm.save();
                target.setGameMode(GameMode.SURVIVAL);
                Bukkit.broadcast(Component.text(target.getName() + " has been revived!").color(NamedTextColor.LIGHT_PURPLE));
            }
            case "reset" -> {
                if (!s.hasPermission("plagun.admin")) { s.sendMessage(Component.text("No permission.").color(NamedTextColor.RED)); return true; }
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) { s.sendMessage(Component.text("Player not online.").color(NamedTextColor.RED)); return true; }
                    lm.remove(target.getUniqueId());
                    s.sendMessage(Component.text("Cleared lives for " + target.getName() + ".").color(NamedTextColor.GREEN));
                } else {
                    lm.all().clear();
                    s.sendMessage(Component.text("Cleared all lives data.").color(NamedTextColor.GREEN));
                }
                lm.save();
            }
            case "list" -> {
                Map<UUID, Integer> all = lm.all();
                if (all.isEmpty()) { s.sendMessage(Component.text("No tracked players.").color(NamedTextColor.GRAY)); return true; }
                s.sendMessage(Component.text("--- Lives ---").color(NamedTextColor.GOLD));
                for (Map.Entry<UUID, Integer> e : all.entrySet()) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                    String name = op.getName() != null ? op.getName() : e.getKey().toString();
                    s.sendMessage(Component.text(" - " + name + ": " + e.getValue()).color(NamedTextColor.AQUA));
                }
            }
            default -> sendHelp(s);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(Component.text("/lives give <player> [amount]").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/lives set <player> <amount>").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/lives check [player]").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/lives revive <player>").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/lives reset [player]").color(NamedTextColor.YELLOW));
        s.sendMessage(Component.text("/lives list").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("give", "set", "check", "revive", "reset", "list"), args[0]);
        }
        if (args.length == 2 && Arrays.asList("give", "set", "check", "revive", "reset").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> opts, String prefix) {
        return opts.stream().filter(o -> o.startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}
