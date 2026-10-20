**Chapters** is a progression mod for NeoForge. You gate **items, fluids, Mekanism chemicals**, **recipes by id**, and **dimensions** behind named *chapters* (stages). **KubeJS** is the preferred authoring path; datapacks remain supported. Optional **JEI** filters what players see. Per-stage **restriction flags** (`pickup`, `inventory`, `craft`, `jei`, `transfer`, `travel`) let you soft-gate content.

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
| `/chapters reload` | Reloads stage **definitions** (after script or datapack edits that affect gates) |

## Getting started as a pack author

1. **Preferred:** define stages with **KubeJS** (`ChaptersEvents.defineStage` in `ServerEvents.loaded`). See [[KubeJS]] and [[Restriction-flags]].
2. Grant progression with **`PlayerStages.of(player).add(...)`** from quests / scripts, or **`/chapters add`**. See [[Examples]].
3. Optional: ship the same stages as **datapack JSON** under `data/<namespace>/chapters/stages/` if you need a script-free pack ([[Stages-datapack]]); run **`/reload`** after JSON changes.
4. Optional: FTB Quests Stage Reward / Tasks and team sharing: [[FTB-integration]].

## Wiki pages

| Page | Purpose |
| --- | --- |
| [[KubeJS]] | **Start here** — `defineStage`, prefixes, `PlayerStages`, flags in scripts |
| [[Restriction-flags]] | `pickup` / `inventory` / `craft` / `jei` / `transfer` / `travel`, overrides, recipes |
| [[Examples]] | Copy-paste KubeJS setups (tiers, fluids, recipes, granular flags) |
| [[FTB-integration]] | FTB Library / Teams / Quests, `syncWithFtbTeams`, party merge |
| [[JEI-and-limitations]] | JEI hide vs gameplay flags, crafting limits |
| [[Stages-datapack]] | Datapack JSON layout (optional alternative to KubeJS) |
| [[Troubleshooting]] | Scripts, flags, multi-stage, JEI sync, FTB party |

### Sample packs

- **KubeJS first:** [examples/kubejs on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs) · [on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs)
- Optional datapack twin: [examples/datapack on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/datapack) · [on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/datapack)

Grab the matching jar from Releases, then paste the samples into your modpack workspace.
