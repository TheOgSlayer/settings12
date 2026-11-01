package com.jalebismp.settings.feature;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketPostListener;
import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.data.Setting;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Explosion Sounds and Server Sound.
 *
 * Explosion Sounds OFF: explosion sounds sent as separate sound packets are dropped, and on versions where the
 * sound travels inside the explosion packet a "stop sound" is sent right after it.
 *
 * Server Sound OFF: sounds whose key matches a pattern in config.yml -> server-sounds are dropped.
 * (Natural game sounds can't be told apart from plugin sounds, so only the listed sounds are affected.)
 */
public class SoundFilter {
    private final JalebiSettings plugin;
    private final List<Pattern> serverSounds = new ArrayList<>();

    public SoundFilter(JalebiSettings plugin) {
        this.plugin = plugin;
        for (String regex : plugin.getConfig().getStringList("server-sounds")) {
            try {
                serverSounds.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException ex) {
                plugin.getLogger().warning("Bad regex in config 'server-sounds': " + regex);
            }
        }
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Server.NAMED_SOUND_EFFECT,
                PacketType.Play.Server.ENTITY_SOUND,
                PacketType.Play.Server.EXPLOSION) {
            @Override
            public void onPacketSending(PacketEvent e) {
                handle(e);
            }
        });
        plugin.getLogger().info("Sound filters enabled (" + serverSounds.size() + " server-sound patterns).");
    }

    private void handle(PacketEvent e) {
        if (e.isPlayerTemporary()) return;
        final Player p = e.getPlayer();
        if (p == null) return;
        UUID id = p.getUniqueId();
        try {
            if (e.getPacketType() == PacketType.Play.Server.EXPLOSION) {
                if (plugin.data().on(id, Setting.EXPLOSION_SOUNDS)) return;
                // the explosion packet itself still has to arrive (knockback, blocks, particles);
                // silence its sound right after it has been sent
                if (e.getNetworkMarker() == null) return;
                e.getNetworkMarker().addPostListener(new PacketPostListener() {
                    @Override
                    public Plugin getPlugin() {
                        return plugin;
                    }

                    @Override
                    public void onPostEvent(PacketEvent event) {
                        try {
                            p.stopSound("minecraft:entity.generic.explode");
                        } catch (Throwable ignored) { }
                    }
                });
                return;
            }

            Sound sound = e.getPacket().getSoundEffects().readSafely(0);
            if (sound == null) return;
            String key = sound.key().asString();
            if (plugin.getConfig().getBoolean("sounds-debug", false))
                plugin.getLogger().info("[sounds-debug] to " + p.getName() + ": " + key);

            if (key.contains("explode")) {
                if (!plugin.data().on(id, Setting.EXPLOSION_SOUNDS)) e.setCancelled(true);
                return;
            }
            if (!plugin.data().on(id, Setting.SERVER_SOUND)) {
                for (Pattern pat : serverSounds) {
                    if (pat.matcher(key).find()) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
            // unknown packet layout on this server version - leave the sound alone
        }
    }
}
