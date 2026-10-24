# Datapack stages

Use this when the pack should not depend on KubeJS. The stage ids, the prefixes, and the flags are the same as [[KubeJS]]. A KubeJS `defineStage` with the **same id** replaces the JSON. It does not merge.

Most packs should use KubeJS. Start on [[Home]] if you can ship scripts.

## Where the file goes

```
data/<namespace>/chapters/stages/<name>.json
```

The stage id is `<namespace>:<name>`.

`data/mypack/chapters/stages/iron_age.json` is the stage `mypack:iron_age`.

Put the folder in the world's datapacks, enable it, then `/reload`. `/chapters reload` runs that same resource reload, so it picks up JSON edits too. A pack the world has never enabled still needs to be turned on first.

## The same example as the wiki home

This is the datapack form of the KubeJS iron-age stage: all of Create, and the Nether.

`data/mypack/chapters/stages/iron_age.json`

```json
{
  "items": ["@create"],
  "dimensions": ["minecraft:the_nether"]
}
```

`@create` inside `items` also locks that mod's fluids and, on 1.21, its Mekanism chemicals. Dimensions are only locked by `dimensions`. Check and grant:

```
/chapters check Steve mypack:iron_age
/chapters add Steve mypack:iron_age
```

Until then, Create content is fully locked and the Nether cannot be entered. Same player-facing result as the script on [[Home]].

## Keys

Omit any key you do not use.

| Key | What you put in it |
| --- | --- |
| `items` | Item ids (`minecraft:diamond`), item tags (`#minecraft:swords`), or a mod (`@create`). `@` also locks that mod's fluids and Mekanism chemicals |
| `fluids` | Fluid ids, `#` fluid tags, or `@` for every fluid from a mod |
| `chemicals` | Mekanism chemical ids, tags, or `@mod`. On **1.21**, indexed when Mekanism is installed. On **26**, accepted and ignored |
| `recipes` | Recipe ids (`minecraft:diamond_pickaxe`). No `#` and no `@`. Vanilla grid and JEI. See [[Limits]] |
| `dimensions` | Dimension ids (`minecraft:the_nether`), `#` dimension tags, or `@` for every loaded dimension from a mod |
| `namespaces` | Mod ids. Same as writing `@mod` under items: items, fluids, and chemicals |
| `fluid_namespaces` | Extra mod ids, fluids only |
| `chemical_namespaces` | Extra mod ids, chemicals only |
| `restrictions` | Stage-wide flags. All `true` if you omit it. See [[Restriction-flags]] |

### Prefixes

| You write | Meaning |
| --- | --- |
| `minecraft:iron_ingot` | That id |
| `#minecraft:swords` | The tag, in whichever list you put it in |
| `@create` | Everything from that mod for that list. In `items`, fluids and chemicals too |

### One entry with different flags

```json
{
  "restrictions": {
    "hideInJei": false
  },
  "items": [
    "minecraft:diamond",
    { "id": "minecraft:emerald", "dropFromInventory": false }
  ]
}
```

Both stay visible in JEI. Diamond is fully locked otherwise. Emerald is visible, stays in the inventory, and still cannot be picked up or crafted on the vanilla grid. Flag rules: [[Restriction-flags]].

## Same id in two files

If several stages list the same item, fluid, recipe, or dimension:

- Owning **any one** of them unlocks it completely.
- While the player has none, a behaviour is blocked if **any** listing has that flag on.

A second pack cannot soften the first by listing the same id with flags set to `false`. That listing is another unlock. Use your own stage ids, and leave an id out when you do not want to gate it.

Inside a **single** file, an explicit id overrides the flags that a tag or `@mod` in that same file expanded onto it.

## A wider file

```json
{
  "items": [
    "minecraft:netherite_ingot",
    "#minecraft:swords"
  ],
  "fluids": ["minecraft:lava"],
  "recipes": ["minecraft:diamond_pickaxe"],
  "dimensions": ["minecraft:the_nether", "minecraft:the_end"]
}
```

Add `"chemicals": ["mekanism:hydrogen"]` on 1.21 when you want it. Safe to leave in on 26: it indexes nothing there.

## Copy-paste pack

A `pack.mcmeta` plus several stages, including a flags example:

- [on `main` (1.21)](https://github.com/GabinFqt/chapters/tree/main/examples/datapack/tutorial)
- [on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/datapack/tutorial)

Copy the `tutorial` folder into `saves/<world>/datapacks/`, enable it, `/reload`.

Stage ids from that pack include `tutorial:intro_nether`, `tutorial:intro_dims`, `tutorial:tier_early`, `tutorial:tier_mid`, `tutorial:tier_late`, `tutorial:fluides_base`, `tutorial:recipe_pickaxe`, and `tutorial:granular_emerald`.
