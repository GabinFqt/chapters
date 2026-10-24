# Chapters

This file holds **two paste-ready descriptions**: [Modrinth](#modrinth) (markdown with tables, code, and inline code) and [CurseForge](#curseforge) (markdown with **#** headings, **bold**, *italic*, and lists; still no tables, fenced code, or backticks, which CurseForge handles poorly).

---

## Modrinth

**Documentation & examples: [GitHub Wiki](https://github.com/GabinFqt/chapters/wiki)**

**Chapters** turns your pack into a story of unlocks. You name each step a *chapter*, lock the content that belongs later, and only open it when players earn that chapter: quest reward, script, or admin command.

Works on **Minecraft 1.21** (Chapters **1.x**) and **Minecraft 26** (Chapters **2.x**). Inspired by *GameStages* / *ItemStages*, built for NeoForge packs that want progression without fighting half a dozen half-integrations.

### What it does for your pack

- **Gate the next era**: Keep netherite, Create, Mekanism, or a whole mod locked until players finish the early game.
- **Hide the spoilers**: With **JEI**, locked items and recipes stay out of the recipe book until unlocked.
- **Enforce on the server**: Locked items cannot be picked up or crafted (vanilla crafting), and they are **dropped from inventory** if somehow obtained early.
- **Lock more than items**: Fluids (buckets, placement, transfers), recipes by id, dimensions (travel cancelled; players already inside return to the Overworld), and Mekanism chemicals when Mekanism is present.
- **One chapter, one mod**: Lock everything from a mod with a single entry when you want a clean “era” boundary.
- **Solo or party**: With **FTB Teams**, the whole party shares unlocks. With **FTB Quests**, grant or require chapters using the built-in Stage reward / task fields.
- **Your tools**: Define chapters with **datapacks** and/or **KubeJS**; grant them with commands, quests, or scripts.

### Good use cases

- Age / chapter packs where each biome or tech tree opens over time  
- Softening early JEI overwhelm by revealing content as players progress  
- Multiplayer co-op where the team unlocks the next chapter together  
- Quest-driven packs that already use FTB Quests and want stages that actually block items  

### Optional integrations

| Integration | What you get |
| --- | --- |
| **JEI** | Locked content hidden client-side until unlock |
| **KubeJS** | Define chapters and grant/check them from scripts |
| **FTB Library + Quests** | Stage Reward, Stage Task, Stage Required, no extra plugin |
| **FTB Teams** | Shared team unlocks for the whole party |
| **Mekanism** | Chemical gating when a matching Mekanism build is available |

### Setup in short

The jar ships **no** preset chapters: you add your own.

1. Define a chapter (datapack JSON or KubeJS).  
2. `/reload` (or restart scripts).  
3. Test with `/chapters check`, then `/chapters add`.  

Full syntax, examples, and troubleshooting live on the [wiki](https://github.com/GabinFqt/chapters/wiki). Sample scripts: [examples/kubejs](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs).

**Commands:** `/chapters add|remove|list|check|reload`

### Compatibility

| Version | Jar |
| --- | --- |
| Minecraft **1.21** (NeoForge 21.1, Java 21) | Chapters **1.x** |
| Minecraft **26** (NeoForge 26.1, Java 25) | Chapters **2.x** |

Vanilla crafting is blocked server-side today; other stations may still differ between JEI hide and server block; details on the wiki.

### Links

- **GitHub:** https://github.com/GabinFqt/chapters  
- **Releases:** https://github.com/GabinFqt/chapters/releases  
- **License:** MIT  

---

## CurseForge

Copy from the next line through the end of this section (CurseForge project description supports Markdown: **#** headings, **bold**, *italic*, and lists).

# Chapters

**Documentation & examples:** https://github.com/GabinFqt/chapters/wiki

**Chapters** turns your pack into a story of unlocks. You name each step a *chapter*, lock the content that belongs later, and only open it when players earn that chapter: quest reward, script, or admin command.

Available for **Minecraft 1.21** (Chapters **1.x**) and **Minecraft 26** (Chapters **2.x**). Inspired by *GameStages* / *ItemStages*, made for NeoForge packs that want real progression without juggling half-finished stage mods.

## What it does for your pack

- **Gate the next era**: Keep netherite, Create, Mekanism, or a whole mod locked until players finish the early game.
- **Hide the spoilers**: With **JEI**, locked items and recipes stay out of the recipe book until unlocked.
- **Enforce on the server**: Locked items cannot be picked up or crafted on the vanilla crafting table, and they are **dropped from inventory** if obtained early.
- **Lock more than items**: Fluids (buckets, cauldrons, transfers), recipes by id, dimensions (travel cancelled; players already inside return to the Overworld), and Mekanism chemicals when Mekanism is available.
- **One chapter, one mod**: Lock an entire mod behind a single chapter when you want a clean era boundary.
- **Solo or party**: With **FTB Teams**, the party shares unlocks. With **FTB Quests**, grant or require chapters using the built-in Stage reward / task fields.
- **Your tools**: Define chapters with **datapacks** and/or **KubeJS**; grant them with commands, quests, or scripts.

## Good use cases

- Age / chapter packs where each tech tree opens over time
- Softening early JEI overwhelm by revealing content as players progress
- Multiplayer co-op where the team unlocks the next chapter together
- Quest packs that already use FTB Quests and want stages that actually block items

## Optional integrations

- **JEI**: hide locked content until unlock
- **KubeJS**: define and grant chapters from scripts
- **FTB Library + Quests**: Stage Reward, Stage Task, Stage Required with no extra wiring
- **FTB Teams**: shared team unlocks
- **Mekanism**: chemical gating when a matching build exists (not enabled on 26 in this release)

## Setup in short

The jar ships **no** preset chapters: you add your own.

1. Define a chapter (datapack or KubeJS).
2. Reload.
3. Test with **/chapters check**, then **/chapters add**.

Full syntax and examples: the **wiki**. Sample scripts on [GitHub](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs).

**Commands:** /chapters add, remove, list, check, reload

## Compatibility

- **Minecraft 1.21** → Chapters **1.x** (NeoForge 21.1, Java 21)
- **Minecraft 26** → Chapters **2.x** (NeoForge 26.1, Java 25)

## Links

- **GitHub:** https://github.com/GabinFqt/chapters
- **Releases:** https://github.com/GabinFqt/chapters/releases
- **License:** MIT

Issues and suggestions are welcome on GitHub.
