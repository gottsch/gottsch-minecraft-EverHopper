# Changelog for EverHopper 1.20.1

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-04-27

### 🎉 Highlights

#### Catch-Up Mechanic — Initial Release
- Hoppers now correctly simulate item transfers for time elapsed while their chunk
  was unloaded or the server was offline.
- Both directions are caught up:
  - **Extract** — pull items from the container above into the hopper.
  - **Insert** — push items from the hopper into the container in its facing direction
    (down, north, south, east, or west).
- Implemented via `@Inject` mixin on `HopperBlockEntity#pushItemsTick`, recording
  `lastGameTime` on each tick and simulating elapsed transfers on chunk reload.
- Redstone-disabled hoppers do not accrue catch-up time — a hopper held off by a
  redstone signal stays paused, matching vanilla behavior.

#### Forge Config API
- Key catch-up parameters are tunable via config files without touching code.
- `everhopper-common.toml` — server-side settings: master toggle, catch-up tick cap,
  delta threshold.
- `everhopper-client.toml` — client-side settings: particle burst and sound cue toggles.

#### Visual + Audio Cue
- A brief burst of green sparkle (`HAPPY_VILLAGER`) and dust puff (`POOF`) particles
  spawns at any hopper that transferred items during catch-up, alongside a quiet
  `ITEM_PICKUP` sound.
- **Deferred-fire pattern** — catch-up runs the moment the chunk reloads, but at that
  point the player is typically far away (at the edge of view distance). Firing the cue
  immediately would target empty space. Instead the hopper sets a `pendingCue` flag in
  NBT, and the cue fires on the next tick where a player is within 16 blocks. So the
  visual and sound trigger when the player walks up to the hopper, not when the chunk
  first popped in.
- Particles can be disabled via `particleBurstEnabled` in `everhopper-client.toml`.
- Sound can be disabled via `soundCueEnabled` in `everhopper-client.toml`.

#### Server-Side Optional
- Vanilla clients can connect to EverHopper-equipped servers (and modded clients can
  connect to vanilla servers) without a version mismatch.
- Implemented via `NetworkRegistry.acceptMissingOr(...)` on the channel's version
  predicates — packet sends silently no-op for clients that lack the channel.
- The catch-up logic itself is **100% server-side**. Only the cosmetic particle/sound
  cue requires the mod on the client; vanilla players still get the gameplay benefit
  of items moving while they were away, just without the visual.

#### Admin / Debug Commands
- `/everhopper inspect [x y z]` — show a hopper's tracked state (`lastGameTime`,
  current game time, computed delta). Defaults to the player's feet.
- `/everhopper tick <radius>` — force `pushItemsTick` on every loaded hopper within
  the radius for instant transfer testing.
- `/everhopper simulate <ticks> <radius>` — backdate `lastGameTime` of every loaded
  hopper in the radius by N ticks, so the next tick triggers catch-up.
- All commands require op level 2. Radius is capped at 64 blocks.

### ⚙️ Configuration

- **`everhopper-common.toml`** — `catchup` section:
  - `catchupEnabled` (boolean, default `true`)
  - `maxCatchupTicks` (long, default `24000` / 1 in-game day, range `1–192000`)
  - `minDeltaThreshold` (int, default `20` / 1 second, range `1–72000`)
- **`everhopper-client.toml`** — `visuals` section:
  - `particleBurstEnabled` (boolean, default `true`)
  - `soundCueEnabled` (boolean, default `true`)

### 🔧 Technical / Developer Notes

- Mixin uses **only** `@Inject` annotations — no `@Overwrite`, no `@Redirect` — for
  broad compatibility with other hopper-touching mods.
- `ModHopperBlockEntityInterface` exposes `lastGameTime` and `pendingCue` accessors
  for the mixin and any future helper code.
- NBT versioning added — `everhopper_version` (int, currently `1`) written on every
  save so future releases can key migrations off this value.
- Persistent NBT keys: `everhopper_lastGameTime` (long), `everhopper_pendingCue`
  (boolean), `everhopper_version` (int).
- `firePendingCueIfReady` runs at the head of every `pushItemsTick`. Short-circuits
  when no cue is pending; checks `ServerLevel.hasNearbyAlivePlayer` only when needed,
  so the per-tick overhead on idle hoppers is negligible.
- Targets `HopperBlockEntity#pushItemsTick` and the constant `MOVE_ITEM_SPEED` —
  the correct names in 1.20.1 official mappings (not the Yarn-style `serverTick` /
  `TRANSFER_COOLDOWN`, which trip up new contributors).
- No access transformers required — every accessed member of `HopperBlockEntity`
  and `HopperBlock` is already public in 1.20.1 official mappings.
- No custom blocks, items, or block entities. Uninstalling is safe; existing hoppers
  fall back to vanilla behavior.
