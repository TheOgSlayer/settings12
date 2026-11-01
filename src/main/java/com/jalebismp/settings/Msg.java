package com.jalebismp.settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

public final class Msg {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX = "<gold><bold>Jalebi</bold> <dark_gray>» ";

    private Msg() { }

    public static Component parse(String mm, TagResolver... resolvers) {
        return MM.deserialize(mm, resolvers);
    }

    public static void send(CommandSender to, String mm, TagResolver... resolvers) {
        to.sendMessage(MM.deserialize(PREFIX + mm, resolvers));
    }
}
