# Chapters

Chapters locks items, fluids, recipes, and dimensions behind **stages** until you grant them.

A stage is an id you choose, like `mypack:iron_age`. `/chapters`, KubeJS, and the FTB Quests Stage fields all use that same id.

| Minecraft | Jar | Java |
| --- | --- | --- |
| **1.21** (NeoForge 21.1) | **1.x** | 21 |
| **26** (NeoForge 26.1) | **2.x** | 25 |

The jar ships **no** stages. You write them.

- **[Releases](https://github.com/GabinFqt/chapters/releases)** (pick the jar for your Minecraft version)
- **[Open an issue](https://github.com/GabinFqt/chapters/issues)** if a page is wrong or missing

## Ten minutes

KubeJS is the way to define stages. Install it next to Chapters. A datapack works if you do not want scripts: [[Stages-datapack]].

### 1. Define the stage

`kubejs/server_scripts/stages.js`:

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:iron_age', [
    '@create',
    'dimension:minecraft:the_nether'
  ])
})
```

`@create` locks that mod's items, fluids, and (on 1.21) Mekanism chemicals. It does not lock dimensions, so the Nether is a separate line.

### 2. Look at the lock

Reload server scripts (`/kubejs reload server-scripts`, or restart). `ServerEvents.loaded` does **not** run on `/reload`. `/chapters reload` is that same resource reload: it picks up datapack JSON, and it does not re-run KubeJS.

```
/chapters check Steve mypack:iron_age
```

Steve does not have the stage, so:

- Create items are hidden in JEI, cannot be picked up, and are dropped from the inventory (about once a second)
- Create fluids cannot be placed or moved by the player with buckets and tanks
- The Nether cannot be entered. A player already inside is sent back to the Overworld

### 3. Grant it

```
/chapters add Steve mypack:iron_age
```

From a script, when you have a player (quest reward, advancement, custom command):

```js
PlayerStages.of(player).add('mypack:iron_age')
```

With **FTB Quests**, add a Stage Reward and type `mypack:iron_age`. No extra script. With **FTB Teams**, the whole party gets it. See [[FTB-integration]].

### 4. Leave the defaults alone unless you mean to soften

Every lock is full until the stage is granted: hidden in JEI, no pickup, dropped from the inventory, no vanilla crafting. To keep an item visible, or let the player hold it, see [[Restriction-flags]].

## What a line means

| You write | It locks |
| --- | --- |
| `'minecraft:netherite_ingot'` | That item |
| `'#minecraft:swords'` | Every item in the tag |
| `'@create'` | That mod's items, fluids, and Mekanism chemicals |
| `'fluid:minecraft:lava'` | That fluid only |
| `'recipe:minecraft:diamond_pickaxe'` | That recipe id on the vanilla grid, plus JEI |
| `'dimension:minecraft:the_nether'` | Travel into that dimension |
| `'chemical:mekanism:hydrogen'` | That Mekanism chemical, on **1.21** when Mekanism is installed |

Full prefix list: [[KubeJS]]. More scripts: [[Examples]].

## What stays possible

- Smithing tables, mechanical crafters, and other machines can still craft. Lock the item as well when those must stay closed.
- Pipes and other automation can still move locked fluids and chemicals. A player clicking a tank is blocked.
- Mekanism chemicals are gated on Minecraft **1.21** only. Chapters **2.x** ignores `chemical:` entries.

Details: [[Limits]].

## Commands

Ops, as usual.

| Command | What it does |
| --- | --- |
| `/chapters add <player> <stage>` | Grants the stage |
| `/chapters remove <player> <stage>` | Revokes it |
| `/chapters list <player>` | Stages that player has |
| `/chapters check <player> <stage>` | `true` or `false` |
| `/chapters reload` | Re-reads datapack JSON (same as `/reload`). Does not re-run KubeJS, and does not take stages away from players |

## Pages

| Page | Use it when |
| --- | --- |
| [[Examples]] | You want a script for one case |
| [[KubeJS]] | You need a prefix or a method |
| [[Restriction-flags]] | You want to soften a lock |
| [[FTB-integration]] | You use Quests or Teams |
| [[Limits]] | You need to know what is still unguarded |
| [[Stages-datapack]] | You do not want KubeJS |
| [[Troubleshooting]] | A lock does not do what you expected |

### Samples in the repo

- KubeJS: [on `main` (1.21)](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs) · [on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs)
- Datapack: [on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/datapack) · [on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/datapack)
