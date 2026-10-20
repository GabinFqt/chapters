# KubeJS

Install **KubeJS** alongside **Chapters**. The script API is registered when both mods load. Common Chapters settings (e.g. FTB Teams sync) live in `config/chapters-common.toml`.

## `ChaptersEvents.defineStage(stageId, entries[, restrictions])`

- **`stageId`**: string like `"mypack:tier2"`.
- **`entries`**: array of strings **or** objects `{ id: '…', jei: false, … }`. Same ideas as datapack lists, but **prefixes** make each line unambiguous:
  - item: `"minecraft:diamond"`, `"#minecraft:swords"`, `"@create"`
  - fluid: `"fluid:minecraft:lava"`
  - Mekanism chemical: `"chemical:mekanism:hydrogen"` (indexed only when Mekanism compat is active; on Minecraft **26** / Chapters **2.x** chemical gating is off until Mekanism ships)
  - recipe: `"recipe:minecraft:diamond_pickaxe"`
  - dimension: `"dimension:minecraft:the_nether"`, `"dimension:#ns:tag"`, `"dimension:@modid"`
- **`restrictions`** (optional): stage-wide defaults, e.g. `{ jei: false }`. See [[Stages-datapack]] for flag names.

Many `defineStage` calls in the **same server tick** are batched internally so indexing stays fast after load.

Example:

```js
ServerEvents.loaded((event) => {
  ChaptersEvents.defineStage('mypack:tier1', [
    'minecraft:netherite_ingot',
    '#minecraft:swords',
    'fluid:minecraft:lava',
    'recipe:minecraft:diamond_pickaxe',
    'dimension:minecraft:the_nether',
    '@create'
  ])

  ChaptersEvents.defineStage(
    'mypack:visible_but_gated',
    [
      'minecraft:diamond',
      { id: 'minecraft:emerald', jei: false, inventory: false }
    ],
    { craft: true }
  )

  ChaptersEvents.defineStage('mypack:mek_early', ['chemical:mekanism:hydrogen'])
})
```

## `PlayerStages.of(player)`

Typical methods you wire to quests / commands / advancements:

| Method | Use |
| --- | --- |
| `.has(stageId)` | boolean check |
| `.get()` | all stage ids currently on the player |
| `.add(stageId)` | grant |
| `.remove(stageId)` | revoke |

Skeleton:

```js
PlayerStages.of(player).add('mypack:tier1')

if (PlayerStages.of(player).has('mypack:tier2')) {
  // ...
}
```

`player` depends on whichever KubeJS event you handle.

## Starter script file

Copy into **`kubejs/server_scripts/`** while you iterate:

- Minecraft **26**: [examples/kubejs on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs)
- Minecraft **1.21**: [examples/kubejs on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs)

See also [[Examples]].
