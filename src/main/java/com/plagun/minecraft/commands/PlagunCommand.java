package com.plagun.minecraft.commands;

import com.plagun.minecraft.PlagunPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PlagunCommand implements CommandExecutor, TabCompleter {

    private final PlagunPlugin plugin;

    public PlagunCommand(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] args) {
        if (!s.hasPermission("plagun.admin")) {
            s.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) { sendInfo(s); return true; }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.lives().load();
                s.sendMessage(Component.text("Plagun config reloaded.").color(NamedTextColor.GREEN));
            }
            case "info", "version" -> sendInfo(s);
            case "save" -> {
                plugin.lives().save();
                plugin.teams().save();
                s.sendMessage(Component.text("Data saved.").color(NamedTextColor.GREEN));
            }
            default -> sendInfo(s);
        }
        return true;
    }

    private void sendInfo(CommandSender s) {
        s.sendMessage(Component.text("PlagunMinecraft v" + plugin.getPluginMeta().getVersion()).color(NamedTextColor.GOLD));
        s.sendMessage(Component.text("Subcommands: /plagun reload | info | save").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "info", "version", "save").stream()
                    .filter(o -> o.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
