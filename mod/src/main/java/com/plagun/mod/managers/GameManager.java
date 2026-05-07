package com.plagun.mod.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.plagun.mod.PlagunMod;
import com.plagun.mod.data.PlagunData;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameManager {

    private static MinecraftServer server;

    private static Vec3d lobbyPos;
    private static RegistryKey<World> lobbyDim;

    private static final List<Vec3d> SPAWNS = new ArrayList<>();
    private static RegistryKey<World> spawnsDim;

    private static int countdownSeconds = 30;
    private static int minPlayers = 2;
    private static boolean invulnCountdown = true;

    private static boolean running = false;
    private static int countdownLeft = 0;
    private static int tickCounter = 0;

    private GameManager() {}

    public static void attach(MinecraftServer s) { server = s; }

    public static void load(MinecraftServer s) {
        attach(s);
        SPAWNS.clear();
        lobbyPos = null;
        lobbyDim = null;
        spawnsDim = null;
        running = false;
        countdownLeft = 0;

        JsonObject root = PlagunData.load();
        if (!root.has("game")) return;
        JsonObject game = root.getAsJsonObject("game");

        if (game.has("countdown-seconds")) countdownSeconds = game.get("countdown-seconds").getAsInt();
        if (game.has("min-players")) minPlayers = game.get("min-players").getAsInt();
        if (game.has("invuln-countdown")) invulnCountdown = game.get("invuln-countdown").getAsBoolean();

        if (game.has("lobby")) {
            JsonObject lo = game.getAsJsonObject("lobby");
            lobbyDim = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(lo.get("dim").getAsString()));
            lobbyPos = new Vec3d(lo.get("x").getAsDouble(), lo.get("y").getAsDouble(), lo.get("z").getAsDouble());
        }
        if (game.has("spawns")) {
            JsonArray arr = game.getAsJsonArray("spawns");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject sp = arr.get(i).getAsJsonObject();
                if (spawnsDim == null) spawnsDim = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(sp.get("dim").getAsString()));
                SPAWNS.add(new Vec3d(sp.get("x").getAsDouble(), sp.get("y").getAsDouble(), sp.get("z").getAsDouble()));
            }
        }
    }

    public static void save() {
        JsonObject root = PlagunData.load();
        JsonObject game = new JsonObject();
        game.addProperty("countdown-seconds", countdownSeconds);
        game.addProperty("min-players", minPlayers);
        game.addProperty("invuln-countdown", invulnCountdown);
        if (lobbyPos != null && lobbyDim != null) {
            JsonObject lo = new JsonObject();
            lo.addProperty("dim", lobbyDim.getValue().toString());
            lo.addProperty("x", lobbyPos.x);
            lo.addProperty("y", lobbyPos.y);
            lo.addProperty("z", lobbyPos.z);
            game.add("lobby", lo);
        }
        if (!SPAWNS.isEmpty() && spawnsDim != null) {
            JsonArray arr = new JsonArray();
            for (Vec3d v : SPAWNS) {
                JsonObject sp = new JsonObject();
                sp.addProperty("dim", spawnsDim.getValue().toString());
                sp.addProperty("x", v.x);
                sp.addProperty("y", v.y);
                sp.addProperty("z", v.z);
                arr.add(sp);
            }
            game.add("spawns", arr);
        }
        root.add("game", game);
        PlagunData.save(root);
    }

    public static void setLobby(ServerPlayerEntity p) {
        lobbyPos = p.getPos();
        lobbyDim = p.getServerWorld().getRegistryKey();
        save();
    }

    public static boolean hasLobby() { return lobbyPos != null && lobbyDim != null; }

    public static void teleportToLobby(ServerPlayerEntity p) {
        if (!hasLobby() || server == null) return;
        ServerWorld w = server.getWorld(lobbyDim);
        if (w == null) return;
        Teleporter.move(server, p, w, lobbyPos.x, lobbyPos.y, lobbyPos.z, p.getYaw(), p.getPitch());
    }

    public static void addSpawn(ServerPlayerEntity p) {
        if (spawnsDim == null) spawnsDim = p.getServerWorld().getRegistryKey();
        SPAWNS.add(p.getPos());
        save();
    }

    public static void clearSpawns() { SPAWNS.clear(); save(); }
    public static int spawnCount() { return SPAWNS.size(); }
    public static boolean isRunning() { return running; }

    public static boolean start() {
        if (server == null || running) return false;
        if (SPAWNS.isEmpty() || spawnsDim == null) return false;
        ServerWorld world = server.getWorld(spawnsDim);
        if (world == null) return false;

        List<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
        if (players.size() < minPlayers) return false;

        Collections.shuffle(players);
        List<Vec3d> spawnsCopy = new ArrayList<>(SPAWNS);
        Collections.shuffle(spawnsCopy);

        for (int i = 0; i < players.size(); i++) {
            ServerPlayerEntity p = players.get(i);
            Vec3d sp = spawnsCopy.get(i % spawnsCopy.size());
            Teleporter.move(server, p, world, sp.x, sp.y, sp.z, p.getYaw(), p.getPitch());
            p.changeGameMode(GameMode.SURVIVAL);
            p.setHealth(p.getMaxHealth());
            p.getHungerManager().setFoodLevel(20);
            p.getInventory().clear();
            if (invulnCountdown) p.setInvulnerable(true);
        }

        running = true;
        countdownLeft = countdownSeconds;
        tickCounter = 0;
        broadcast(Text.literal("Hunger Games starting in " + countdownSeconds + "s!").formatted(Formatting.GOLD));
        return true;
    }

    public static boolean stop() {
        if (!running) return false;
        running = false;
        countdownLeft = 0;
        if (server != null) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) p.setInvulnerable(false);
        }
        broadcast(Text.literal("Hunger Games stopped.").formatted(Formatting.RED));
        return true;
    }

    public static void tick() {
        if (!running || server == null) return;
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        if (countdownLeft <= 0) {
            broadcast(Text.literal("GO! May the odds be in your favor.").formatted(Formatting.GREEN));
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) p.setInvulnerable(false);
            running = false;
            return;
        }
        if (countdownLeft <= 5 || countdownLeft % 10 == 0) {
            broadcast(Text.literal("Starts in " + countdownLeft + "s").formatted(Formatting.YELLOW));
        }
        countdownLeft--;
    }

    private static void broadcast(Text msg) {
        if (server != null) server.getPlayerManager().broadcast(msg, false);
        else PlagunMod.LOGGER.info("[broadcast w/o server] {}", msg.getString());
    }
}
