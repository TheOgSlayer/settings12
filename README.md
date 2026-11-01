# JalebiSettings

Settings GUI + follow/friends system for **Jalebi SMP** (Paper **1.21.6+**, Java 21 - uses the vanilla Dialog screens).

## Commands
| Command | What it does |
|---|---|
| `/settings` | Opens the settings menu |
| `/follow <player>` / `/unfollow <player>` | Follow / unfollow |
| `/friends`, `/followers`, `/following` | Open the 3-tab GUI on that tab |

**Friend** = you follow them *and* they follow you back.
**"Friends/Followed"** in a setting = players *you* follow (friends are included).

## Build
GitHub Actions builds automatically on every push (Actions -> latest run -> Artifacts).
Tag `v1.0.0` to get a GitHub Release with the jar. Local: `mvn package`.

## Optional dependency
ProtocolLib is needed for: Server Hotbar Messages, Totem Particles, Explosion Particles, Explosion Sounds.

## Hooking your other plugins (pay, bounty, auction, ...)
```java
import com.jalebismp.settings.SettingsAPI;
import com.jalebismp.settings.data.Setting;

if (SettingsAPI.isOn(player.getUniqueId(), Setting.PAY_ALERTS)) { /* send alert */ }
if (SettingsAPI.allows(receiverUuid, senderUuid, Setting.ALLOW_PAYMENTS)) { /* ok */ }
```

## v1.1.0
- **Alerts now work** (needs ProtocolLib): Pay, Teleport, Bounty, Order, Auction and Shard alerts are hidden for players who turn them OFF. Matching is by message text - patterns are in `config.yml -> alerts` and can be edited without recompiling.
- **Teleport Confirm Menu** now works: OFF = `/tpa` and `/tpahere` send the request immediately (JalebiTPA must be installed).
- **Menu icons**: item sprites in front of names in `/settings` (`config.yml -> icons`, needs 1.21.9+).

## v1.2.0
- **Item Worth Lore** now hides/shows SellWorth's "Worth: $x" lore line per player (needs ProtocolLib + SellWorth).
- **Combat Alerts** (Notifications): hides Jalebi Combat's chat messages. **Combat Timer** (Visuals): hides/shows the action-bar countdown. The old built-in boss bar was removed.
- **Explosion Sounds** and **Server Sound** now filter at packet level (`config.yml -> server-sounds`, `sounds-debug`).
- **Randomised Coords** removed - F3 coordinates are computed by the client and can't be faked from a plugin.
- **Mob Spawns** is now a spawn blocker: OFF stops hostile mobs spawning inside your render distance (default ON).
