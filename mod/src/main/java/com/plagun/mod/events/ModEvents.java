package com.plagun.mod.events;

import com.plagun.mod.PlagunState;
import com.plagun.mod.managers.GameManager;
import com.plagun.mod.managers.LivesManager;
import com.plagun.mod.managers.ModerationManager;
import com.plagun.mod.managers.TeamManager;
import com.plagun.mod.managers.Teleporter;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class ModEvents {

    private ModEvents() {}

    public static void register() {
        // Lifecycle: load/save, attach managers
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LivesManager.load();
            TeamManager.load(server);
            GameManager.load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LivesManager.save();
            TeamManager.save();
            GameManager.save();
            PlagunState.init();
        });

        // Welcome FX on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("PLAGUN").formatted(Formatting.GOLD, Formatting.BOLD)));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal("Welcome, " + player.getGameProfile().getName() + "!")
                            .formatted(Formatting.AQUA)));

            server.getPlayerManager().broadcast(
                    Text.literal("→ " + player.getGameProfile().getName() + " joined")
                            .formatted(Formatting.LIGHT_PURPLE), false);

            ServerWorld world = (ServerWorld) player.getWorld();
            world.spawnParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 0.5, 0.5, 0.08);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
                    SoundCategory.PLAYERS, 0.6f, 1.4f);

            // Re-apply spectator if perma-dead persisted from previous session
            if (LivesManager.isPermaDead(player.getUuid())) {
                player.changeGameMode(LivesManager.deathGameMode());
                player.sendMessage(Text.literal("You are permanently dead. Awaiting revival.").formatted(Formatting.DARK_RED));
            }

            // Re-apply scoreboard team membership in case scoreboard was reset
            TeamManager.PTeam pt = TeamManager.findFor(player.getUuid());
            if (pt != null) TeamManager.applyToScoreboard(pt, player.getUuid());
        });

        // Lives: decrement on death, broadcast on perma-death
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;

            // Death FX
            ServerWorld world = (ServerWorld) player.getWorld();
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    60, 0.7, 1.0, 0.7, 0.1);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 0.5, 0.7, 0.5, 0.05);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.PLAYERS, 0.4f, 1.6f);

            // Lives bookkeeping
            if (!LivesManager.has(player.getUuid())) return;
            int remaining = LivesManager.decrement(player.getUuid());
            LivesManager.save();
            if (remaining <= 0) {
                if (LivesManager.broadcastFinal() && player.getServer() != null) {
                    player.getServer().getPlayerManager().broadcast(
                            Text.literal(player.getGameProfile().getName() + " is permanently dead. RIP.")
                                    .formatted(Formatting.DARK_RED), false);
                }
            } else {
                player.sendMessage(Text.literal("You died. Lives left: " + remaining).formatted(Formatting.RED));
            }
        });

        // Lives: lock perma-dead players into spectator after respawn
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (LivesManager.isPermaDead(newPlayer.getUuid())) {
                newPlayer.changeGameMode(LivesManager.deathGameMode());
                newPlayer.sendMessage(Text.literal("You have no lives left. Wait for an admin to revive you.").formatted(Formatting.DARK_RED));
            }
            // Re-apply team scoreboard entry on respawn
            TeamManager.PTeam pt = TeamManager.findFor(newPlayer.getUuid());
            if (pt != null) TeamManager.applyToScoreboard(pt, newPlayer.getUuid());
        });

        // Tick: hunger games countdown + freeze enforcement
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GameManager.tick();

            if (!ModerationManager.frozenIds().isEmpty()) {
                for (UUID id : ModerationManager.frozenIds()) {
                    ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                    if (p == null) continue;
                    Vec3d pos = ModerationManager.frozenPos(id);
                    if (pos == null) continue;
                    if (p.squaredDistanceTo(pos.x, pos.y, pos.z) > 0.04) {
                        Teleporter.move(server, p, (ServerWorld) p.getWorld(), pos.x, pos.y, pos.z, p.getYaw(), p.getPitch());
                    }
                }
            }
        });

        // PvP toggle: cancel player-vs-player melee when disabled
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (PlagunState.isPvpEnabled()) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp && entity instanceof PlayerEntity) {
                sp.sendMessageToClient(Text.literal("PvP is disabled on this server.").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
