package com.plagun.mod.events;

import com.plagun.mod.PlagunState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

public final class ModEvents {

    private ModEvents() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                com.plagun.mod.PlagunMod.LOGGER.info("Server tick loop joined by Plagun mod."));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            // Welcome title + subtitle
            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("PLAGUN").formatted(Formatting.GOLD, Formatting.BOLD)));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal("Welcome, " + player.getGameProfile().getName() + "!")
                            .formatted(Formatting.AQUA)));

            // Cheerful broadcast
            server.getPlayerManager().broadcast(
                    Text.literal("→ " + player.getGameProfile().getName() + " joined")
                            .formatted(Formatting.LIGHT_PURPLE), false);

            // Sparkle burst around the joining player
            ServerWorld world = (ServerWorld) player.getWorld();
            world.spawnParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 0.5, 0.5, 0.08);

            // Soft chime
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
                    SoundCategory.PLAYERS, 0.6f, 1.4f);
        });

        // Reset transient state on shutdown
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PlagunState.init());

        // Death FX: explosion of particles + colorful broadcast
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            ServerWorld world = (ServerWorld) player.getWorld();
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    60, 0.7, 1.0, 0.7, 0.1);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 0.5, 0.7, 0.5, 0.05);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.PLAYERS, 0.4f, 1.6f);
        });

        // PvP toggle: cancel player-vs-player melee when disabled
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (PlagunState.isPvpEnabled()) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp && entity instanceof PlayerEntity) {
                sp.sendMessageToClient(Text.literal("PvP is disabled on this server.")
                        .formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
