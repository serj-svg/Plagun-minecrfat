package com.plagun.mod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.plagun.mod.managers.ModerationManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.EnumSet;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ModerationCommands {

    private ModerationCommands() {}

    private static final Set<PlayerPositionLookS2CPacket.PositionFlag> NO_FLAGS =
            EnumSet.noneOf(PlayerPositionLookS2CPacket.PositionFlag.class);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("pfreeze")
                .requires(s -> s.hasPermissionLevel(2))
                .then(argument("target", EntityArgumentType.player()).executes(ctx -> {
                    ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "target");
                    ModerationManager.freeze(t);
                    t.sendMessage(Text.literal("You have been frozen.").formatted(Formatting.AQUA));
                    ctx.getSource().sendFeedback(() -> Text.literal("Froze " + t.getName().getString()).formatted(Formatting.GREEN), true);
                    return Command.SINGLE_SUCCESS;
                })));

        dispatcher.register(literal("punfreeze")
                .requires(s -> s.hasPermissionLevel(2))
                .then(argument("target", EntityArgumentType.player()).executes(ctx -> {
                    ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "target");
                    ModerationManager.unfreeze(t.getUuid());
                    t.sendMessage(Text.literal("You have been unfrozen.").formatted(Formatting.AQUA));
                    ctx.getSource().sendFeedback(() -> Text.literal("Unfroze " + t.getName().getString()).formatted(Formatting.GREEN), true);
                    return Command.SINGLE_SUCCESS;
                })));

        dispatcher.register(literal("pheal")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> healOne(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
                .then(argument("target", EntityArgumentType.player())
                        .executes(ctx -> healOne(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target")))));

        dispatcher.register(literal("pfeed")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> feedOne(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
                .then(argument("target", EntityArgumentType.player())
                        .executes(ctx -> feedOne(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target")))));

        dispatcher.register(literal("pvanish")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    boolean v = ModerationManager.toggleVanish(p);
                    p.sendMessage(Text.literal(v ? "You are now vanished." : "You are no longer vanished.").formatted(Formatting.AQUA));
                    return Command.SINGLE_SUCCESS;
                }));

        dispatcher.register(literal("ptp")
                .requires(s -> s.hasPermissionLevel(2))
                .then(argument("target", EntityArgumentType.player()).executes(ctx -> {
                    ServerPlayerEntity self = ctx.getSource().getPlayerOrThrow();
                    ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "target");
                    self.teleport(t.getServerWorld(), t.getX(), t.getY(), t.getZ(), NO_FLAGS, self.getYaw(), self.getPitch(), false);
                    self.sendMessage(Text.literal("Teleported to " + t.getName().getString()).formatted(Formatting.GREEN));
                    return Command.SINGLE_SUCCESS;
                })));

        dispatcher.register(literal("pbroadcast")
                .requires(s -> s.hasPermissionLevel(2))
                .then(argument("message", StringArgumentType.greedyString()).executes(ctx -> {
                    String msg = StringArgumentType.getString(ctx, "message");
                    if (ctx.getSource().getServer() != null) {
                        ctx.getSource().getServer().getPlayerManager().broadcast(
                                Text.literal("[Broadcast] " + msg).formatted(Formatting.GOLD), false);
                    }
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private static int healOne(ServerCommandSource s, ServerPlayerEntity t) {
        t.setHealth(t.getMaxHealth());
        t.extinguish();
        s.sendFeedback(() -> Text.literal("Healed " + t.getName().getString()).formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int feedOne(ServerCommandSource s, ServerPlayerEntity t) {
        t.getHungerManager().setFoodLevel(20);
        t.getHungerManager().setSaturationLevel(20f);
        s.sendFeedback(() -> Text.literal("Fed " + t.getName().getString()).formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }
}
