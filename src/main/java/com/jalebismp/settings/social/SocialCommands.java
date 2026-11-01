package com.jalebismp.settings.social;

import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.Msg;
import com.jalebismp.settings.gui.SettingsDialogs.Tab;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SocialCommands implements TabExecutor {
    private final JalebiSettings plugin;

    public SocialCommands(JalebiSettings plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        switch (cmd.getName().toLowerCase()) {
            case "friends" -> plugin.dialogs().openSocial(p, Tab.FRIENDS, 0);
            case "followers" -> plugin.dialogs().openSocial(p, Tab.FOLLOWERS, 0);
            case "following" -> plugin.dialogs().openSocial(p, Tab.FOLLOWING, 0);
            case "follow", "unfollow" -> {
                boolean follow = cmd.getName().equalsIgnoreCase("follow");
                if (args.length < 1) {
                    Msg.send(p, "<red>Usage: /" + cmd.getName().toLowerCase() + " <player>");
                    return true;
                }
                UUID target = resolve(args[0]);
                if (target == null) {
                    Msg.send(p, "<red>Player not found. They must have joined the server before.");
                    return true;
                }
                if (follow) plugin.social().follow(p, target);
                else plugin.social().unfollow(p, target);
            }
            default -> { }
        }
        return true;
    }

    private UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        return plugin.data().findByName(name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p) || args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        Collection<String> pool;
        if (cmd.getName().equalsIgnoreCase("unfollow")) {
            pool = plugin.data().following(p.getUniqueId()).stream().map(plugin.data()::nameOf).toList();
        } else if (cmd.getName().equalsIgnoreCase("follow")) {
            pool = plugin.data().knownNames();
        } else {
            return List.of();
        }
        return pool.stream()
                .filter(n -> n.toLowerCase().startsWith(prefix) && !n.equalsIgnoreCase(p.getName()))
                .sorted().limit(30).collect(Collectors.toList());
    }
}
