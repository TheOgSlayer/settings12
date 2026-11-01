package com.jalebismp.settings.data;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum Value {
    ON("On", NamedTextColor.GREEN, Material.LIME_DYE),
    OFF("Off", NamedTextColor.RED, Material.RED_DYE),
    ANYONE("Anyone", NamedTextColor.GREEN, Material.LIME_DYE),
    FRIENDS("Friends/Followed", NamedTextColor.YELLOW, Material.YELLOW_DYE);

    private final String display;
    private final NamedTextColor color;
    private final Material dye;

    Value(String display, NamedTextColor color, Material dye) {
        this.display = display;
        this.color = color;
        this.dye = dye;
    }

    public String display() { return display; }
    public NamedTextColor color() { return color; }
    public Material dye() { return dye; }
}
