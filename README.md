# JujuCore

My all in one plugin for our Paper server. Basically the Essentials stuff we actually use plus vanish, chest locking, some villager helpers and a bunch of trolls.

Made for Paper 26.3 (Java 25). In game messages are in German since that's what we play in.

## Build

```bash
./gradlew build
```

Jar ends up in `build/libs/JujuCore-1.0-SNAPSHOT.jar`, drop it in `plugins/`.

## Commands

The usual stuff: `/gm` (+ `/gmc /gms /gma /gmsp`), `/fly`, `/heal`, `/feed`, `/repair`, `/speed`, `/top`, `/god`, `/freeze`, `/hat`, `/workbench`, `/trash`, `/ec`, `/invsee`, `/near`, `/seen`, `/playtime`, `/check`, `/day`, `/night`, `/weather`, `/broadcast`, `/clearchat`, `/clearinventory`, `/suicide`

Teleport: `/spawn`, `/setspawn`, `/home`, `/sethome`, `/delhome`, `/back`, `/tp`, `/tphere`, `/tpa`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/warp`, `/warps`, `/setwarp`, `/delwarp`

Chat / staff: `/msg`, `/r`, `/socialspy`, `/staffchat`, `/report`, `/reports`

`/tp <player> [target]` is handled by the plugin, everything else (`/tp x y z`, `~ ~ ~`, selectors) just goes to vanilla `/minecraft:tp`.

## Kits

Kits are saved from your current inventory.

```text
/createkit starter 86400    (cooldown in seconds)
/kit starter
/kits
/delkit starter
```

Stored in `plugins/JujuCore/kits.yml`, cooldowns in `data.yml`.

```text
jujucore.kit.<name>           use one kit
jujucore.kit                  use all kits
jujucore.kit.create           create kits
jujucore.kit.delete           delete kits
jujucore.kit.cooldown.bypass  ignore cooldowns
```

## Vanish

`/v` hides you from the world, tab list, locator bar, server list and `/list`. It stays on after relogs and restarts.

While vanished there are no join/quit/death/advancement messages, mobs ignore you, you don't pick up items, don't trigger pressure plates or sculk sensors, and chests open silently (read only though). Public chat needs a `!` in front so you don't leak by accident.

## Chest locking (/kiste)

Chests, barrels and shulker boxes are open for everyone until whoever placed them does `/kiste sichern`. You can only lock chests you placed yourself (both halves for double chests), so nobody can lock someone else out. Old chests from before placers were saved can only be locked by an admin.

Locked chests can only be opened by the owner and their friends. Other people can't break them, blow them up, attach their own chest to them or pull items out with hoppers.

```text
/kiste info             who owns it, who has access
/kiste add <name>       give access
/kiste remove <name>    take access away
/kiste sichern          lock a chest you placed
/kiste sichern <name>   (admin) lock it for someone else
/kiste frei             unlock it
/kiste log [page]       who opened it and what they took/put in
```

`jujucore.kiste.bypass` (op) can open, break and check every chest. The log is in `plugins/JujuCore/kisten-log.txt` and rotates at 5 MB.

## Villager

```text
/villager book <enchantment> [level]
/villager clearhate [radius]
```

`/villager book` changes the book trade of the librarian you're looking at (max 6 blocks), e.g. `/villager book mending` or `/villager book sharpness 5`. No level = max level. If it has no book trade yet it gets a new one. Job and trades stay.

`/villager clearhate` makes villagers in the radius (default 64) forget all gossip about players so prices go back to normal.

Permission: `jujucore.villager` (op by default)

## Trolls

```text
/troll <player> <jumpscare|fakeop|fakedeop|creeper|launch|slap|spin|blind|fakeban|herobrine|ghost|fakelag|hiccup|nod|shuffle|diamonds|tnt|fakecrash|random>
```

The target never gets told it was a troll, it's supposed to look like lag, a bug or a real server message. `fakeban` and `fakecrash` kick with the real vanilla ban / connection reset screen, `random` never picks those two.

Permission: `jujucore.troll`

## Hidden plugin list

Non ops can't see `/plugins`, `/pl`, `/version`, `/about`, `/paper` etc. and namespaced commands are removed from tab complete. They just get the vanilla unknown command error. Bypass: `jujucore.plugins.bypass`. The list is in `config.yml` under `blocked-plugin-commands`.
