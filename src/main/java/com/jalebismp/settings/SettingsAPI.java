package com.jalebismp.settings;

import com.jalebismp.settings.data.Setting;
import com.jalebismp.settings.data.Value;

import java.util.UUID;

/** Use this from your other plugins (pay, bounty, auction, ...) to respect player settings. */
public final class SettingsAPI {
    private SettingsAPI() { }

    public static Value get(UUID player, Setting s) {
        return JalebiSettings.get().data().get(player, s);
    }

    /** Toggle settings: true if ON. Tri-state settings: true if not OFF. */
    public static boolean isOn(UUID player, Setting s) {
        return JalebiSettings.get().data().on(player, s);
    }

    /** Should {@code receiver} receive something from {@code sender} under this setting? */
    public static boolean allows(UUID receiver, UUID sender, Setting s) {
        return JalebiSettings.get().data().allows(receiver, sender, s);
    }

    public static boolean isFollowing(UUID a, UUID b) { return JalebiSettings.get().data().isFollowing(a, b); }
    public static boolean isFriend(UUID a, UUID b) { return JalebiSettings.get().data().isFriend(a, b); }
}
