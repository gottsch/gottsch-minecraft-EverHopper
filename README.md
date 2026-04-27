# EverHopper

**Hoppers keep transferring items while you're away.**

Built a sorting system or item farm at a remote outpost? Vanilla hoppers freeze the moment their chunk unloads — meaning every item that should have flowed through them while you were gone simply didn't. EverHopper fixes that.

---

## How it works

EverHopper doesn't simulate hopper transfers on unloaded chunks (that would be expensive and laggy). Instead, every hopper records the game time on each tick. When the chunk reloads, EverHopper looks at how much in-game time elapsed while it was unloaded and **catches the hopper up** — pulling items from the container above and pushing items into the container below as if it had been ticking the whole time.

Walk away for an hour, a day, a week — when you come back and the chunks reload, your sorting system, mob farm collectors, and storage chains advance to where they would have been under vanilla rules. Same end result, zero overhead while you were gone.

> **Note:** In singleplayer, the world only ticks while you're playing. EverHopper catches up based on **in-game time elapsed**, not real-world time, so closing the game and coming back tomorrow won't fast-forward your hoppers.

> **Redstone-disabled hoppers** don't accrue catch-up time. A hopper held off by a redstone signal stays paused, just like in vanilla.

---

## Features

- ✅ Catches up both directions — pull from container above, push to container below
- ✅ Respects facing direction (down, north, south, east, west)
- ✅ Respects redstone disable — paused hoppers stay paused
- ✅ Visual cue when you walk back: a brief green sparkle and pickup ping at any hopper that transferred items while you were away
- ✅ Capped per-event simulation prevents lag spikes on chunk load (default cap: one in-game day, configurable)
- ✅ Fully configurable — catch-up cap, delta threshold, particles, and sound can all be tuned or disabled
- ✅ **Server-side optional**: vanilla clients can connect to EverHopper-equipped servers. The catch-up runs server-side; the visual cue is the only client-side concern, and it gracefully degrades for vanilla players

---

## Compatibility

EverHopper uses **only** `@Inject` mixins on `HopperBlockEntity#pushItemsTick` — no overwrites, no replacements. It introduces **no custom blocks, no block entities, and no items**. This makes it broadly compatible with other mixin-based hopper mods and means uninstalling it is safe: your world keeps every existing hopper, they just go back to vanilla behavior.

- ✅ Server-side friendly — clients don't need to install it
- ✅ No worldgen changes
- ✅ No new items or blocks
- ✅ Works alongside other hopper mods that don't replace `pushItemsTick` entirely

---

## Why use EverHopper?

- **Remote item farms work.** Auto-sorters and storage hoppers at distant bases actually move items.
- **Mob-farm collection chains stay full.** Items dropped while you were away funnel down through your hopper line on chunk reload.
- **Multiplayer-friendly.** Server admins can install EverHopper without forcing every player to install the client side.
- **Lightweight.** No background ticking, no extra block entities, no chunk-loading.
- **Catch-up only.** Hoppers never transfer faster than they would have under vanilla rules — just no slower.

---

## Commands

EverHopper ships with admin/debug commands under `/everhopper` (requires op level 2).

| Command | Purpose |
| --- | --- |
| `/everhopper inspect [x y z]` | Show the hopper's tracked state: `lastGameTime`, current game time, and computed delta. Defaults to the player's feet. |
| `/everhopper tick <radius>` | Force `pushItemsTick` on every loaded hopper within the radius — useful for instant transfer without waiting. |
| `/everhopper simulate <ticks> <radius>` | Backdate the `lastGameTime` of every loaded hopper in the radius by N ticks; the next natural tick will trigger catch-up. |

These are intended for server admins and modpack debugging. They have no effect on normal gameplay. Radius is capped at 64 blocks.

---

## Companion mods

- **[EverFurnace](https://modrinth.com/mod/everfurnace)** — same catch-up philosophy applied to furnaces, blast furnaces, and smokers.
- **[EverCrops](https://modrinth.com/mod/evercrops)** — and to your wheat, carrots, melons, sweet berries, nether wart, and cocoa.

---

## License

Licensed under **LGPL-3.0**. Source available on [GitHub](https://github.com/gottsch/gottsch-minecraft-EverHopper).
