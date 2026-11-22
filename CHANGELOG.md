# Chapters 1.6

## Changes
- **FTB Teams, removed stages**: a stage removed with `/chapters remove`, FTB Quests, or KubeJS no longer comes back after leaving and rejoining the world. Stages left in the player file from before team sync handled them were copied back onto the team at every login. That copy is now cleared once it is on the team (at login or on party join), and a remove also drops it. In a world that already hit this, the old stages come back one last time on the first login with this version: remove them once more and they stay gone.
- **KubeJS `stages.remove`**: with team sync on, it returns `true` only when the team actually had the stage.

---

# Chapters 1.5

## Changes
- **Large packs**: lock checks, JEI hides and reveals, crafting, and stage sync are faster. The stage index sent to the client is a smaller namespace-grouped payload, so the client and server need this same version.
- **Vanilla buckets**: filling or emptying a bucket is now cancelled when that fluid has `preventTransfer` on. Previously only tank and `FluidUtil` transfers were gated, so a normal bucket click could still move a locked fluid until the inventory audit dropped the bucket.

---

# Chapters 1.4

## Changes
- **JEI after respawn**: unlocked items and recipes stay visible after death, including when leaving the End. Stage sync now runs once the new player exists, instead of racing the death copy and sending an empty stage set (which re-hid everything and stalled the client).
- **JEI runtime updates**: hides and reveals in the same tick are applied once. Items, fluids, and chemicals are added or removed in a single JEI call per type, and unlocking a large stage reveals its recipes in one pass. `/reload` and rejoining log one line per type instead of one line per item.

---

# Chapters 1.3

## Changes
- **Restriction flags**: optional `restrictions` object and per-entry overrides (`hideInJei`, `preventPickup`, `dropFromInventory`, `preventCraft`, `preventTransfer`, `preventTravel`) on datapack / KubeJS stages. Defaults stay fully enforced for legacy JSON.
- **FTB Teams config**: `config/chapters-common.toml` → `syncWithFtbTeams` (default `true`); set `false` for standalone per-player stages while FTB Teams is installed.

---

# Chapters 1.2

## Changes
- **Dimension locks**: datapack `dimensions` / KubeJS `dimension:…` gate travel into named dimensions; players already inside are returned to the Overworld when a stage is lost or definitions reload.

## Requirements
- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Mekanism (optional, for chemical gating)
- JEI (optional, to hide locked recipes/items client-side)
- FTB Library / FTB Teams / FTB Quests (all optional; auto-detected at runtime)

---

# Chapters 1.1

FTB Library / FTB Teams / FTB Quests integration.

## Changes
- Chapters now registers itself as FTB Library's active stage provider when `ftblibrary` is installed; FTB Quests' built-in **Stage Reward**, **Stage Task**, and "Stage Required" field all use chapter ids out of the box — no extra setup required.
- When `ftbteams` is also installed, chapter unlocks are **team-scoped** through FTB Teams' `TEAM_STAGES` property: every party member instantly shares unlocks, the inventory auditor and JEI hide/reveal run for all online members, and `/chapters add/remove`, KubeJS `PlayerStages.of(player)`, and FTB Quests Stage Reward all converge on the same team-wide store.
- One-time migration on first login: pre-existing per-player attachment unlocks on a player's personal team are copied into `TEAM_STAGES` (party teams are skipped to avoid leaking personal unlocks).
- New `examples/kubejs/server_scripts/ftbquests_chapter_reward.js` sample showing the FTB Quests **Custom Reward** path for conditional chapter grants.
- New optional dependencies declared in `neoforge.mods.toml` and advertised on Modrinth/CurseForge: `ftb-library`, `ftb-teams`, `ftb-quests`.
- Workaround for an upstream FTB Teams `TEAM_STAGES` aliasing bug: `TeamProperties.TEAM_STAGES` ships with a single shared `HashSet` as its default value, and `TeamStagesHelper.updateStages` mutates that instance in place — meaning a stage write on team A leaks into every other team's first read of the property. Chapters now bypasses `TeamStagesHelper`, copies the current value into a fresh `HashSet` before mutating, and treats the shared default as empty on read, so each FTB team keeps its own isolated stage set.

## Requirements
- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Mekanism (optional, for chemical gating)
- JEI (optional, to hide locked recipes/items client-side)
- FTB Library / FTB Teams / FTB Quests (all optional; auto-detected at runtime)

---

# Chapters 1.0

First release under the new name **Chapters** (formerly Stagecraft).

## Changes
- Full mod rename: `stagecraft` → `chapters` (mod id, Java package, asset namespace, mixins, KubeJS bindings)
- Bumped `mod_version` to `1.0`
- KubeJS API: `ChaptersEvents` (formerly `StagecraftEvents`)
- New `chapters_logo.png`

## Requirements
- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Mekanism (optional, for chemical gating)
- JEI (optional, to hide locked recipes/items client-side)
