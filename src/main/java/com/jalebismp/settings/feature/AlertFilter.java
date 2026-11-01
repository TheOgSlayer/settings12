package com.jalebismp.settings.feature;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.data.Setting;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Drops alert messages sent by the other Jalebi plugins when the receiving player has the matching
 * alert setting turned OFF (Pay / Teleport / Bounty / Auction / Order / Shard alerts).
 *
 * The other plugins just call player.sendMessage(...), so the messages are recognised by their text.
 * The patterns live in config.yml under "alerts" and can be edited without recompiling.
 */
public class AlertFilter {
    private record Rule(Setting setting, Pattern pattern) {}

    private static final String SMALL_CAPS = "\u1D00\u0299\u1D04\u1D05\u1D07\uA730\u0262\u029C\u026A\u1D0A\u1D0B\u029F\u1D0D\u0274\u1D0F\u1D18\u01EB\u0280\uA731\u1D1B\u1D1C\u1D20\u1D21x\u028F\u1D22";

    private final JalebiSettings plugin;
    private final List<Rule> rules = new ArrayList<>();
    /** player -> time until which blank chat lines are dropped (the spacer lines around a muted request). */
    private final Map<UUID, Long> swallowBlank = new ConcurrentHashMap<>();

    public AlertFilter(JalebiSettings plugin) {
        this.plugin = plugin;
        // order matters: first matching rule wins
        load("alerts.pay", Setting.PAY_ALERTS);
        load("alerts.teleport", Setting.TELEPORT_ALERTS);
        load("alerts.bounty", Setting.BOUNTY_ALERTS);
        load("alerts.order", Setting.ORDER_ALERTS);
        load("alerts.auction", Setting.AUCTION_ALERTS);
        load("alerts.shard", Setting.SHARD_REWARD_ALERTS);
        load("alerts.combat", Setting.COMBAT_ALERTS);
        load("alerts.combat-timer", Setting.COMBAT_TIMER); // action bar "Combat: 12s"

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Server.SYSTEM_CHAT,
                PacketType.Play.Server.SET_ACTION_BAR_TEXT) {
            @Override
            public void onPacketSending(PacketEvent e) {
                handle(e);
            }
        });
        plugin.getLogger().info("Alert filters enabled (" + rules.size() + " patterns).");
    }

    private void load(String path, Setting setting) {
        for (String regex : plugin.getConfig().getStringList(path)) {
            try {
                rules.add(new Rule(setting, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
            } catch (PatternSyntaxException ex) {
                plugin.getLogger().warning("Bad regex in config '" + path + "': " + regex);
            }
        }
    }

    private void handle(PacketEvent e) {
        if (e.isPlayerTemporary()) return;
        Player p = e.getPlayer();
        if (p == null) return;
        try {
            String text = normalize(plain(e.getPacket()));
            if (text == null) return;
            UUID id = p.getUniqueId();
            if (plugin.getConfig().getBoolean("alerts-debug", false) && !text.isBlank())
                plugin.getLogger().info("[alerts-debug] to " + p.getName() + ": " + text);

            if (text.isBlank()) {
                Long until = swallowBlank.get(id);
                if (until != null) {
                    if (System.currentTimeMillis() <= until) e.setCancelled(true);
                    else swallowBlank.remove(id);
                }
                return;
            }

            for (Rule r : rules) {
                if (!r.pattern().matcher(text).find()) continue;
                if (!plugin.data().on(id, r.setting())) {
                    e.setCancelled(true);
                    // request messages are wrapped in empty spacer lines - drop those too
                    if (r.setting() == Setting.TELEPORT_ALERTS) swallowBlank.put(id, System.currentTimeMillis() + 150L);
                }
                return; // matched a rule: stop here either way
            }
        } catch (Throwable ignored) {
            // unknown packet layout on this server version - leave the message alone
        }
    }

    /** True when the packet is the Combat plugin's action-bar countdown. */
    static boolean isCombatTimer(PacketContainer packet) {
        try {
            String t = normalize(plain(packet));
            return t != null && t.matches("^\\W*combat:\\s*\\d+.*");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Plain text of a SYSTEM_CHAT / SET_ACTION_BAR_TEXT packet. */
    private static String plain(PacketContainer packet) {
        try {
            WrappedChatComponent w = packet.getChatComponents().readSafely(0);
            if (w != null) return fromJson(w.getJson());
        } catch (Throwable ignored) { }
        try {
            String s = packet.getStrings().readSafely(0);
            if (s != null) return fromJson(s);
        } catch (Throwable ignored) { }
        return null;
    }

    private static String fromJson(String json) {
        try {
            return PlainTextComponentSerializer.plainText().serialize(GsonComponentSerializer.gson().deserialize(json));
        } catch (Throwable t) {
            return json; // last resort: match against the raw json
        }
    }

    /** Small-caps -> ascii, lowercase, single spaces. */
    private static String normalize(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        boolean space = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int idx = SMALL_CAPS.indexOf(c);
            if (idx >= 0) c = (char) ('a' + idx);
            if (Character.isWhitespace(c)) {
                if (!space && sb.length() > 0) sb.append(' ');
                space = true;
            } else {
                sb.append(Character.toLowerCase(c));
                space = false;
            }
        }
        return sb.toString().trim();
    }
}
