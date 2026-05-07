package com.plagun.mod.managers;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Locale;

/**
 * Version-stable teleport helper. Routes through Minecraft's vanilla command
 * pipeline (/execute in &lt;dim&gt; run tp &lt;player&gt; ...) so we don't have to track
 * yarn renames of low-level teleport APIs (PositionFlag, RelativeMovement,
 * TeleportTarget, etc.).
 */
public final class Teleporter {

    private Teleporter() {}

    public static void move(MinecraftServer server, ServerPlayerEntity p,
                            ServerWorld world, double x, double y, double z,
                            float yaw, float pitch) {
        if (server == null || world == null || p == null) return;
        Identifier dim = world.getRegistryKey().getValue();
        String name = p.getNameForScoreboard();
        String cmd = String.format(Locale.ROOT,
                "execute in %s run tp %s %.4f %.4f %.4f %.2f %.2f",
                dim, name, x, y, z, yaw, pitch);
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), cmd);
    }
}
