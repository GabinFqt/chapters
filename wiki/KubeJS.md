# KubeJS

Install **KubeJS** alongside **Chapters**. There is no separate Chapters config file — the script API is registered when both mods load.

## `ChaptersEvents.defineStage(stageId, entries)`

- **`stageId`** — string like `"mypack:tier2"`.
- **`entries`** — array of strings. Same ideas as datapack lists, but **prefixes** make each line unambiguous:
  - item: `"minecraft:diamond"`, `"#minecraft:swords"`, `"@create"`
  - fluid: `"fluid:minecraft:lava"`
  - Mekanism chemical: `"chemical:mekanism:hydrogen"`
  - recipe: `"recipe:minecraft:diamond_pickaxe"`

Many `defineStage` calls in the **same server tick** are batched internally so indexing stays fast after load.

Example:

```js
ServerEvents.loaded((event) => {
  ChaptersEvents.defineStage('mypack:tier1', [
    'minecraft:netherite_ingot',
    '#minecraft:swords',
    'fluid:minecraft:lava',
    'recipe:minecraft:diamond_pickaxe',
    '@create'
  ])

  ChaptersEvents.defineStage('mypack:mek_early', ['chemical:mekanism:hydrogen'])
})
```

## `PlayerStages.of(player)`

Typical methods you’ll wire to quests / commands / advancements:

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

[**examples/kubejs/server_scripts/main.js**](https://github.com/GabinFqt/chapters/blob/main/examples/kubejs/server_scripts/main.js) on GitHub mirrors the kind of setups in [[Examples]]. Copy those lines into **`kubejs/server_scripts/`** in your instance while you iterate.
