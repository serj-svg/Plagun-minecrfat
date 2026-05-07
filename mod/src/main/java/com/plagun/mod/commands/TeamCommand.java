package com.plagun.mod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.plagun.mod.managers.TeamManager;
import com.plagun.mod.managers.TeamManager.PTeam;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class TeamCommand {

    private TeamCommand() {}

    private static final SuggestionProvider<ServerCommandSource> COLOR_SUG = (ctx, b) -> suggest(b, TeamManager.COLORS.keySet());
    private static final SuggestionProvider<ServerCommandSource> TEAM_SUG = (ctx, b) -> suggest(b, TeamManager.all().keySet());

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("pteam")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> { help(ctx.getSource()); return Command.SINGLE_SUCCESS; })
                .then(literal("create")
                        .then(argument("name", StringArgumentType.word())
                                .then(argument("color", StringArgumentType.word()).suggests(COLOR_SUG)
                                        .executes(TeamCommand::create))))
                .then(literal("delete")
                        .then(argument("name", StringArgumentType.word()).suggests(TEAM_SUG)
                                .executes(TeamCommand::delete)))
                .then(literal("add")
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("team", StringArgumentType.word()).suggests(TEAM_SUG)
                                        .executes(TeamCommand::addPlayer))))
                .then(literal("remove")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(TeamCommand::removePlayer)))
                .then(literal("list").executes(TeamCommand::list))
                .then(literal("info")
                        .then(argument("name", StringArgumentType.word()).suggests(TEAM_SUG)
                                .executes(TeamCommand::info)))
                .then(literal("clear").executes(TeamCommand::clear))
                .then(literal("auto")
                        .then(argument("numTeams", IntegerArgumentType.integer(1))
                                .then(argument("playersPerTeam", IntegerArgumentType.integer(1))
                                        .executes(TeamCommand::auto)))));
    }

    private static int create(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        String color = StringArgumentType.getString(ctx, "color");
        PTeam t = TeamManager.create(name, color);
        if (t == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("Team exists or invalid color. Colors: " + String.join(", ", TeamManager.COLORS.keySet()))
                    .formatted(Formatting.RED), false);
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal("Created team '" + t.name + "' (" + t.colorKey + ")")
                    .formatted(t.color), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int delete(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        if (TeamManager.delete(name)) {
            ctx.getSource().sendFeedback(() -> Text.literal("Deleted team '" + name + "'.").formatted(Formatting.GREEN), true);
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal("Team not found.").formatted(Formatting.RED), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addPlayer(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        String teamName = StringArgumentType.getString(ctx, "team");
        if (!TeamManager.addPlayer(teamName, player)) {
            ctx.getSource().sendFeedback(() -> Text.literal("Team not found.").formatted(Formatting.RED), false);
        } else {
            PTeam t = TeamManager.get(teamName);
            ctx.getSource().sendFeedback(() -> Text.literal("Added " + player.getName().getString() + " to team " + t.name)
                    .formatted(t.color), true);
            player.sendMessage(Text.literal("You joined team " + t.name + "!").formatted(t.color));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int removePlayer(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        if (TeamManager.removePlayer(player)) {
            ctx.getSource().sendFeedback(() -> Text.literal("Removed " + player.getName().getString() + " from their team.")
                    .formatted(Formatting.GREEN), true);
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal(player.getName().getString() + " is not in any team.")
                    .formatted(Formatting.GRAY), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        Map<String, PTeam> all = TeamManager.all();
        if (all.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("No teams.").formatted(Formatting.GRAY), false);
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("--- Teams ---").formatted(Formatting.GOLD), false);
        for (PTeam t : all.values()) {
            ctx.getSource().sendFeedback(() -> Text.literal(" - " + t.name + " (" + t.colorKey + ") - " + t.members.size() + " players")
                    .formatted(t.color), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        PTeam t = TeamManager.get(name);
        if (t == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("Team not found.").formatted(Formatting.RED), false);
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("Team: " + t.name + " (" + t.colorKey + ")").formatted(t.color), false);
        if (t.members.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("  (no players)").formatted(Formatting.GRAY), false);
        } else {
            for (UUID id : t.members) {
                String n = ctx.getSource().getServer() == null ? id.toString() : resolveName(ctx, id);
                ctx.getSource().sendFeedback(() -> Text.literal("  - " + n).formatted(Formatting.WHITE), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static String resolveName(CommandContext<ServerCommandSource> ctx, UUID id) {
        ServerPlayerEntity p = ctx.getSource().getServer().getPlayerManager().getPlayer(id);
        if (p != null) return p.getName().getString();
        var opt = ctx.getSource().getServer().getUserCache() == null
                ? java.util.Optional.<com.mojang.authlib.GameProfile>empty()
                : ctx.getSource().getServer().getUserCache().getByUuid(id);
        return opt.map(com.mojang.authlib.GameProfile::getName).orElse(id.toString());
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) {
        TeamManager.clearAllMembers();
        ctx.getSource().sendFeedback(() -> Text.literal("Cleared all team memberships.").formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int auto(CommandContext<ServerCommandSource> ctx) {
        int numTeams = IntegerArgumentType.getInteger(ctx, "numTeams");
        int perTeam = IntegerArgumentType.getInteger(ctx, "playersPerTeam");
        if (TeamManager.all().size() < numTeams) {
            ctx.getSource().sendFeedback(() -> Text.literal("Not enough teams configured (" + TeamManager.all().size() + " < " + numTeams + "). Create teams first.")
                    .formatted(Formatting.RED), false);
            return Command.SINGLE_SUCCESS;
        }
        if (ctx.getSource().getServer() == null) return Command.SINGLE_SUCCESS;
        List<ServerPlayerEntity> online = new ArrayList<>(ctx.getSource().getServer().getPlayerManager().getPlayerList());
        int needed = numTeams * perTeam;
        if (online.size() < needed) {
            ctx.getSource().sendFeedback(() -> Text.literal("Warning: only " + online.size() + " players online, need " + needed + ".")
                    .formatted(Formatting.YELLOW), false);
        }
        Map<String, List<ServerPlayerEntity>> result = TeamManager.autoDistribute(online, numTeams, perTeam);
        if (result == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("Distribution failed.").formatted(Formatting.RED), false);
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("--- Auto distribution ---").formatted(Formatting.GOLD), true);
        for (Map.Entry<String, List<ServerPlayerEntity>> e : result.entrySet()) {
            PTeam t = TeamManager.get(e.getKey());
            String names = e.getValue().stream().map(p -> p.getName().getString()).collect(Collectors.joining(", "));
            ctx.getSource().sendFeedback(() -> Text.literal(t.name + " (" + e.getValue().size() + "): " + names).formatted(t.color), false);
            for (ServerPlayerEntity p : e.getValue()) {
                p.sendMessage(Text.literal("You were assigned to team " + t.name + "!").formatted(t.color));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void help(ServerCommandSource s) {
        s.sendFeedback(() -> Text.literal("/pteam create <name> <color>").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/pteam delete <name>").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/pteam add <player> <team>").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/pteam remove <player>").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/pteam list | info <name> | clear").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("/pteam auto <numTeams> <playersPerTeam>").formatted(Formatting.YELLOW), false);
        s.sendFeedback(() -> Text.literal("Colors: " + String.join(", ", TeamManager.COLORS.keySet())).formatted(Formatting.GRAY), false);
    }

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder b, Iterable<String> values) {
        String rem = b.getRemaining().toLowerCase();
        for (String v : values) if (v.toLowerCase().startsWith(rem)) b.suggest(v);
        return b.buildFuture();
    }
}
