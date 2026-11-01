package com.jalebismp.settings.feature;

import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.data.Setting;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Enemy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Night Vision, Mob Spawns (spawn blocker) and Phantom Spawning. Runs on the main thread. */
public class GeneralTask implements Runnable, Listener {
    private final JalebiSettings plugin;
    private final NamespacedKey tag;
    private final Random rnd = new Random();
    private final java.util.Set<CreatureSpawnEvent.SpawnReason> blockedReasons = java.util.EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);

    public GeneralTask(JalebiSettings plugin) {
        this.plugin = plugin;
        this.tag = new NamespacedKey(plugin, "settings_spawned");
        for (String r : plugin.getConfig().getStringList("mob-spawns.blocked-reasons")) {
            try { blockedReasons.add(CreatureSpawnEvent.SpawnReason.valueOf(r.toUpperCase())); }
            catch (IllegalArgumentException ex) { plugin.getLogger().warning("Unknown spawn reason in config: " + r); }
        }
    }

    /**
     * Mob Spawns OFF: hostile mobs do not spawn (naturally) anywhere inside the render distance of a player
     * who has the setting off. Other players nearby are affected too, since the mobs would spawn in view of them.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Enemy)) return;
        if (!blockedReasons.contains(e.getSpawnReason())) return;
        Location loc = e.getLocation();
        World w = loc.getWorld();
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            if (plugin.data().on(p.getUniqueId(), Setting.MOB_SPAWNS)) continue;
            double r = Math.max(2, p.getViewDistance()) * 16.0;
            Location pl = p.getLocation();
            if (Math.abs(pl.getX() - loc.getX()) <= r && Math.abs(pl.getZ() - loc.getZ()) <= r) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { applyNightVision(e.getPlayer(), false); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> applyNightVision(e.getPlayer(), false));
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR) continue;
            applyNightVision(p, false);
            if (plugin.data().on(p.getUniqueId(), Setting.PHANTOM_SPAWNING)) spawnPhantoms(p);
        }
    }

    public void applyNightVision(Player p, boolean removeIfOff) {
        boolean want = plugin.data().on(p.getUniqueId(), Setting.NIGHT_VISION);
        PotionEffect cur = p.getPotionEffect(PotionEffectType.NIGHT_VISION);
        if (want) {
            if (cur == null || !cur.isInfinite())
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        } else if (removeIfOff && cur != null && cur.isInfinite()) {
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    private long countTagged(Player p, String kind, double radius) {
        return p.getNearbyEntities(radius, radius / 2, radius).stream()
                .filter(en -> kind.equals(en.getPersistentDataContainer().get(tag, PersistentDataType.STRING)))
                .count();
    }

    private void spawnPhantoms(Player p) {
        if (p.getWorld().getEnvironment() != World.Environment.NORMAL) return;
        FileConfiguration c = plugin.getConfig();
        int max = c.getInt("phantom-spawns.max-nearby", 3);
        int per = c.getInt("phantom-spawns.per-cycle", 1);

        long nearby = countTagged(p, "phantom", 64);
        for (int i = 0; i < per && nearby < max; i++) {
            Location base = p.getLocation();
            Location loc = base.clone().add(rnd.nextInt(25) - 12, 12 + rnd.nextInt(9), rnd.nextInt(25) - 12);
            if (!loc.getBlock().isPassable() || !loc.clone().add(0, 1, 0).getBlock().isPassable()) continue;
            Entity en = loc.getWorld().spawnEntity(loc, EntityType.PHANTOM, CreatureSpawnEvent.SpawnReason.CUSTOM);
            en.getPersistentDataContainer().set(tag, PersistentDataType.STRING, "phantom");
            if (en instanceof Mob m) m.setTarget(p);
            nearby++;
        }
    }
}
