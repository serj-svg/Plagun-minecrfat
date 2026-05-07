package com.plagun.mod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.plagun.mod.managers.GameManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class HungerGamesCommand {

    private HungerGamesCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("hg")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> { help(ctx.getSource()); return Command.SINGLE_SUCCESS; })
                .then(literal("start").executes(ctx -> {
                    if (!GameManager.start()) {
                        ctx.getSource().sendFeedback(() -> Text.literal("Cannot start: not enough players or no spawns.").formatted(Formatting.RED), false);
                    } else {
                        ctx.getSource().sendFeedback(() -> Text.literal("Game started.").formatted(Formatting.GREEN), true);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("stop").executes(ctx -> {
                    if (!GameManager.stop()) {
                        ctx.getSource().sendFeedback(() -> Text.literal("Game is not running.").formatted(Formatting.GRAY), false);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("setlobby").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    GameManager.setLobby(p);
                    ctx.getSource().sendFeedback(() -> Text.literal("Lobby set to your current location.").formatted(Formatting.GREEN), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("setspawn").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    GameManager.addSpawn(p);
                    ctx.getSource().sendFeedback(() -> Text.literal("Spawn #" + GameManager.spawnCount() + " added.").formatted(Formatting.GREEN), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("clearspawns").executes(ctx -> {
                    GameManager.clearSpawns();
                    ctx.getSource().sendFeedback(() -> Text.literal("All spawns cleared.").formatted(Formatting.GREEN), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("tplobby")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            if (!GameManager.hasLobby()) {
                                ctx.getSource().sendFeedback(() -> Text.literal("Lobby not set.").formatted(Formatting.RED), false);
                                return Command.SINGLE_SUCCESS;
                            }
                            GameManager.teleportToLobby(p);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "target");
                                    if (!GameManager.hasLobby()) {
                                        ctx.getSource().sendFeedback(() -> Text.literal("Lobby not set.").formatted(Formatting.RED), false);
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    GameManager.teleportToLobby(t);
                                    ctx.getSource().sendFeedback(() -> Text.literal("Teleported " + t.getName().getString() + " to lobby.").formatted(Formatting.GREEN), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(literal("tpall").executes(ctx -> {
                    if (!GameManager.hasLobby()) {
                        ctx.getSource().sendFeedback(() -> Text.literal("Lobby not set.").formatted(Formatting.RED), false);
                        return Command.SINGLE_SUCCESS;
                    }
                    int n = 0;
                    if (ctx.getSource().getServer() != null) {
                        for (ServerPlayerEntity p : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
                            GameManager.teleportToLobby(p);
                            n++;
                        }
                    }
                    final int finalN = n;
                    ctx.getSource().sendFeedback(() -> Text.literal("Teleported " + finalN + " players to lobby.").formatted(Formatting.GREEN), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("spectate").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    p.changeGameMode(GameMode.SPECTATOR);
                    ctx.getSource().sendFeedback(() -> Text.literal("You are now spectating.").formatted(Formatting.AQUA), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("info").executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("Running: " + GameManager.isRunning()).formatted(Formatting.AQUA), false);
                    ctx.getSource().sendFeedback(() -> Text.literal("Spawns: " + GameManager.spawnCount()).formatted(Formatting.AQUA), false);
                    ctx.getSource().sendFeedback(() -> Text.literal("Lobby: " + (GameManager.hasLobby() ? "set" : "not set")).formatted(Formatting.AQUA), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private static void help(ServerCommandSource s) {
        s.sendFeedback(() -> Text.literal("/hg start | stop | info").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/hg setlobby | setspawn | clearspawns").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/hg tplobby [player] | tpall | spectate").formatted(Formatting.YELLOW), false);
    }
}
