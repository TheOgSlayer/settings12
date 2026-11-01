package com.jalebismp.settings.social;

import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.Msg;
import com.jalebismp.settings.data.DataManager;
import com.jalebismp.settings.data.Setting;
import com.jalebismp.settings.data.Value;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SocialService {
    private final JalebiSettings plugin;

    public SocialService(JalebiSettings plugin) { this.plugin = plugin; }

    public void follow(Player me, UUID target) {
        DataManager d = plugin.data();
        UUID id = me.getUniqueId();
        String targetName = d.nameOf(target);
        if (id.equals(target)) {
            Msg.send(me, "<red>You can't follow yourself.");
            return;
        }
        if (d.isFollowing(id, target)) {
            Msg.send(me, "<red>You are already following <white><name><red>.", Placeholder.unparsed("name", targetName));
            return;
        }
        d.follow(id, target);
        boolean mutual = d.isFollowing(target, id);
        Msg.send(me, "<green>You are now following <white><name><green>." + (mutual ? " You are now <yellow>friends<green>!" : ""),
                Placeholder.unparsed("name", targetName));

        Player t = Bukkit.getPlayer(target);
        if (t != null && d.get(target, Setting.FOLLOW_ALERTS) != Value.OFF) {
            Msg.send(t, "<white><name> <green>started following you."
                            + (mutual ? " You are now <yellow>friends<green>!" : " <gray>Use <white>/follow <name> <gray>to follow back."),
                    Placeholder.unparsed("name", me.getName()));
        }
    }

    public void unfollow(Player me, UUID target) {
        DataManager d = plugin.data();
        String name = d.nameOf(target);
        if (!d.isFollowing(me.getUniqueId(), target)) {
            Msg.send(me, "<red>You are not following <white><name><red>.", Placeholder.unparsed("name", name));
            return;
        }
        d.unfollow(me.getUniqueId(), target);
        Msg.send(me, "<yellow>You unfollowed <white><name><yellow>.", Placeholder.unparsed("name", name));
    }
}
