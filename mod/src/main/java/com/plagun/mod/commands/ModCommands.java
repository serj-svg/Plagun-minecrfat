package com.plagun.mod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.plagun.mod.PlagunState;
import com.plagun.mod.managers.GameManager;
import com.plagun.mod.managers.LivesManager;
import com.plagun.mod.managers.TeamManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;
import java.util.Collections;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Root /plagun admin command and the legacy fun commands (heal/feed/fly/sparkle/pvp).
 * Other feature roots ( /lives, /pteam, /hg, /pfreeze... ) live in their own files.
 */
public final class ModCommands {

    private ModCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerRoot(dispatcher);
            LivesCommand.register(dispatcher);
            TeamCommand.register(dispatcher);
            HungerGamesCommand.register(dispatcher);
            ModerationCommands.register(dispatcher);
        });
    }

    private static void registerRoot(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("plagun")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> { info(ctx.getSource()); return Command.SINGLE_SUCCESS; })
                .then(literal("version")
                        .requires(s -> true)
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(() -> Text.literal("Plagun mod v1.0.0").formatted(Formatting.GOLD), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(literal("info").executes(ctx -> { info(ctx.getSource()); return Command.SINGLE_SUCCESS; }))
                .then(literal("reload").executes(ctx -> {
                    LivesManager.load();
                    if (ctx.getSource().getServer() != null) {
                        TeamManager.load(ctx.getSource().getServer());
                        GameManager.load(ctx.getSource().getServer());
                    }
                    ctx.getSource().sendFeedback(() -> Text.literal("Plagun reloaded.").formatted(Formatting.GREEN), true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("save").executes(ctx -> {
                    LivesManager.save();
                    TeamManager.save();
                    GameManager.save();
                    ctx.getSource().sendFeedback(() -> Text.literal("Plagun data saved.").formatted(Formatting.GREEN), true);
                    return Command.SINGLE_SUCCESS;
                }))
                // Fun base commands kept from earlier scaffold
                .then(literal("heal")
                        .executes(ctx -> heal(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow())))
                        .then(argument("targets", EntityArgumentType.players())
                                .executes(ctx -> heal(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                .then(literal("feed")
                        .executes(ctx -> feed(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow())))
                        .then(argument("targets", EntityArgumentType.players())
                                .executes(ctx -> feed(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                .then(literal("fly")
                        .executes(ctx -> toggleFly(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow())))
                        .then(argument("targets", EntityArgumentType.players())
                                .executes(ctx -> toggleFly(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                .then(literal("sparkle")
                        .executes(ctx -> toggleSparkle(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow())))
                        .then(argument("targets", EntityArgumentType.players())
                                .executes(ctx -> toggleSparkle(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                .then(literal("pvp")
                        .then(argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean v = BoolArgumentType.getBool(ctx, "enabled");
                                    PlagunState.setPvpEnabled(v);
                                    ctx.getSource().sendFeedback(() ->
                                            Text.literal("PvP " + (v ? "enabled" : "disabled"))
                                                    .formatted(v ? Formatting.GREEN : Formatting.RED), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
        );
    }

    private static void info(ServerCommandSource s) {
        s.sendFeedback(() -> Text.literal("Plagun mod v1.0.0").formatted(Formatting.GOLD), false);
        s.sendFeedback(() -> Text.literal("Roots: /plagun /lives /pteam /hg /pfreeze /punfreeze /pheal /pfeed /pvanish /ptp /pbroadcast")
                .formatted(Formatting.YELLOW), false);
    }

    private static int heal(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity p : targets) {
            p.setHealth(p.getMaxHealth());
            p.extinguish();
            ((ServerWorld) p.getWorld()).spawnParticles(ParticleTypes.HEART,
                    p.getX(), p.getY() + 1.0, p.getZ(), 16, 0.5, 0.5, 0.5, 0.05);
            p.sendMessage(Text.literal("You were healed.").formatted(Formatting.LIGHT_PURPLE));
        }
        source.sendFeedback(() -> Text.literal("Healed " + targets.size() + " player(s).").formatted(Formatting.GREEN), true);
        return targets.size();
    }

    private static int feed(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity p : targets) {
            p.getHungerManager().setFoodLevel(20);
            p.getHungerManager().setSaturationLevel(20f);
            p.sendMessage(Text.literal("You feel full.").formatted(Formatting.LIGHT_PURPLE));
        }
        source.sendFeedback(() -> Text.literal("Fed " + targets.size() + " player(s).").formatted(Formatting.GREEN), true);
        return targets.size();
    }

    private static int toggleFly(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity p : targets) {
            p.getAbilities().allowFlying = !p.getAbilities().allowFlying;
            if (!p.getAbilities().allowFlying) p.getAbilities().flying = false;
            p.sendAbilitiesUpdate();
            p.sendMessage(Text.literal("Flight: " + (p.getAbilities().allowFlying ? "ON" : "OFF"))
                    .formatted(p.getAbilities().allowFlying ? Formatting.AQUA : Formatting.GRAY));
        }
        source.sendFeedback(() -> Text.literal("Toggled flight for " + targets.size() + " player(s).").formatted(Formatting.GREEN), true);
        return targets.size();
    }

    private static int toggleSparkle(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        int on = 0;
        for (ServerPlayerEntity p : targets) {
            boolean enabled = PlagunState.toggleSparkle(p.getUuid());
            if (enabled) on++;
            p.sendMessage(Text.literal("Sparkle trail: " + (enabled ? "ON" : "OFF"))
                    .formatted(enabled ? Formatting.LIGHT_PURPLE : Formatting.GRAY));
        }
        final int onFinal = on;
        source.sendFeedback(() -> Text.literal("Sparkle ON for " + onFinal + "/" + targets.size() + " player(s).").formatted(Formatting.GREEN), true);
        return targets.size();
    }
}
