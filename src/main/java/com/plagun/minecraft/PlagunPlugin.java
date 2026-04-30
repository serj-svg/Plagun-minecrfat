package com.plagun.minecraft;

import com.plagun.minecraft.commands.HungerGamesCommand;
import com.plagun.minecraft.commands.LivesCommand;
import com.plagun.minecraft.commands.ModerationCommands;
import com.plagun.minecraft.commands.PlagunCommand;
import com.plagun.minecraft.commands.TeamCommand;
import com.plagun.minecraft.listeners.PlayerListener;
import com.plagun.minecraft.managers.GameManager;
import com.plagun.minecraft.managers.LivesManager;
import com.plagun.minecraft.managers.ModerationManager;
import com.plagun.minecraft.managers.TeamManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlagunPlugin extends JavaPlugin {

    private LivesManager livesManager;
    private TeamManager teamManager;
    private GameManager gameManager;
    private ModerationManager moderationManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.livesManager = new LivesManager(this);
        this.teamManager = new TeamManager(this);
        this.gameManager = new GameManager(this);
        this.moderationManager = new ModerationManager(this);

        livesManager.load();
        teamManager.load();

        registerCommand("lives", new LivesCommand(this));
        registerCommand("pteam", new TeamCommand(this));
        registerCommand("hg", new HungerGamesCommand(this));
        registerCommand("plagun", new PlagunCommand(this));

        ModerationCommands mod = new ModerationCommands(this);
        registerCommand("pfreeze", mod);
        registerCommand("punfreeze", mod);
        registerCommand("pheal", mod);
        registerCommand("pfeed", mod);
        registerCommand("pvanish", mod);
        registerCommand("ptp", mod);
        registerCommand("pbroadcast", mod);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("PlagunMinecraft enabled (v" + getPluginMeta().getVersion() + ")");
    }

    @Override
    public void onDisable() {
        if (livesManager != null) livesManager.save();
        if (teamManager != null) teamManager.unregisterAll();
        getLogger().info("PlagunMinecraft disabled.");
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is not declared in plugin.yml");
            return;
        }
        if (executor instanceof CommandExecutor ce) {
            command.setExecutor(ce);
        }
        if (executor instanceof TabCompleter tc) {
            command.setTabCompleter(tc);
        }
    }

    public LivesManager lives() { return livesManager; }
    public TeamManager teams() { return teamManager; }
    public GameManager game() { return gameManager; }
    public ModerationManager moderation() { return moderationManager; }
}
