package com.plagun.mod.managers;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory moderation state: frozen players (snapshot pos), vanish set.
 * Not persisted across restarts (intentional — moderation is a session thing).
 */
public final class ModerationManager {

    private static final Map<UUID, Vec3d> FROZEN = new HashMap<>();
    private static final Set<UUID> VANISHED = new HashSet<>();

    private ModerationManager() {}

    public static void freeze(ServerPlayerEntity p) {
        FROZEN.put(p.getUuid(), p.getPos());
    }

    public static void unfreeze(UUID id) { FROZEN.remove(id); }
    public static boolean isFrozen(UUID id) { return FROZEN.containsKey(id); }
    public static Vec3d frozenPos(UUID id) { return FROZEN.get(id); }
    public static Set<UUID> frozenIds() { return FROZEN.keySet(); }

    public static boolean toggleVanish(ServerPlayerEntity p) {
        UUID id = p.getUuid();
        if (VANISHED.remove(id)) {
            p.removeStatusEffect(StatusEffects.INVISIBILITY);
            return false;
        }
        VANISHED.add(id);
        // Long-duration invisibility (max int seconds, no particles, no icon)
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        return true;
    }

    public static boolean isVanished(UUID id) { return VANISHED.contains(id); }
}
