package com.jalebismp.settings.feature;

import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.data.Setting;
import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPListener implements Listener {
    private final JalebiSettings plugin;

    public PvPListener(JalebiSettings plugin) { this.plugin = plugin; }

    /** Fast Crystals: a hit removes the crystal and detonates it immediately. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystalHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof EnderCrystal crystal)) return;
        if (!(e.getDamager() instanceof Player p)) return;
        if (!plugin.data().on(p.getUniqueId(), Setting.FAST_CRYSTALS)) return;

        e.setCancelled(true);
        Location loc = crystal.getLocation();
        crystal.remove();
        loc.getWorld().createExplosion(loc, 6.0f, false, true, p);
    }
}
