package com.plagun.mod.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.plagun.mod.managers.LivesManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class LivesCommand {

    private LivesCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("lives")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> { help(ctx.getSource()); return Command.SINGLE_SUCCESS; })
                .then(literal("give")
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> give(ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "target"),
                                        LivesManager.getDefault()))
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> give(ctx.getSource(),
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(literal("set")
                        .then(argument("target", EntityArgumentType.player())
                                .then(argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> set(ctx.getSource(),
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(literal("check")
                        .executes(ctx -> check(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> check(ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "target")))))
                .then(literal("revive")
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> revive(ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "target")))))
                .then(literal("reset")
                        .executes(ctx -> resetAll(ctx.getSource()))
                        .then(argument("target", EntityArgumentType.player())
                                .executes(ctx -> resetOne(ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "target")))))
                .then(literal("list")
                        .executes(ctx -> list(ctx.getSource()))));
    }

    private static int give(ServerCommandSource s, ServerPlayerEntity t, int amount) {
        LivesManager.give(t.getUuid(), amount);
        LivesManager.save();
        int total = LivesManager.get(t.getUuid());
        s.sendFeedback(() -> Text.literal("Gave " + amount + " lives to " + t.getName().getString() + " (total: " + total + ")")
                .formatted(Formatting.GREEN), true);
        t.sendMessage(Text.literal("You received " + amount + " lives. Total: " + total).formatted(Formatting.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int set(ServerCommandSource s, ServerPlayerEntity t, int amount) {
        LivesManager.set(t.getUuid(), amount);
        LivesManager.save();
        s.sendFeedback(() -> Text.literal("Set " + t.getName().getString() + "'s lives to " + amount).formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int check(ServerCommandSource s, ServerPlayerEntity t) {
        if (!LivesManager.has(t.getUuid())) {
            s.sendFeedback(() -> Text.literal(t.getName().getString() + " has no lives entry. Use /lives give.").formatted(Formatting.GRAY), false);
        } else {
            s.sendFeedback(() -> Text.literal(t.getName().getString() + " has " + LivesManager.get(t.getUuid()) + " lives.").formatted(Formatting.AQUA), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int revive(ServerCommandSource s, ServerPlayerEntity t) {
        LivesManager.revive(t);
        s.sendFeedback(() -> Text.literal(t.getName().getString() + " has been revived!").formatted(Formatting.LIGHT_PURPLE), true);
        if (s.getServer() != null) {
            s.getServer().getPlayerManager().broadcast(
                    Text.literal(t.getName().getString() + " has been revived!").formatted(Formatting.LIGHT_PURPLE), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int resetAll(ServerCommandSource s) {
        LivesManager.clearAll();
        LivesManager.save();
        s.sendFeedback(() -> Text.literal("Cleared all lives data.").formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetOne(ServerCommandSource s, ServerPlayerEntity t) {
        LivesManager.remove(t.getUuid());
        LivesManager.save();
        s.sendFeedback(() -> Text.literal("Cleared lives for " + t.getName().getString()).formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int list(ServerCommandSource s) {
        Map<UUID, Integer> all = LivesManager.all();
        if (all.isEmpty()) {
            s.sendFeedback(() -> Text.literal("No tracked players.").formatted(Formatting.GRAY), false);
            return Command.SINGLE_SUCCESS;
        }
        s.sendFeedback(() -> Text.literal("--- Lives ---").formatted(Formatting.GOLD), false);
        for (Map.Entry<UUID, Integer> e : all.entrySet()) {
            String name = resolveName(s, e.getKey());
            s.sendFeedback(() -> Text.literal(" - " + name + ": " + e.getValue()).formatted(Formatting.AQUA), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static String resolveName(ServerCommandSource s, UUID id) {
        if (s.getServer() == null) return id.toString();
        ServerPlayerEntity online = s.getServer().getPlayerManager().getPlayer(id);
        if (online != null) return online.getName().getString();
        Optional<GameProfile> opt = s.getServer().getUserCache() == null
                ? Optional.empty() : s.getServer().getUserCache().getByUuid(id);
        return opt.map(GameProfile::getName).orElse(id.toString());
    }

    private static void help(ServerCommandSource s) {
        s.sendFeedback(() -> Text.literal("/lives give <player> [amount]").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/lives set <player> <amount>").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/lives check [player]").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/lives revive <player>").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/lives reset [player]").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/lives list").formatted(Formatting.YELLOW), false);
    }
}
