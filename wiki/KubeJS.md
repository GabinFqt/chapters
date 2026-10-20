# KubeJS

**KubeJS is the recommended way to author Chapters stages.** Install **KubeJS** alongside **Chapters**; the script API registers when both mods load. Datapacks still work for packs that cannot ship scripts; see [[Stages-datapack]].

Common config (e.g. FTB Teams sync) lives in `config/chapters-common.toml` ([[FTB-integration]]). Restriction flag semantics: [[Restriction-flags]].

Runtime stages from KubeJS **replace** a datapack stage with the **same id** (they do not merge entry-by-entry). Prefer different ids, or treat KubeJS as the single source for that stage.

---

## Quick start

1. Put a script under `kubejs/server_scripts/` (see [examples on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs) / [on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs)).
2. Define stages inside `ServerEvents.loaded`.
3. Grant unlocks with `PlayerStages.of(player).add(...)` from quests, commands wrappers, or other KubeJS events.
4. After script edits, use your usual KubeJS reload / server restart, then `/chapters reload` if definitions changed while the server was up.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:tier1', [
    'minecraft:netherite_ingot',
    '#minecraft:swords',
    'fluid:minecraft:lava',
    'recipe:minecraft:diamond_pickaxe',
    'dimension:minecraft:the_nether',
    '@create'
  ])
})

// Elsewhere (quest reward, custom command, …):
PlayerStages.of(player).add('mypack:tier1')
```

---

## `ChaptersEvents.defineStage(stageId, entries[, restrictions])`

| Argument | Meaning |
| --- | --- |
| `stageId` | String like `'mypack:tier2'` |
| `entries` | Array of strings **or** objects `{ id: '…', jei: false, … }` |
| `restrictions` | Optional stage-wide flag defaults, e.g. `{ jei: false, inventory: false }` |

### Entry prefixes

| You write | Meaning |
| --- | --- |
| `'minecraft:diamond'` | One item |
| `'#minecraft:swords'` | Item tag |
| `'@create'` | Every item from that mod (also gates that mod’s fluids/chemicals unless you only use fluid/chemical prefixes) |
| `'fluid:minecraft:lava'` | Fluid (tags: `'fluid:#minecraft:water'`, mods: `'fluid:@create'`) |
| `'chemical:mekanism:hydrogen'` | Mekanism chemical (only indexed when compat is active; off on Minecraft **26** / Chapters **2.x** until Mekanism ships) |
| `'recipe:minecraft:diamond_pickaxe'` | Recipe registry id (vanilla crafting grid + JEI) |
| `'dimension:minecraft:the_nether'` | Dimension (`'dimension:#ns:tag'`, `'dimension:@modid'`) |

Object form: `{ id: 'minecraft:emerald', jei: false, inventory: false }` (alias key `item` also accepted). Starts from stage `restrictions`, then applies only the keys you set.

Many `defineStage` calls in the **same server tick** are batched so indexing stays fast after load.

### Restriction flags in scripts

```js
ServerEvents.loaded(() => {
  // Soft copper: hold + JEI visible, no craft; raw copper free to pick up
  ChaptersEvents.defineStage(
    'mypack:copper_era',
    [
      'minecraft:copper_ingot',
      { id: 'minecraft:raw_copper', pickup: false }
    ],
    { pickup: true, inventory: false, craft: true, jei: false }
  )

  // Quest tease: show netherite in JEI, still hard-lock gameplay
  ChaptersEvents.defineStage(
    'mypack:tease_netherite',
    ['minecraft:netherite_ingot'],
    { jei: false }
  )

  // Fluid with transfer off (still a defining stage / JEI hide if jei true)
  ChaptersEvents.defineStage('mypack:water_sight', [
    { id: 'fluid:minecraft:water', transfer: false, jei: true }
  ])
})
```

Full flag table, multi-stage OR unlock, and pitfalls: [[Restriction-flags]].

### Clearing a runtime stage

```js
ChaptersEvents.clearStage('mypack:tier1')
```

---

## `PlayerStages.of(player)`

| Method | Use |
| --- | --- |
| `.has(stageId)` | boolean check |
| `.get()` | all effective stage ids for this player |
| `.add(stageId)` | grant |
| `.remove(stageId)` | revoke |

```js
PlayerStages.of(player).add('mypack:tier1')

if (PlayerStages.of(player).has('mypack:tier2')) {
  // ...
}
```

`player` depends on the KubeJS event you handle.

With **FTB Teams** and `syncWithFtbTeams = true`, these methods read/write the **team** store (party shares unlocks). With sync off or no Teams, they use the per-player attachment. Pair with FTB Quests Custom Reward: [[FTB-integration]].

---

## Multi-stage unlock

If two `defineStage` calls list the same item, owning **either** stage unlocks it completely. Flags from both apply with OR **only while the player owns neither**. Do not list an id with all flags `false` just to document it; that still becomes an unlock key for other packs. See [[Restriction-flags]].

---

## Starter scripts

Copy into **`kubejs/server_scripts/`**:

- Minecraft **26**: [examples/kubejs on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs)
- Minecraft **1.21**: [examples/kubejs on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs)

More copy-paste patterns: [[Examples]].
