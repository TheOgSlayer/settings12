package com.jalebismp.settings.feature;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.data.DataManager;
import com.jalebismp.settings.data.Setting;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * ProtocolLib-based filters: Server Hotbar Messages, Totem Particles,
 * Explosion Particles.
 */
public class PacketHooks {
    private static final Set<String> TOTEM = Set.of("TOTEM_OF_UNDYING", "TOTEM");
    private static final Set<String> EXPLOSION = Set.of("EXPLOSION", "EXPLOSION_EMITTER", "EXPLOSION_LARGE", "EXPLOSION_HUGE");

    public PacketHooks(JalebiSettings plugin) {
        final JalebiSettings jp = plugin;
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGH,
                PacketType.Play.Server.SET_ACTION_BAR_TEXT,
                PacketType.Play.Server.ENTITY_STATUS,
                PacketType.Play.Server.WORLD_PARTICLES) {

            @Override
            public void onPacketSending(PacketEvent e) {
                if (e.isPlayerTemporary()) return;
                Player p = e.getPlayer();
                if (p == null) return;
                UUID id = p.getUniqueId();
                DataManager d = jp.data();
                PacketType t = e.getPacketType();
                try {
                    if (t == PacketType.Play.Server.SET_ACTION_BAR_TEXT) {
                        // the combat timer is controlled by its own setting (see AlertFilter)
                        if (!d.on(id, Setting.SERVER_HOTBAR_MESSAGES) && !AlertFilter.isCombatTimer(e.getPacket())) e.setCancelled(true);
                    } else if (t == PacketType.Play.Server.ENTITY_STATUS) {
                        // status 35 = totem of undying pop (animation + particles are client-side)
                        if (e.getPacket().getBytes().read(0) == 35 && !d.on(id, Setting.TOTEM_PARTICLES)) e.setCancelled(true);
                    } else if (t == PacketType.Play.Server.WORLD_PARTICLES) {
                        Particle particle = e.getPacket().getNewParticles().read(0).getParticle();
                        String name = String.valueOf(particle);
                        if (TOTEM.contains(name) && !d.on(id, Setting.TOTEM_PARTICLES)) e.setCancelled(true);
                        else if (EXPLOSION.contains(name) && !d.on(id, Setting.EXPLOSION_PARTICLES)) e.setCancelled(true);
                    }
                } catch (Throwable ignored) {
                    // packet layout differs on this server version - leave the packet alone
                }
            }
        });
        plugin.getLogger().info("ProtocolLib found: hotbar / totem / explosion filters enabled.");
    }
}
