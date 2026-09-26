**Chapters** is a progression mod for NeoForge. You gate **items, fluids, Mekanism chemicals**, **recipes by id**, and **dimensions** behind named *chapters* (stages) using **datapacks** and optionally **KubeJS**, with optional **JEI** so players only see what they can use.

| Minecraft | Chapters jar | Java |
| --- | --- | --- |
| **1.21** (NeoForge 21.1) | **1.x** | 21 |
| **26** (NeoForge 26.1) | **2.x** | 25 |

- **[Download releases](https://github.com/GabinFqt/chapters/releases)** (pick the jar for your Minecraft version)
- **Something wrong or missing from the wiki?** [Open an issue](https://github.com/GabinFqt/chapters/issues)

## Commands (quick reference)

All require permission level as usual for multiplayer.

| Command | What it does |
| --- | --- |
| `/chapters add <player> <stage>` | Grants a stage (e.g. `mypack:tier2`) |
| `/chapters remove <player> <stage>` | Revokes it |
| `/chapters list <player>` | Lists that player’s current stages |
| `/chapters check <player> <stage>` | `true` / `false` |
| `/chapters reload` | Reloads stage **definitions** (after datapack or script edits that affect gates) |

## Getting started as a pack author

1. Define stages in **datapack JSON** under `data/<namespace>/chapters/stages/` and/or with **KubeJS**. See [[Stages-datapack]] and [[KubeJS]].
2. Run **`/reload`** after changing datapacks (operator).
3. KubeJS: stage definitions typically apply on **`ServerEvents.loaded`**; reloading behaviour follows your usual KubeJS/server restart habits.
4. Grant progression with **`/chapters add`** or **`PlayerStages.of(player).add(...)`** from quests, advancements, etc. See [[Examples]].

## Wiki pages

| Page | Purpose |
| --- | --- |
| [[Stages-datapack]] | Where to put JSON, keys, `#` tags, `@mods` |
| [[KubeJS]] | `defineStage`, `fluid:`, `recipe:`, `dimension:`, `PlayerStages` |
| [[Examples]] | Copy-paste setups (tiers, fluids, recipes, Mekanism) |
| [[FTB-integration]] | FTB Library / FTB Teams / FTB Quests (Stage Reward, team-wide unlocks) |
| [[JEI-and-limitations]] | What JEI hides and what crafting is blocked |
| [[Troubleshooting]] | datapack not applying, conflicting stages, JEI quirks, KubeJS crash |

### Sample packs

Copy a **tutorial datapack** and **KubeJS snippets** from GitHub:

- Minecraft **26**: [examples on branch `26`](https://github.com/GabinFqt/chapters/tree/26/examples)
- Minecraft **1.21**: [examples on `main`](https://github.com/GabinFqt/chapters/tree/main/examples)

Grab the matching jar from Releases, then paste the samples into your world or modpack workspace.
