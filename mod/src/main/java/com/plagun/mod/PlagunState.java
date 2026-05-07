package com.plagun.mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory mod state. Persistence will be added later.
 */
public final class PlagunState {

    private static final Set<UUID> SPARKLE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static volatile boolean pvpEnabled = true;

    private PlagunState() {}

    public static void init() {
        SPARKLE_PLAYERS.clear();
        pvpEnabled = true;
    }

    public static boolean toggleSparkle(UUID id) {
        if (SPARKLE_PLAYERS.remove(id)) return false;
        SPARKLE_PLAYERS.add(id);
        return true;
    }

    public static boolean hasSparkle(UUID id) {
        return SPARKLE_PLAYERS.contains(id);
    }

    public static Set<UUID> sparkleSet() {
        return SPARKLE_PLAYERS;
    }

    public static boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public static void setPvpEnabled(boolean enabled) {
        pvpEnabled = enabled;
    }
}
