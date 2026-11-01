package com.jalebismp.settings.feature;

import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.data.Setting;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;

/** Public chat, broadcasts, death / advancement / join / leave message filtering. */
public class ChatListener implements Listener {
    private final JalebiSettings plugin;

    public ChatListener(JalebiSettings plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent e) {
        Player sender = e.getPlayer();
        e.viewers().removeIf(a -> a instanceof Player p
                && !p.equals(sender)
                && !plugin.data().on(p.getUniqueId(), Setting.PUBLIC_CHAT));
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onBroadcast(BroadcastMessageEvent e) {
        e.getRecipients().removeIf(r -> r instanceof Player p
                && !plugin.data().on(p.getUniqueId(), Setting.SERVER_CHAT_MESSAGES));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Component msg = e.deathMessage();
        if (msg == null) return;
        e.deathMessage(null);
        sendFiltered(e.getPlayer(), msg, Setting.DEATH_MESSAGES);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        Component msg = e.message();
        if (msg == null) return;
        e.message(null);
        sendFiltered(e.getPlayer(), msg, Setting.ADVANCEMENT_MESSAGES);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        plugin.data().remember(e.getPlayer());
        Component msg = e.joinMessage();
        if (msg == null) return;
        e.joinMessage(null);
        sendFiltered(e.getPlayer(), msg, Setting.JOIN_LEAVE_MESSAGES);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        Component msg = e.quitMessage();
        if (msg == null) return;
        e.quitMessage(null);
        sendFiltered(e.getPlayer(), msg, Setting.JOIN_LEAVE_MESSAGES);
    }

    /** Viewer sees it if the setting is Anyone, or Friends/Followed and the viewer follows the subject. */
    private void sendFiltered(Player subject, Component msg, Setting setting) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (plugin.data().allows(viewer.getUniqueId(), subject.getUniqueId(), setting)) viewer.sendMessage(msg);
        }
        Bukkit.getConsoleSender().sendMessage(msg);
    }
}
