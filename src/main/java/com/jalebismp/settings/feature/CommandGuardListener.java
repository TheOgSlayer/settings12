package com.jalebismp.settings.feature;

import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.Msg;
import com.jalebismp.settings.data.Setting;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces Private Messages / Teleport Requests / Teleport Here Requests / Allow Payments
 * for commands from other plugins (configurable in config.yml -> guarded-commands).
 */
public class CommandGuardListener implements Listener {
    private final JalebiSettings plugin;
    private final Map<String, Setting> commands = new HashMap<>();
    private final Map<Setting, String> wording = new HashMap<>();

    public CommandGuardListener(JalebiSettings plugin) {
        this.plugin = plugin;
        wording.put(Setting.PRIVATE_MESSAGES, "messages");
        wording.put(Setting.TELEPORT_REQUESTS, "teleport requests");
        wording.put(Setting.TELEPORT_HERE_REQUESTS, "teleport here requests");
        wording.put(Setting.ALLOW_PAYMENTS, "payments");

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("guarded-commands");
        if (sec == null) return;
        load(sec, "private-message", Setting.PRIVATE_MESSAGES);
        load(sec, "teleport-request", Setting.TELEPORT_REQUESTS);
        load(sec, "teleport-here-request", Setting.TELEPORT_HERE_REQUESTS);
        load(sec, "payment", Setting.ALLOW_PAYMENTS);
    }

    private void load(ConfigurationSection sec, String key, Setting setting) {
        for (String c : sec.getStringList(key)) commands.put(c.toLowerCase(Locale.ROOT), setting);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (plugin.getConfig().getBoolean("guard.staff-bypass", true) && p.hasPermission("jalebisettings.bypass")) return;
        String raw = e.getMessage();
        if (raw.length() < 2) return;
        String[] parts = raw.substring(1).trim().split("\\s+");
        if (parts.length < 2) return;

        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);

        Setting s = commands.get(label);
        if (s == null) return;
        Player target = Bukkit.getPlayer(parts[1]);
        if (target == null || target.equals(p)) return;

        if (!plugin.data().allows(target.getUniqueId(), p.getUniqueId(), s)) {
            e.setCancelled(true);
            Msg.send(p, "<red><name> is not accepting " + wording.get(s) + " from you.",
                    Placeholder.unparsed("name", target.getName()));
        }
    }
}
