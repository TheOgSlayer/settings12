package com.jalebismp.settings.feature;

import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.data.Setting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Teleport Confirm Menu: when the player has it OFF, /tpa and /tpahere send the request straight away
 * instead of opening JalebiTPA's confirm GUI. (JalebiTPA's own checks - online, not yourself - are repeated here;
 * the Teleport Requests / Teleport Here Requests privacy guard has already run at HIGH priority.)
 */
public class TeleportMenuListener implements Listener {
    private final JalebiSettings plugin;
    private Object manager;
    private Method sendRequest;

    public TeleportMenuListener(JalebiSettings plugin) {
        this.plugin = plugin;
    }

    private boolean resolve() {
        if (sendRequest != null) return true;
        try {
            Plugin tpa = Bukkit.getPluginManager().getPlugin("JalebiTPA");
            if (tpa == null || !tpa.isEnabled()) return false;
            Field f = tpa.getClass().getDeclaredField("manager");
            f.setAccessible(true);
            manager = f.get(tpa);
            sendRequest = manager.getClass().getMethod("sendRequest", Player.class, Player.class, boolean.class);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Teleport Confirm Menu: could not hook JalebiTPA (" + t + ")");
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String raw = e.getMessage();
        if (raw.length() < 2) return;
        String[] parts = raw.substring(1).trim().split("\\s+");
        if (parts.length != 2) return;

        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);
        boolean here = label.equals("tpahere");
        if (!here && !label.equals("tpa")) return;

        if (plugin.data().on(p.getUniqueId(), Setting.TELEPORT_CONFIRM_MENU)) return; // menu wanted
        Player target = Bukkit.getPlayerExact(parts[1]);
        if (target == null || target.equals(p)) return; // let JalebiTPA print its normal error
        if (!resolve()) return;

        e.setCancelled(true);
        try {
            sendRequest.invoke(manager, p, target, here);
        } catch (Throwable t) {
            plugin.getLogger().warning("Teleport Confirm Menu: sendRequest failed (" + t + ")");
        }
    }
}
