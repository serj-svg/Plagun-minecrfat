package com.plagun.mod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plagun.mod.PlagunState;
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

public final class ModCommands {

    private ModCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerRoot(dispatcher));
    }

    private static void registerRoot(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = net.minecraft.server.command.CommandManager.literal("plagun")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(() ->
                            Text.literal("Plagun mod v1.0.0").formatted(Formatting.GOLD), false);
                    ctx.getSource().sendFeedback(() ->
                            Text.literal("Subcommands: heal, feed, fly, sparkle, pvp, version").formatted(Formatting.YELLOW), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(net.minecraft.server.command.CommandManager.literal("version")
                        .requires(s -> true)
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(() -> Text.literal("Plagun 1.0.0").formatted(Formatting.GOLD), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(net.minecraft.server.command.CommandManager.literal("heal")
                        .executes(ctx -> heal(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow())))
                        .then(net.minecraft.server.command.CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(ctx -> heal(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                .then(net.minecraft.server.command.CommandManager.literal("feed")
                        .executes(ctx -> feed(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow())))
                        .then(net.minecraft.server.command.CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(ctx -> feed(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                .then(net.minecraft.server.command.CommandManager.literal("fly")
                        .executes(ctx -> toggleFly(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow())))
                        .then(net.minecraft.server.command.CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(ctx -> toggleFly(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                .then(net.minecraft.server.command.CommandManager.literal("sparkle")
                        .executes(ctx -> toggleSparkle(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrThrow())))
                        .then(net.minecraft.server.command.CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(ctx -> toggleSparkle(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "targets")))))
                .then(net.minecraft.server.command.CommandManager.literal("pvp")
                        .then(net.minecraft.server.command.CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean v = BoolArgumentType.getBool(ctx, "enabled");
                                    PlagunState.setPvpEnabled(v);
                                    ctx.getSource().sendFeedback(() ->
                                            Text.literal("PvP " + (v ? "enabled" : "disabled"))
                                                    .formatted(v ? Formatting.GREEN : Formatting.RED), true);
                                    return Command.SINGLE_SUCCESS;
                                })));

        dispatcher.register(root);
    }

    private static int heal(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity p : targets) {
            p.setHealth(p.getMaxHealth());
            p.extinguish();
            spawnFx((ServerWorld) p.getWorld(), p, ParticleTypes.HEART);
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

    private static void spawnFx(ServerWorld world, ServerPlayerEntity p, net.minecraft.particle.ParticleEffect type) {
        world.spawnParticles(type, p.getX(), p.getY() + 1.0, p.getZ(), 16, 0.5, 0.5, 0.5, 0.05);
    }

    @SuppressWarnings("unused")
    private static ServerPlayerEntity selfOrThrow(ServerCommandSource s) throws CommandSyntaxException {
        return s.getPlayerOrThrow();
    }
}
