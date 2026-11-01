package com.jalebismp.settings;

import com.jalebismp.settings.data.DataManager;
import com.jalebismp.settings.data.Setting;
import com.jalebismp.settings.data.Value;
import com.jalebismp.settings.feature.*;
import com.jalebismp.settings.gui.SettingsDialogs;
import com.jalebismp.settings.social.SocialCommands;
import com.jalebismp.settings.social.SocialService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class JalebiSettings extends JavaPlugin {
    private static JalebiSettings instance;
    private DataManager data;
    private SocialService social;
    private GeneralTask general;
    private SettingsDialogs dialogs;

    public static JalebiSettings get() { return instance; }
    public DataManager data() { return data; }
    public SocialService social() { return social; }
    public SettingsDialogs dialogs() { return dialogs; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        data = new DataManager(this);
        data.load();
        social = new SocialService(this);
        dialogs = new SettingsDialogs(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new CommandGuardListener(this), this);
        pm.registerEvents(new PvPListener(this), this);
        pm.registerEvents(new TeleportMenuListener(this), this);

        general = new GeneralTask(this);
        pm.registerEvents(general, this);
        long interval = Math.max(20L, getConfig().getLong("general.task-interval-ticks", 100L));
        getServer().getScheduler().runTaskTimer(this, general, 100L, interval);

        getCommand("settings").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player p) dialogs.openMain(p);
            else sender.sendMessage("Players only.");
            return true;
        });
        SocialCommands sc = new SocialCommands(this);
        for (String name : new String[]{"follow", "unfollow", "friends", "followers", "following"}) {
            getCommand(name).setExecutor(sc);
            getCommand(name).setTabCompleter(sc);
        }

        if (pm.isPluginEnabled("ProtocolLib")) {
            try {
                new PacketHooks(this);
                new AlertFilter(this);
                new SoundFilter(this);
                if (pm.getPlugin("SellWorth") != null) new WorthLoreFilter(this);
                else getLogger().info("SellWorth not found: Item Worth Lore setting has nothing to hide.");
            } catch (Throwable t) {
                getLogger().warning("ProtocolLib hooks failed: " + t.getMessage());
            }
        } else {
            getLogger().info("ProtocolLib not found: hotbar / totem / explosion / alert settings will not take effect.");
        }

        for (Player p : Bukkit.getOnlinePlayers()) data.remember(p);
        getServer().getScheduler().runTaskTimerAsynchronously(this, data::save, 6000L, 6000L);
    }

    /** Menu icon (emoji / symbol) for a setting, from config.yml -> icons.settings. Empty if none. */
    public String glyph(Setting s) {
        return getConfig().getString("icons.settings." + s.name().toLowerCase(java.util.Locale.ROOT), "");
    }

    /** Menu icon for a category, from config.yml -> icons.categories. Empty if none. */
    public String glyph(com.jalebismp.settings.data.Category c) {
        return getConfig().getString("icons.categories." + c.name().toLowerCase(java.util.Locale.ROOT), "");
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
    }

    /** Called by the GUI after a player changes a setting (for settings that need an immediate effect). */
    public void onSettingChanged(Player p, Setting s, Value v) {
        // re-send the open inventory so worth lore appears / disappears straight away
        if (s == Setting.ITEM_WORTH_LORE) p.updateInventory();
        if (s == Setting.NIGHT_VISION) general.applyNightVision(p, true);
        // Auction quick buy / sell: run the auction plugin's command as the player
        if (s == Setting.AUCTION_QUICK_BUY) p.performCommand(getConfig().getString("commands.quick-buy", "ah fastbuy"));
        if (s == Setting.AUCTION_QUICK_SELL) p.performCommand(getConfig().getString("commands.quick-sell", "ah fastsell"));
    }
}
