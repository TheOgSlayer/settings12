package com.jalebismp.settings.gui;

import com.jalebismp.settings.JalebiSettings;
import com.jalebismp.settings.Msg;
import com.jalebismp.settings.data.Category;
import com.jalebismp.settings.data.DataManager;
import com.jalebismp.settings.data.Setting;
import com.jalebismp.settings.data.Value;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/** All GUIs are vanilla Dialog screens (Paper Dialog API, 1.21.6+). */
public class SettingsDialogs {

    public enum Tab {
        FRIENDS("Friends"), FOLLOWERS("Followers"), FOLLOWING("Following");
        final String display;
        Tab(String display) { this.display = display; }
    }

    private static final int PER_PAGE = 12;

    private final JalebiSettings plugin;

    public SettingsDialogs(JalebiSettings plugin) { this.plugin = plugin; }

    // ------------------------------------------------------------------ helpers

    private ActionButton button(Component label, Component tooltip, int width, Consumer<Player> onClick, Player p) {
        DialogAction action = DialogAction.customClick(
                (view, audience) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (p.isOnline()) onClick.accept(p);
                }),
                ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build());
        ActionButton.Builder b = ActionButton.builder(label).width(width).action(action);
        if (tooltip != null) b.tooltip(tooltip);
        return b.build();
    }

    private void show(Player p, Component title, List<Component> bodyLines,
                      List<ActionButton> buttons, ActionButton exit, int columns) {
        try {
        List<DialogBody> body = new ArrayList<>();
        for (Component c : bodyLines) body.add(DialogBody.plainMessage(c));
        DialogBase base = DialogBase.builder(title)
                .canCloseWithEscape(true)
                .pause(false) // vanilla rejects pause=true together with afterAction NONE
                .afterAction(DialogBase.DialogAfterAction.NONE) // stay open until we show the next dialog
                .body(body)
                .build();
        Dialog dialog = Dialog.create(b -> b.empty()
                    .base(base)
                    .type(DialogType.multiAction(buttons, exit, columns)));
        p.showDialog(dialog);
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not open dialog '" + title + "'", t);
            p.sendMessage(Component.text("Could not open that menu - check the server console.", NamedTextColor.RED));
        }
    }

    /**
     * Icon in front of a name: item sprite built from the config item name (needs 1.21.9+ client and server).
     * If the server's MiniMessage doesn't know the tag, no icon is shown.
     */
    private Component icon(String name) {
        if (name == null || name.isBlank()) return Component.empty();
        try {
            String atlas = plugin.getConfig().getString("icons.atlas", "items");
            String prefix = plugin.getConfig().getString("icons.path-prefix", "item/");
            Component c = Msg.parse("<sprite:" + atlas + ":" + prefix + name.trim() + "> ");
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
            if (plain.contains("<")) return Component.empty(); // unknown tag was printed literally
            return c;
        } catch (Throwable t) {
            return Component.empty();
        }
    }

    // ------------------------------------------------------------------ main menu

    public void openMain(Player p) {
        List<ActionButton> buttons = new ArrayList<>();
        for (Category c : Category.values()) {
            buttons.add(button(icon(plugin.glyph(c)).append(Component.text(c.display())),
                    Component.text("Open " + c.display() + " settings", NamedTextColor.GRAY),
                    150, pl -> openCategory(pl, c), p));
        }
        show(p, Component.text("Settings"),
                List.of(Component.text("Choose a category to change your Jalebi SMP settings", NamedTextColor.GRAY)),
                buttons, null, 2);
    }

    // ------------------------------------------------------------------ category

    public void openCategory(Player p, Category cat) {
        List<ActionButton> buttons = new ArrayList<>();
        for (Setting s : Setting.in(cat)) {
            Value cur = plugin.data().get(p.getUniqueId(), s);
            Component label = Component.text(s.display() + ": ", NamedTextColor.WHITE)
                    .append(Component.text(cur.display(), cur.color()));
            Component tip = Component.text(s.description(), NamedTextColor.GRAY).appendNewline()
                    .append(Component.text(s.options().size() > 2
                            ? "Click to cycle: Anyone > Friends/Followed > Off" : "Click to toggle", NamedTextColor.DARK_GRAY));
            buttons.add(button(label, tip, 280, pl -> {
                Value now = plugin.data().get(pl.getUniqueId(), s);
                Value next = s.cycle(now, true);
                plugin.data().set(pl.getUniqueId(), s, next);
                plugin.onSettingChanged(pl, s, next);
                openCategory(pl, cat);
            }, p));
        }
        ActionButton back = button(Component.text("Back"), Component.text("Return to the main settings menu", NamedTextColor.GRAY),
                280, this::openMain, p);
        show(p, Component.text(cat.display() + " Settings"),
                List.of(Component.text("Click an option to change it.", NamedTextColor.GRAY)),
                buttons, back, 1);
    }

    // ------------------------------------------------------------------ friends / followers / following

    private Set<UUID> listFor(UUID me, Tab t) {
        DataManager d = plugin.data();
        return switch (t) {
            case FRIENDS -> d.friends(me);
            case FOLLOWERS -> d.followers(me);
            case FOLLOWING -> d.following(me);
        };
    }

    public void openSocial(Player p, Tab tab, int page) {
        DataManager d = plugin.data();
        UUID me = p.getUniqueId();
        List<ActionButton> buttons = new ArrayList<>();

        // tabs (first row)
        for (Tab t : Tab.values()) {
            boolean sel = t == tab;
            Component label = Component.text((sel ? "[" + t.display + "]" : t.display) + " (" + listFor(me, t).size() + ")",
                    sel ? NamedTextColor.GREEN : NamedTextColor.WHITE);
            buttons.add(button(label,
                    Component.text(sel ? "Currently viewing" : "Click to view", NamedTextColor.GRAY),
                    110, pl -> openSocial(pl, t, 0), p));
        }

        List<UUID> all = new ArrayList<>(listFor(me, tab));
        all.sort(Comparator.comparing((UUID u) -> Bukkit.getPlayer(u) == null)
                .thenComparing(u -> d.nameOf(u).toLowerCase()));
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        final int pg = Math.min(Math.max(page, 0), pages - 1);

        for (UUID id : all.subList(pg * PER_PAGE, Math.min(all.size(), pg * PER_PAGE + PER_PAGE))) {
            boolean online = Bukkit.getPlayer(id) != null;
            boolean iFollow = d.isFollowing(me, id);
            boolean followsMe = d.isFollowing(id, me);

            Component tip = Component.text(online ? "Online" : "Offline", online ? NamedTextColor.GREEN : NamedTextColor.RED).appendNewline();
            if (iFollow && followsMe) tip = tip.append(Component.text("Friends", NamedTextColor.YELLOW)).appendNewline();
            else if (iFollow) tip = tip.append(Component.text("You follow them", NamedTextColor.GRAY)).appendNewline();
            else if (followsMe) tip = tip.append(Component.text("They follow you", NamedTextColor.GRAY)).appendNewline();
            String hint = tab == Tab.FOLLOWERS
                    ? (iFollow ? "You already follow them" : "Click to follow back")
                    : "Click to unfollow";
            tip = tip.append(Component.text(hint, NamedTextColor.DARK_GRAY));

            buttons.add(button(Component.text(d.nameOf(id), online ? NamedTextColor.GREEN : NamedTextColor.GRAY), tip, 110, pl -> {
                if (tab == Tab.FOLLOWERS) {
                    if (!plugin.data().isFollowing(pl.getUniqueId(), id)) plugin.social().follow(pl, id);
                } else {
                    plugin.social().unfollow(pl, id);
                }
                openSocial(pl, tab, pg);
            }, p));
        }

        if (pg > 0) buttons.add(button(Component.text("<< Previous"), null, 110, pl -> openSocial(pl, tab, pg - 1), p));
        if (pg < pages - 1) buttons.add(button(Component.text("Next >>"), null, 110, pl -> openSocial(pl, tab, pg + 1), p));

        List<Component> body = new ArrayList<>();
        body.add(Component.text(tab.display + " - page " + (pg + 1) + "/" + pages, NamedTextColor.GRAY));
        if (all.isEmpty()) {
            body.add(Component.text(switch (tab) {
                case FRIENDS -> "No friends yet. Follow someone and have them follow you back.";
                case FOLLOWERS -> "Nobody follows you yet.";
                case FOLLOWING -> "You aren't following anyone. Use /follow <player>.";
            }, NamedTextColor.RED));
        } else {
            body.add(Component.text(tab == Tab.FOLLOWERS ? "Click a player to follow them back." : "Click a player to unfollow them.",
                    NamedTextColor.DARK_GRAY));
        }

        ActionButton settings = button(Component.text("Settings"), Component.text("Open the settings menu", NamedTextColor.GRAY),
                150, this::openMain, p);
        show(p, Component.text("Friends & Followers"), body, buttons, settings, 3);
    }
}
