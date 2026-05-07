package com.plagun.mod.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.plagun.mod.PlagunMod;
import com.plagun.mod.data.PlagunData;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public final class TeamManager {

    public static final Map<String, Formatting> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put("red", Formatting.RED);
        COLORS.put("blue", Formatting.BLUE);
        COLORS.put("yellow", Formatting.YELLOW);
        COLORS.put("green", Formatting.GREEN);
        COLORS.put("aqua", Formatting.AQUA);
        COLORS.put("purple", Formatting.LIGHT_PURPLE);
        COLORS.put("white", Formatting.WHITE);
        COLORS.put("black", Formatting.BLACK);
        COLORS.put("gold", Formatting.GOLD);
        COLORS.put("gray", Formatting.GRAY);
        COLORS.put("dark_red", Formatting.DARK_RED);
        COLORS.put("dark_blue", Formatting.DARK_BLUE);
        COLORS.put("dark_green", Formatting.DARK_GREEN);
        COLORS.put("dark_aqua", Formatting.DARK_AQUA);
        COLORS.put("dark_purple", Formatting.DARK_PURPLE);
    }

    public static final class PTeam {
        public final String name;
        public final String colorKey;
        public final Formatting color;
        public final Set<UUID> members = new LinkedHashSet<>();

        PTeam(String name, String colorKey, Formatting color) {
            this.name = name;
            this.colorKey = colorKey;
            this.color = color;
        }
    }

    private static final Map<String, PTeam> TEAMS = new LinkedHashMap<>();
    private static MinecraftServer server;

    private TeamManager() {}

    public static void attach(MinecraftServer s) { server = s; }

    public static Scoreboard board() {
        return server == null ? null : server.getScoreboard();
    }

    public static void load(MinecraftServer s) {
        attach(s);
        TEAMS.clear();
        JsonObject root = PlagunData.load();
        if (!root.has("teams")) return;
        JsonObject teams = root.getAsJsonObject("teams");
        for (String name : teams.keySet()) {
            JsonObject t = teams.getAsJsonObject(name);
            String colorKey = t.has("color") ? t.get("color").getAsString() : "white";
            createInternal(name, colorKey);
            PTeam p = TEAMS.get(name.toLowerCase(Locale.ROOT));
            if (p != null && t.has("members")) {
                JsonArray arr = t.getAsJsonArray("members");
                for (int i = 0; i < arr.size(); i++) {
                    try {
                        UUID id = UUID.fromString(arr.get(i).getAsString());
                        p.members.add(id);
                        applyToScoreboard(p, id);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    public static void save() {
        JsonObject root = PlagunData.load();
        JsonObject teams = new JsonObject();
        for (PTeam p : TEAMS.values()) {
            JsonObject t = new JsonObject();
            t.addProperty("color", p.colorKey);
            JsonArray arr = new JsonArray();
            for (UUID id : p.members) arr.add(id.toString());
            t.add("members", arr);
            teams.add(p.name, t);
        }
        root.add("teams", teams);
        PlagunData.save(root);
    }

    public static PTeam create(String name, String colorKey) {
        if (server == null) return null;
        if (TEAMS.containsKey(name.toLowerCase(Locale.ROOT))) return null;
        if (!COLORS.containsKey(colorKey.toLowerCase(Locale.ROOT))) return null;
        PTeam t = createInternal(name, colorKey);
        save();
        return t;
    }

    private static PTeam createInternal(String name, String colorKey) {
        String key = name.toLowerCase(Locale.ROOT);
        Formatting color = COLORS.getOrDefault(colorKey.toLowerCase(Locale.ROOT), Formatting.WHITE);
        Scoreboard sb = board();
        if (sb == null) {
            PlagunMod.LOGGER.warn("Scoreboard unavailable; skipping team registration for {}", name);
            return null;
        }
        String sbName = "pmc_" + key;
        if (sbName.length() > 16) sbName = sbName.substring(0, 16);
        Team existing = sb.getTeam(sbName);
        if (existing != null) sb.removeTeam(existing);
        Team scoreboardTeam = sb.addTeam(sbName);
        scoreboardTeam.setColor(color);
        scoreboardTeam.setPrefix(Text.literal("[" + name.toUpperCase(Locale.ROOT) + "] ").formatted(color));
        scoreboardTeam.setDisplayName(Text.literal(name).formatted(color));
        scoreboardTeam.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        scoreboardTeam.setFriendlyFireAllowed(false);
        scoreboardTeam.setShowFriendlyInvisibles(true);

        PTeam t = new PTeam(name, colorKey.toLowerCase(Locale.ROOT), color);
        TEAMS.put(key, t);
        return t;
    }

    public static boolean delete(String name) {
        PTeam t = TEAMS.remove(name.toLowerCase(Locale.ROOT));
        if (t == null) return false;
        Scoreboard sb = board();
        if (sb != null) {
            String sbName = "pmc_" + name.toLowerCase(Locale.ROOT);
            if (sbName.length() > 16) sbName = sbName.substring(0, 16);
            Team st = sb.getTeam(sbName);
            if (st != null) sb.removeTeam(st);
        }
        save();
        return true;
    }

    public static PTeam get(String name) { return TEAMS.get(name.toLowerCase(Locale.ROOT)); }
    public static Map<String, PTeam> all() { return Collections.unmodifiableMap(TEAMS); }

    public static PTeam findFor(UUID id) {
        for (PTeam t : TEAMS.values()) if (t.members.contains(id)) return t;
        return null;
    }

    public static boolean addPlayer(String teamName, ServerPlayerEntity player) {
        PTeam t = get(teamName);
        if (t == null) return false;
        removePlayer(player);
        t.members.add(player.getUuid());
        applyToScoreboard(t, player.getUuid());
        save();
        return true;
    }

    public static boolean removePlayer(ServerPlayerEntity player) {
        PTeam t = findFor(player.getUuid());
        if (t == null) return false;
        t.members.remove(player.getUuid());
        Scoreboard sb = board();
        if (sb != null) {
            String sbName = "pmc_" + t.name.toLowerCase(Locale.ROOT);
            if (sbName.length() > 16) sbName = sbName.substring(0, 16);
            Team st = sb.getTeam(sbName);
            if (st != null) sb.removeScoreHolderFromTeam(player.getNameForScoreboard(), st);
        }
        save();
        return true;
    }

    public static void applyToScoreboard(PTeam t, UUID id) {
        if (server == null) return;
        Scoreboard sb = board();
        if (sb == null) return;
        String sbName = "pmc_" + t.name.toLowerCase(Locale.ROOT);
        if (sbName.length() > 16) sbName = sbName.substring(0, 16);
        Team st = sb.getTeam(sbName);
        if (st == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
        String holder = p != null ? p.getNameForScoreboard() : nameFromProfile(id);
        if (holder == null) return;
        sb.addScoreHolderToTeam(holder, st);
    }

    public static void clearAllMembers() {
        Scoreboard sb = board();
        for (PTeam t : TEAMS.values()) {
            if (sb != null) {
                String sbName = "pmc_" + t.name.toLowerCase(Locale.ROOT);
                if (sbName.length() > 16) sbName = sbName.substring(0, 16);
                Team st = sb.getTeam(sbName);
                if (st != null) {
                    for (UUID id : t.members) {
                        ServerPlayerEntity p = server == null ? null : server.getPlayerManager().getPlayer(id);
                        String holder = p != null ? p.getNameForScoreboard() : nameFromProfile(id);
                        if (holder != null) sb.removeScoreHolderFromTeam(holder, st);
                    }
                }
            }
            t.members.clear();
        }
        save();
    }

    public static void unregisterAll() {
        Scoreboard sb = board();
        if (sb != null) {
            for (PTeam t : TEAMS.values()) {
                String sbName = "pmc_" + t.name.toLowerCase(Locale.ROOT);
                if (sbName.length() > 16) sbName = sbName.substring(0, 16);
                Team st = sb.getTeam(sbName);
                if (st != null) sb.removeTeam(st);
            }
        }
        TEAMS.clear();
    }

    public static Map<String, List<ServerPlayerEntity>> autoDistribute(List<ServerPlayerEntity> players, int teamCount, int perTeam) {
        if (teamCount <= 0 || perTeam <= 0) return null;
        if (TEAMS.size() < teamCount) return null;

        List<ServerPlayerEntity> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        List<PTeam> chosen = new ArrayList<>(TEAMS.values()).subList(0, teamCount);

        clearAllMembers();

        Map<String, List<ServerPlayerEntity>> assignments = new LinkedHashMap<>();
        for (PTeam t : chosen) assignments.put(t.name, new ArrayList<>());

        int idx = 0;
        for (ServerPlayerEntity p : shuffled) {
            boolean placed = false;
            for (int i = 0; i < chosen.size(); i++) {
                PTeam t = chosen.get((idx + i) % chosen.size());
                if (assignments.get(t.name).size() < perTeam) {
                    assignments.get(t.name).add(p);
                    t.members.add(p.getUuid());
                    applyToScoreboard(t, p.getUuid());
                    placed = true;
                    idx = (idx + i + 1) % chosen.size();
                    break;
                }
            }
            if (!placed) break;
        }
        save();
        return assignments;
    }

    private static String nameFromProfile(UUID id) {
        if (server == null) return null;
        Optional<GameProfile> opt = server.getUserCache() == null
                ? Optional.empty()
                : server.getUserCache().getByUuid(id);
        return opt.map(GameProfile::getName).orElse(null);
    }
}
