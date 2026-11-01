package com.jalebismp.settings.data;

import org.bukkit.Material;

public enum Category {
    CHAT("Chat", Material.OAK_SIGN, 11),
    NOTIFICATIONS("Notifications", Material.BELL, 15),
    PVP("PvP", Material.DIAMOND_SWORD, 20),
    VISUALS("Visuals", Material.BLAZE_ROD, 24),
    PRIVACY("Privacy", Material.IRON_DOOR, 29),
    GENERAL("General", Material.NETHER_STAR, 33);

    private final String display;
    private final Material icon;
    private final int slot;

    Category(String display, Material icon, int slot) {
        this.display = display;
        this.icon = icon;
        this.slot = slot;
    }

    public String display() { return display; }
    public Material icon() { return icon; }
    public int slot() { return slot; }

    public static Category bySlot(int slot) {
        for (Category c : values()) if (c.slot == slot) return c;
        return null;
    }
}
