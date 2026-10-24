# Chapters

This file holds **two paste-ready descriptions**: [Modrinth](#modrinth) (markdown, tables, and fenced code) and [CurseForge](#curseforge) (headings, bold, italic, and lists). The CurseForge section has no tables, fenced code, or backticks.

---

## Modrinth

**Guide, recipes, and troubleshooting: [GitHub Wiki](https://github.com/GabinFqt/chapters/wiki)**

**Chapters** locks items, fluids, recipes, and dimensions behind **stages** until you grant them.

A stage is an id you choose, like `mypack:iron_age`. The `/chapters` commands, KubeJS, and the FTB Quests Stage fields all use that same id.

The jar ships **no** stages. You write them. Chapters runs on its own. KubeJS is the way the examples below are written. A datapack works if you do not want scripts. JEI, FTB Quests, FTB Teams, and Mekanism are optional.

### What you can lock

- **Items.** Hidden in JEI, no ground pickup, dropped from the inventory (about once a second), no crafting on the vanilla grid.
- **Fluids.** The player cannot place them or move them with buckets and tanks. JEI hides them.
- **One recipe.** That recipe id will not craft on the vanilla grid, and JEI hides it. The output item stays free unless you also lock the item.
- **Dimensions.** Travel is cancelled (portals, `/tp`). Players already inside are sent back to the Overworld.
- **A whole mod.** `@create` locks that mod's items, fluids, and Mekanism chemicals. It does not lock dimensions. Add a `dimension:` line for those.
- **Mekanism chemicals**, on Minecraft **1.21** when Mekanism is installed. The player cannot move them by hand. JEI hides them.

You can soften a lock (keep it visible in JEI, or let the player hold the item). The wiki page [Restriction flags](https://github.com/GabinFqt/chapters/wiki/Restriction-flags) shows how. Most packs should leave the defaults: fully locked until the stage is granted.

### Example

Install KubeJS. Put this in `kubejs/server_scripts/stages.js`:

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:iron_age', [
    '@create',
    'dimension:minecraft:the_nether'
  ])
})
```

Reload server scripts, then:

```
/chapters check Steve mypack:iron_age
/chapters add Steve mypack:iron_age
```

Nothing from Create, and no Nether, until that stage is granted.

With **FTB Quests**, add a Stage Reward and type `mypack:iron_age`. No extra plugin. With **FTB Teams**, the whole party gets the stage.

More recipes (one item, a tag, fluids, a single recipe, a soft lock): the [wiki](https://github.com/GabinFqt/chapters/wiki/Examples).

### What stays possible

- Smithing tables, mechanical crafters, and other machines can still craft. Lock the item as well when those must stay closed.
- Pipes and other automation can still move locked fluids and chemicals. A player clicking a tank is blocked.
- Mekanism chemicals are gated on Minecraft **1.21** only (Chapters **1.x**). Chapters **2.x** (Minecraft **26**) ignores `chemical:` entries.

### Commands

`/chapters add`, `remove`, `list`, `check`, `reload`

### Versions

| Minecraft | Jar |
| --- | --- |
| **1.21** (NeoForge 21.1, Java 21) | Chapters **1.x** |
| **26** (NeoForge 26.1, Java 25) | Chapters **2.x** |

### Links

- **GitHub:** https://github.com/GabinFqt/chapters
- **Releases:** https://github.com/GabinFqt/chapters/releases
- **License:** MIT

---

## CurseForge

Copy from the next line through the end of this section.

# Chapters

**Guide, recipes, and troubleshooting:** https://github.com/GabinFqt/chapters/wiki

**Chapters** locks items, fluids, recipes, and dimensions behind **stages** until you grant them.

A stage is an id you choose, like mypack:iron_age. The /chapters commands, KubeJS, and the FTB Quests Stage fields all use that same id.

The jar ships **no** stages. You write them. Chapters runs on its own. KubeJS is the way the example below is written. A datapack works if you do not want scripts. JEI, FTB Quests, FTB Teams, and Mekanism are optional.

## What you can lock

- **Items.** Hidden in JEI, no ground pickup, dropped from the inventory (about once a second), no crafting on the vanilla grid.
- **Fluids.** The player cannot place them or move them with buckets and tanks. JEI hides them.
- **One recipe.** That recipe id will not craft on the vanilla grid, and JEI hides it. The output item stays free unless you also lock the item.
- **Dimensions.** Travel is cancelled (portals, /tp). Players already inside are sent back to the Overworld.
- **A whole mod.** @create locks that mod's items, fluids, and Mekanism chemicals. It does not lock dimensions.
- **Mekanism chemicals,** on Minecraft **1.21** when Mekanism is installed. The player cannot move them by hand. JEI hides them.

You can soften a lock (keep it visible in JEI, or let the player hold the item). The wiki page Restriction flags shows how. Most packs should leave the defaults: fully locked until the stage is granted.

## Example

Install KubeJS. Create kubejs/server_scripts/stages.js and call this inside ServerEvents.loaded:

ChaptersEvents.defineStage('mypack:iron_age', ['@create', 'dimension:minecraft:the_nether'])

Nothing from Create, and no Nether, until you grant the stage:

- /chapters check Steve mypack:iron_age
- /chapters add Steve mypack:iron_age

With **FTB Quests**, add a Stage Reward and type mypack:iron_age. No extra plugin. With **FTB Teams**, the whole party gets the stage.

The formatted script and more recipes are on the wiki.

## What stays possible

- Smithing tables, mechanical crafters, and other machines can still craft. Lock the item as well when those must stay closed.
- Pipes and other automation can still move locked fluids and chemicals. A player clicking a tank is blocked.
- Mekanism chemicals are gated on Minecraft **1.21** only (Chapters **1.x**). Chapters **2.x** (Minecraft **26**) ignores chemical entries.

## Commands

/chapters add, remove, list, check, reload

## Versions

- **Minecraft 1.21** (NeoForge 21.1, Java 21): Chapters **1.x**
- **Minecraft 26** (NeoForge 26.1, Java 25): Chapters **2.x**

## Links

- **GitHub:** https://github.com/GabinFqt/chapters
- **Releases:** https://github.com/GabinFqt/chapters/releases
- **License:** MIT
