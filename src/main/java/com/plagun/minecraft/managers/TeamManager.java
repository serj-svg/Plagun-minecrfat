package com.plagun.minecraft.managers;

import com.plagun.minecraft.PlagunPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TeamManager {

    public static final Map<String, NamedTextColor> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put("red", NamedTextColor.RED);
        COLORS.put("blue", NamedTextColor.BLUE);
        COLORS.put("yellow", NamedTextColor.YELLOW);
        COLORS.put("green", NamedTextColor.GREEN);
        COLORS.put("aqua", NamedTextColor.AQUA);
        COLORS.put("purple", NamedTextColor.LIGHT_PURPLE);
        COLORS.put("white", NamedTextColor.WHITE);
        COLORS.put("black", NamedTextColor.BLACK);
        COLORS.put("gold", NamedTextColor.GOLD);
        COLORS.put("gray", NamedTextColor.GRAY);
        COLORS.put("dark_red", NamedTextColor.DARK_RED);
        COLORS.put("dark_blue", NamedTextColor.DARK_BLUE);
        COLORS.put("dark_green", NamedTextColor.DARK_GREEN);
        COLORS.put("dark_aqua", NamedTextColor.DARK_AQUA);
        COLORS.put("dark_purple", NamedTextColor.DARK_PURPLE);
    }

    private final PlagunPlugin plugin;
    private final Map<String, PTeam> teams = new HashMap<>();
    private File file;
    private YamlConfiguration data;

    public TeamManager(PlagunPlugin plugin) {
        this.plugin = plugin;
    }

    public Scoreboard board() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "teams.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try { file.createNewFile(); } catch (IOException e) { plugin.getLogger().warning("Cannot create teams.yml"); }
        }
        data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = data.getConfigurationSection("teams");
        if (sec != null) {
            for (String name : sec.getKeys(false)) {
                String color = sec.getString(name + ".color", "white");
                List<String> uuids = sec.getStringList(name + ".players");
                PTeam t = createInternal(name, color);
                for (String s : uuids) {
                    try {
                        UUID id = UUID.fromString(s);
                        t.members.add(id);
                        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                        if (op.getName() != null) {
                            t.scoreboardTeam.addEntry(op.getName());
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    public void save() {
        if (data == null) return;
        data.set("teams", null);
        for (PTeam t : teams.values()) {
            String base = "teams." + t.name + ".";
            data.set(base + "color", t.colorKey);
            List<String> ids = new ArrayList<>();
            for (UUID id : t.members) ids.add(id.toString());
            data.set(base + "players", ids);
        }
        try { data.save(file); } catch (IOException e) { plugin.getLogger().warning("Cannot save teams.yml"); }
    }

    public PTeam create(String name, String colorKey) {
        if (teams.containsKey(name.toLowerCase(Locale.ROOT))) return null;
        if (!COLORS.containsKey(colorKey.toLowerCase(Locale.ROOT))) return null;
        PTeam t = createInternal(name, colorKey);
        save();
        return t;
    }

    private PTeam createInternal(String name, String colorKey) {
        String key = name.toLowerCase(Locale.ROOT);
        String colorLower = colorKey.toLowerCase(Locale.ROOT);
        NamedTextColor color = COLORS.getOrDefault(colorLower, NamedTextColor.WHITE);

        Scoreboard sb = board();
        String sbName = "pmc_" + key;
        if (sbName.length() > 16) sbName = sbName.substring(0, 16);
        Team existing = sb.getTeam(sbName);
        if (existing != null) existing.unregister();
        Team scoreboardTeam = sb.registerNewTeam(sbName);
        scoreboardTeam.color(color);
        scoreboardTeam.prefix(Component.text("[" + name.toUpperCase(Locale.ROOT) + "] ").color(color));
        scoreboardTeam.displayName(Component.text(name).color(color));
        scoreboardTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        scoreboardTeam.setAllowFriendlyFire(false);
        scoreboardTeam.setCanSeeFriendlyInvisibles(true);

        PTeam t = new PTeam(name, colorLower, color, scoreboardTeam);
        teams.put(key, t);
        return t;
    }

    public boolean delete(String name) {
        PTeam t = teams.remove(name.toLowerCase(Locale.ROOT));
        if (t == null) return false;
        try { t.scoreboardTeam.unregister(); } catch (IllegalStateException ignored) {}
        save();
        return true;
    }

    public PTeam get(String name) {
        return teams.get(name.toLowerCase(Locale.ROOT));
    }

    public Map<String, PTeam> getAll() {
        return Collections.unmodifiableMap(teams);
    }

    public PTeam findPlayerTeam(UUID id) {
        for (PTeam t : teams.values()) {
            if (t.members.contains(id)) return t;
        }
        return null;
    }

    public boolean addPlayer(String teamName, Player player) {
        PTeam t = get(teamName);
        if (t == null) return false;
        removePlayer(player);
        t.members.add(player.getUniqueId());
        t.scoreboardTeam.addEntry(player.getName());
        save();
        return true;
    }

    public boolean removePlayer(Player player) {
        PTeam t = findPlayerTeam(player.getUniqueId());
        if (t == null) return false;
        t.members.remove(player.getUniqueId());
        t.scoreboardTeam.removeEntry(player.getName());
        save();
        return true;
    }

    public void clearAllMembers() {
        for (PTeam t : teams.values()) {
            for (UUID id : new ArrayList<>(t.members)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                if (op.getName() != null) {
                    t.scoreboardTeam.removeEntry(op.getName());
                }
            }
            t.members.clear();
        }
        save();
    }

    public void unregisterAll() {
        for (PTeam t : teams.values()) {
            try { t.scoreboardTeam.unregister(); } catch (IllegalStateException ignored) {}
        }
        teams.clear();
    }

    /**
     * Random distribution of players across N teams with a max of M per team.
     * Returns the mapping of team name -> assigned players, or null if not enough teams.
     */
    public Map<String, List<Player>> autoDistribute(List<Player> players, int teamCount, int playersPerTeam) {
        if (teamCount <= 0 || playersPerTeam <= 0) return null;
        if (teams.size() < teamCount) return null;

        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        List<PTeam> chosen = new ArrayList<>(teams.values()).subList(0, teamCount);

        clearAllMembers();

        Map<String, List<Player>> assignments = new LinkedHashMap<>();
        for (PTeam t : chosen) assignments.put(t.name, new ArrayList<>());

        int idx = 0;
        for (Player p : shuffled) {
            boolean placed = false;
            for (int i = 0; i < chosen.size(); i++) {
                PTeam t = chosen.get((idx + i) % chosen.size());
                if (assignments.get(t.name).size() < playersPerTeam) {
                    assignments.get(t.name).add(p);
                    t.members.add(p.getUniqueId());
                    t.scoreboardTeam.addEntry(p.getName());
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

    public static class PTeam {
        public final String name;
        public final String colorKey;
        public final NamedTextColor color;
        public final Team scoreboardTeam;
        public final List<UUID> members = new ArrayList<>();

        public PTeam(String name, String colorKey, NamedTextColor color, Team scoreboardTeam) {
            this.name = name;
            this.colorKey = colorKey;
            this.color = color;
            this.scoreboardTeam = scoreboardTeam;
        }

    }
}
