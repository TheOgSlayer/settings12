package com.jalebismp.settings.data;

import com.jalebismp.settings.JalebiSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Stores per-player settings, follow relations and known names. Thread-safe for reads. */
public class DataManager {
    private final JalebiSettings plugin;
    private final File file;
    private final Map<UUID, Map<Setting, Value>> settings = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> following = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> followers = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private volatile Map<Setting, Value> defaults = new EnumMap<>(Setting.class);
    private volatile boolean dirty;

    public DataManager(JalebiSettings plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------------- settings ----------------

    public void reloadDefaults() {
        Map<Setting, Value> map = new EnumMap<>(Setting.class);
        for (Setting s : Setting.values()) {
            Value v = s.def();
            String cfg = plugin.getConfig().getString("defaults." + s.name().toLowerCase());
            if (cfg != null) {
                try {
                    Value parsed = Value.valueOf(cfg.toUpperCase());
                    if (s.options().contains(parsed)) v = parsed;
                } catch (IllegalArgumentException ignored) { }
            }
            map.put(s, v);
        }
        defaults = map;
    }

    public Value get(UUID id, Setting s) {
        Map<Setting, Value> m = settings.get(id);
        Value v = m == null ? null : m.get(s);
        return v != null ? v : defaults.getOrDefault(s, s.def());
    }

    public void set(UUID id, Setting s, Value v) {
        settings.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(s, v);
        dirty = true;
    }

    /** True if the value is anything other than OFF. */
    public boolean on(UUID id, Setting s) {
        return get(id, s) != Value.OFF;
    }

    /** Should {@code receiver} get something coming from {@code sender} under setting {@code s}? */
    public boolean allows(UUID receiver, UUID sender, Setting s) {
        if (receiver.equals(sender)) return true;
        return switch (get(receiver, s)) {
            case ON, ANYONE -> true;
            case OFF -> false;
            case FRIENDS -> isFollowing(receiver, sender);
        };
    }

    // ---------------- social ----------------

    public boolean isFollowing(UUID a, UUID b) {
        Set<UUID> s = following.get(a);
        return s != null && s.contains(b);
    }

    public boolean isFriend(UUID a, UUID b) {
        return isFollowing(a, b) && isFollowing(b, a);
    }

    public void follow(UUID a, UUID b) {
        following.computeIfAbsent(a, k -> ConcurrentHashMap.newKeySet()).add(b);
        followers.computeIfAbsent(b, k -> ConcurrentHashMap.newKeySet()).add(a);
        dirty = true;
    }

    public void unfollow(UUID a, UUID b) {
        Set<UUID> f = following.get(a);
        if (f != null) f.remove(b);
        Set<UUID> r = followers.get(b);
        if (r != null) r.remove(a);
        dirty = true;
    }

    public Set<UUID> following(UUID id) { return new HashSet<>(following.getOrDefault(id, Set.of())); }
    public Set<UUID> followers(UUID id) { return new HashSet<>(followers.getOrDefault(id, Set.of())); }

    public Set<UUID> friends(UUID id) {
        Set<UUID> out = following(id);
        out.retainAll(followers.getOrDefault(id, Set.of()));
        return out;
    }

    // ---------------- names ----------------

    public void remember(Player p) {
        String old = names.put(p.getUniqueId(), p.getName());
        if (!p.getName().equals(old)) dirty = true;
    }

    public String nameOf(UUID id) { return names.getOrDefault(id, "Unknown"); }

    public UUID findByName(String name) {
        for (Map.Entry<UUID, String> e : names.entrySet())
            if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        return null;
    }

    public Collection<String> knownNames() { return names.values(); }

    // ---------------- persistence ----------------

    public void load() {
        reloadDefaults();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = y.getConfigurationSection("players");
        if (players == null) return;
        for (String key : players.getKeys(false)) {
            UUID id;
            try { id = UUID.fromString(key); } catch (IllegalArgumentException ex) { continue; }
            ConfigurationSection sec = players.getConfigurationSection(key);
            if (sec == null) continue;
            names.put(id, sec.getString("name", "Unknown"));
            ConfigurationSection ss = sec.getConfigurationSection("settings");
            if (ss != null) {
                for (String k : ss.getKeys(false)) {
                    try {
                        Setting s = Setting.valueOf(k.toUpperCase());
                        Value v = Value.valueOf(ss.getString(k, "").toUpperCase());
                        if (s.options().contains(v)) set(id, s, v);
                    } catch (IllegalArgumentException ignored) { }
                }
            }
            for (String f : sec.getStringList("following")) {
                try { follow(id, UUID.fromString(f)); } catch (IllegalArgumentException ignored) { }
            }
        }
        dirty = false;
    }

    public synchronized void save() {
        if (!dirty) return;
        dirty = false;
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            UUID id = e.getKey();
            String base = "players." + id;
            y.set(base + ".name", e.getValue());
            Map<Setting, Value> m = settings.get(id);
            if (m != null)
                for (Map.Entry<Setting, Value> en : m.entrySet())
                    y.set(base + ".settings." + en.getKey().name().toLowerCase(), en.getValue().name());
            Set<UUID> f = following.get(id);
            if (f != null && !f.isEmpty()) y.set(base + ".following", f.stream().map(UUID::toString).toList());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save data.yml: " + ex.getMessage());
            dirty = true;
        }
    }
}
