package com.plagun.mod.effects;

import com.plagun.mod.PlagunState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Iterator;
import java.util.UUID;

public final class SparkleManager {

    private static int tick = 0;

    private SparkleManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PlagunState.sparkleSet().isEmpty()) return;
            tick++;
            if (tick % 3 != 0) return;

            Iterator<UUID> it = PlagunState.sparkleSet().iterator();
            while (it.hasNext()) {
                UUID id = it.next();
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                if (p == null) continue;
                ServerWorld world = (ServerWorld) p.getWorld();

                // Cycle through three particle types for a colorful trail
                int phase = (tick / 3) % 3;
                switch (phase) {
                    case 0 -> world.spawnParticles(ParticleTypes.END_ROD,
                            p.getX(), p.getY() + 0.1, p.getZ(),
                            4, 0.25, 0.05, 0.25, 0.0);
                    case 1 -> world.spawnParticles(ParticleTypes.WAX_ON,
                            p.getX(), p.getY() + 0.1, p.getZ(),
                            6, 0.3, 0.1, 0.3, 0.01);
                    case 2 -> world.spawnParticles(ParticleTypes.GLOW,
                            p.getX(), p.getY() + 0.1, p.getZ(),
                            5, 0.3, 0.05, 0.3, 0.0);
                }
            }
        });
    }
}
