package com.jalebismp.settings.data;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public enum Setting {
    // ---- Chat ----
    PUBLIC_CHAT(Category.CHAT, "Public Chat", Material.WRITABLE_BOOK, Type.TOGGLE, Value.ON, "See public chat messages."),
    PRIVATE_MESSAGES(Category.CHAT, "Private Messages", Material.PAPER, Type.TRI, Value.ANYONE, "Who can send you private messages."),
    SERVER_CHAT_MESSAGES(Category.CHAT, "Server Chat Messages", Material.BOOK, Type.TOGGLE, Value.ON, "See server broadcast messages."),
    SERVER_HOTBAR_MESSAGES(Category.CHAT, "Server Hotbar Messages", Material.NAME_TAG, Type.TOGGLE, Value.ON, "See action bar messages from the server."),
    DEATH_MESSAGES(Category.CHAT, "Death Messages", Material.SKELETON_SKULL, Type.TRI, Value.ANYONE, "Whose death messages you see."),
    ADVANCEMENT_MESSAGES(Category.CHAT, "Advancement Messages", Material.KNOWLEDGE_BOOK, Type.TRI, Value.ANYONE, "Whose advancement messages you see."),
    JOIN_LEAVE_MESSAGES(Category.CHAT, "Join/Leave Messages", Material.OAK_DOOR, Type.TRI, Value.ANYONE, "Whose join/leave messages you see."),

    // ---- Notifications ----
    PAY_ALERTS(Category.NOTIFICATIONS, "Pay Alerts", Material.GOLD_INGOT, Type.TOGGLE, Value.ON, "Get notified when you receive a payment."),
    TELEPORT_ALERTS(Category.NOTIFICATIONS, "Teleport Alerts", Material.ENDER_PEARL, Type.TOGGLE, Value.ON, "Get notified about teleport requests."),
    BOUNTY_ALERTS(Category.NOTIFICATIONS, "Bounty Alerts", Material.TARGET, Type.TOGGLE, Value.ON, "Get notified about bounties."),
    COMBAT_ALERTS(Category.NOTIFICATIONS, "Combat Alerts", Material.IRON_SWORD, Type.TOGGLE, Value.ON, "Show combat messages in chat."),
    AUCTION_ALERTS(Category.NOTIFICATIONS, "Auction Alerts", Material.EMERALD, Type.TOGGLE, Value.ON, "Get notified about auction activity."),
    ORDER_ALERTS(Category.NOTIFICATIONS, "Order Alerts", Material.CHEST, Type.TOGGLE, Value.ON, "Get notified about order activity."),
    SHARD_REWARD_ALERTS(Category.NOTIFICATIONS, "Shard Reward Alerts", Material.AMETHYST_SHARD, Type.TOGGLE, Value.ON, "Get notified about shard rewards."),
    SERVER_SOUND(Category.NOTIFICATIONS, "Server Sound", Material.NOTE_BLOCK, Type.TOGGLE, Value.ON, "Play sounds for server notifications."),
    FOLLOW_ALERTS(Category.NOTIFICATIONS, "Follow Alerts", Material.PLAYER_HEAD, Type.TOGGLE, Value.ON, "Get notified when someone follows you."),

    // ---- PvP ----
    FAST_CRYSTALS(Category.PVP, "Fast Crystals", Material.END_CRYSTAL, Type.TOGGLE, Value.OFF, "End crystals explode instantly when hit."),
    TOTEM_PARTICLES(Category.PVP, "Totem Particles", Material.TOTEM_OF_UNDYING, Type.TOGGLE, Value.ON, "Show totem pop particles."),
    EXPLOSION_PARTICLES(Category.PVP, "Explosion Particles", Material.GUNPOWDER, Type.TOGGLE, Value.ON, "Show explosion particles."),
    EXPLOSION_SOUNDS(Category.PVP, "Explosion Sounds", Material.TNT, Type.TOGGLE, Value.ON, "Hear explosion sounds."),

    // ---- Visuals ----
    ITEM_WORTH_LORE(Category.VISUALS, "Item Worth Lore", Material.GOLD_NUGGET, Type.TOGGLE, Value.ON, "Show item worth in item lore."),
    TELEPORT_CONFIRM_MENU(Category.VISUALS, "Teleport Confirm Menu", Material.ENDER_EYE, Type.TOGGLE, Value.ON, "Ask for confirmation before teleporting."),
    COMBAT_TIMER(Category.VISUALS, "Combat Timer", Material.CLOCK, Type.TOGGLE, Value.ON, "Show a combat countdown bar."),

    // ---- Privacy ----
    TELEPORT_REQUESTS(Category.PRIVACY, "Teleport Requests", Material.COMPASS, Type.TRI, Value.ANYONE, "Who can send you /tpa requests."),
    TELEPORT_HERE_REQUESTS(Category.PRIVACY, "Teleport Here Requests", Material.MAP, Type.TRI, Value.ANYONE, "Who can send you /tpahere requests."),
    ALLOW_PAYMENTS(Category.PRIVACY, "Allow Payments", Material.GOLD_INGOT, Type.TRI, Value.ANYONE, "Who can pay you."),

    // ---- General ----
    AUCTION_QUICK_BUY(Category.GENERAL, "Auction Quick Buy", Material.EMERALD, Type.TOGGLE, Value.OFF, "Buy auction items without a confirm screen."),
    AUCTION_QUICK_SELL(Category.GENERAL, "Auction Quick Sell", Material.GOLD_INGOT, Type.TOGGLE, Value.OFF, "Sell items to auction without a confirm screen."),
    MOB_SPAWNS(Category.GENERAL, "Mob Spawns", Material.ZOMBIE_HEAD, Type.TOGGLE, Value.ON, "OFF stops hostile mobs spawning in your render distance."),
    PHANTOM_SPAWNING(Category.GENERAL, "Phantom Spawning", Material.PHANTOM_MEMBRANE, Type.TOGGLE, Value.OFF, "Spawns phantoms around you, slept or not."),
    NIGHT_VISION(Category.GENERAL, "Night Vision", Material.GOLDEN_CARROT, Type.TOGGLE, Value.OFF, "Infinite night vision.");

    public enum Type { TOGGLE, TRI }

    private final Category category;
    private final String display;
    private final Material icon;
    private final Type type;
    private final Value def;
    private final String description;

    Setting(Category category, String display, Material icon, Type type, Value def, String description) {
        this.category = category;
        this.display = display;
        this.icon = icon;
        this.type = type;
        this.def = def;
        this.description = description;
    }

    public Category category() { return category; }
    public String display() { return display; }
    public Material icon() { return icon; }
    public Type type() { return type; }
    public Value def() { return def; }
    public String description() { return description; }

    public List<Value> options() {
        return type == Type.TOGGLE ? List.of(Value.ON, Value.OFF) : List.of(Value.ANYONE, Value.FRIENDS, Value.OFF);
    }

    public Value cycle(Value current, boolean forward) {
        List<Value> o = options();
        int i = o.indexOf(current);
        if (i < 0) return o.get(0);
        int n = o.size();
        return o.get((i + (forward ? 1 : n - 1)) % n);
    }

    public static List<Setting> in(Category c) {
        List<Setting> out = new ArrayList<>();
        for (Setting s : values()) if (s.category == c) out.add(s);
        return out;
    }
}
