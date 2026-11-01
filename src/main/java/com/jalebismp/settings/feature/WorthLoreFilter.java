package com.jalebismp.settings.feature;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.data.Setting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Item Worth Lore: SellWorth adds its "Worth: $x" line to items inside outgoing inventory packets (and marks the
 * item with the sellworth:worth_lore flag). When a player has the setting OFF, this removes that line and the flag
 * again. It runs at HIGHEST so it always comes after SellWorth's own HIGH listener.
 */
public class WorthLoreFilter {
    private final JalebiSettings plugin;
    private final NamespacedKey flag;

    public WorthLoreFilter(JalebiSettings plugin) {
        this.plugin = plugin;
        Plugin sw = plugin.getServer().getPluginManager().getPlugin("SellWorth");
        this.flag = new NamespacedKey(sw, "worth_lore");

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Server.SET_SLOT,
                PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent e) {
                handle(e);
            }
        });
        plugin.getLogger().info("Item Worth Lore filter enabled (SellWorth found).");
    }

    private void handle(PacketEvent e) {
        if (e.isPlayerTemporary()) return;
        Player p = e.getPlayer();
        if (p == null || plugin.data().on(p.getUniqueId(), Setting.ITEM_WORTH_LORE)) return;
        try {
            PacketContainer packet = e.getPacket();
            if (e.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                ItemStack item = packet.getItemModifier().readSafely(0);
                ItemStack clean = strip(item);
                if (clean != null) packet.getItemModifier().write(0, clean);
            } else {
                List<ItemStack> items = packet.getItemListModifier().readSafely(0);
                if (items != null) {
                    List<ItemStack> out = new ArrayList<>(items.size());
                    boolean changed = false;
                    for (ItemStack it : items) {
                        ItemStack clean = strip(it);
                        if (clean != null) changed = true;
                        out.add(clean != null ? clean : it);
                    }
                    if (changed) packet.getItemListModifier().write(0, out);
                }
                ItemStack carried = packet.getItemModifier().readSafely(0);
                ItemStack cleanCarried = strip(carried);
                if (cleanCarried != null) packet.getItemModifier().write(0, cleanCarried);
            }
        } catch (Throwable ignored) {
            // unknown packet layout on this server version - leave the items alone
        }
    }

    /** @return a copy without the worth line, or null if the item had none. */
    private ItemStack strip(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(flag, PersistentDataType.BYTE)) return null;

        ItemStack copy = item.clone();
        ItemMeta m = copy.getItemMeta();
        List<Component> lore = m.lore();
        if (lore != null && !lore.isEmpty()) {
            lore = new ArrayList<>(lore);
            String last = PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1)).trim();
            if (last.toLowerCase().startsWith("worth")) lore.remove(lore.size() - 1);
            m.lore(lore.isEmpty() ? null : lore);
        }
        m.getPersistentDataContainer().remove(flag);
        copy.setItemMeta(m);
        return copy;
    }
}
