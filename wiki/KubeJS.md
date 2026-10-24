# KubeJS

KubeJS is the way to define stages. Install **KubeJS** next to **Chapters**. The script API shows up when both mods load.

A datapack works if you cannot ship scripts: [[Stages-datapack]]. A KubeJS `defineStage` with the **same id** as a datapack file **replaces** that file. It does not merge entries. Prefer one source per stage id.

Flag meanings: [[Restriction-flags]]. FTB Teams storage: [[FTB-integration]].

---

## Define

Put this in `kubejs/server_scripts/`. It runs from `ServerEvents.loaded`.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:iron_age', [
    'minecraft:netherite_ingot',
    '#minecraft:swords',
    '@create',
    'fluid:minecraft:lava',
    'recipe:minecraft:diamond_pickaxe',
    'dimension:minecraft:the_nether'
  ])
})
```

Reload with `/kubejs reload server-scripts` or a restart. `/reload` and `/chapters reload` do **not** rerun `ServerEvents.loaded`. `/chapters reload` re-reads datapack JSON and keeps the KubeJS stages already registered.

Several `defineStage` calls in the same load are applied together.

### `ChaptersEvents.defineStage(stageId, entries, restrictions)`

| Argument | Meaning |
| --- | --- |
| `stageId` | `'mypack:iron_age'` |
| `entries` | Strings, or objects `{ id: '…', hideInJei: false }` |
| `restrictions` | Optional defaults for the whole stage, such as `{ hideInJei: false }` |

`restrictions` can be omitted. Missing flags stay at their default, which is `true` (fully locked).

### Prefixes

| You write | Meaning |
| --- | --- |
| `'minecraft:diamond'` | One item |
| `'#minecraft:swords'` | Item tag |
| `'@create'` | Every item from that mod, plus its fluids and Mekanism chemicals |
| `'fluid:minecraft:lava'` | One fluid |
| `'fluid:#minecraft:water'` | Fluid tag |
| `'fluid:@create'` | Fluids from that mod only |
| `'chemical:mekanism:hydrogen'` | One Mekanism chemical. Indexed on **1.21** when Mekanism is installed. On **26**, the line is accepted and locks nothing |
| `'chemical:#mekanism:gases'` | Chemical tag (same 1.21 rule) |
| `'chemical:@mekanism'` | Chemicals from that mod only |
| `'recipe:minecraft:diamond_pickaxe'` | That recipe id. Vanilla grid and JEI. The output item stays free unless you also lock the item |
| `'dimension:minecraft:the_nether'` | One dimension |
| `'dimension:#namespace:tag'` | Dimension tag |
| `'dimension:@modid'` | Every loaded dimension from that namespace |

An item `'@mod'` line does **not** lock dimensions.

Object form, when one entry needs different flags:

```js
{ id: 'minecraft:raw_copper', preventPickup: false }
```

`item` is accepted as an alias of `id`. The object starts from the stage `restrictions`, then applies the keys you set.

### Remove a runtime stage

```js
ChaptersEvents.clearStage('mypack:iron_age')
```

This drops the KubeJS definition. It does not take the stage away from players who already have it. Use `PlayerStages.of(player).remove(...)` for that.

---

## Grant, check, revoke

| Call | Use |
| --- | --- |
| `PlayerStages.of(player).has(stageId)` | `true` / `false` |
| `PlayerStages.of(player).get()` | Stage ids that player effectively has |
| `PlayerStages.of(player).add(stageId)` | Grant |
| `PlayerStages.of(player).remove(stageId)` | Revoke |

```js
PlayerStages.of(player).add('mypack:iron_age')

if (PlayerStages.of(player).has('mypack:tier_mid')) {
  player.tell('Mid game is open.')
}
```

`player` is the one from your KubeJS event.

With **FTB Teams** and `syncWithFtbTeams` left on (the default), these calls read and write the **team**. The party shares the stage. With the config off, or without Teams, they use the per-player store. See [[FTB-integration]].

---

## Same item on two stages

If two `defineStage` calls list the same item, owning **either** stage unlocks it for every behaviour. While the player has neither, a behaviour stays blocked if **any** of those stages enables it.

Listing an item with every flag set to `false` still counts. Granting that stage unlocks the item for every other pack that locked it. Leave the id out if you only wanted a comment. Full rules: [[Restriction-flags]].

---

## Samples

- [on `main` (1.21)](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs)
- [on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs)

Recipes written out: [[Examples]].
