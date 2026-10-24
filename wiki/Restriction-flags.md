# Restriction flags

Pack authors can toggle **which behaviours** a locked stage enforces. Defaults match older Chapters builds: **every flag is `true`**, so a stage that only lists `'minecraft:diamond'` still fully gates that diamond.

Flag names describe what is **turned on** while the content stays locked (`preventPickup: true` = pickup blocked, `hideInJei: true` = hidden in JEI).

**Preferred authoring path: [[KubeJS]]** (`ChaptersEvents.defineStage`). Datapack JSON supports the same flags; see [[Stages-datapack]] if you ship stages without scripts.

Flags live on:

1. **Stage-wide defaults**: third argument of `defineStage(id, entries, restrictions)`, or the `"restrictions"` object in a datapack file.
2. **Per-entry overrides**: objects in the entries array, e.g. `{ id: 'minecraft:emerald', hideInJei: false }`.

Missing keys leave the base value unchanged.


---

## Flag reference

| Flag | Typical content | What it does while locked (`true`) |
| --- | --- | --- |
| `preventPickup` | Items | Blocks picking the item up from the world |
| `dropFromInventory` | Items (and filled buckets of gated fluids) | Auto-drops the item from the player inventory (auditor) |
| `preventCraft` | Items **or** recipe ids | Blocks vanilla crafting-grid result / recipe |
| `hideInJei` | Items, fluids, chemicals, recipes | Hides the content in JEI (client) |
| `preventTransfer` | Fluids, chemicals | Blocks tank transfers, bucket place/fill, related fluid util paths |
| `preventTravel` | Dimensions | Blocks entering the dimension / ejects players already inside |

Flags are **independent**. Turning `dropFromInventory` off does **not** turn off `preventPickup` or `preventCraft`. A common pattern is “can hold, cannot craft, still visible in JEI”:

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage(
    'mypack:soft_copper',
    ['minecraft:copper_ingot'],
    { preventPickup: true, dropFromInventory: false, preventCraft: true, hideInJei: false }
  )
})
```

Tutorial equivalents:

- KubeJS samples: [examples/kubejs on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs) / [on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs)
- Optional datapack twin: [`granular_emerald.json`](https://github.com/GabinFqt/chapters/blob/main/examples/datapack/tutorial/data/tutorial/chapters/stages/granular_emerald.json)

---

## Mental model (read this once)

Content is gated by **edges**: `contentId → (stageId → flags)`.

```
While the player owns NONE of the stages that list this content:
  a behaviour is blocked if ANY of those stages has that flag true.

As soon as the player owns ANY one of those stages:
  ALL behaviours unlock for that content (every flag stops applying).
```

So:

- Unlock is **OR across stages** (one stage is enough).
- Restriction while locked is **OR across flags** (any listing that enables the flag blocks).
- You **cannot** soften another pack’s hard lock by listing the same id with flags set to `false`. Listing an id still registers that stage as a defining unlock key.

Prefer **distinct stage ids** per pack when you need separate progression tracks for the same item.

---

## Which flags matter for which entry kinds?

Every entry accepts the six flag names, but **only the matching handler reads them**:

| Entry kind (KubeJS prefix) | Flags that actually do something |
| --- | --- |
| Item (`'minecraft:…'`, `'#tag'`, `'@mod'`) | `preventPickup`, `dropFromInventory`, `preventCraft`, `hideInJei` |
| Fluid (`'fluid:…'`) | `preventTransfer`, `hideInJei` (filled buckets inherit fluid gates for inventory / JEI bucket hide) |
| Chemical (`'chemical:…'`, when Mekanism compat is active) | `preventTransfer`, `hideInJei` |
| Recipe (`'recipe:…'`) | `preventCraft`, `hideInJei` |
| Dimension (`'dimension:…'`) | `preventTravel` |

Examples of no-ops that still parse cleanly:

- `{ id: 'minecraft:diamond', preventTravel: false }` does nothing useful (`preventTravel` is for dimensions).
- `{ id: 'dimension:minecraft:the_end', preventPickup: false }` does nothing useful.
- A dimension with `{ id: 'dimension:minecraft:the_nether', preventTravel: false }`: still a defining unlock key, but travel is allowed.

---

## Stage defaults vs per-entry overrides (KubeJS)

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage(
    'mypack:copper_era',
    [
      'minecraft:copper_ingot',
      { id: 'minecraft:raw_copper', preventPickup: false }
    ],
    { hideInJei: false, dropFromInventory: false }
  )
})
```

- String entries inherit the stage defaults (`copper_ingot`: hideInJei false, dropFromInventory false, preventPickup true, …).
- Object entries start from those defaults, then apply only the keys you set (`raw_copper` keeps hideInJei/dropFromInventory false, and sets preventPickup false). Use `id` or `item`.

### Explicit id vs tag / namespace (same stage)

Within **one** `defineStage` call, Chapters expands tags and namespaces first, then applies **explicit ids**, which **replace** flags for that id on that stage.

```js
ChaptersEvents.defineStage('mypack:planks', [
  '#minecraft:planks',
  { id: 'minecraft:oak_planks', preventPickup: false }
])
```

`oak_planks` gets `preventPickup: false`. Other planks from the tag keep the stage defaults (usually all true).

Across **different** stages that list the same id, flags still **OR** while locked, and owning either stage unlocks everything.

---

## Recipes vs item craft

```js
// Block crafting the diamond item as a result
ChaptersEvents.defineStage('mypack:no_diamond_craft', ['minecraft:diamond'], { preventCraft: true })

// Block one vanilla recipe id even if the output item is unlocked elsewhere
ChaptersEvents.defineStage('mypack:no_pick_recipe', ['recipe:minecraft:diamond_pickaxe'], { preventCraft: true })
```

- `preventCraft: false` means Chapters will not block that path (JEI may still hide if `hideInJei` stays true).
- Other machines (smithing, mod crafters) are **not** covered the same way. See [[JEI-and-limitations]].

---

## Fluids, buckets, transfer

```js
ChaptersEvents.defineStage('mypack:fluids', [
  'fluid:minecraft:lava',
  { id: 'fluid:minecraft:water', preventTransfer: false, hideInJei: true }
])
```

With a locked fluid and `preventTransfer: true` (default):

- Players cannot freely fill/empty / transfer that fluid through Chapters’ fluid gates.
- Filled buckets are treated as locked for **inventory** drops when the fluid has `dropFromInventory`.
- JEI can hide the fluid and its bucket when `hideInJei` is true.

With `preventTransfer: false`:

- The fluid can still appear in the lock index (owning a listing stage still unlocks it).
- Bucket place/fill and tank transfers are **not** blocked by Chapters for that fluid.
- Client prediction (Minecraft **26** bucket mixins) follows the same `preventTransfer` rule as the server.

Automation / null-player fluid moves are intentionally less gated than player GUI transfers. Chemical GUI transfers follow the same idea when Mekanism compat is on (`'chemical:…'` entries).

---

## JEI vs gameplay

`hideInJei: false` means: **do not hide** this content in JEI, while other flags can still gate gameplay.

| Situation | Gameplay | JEI |
| --- | --- | --- |
| Owns no defining stage; at least one listing has `hideInJei: true` | Restricted per other flags | Hidden |
| Owns a stage that listed the item (even with `hideInJei: false`) | Fully unlocked | Shown |
| Only listings have `hideInJei: false` and player owns none | Other flags may still block | Stays visible |

Details: [[JEI-and-limitations]].

---

## Common recipes (KubeJS)

### Soft gate: visible, holdable, not craftable

```js
ChaptersEvents.defineStage(
  'mypack:soft_diamond',
  ['minecraft:diamond'],
  { dropFromInventory: false, hideInJei: false, preventCraft: true, preventPickup: true }
)
```

Players can see and keep diamonds (e.g. from `/give`), but cannot craft them on the vanilla grid. Adjust `preventPickup` if ground loot should be allowed.

### Quest tease: show in JEI, block everything else

```js
ChaptersEvents.defineStage(
  'mypack:tease_netherite',
  ['minecraft:netherite_ingot'],
  { hideInJei: false }
)
```

JEI shows the item; pickup / inventory drop / craft stay enforced (defaults true). Grant with `PlayerStages.of(player).add('mypack:tease_netherite')` from a quest script.

### Dimension late-game only

```js
ChaptersEvents.defineStage('mypack:endgame', ['dimension:minecraft:the_end'])
```

All defaults: travel blocked until the stage is granted.

### Do not use a “flags all false” stage to unlock someone else’s lock

```js
ChaptersEvents.defineStage(
  'mypack:docs_only',
  ['minecraft:diamond'],
  {
    preventPickup: false, dropFromInventory: false, preventCraft: false,
    hideInJei: false, preventTransfer: false, preventTravel: false
  }
)
```

This still registers your stage as a defining stage for diamond. Anyone who obtains **your** stage unlocks diamond for packs that also listed it with hard flags. Prefer not listing the id at all if you only want documentation.

---

## Granting unlocks from scripts

```js
PlayerStages.of(player).add('mypack:soft_diamond')
PlayerStages.of(player).remove('mypack:soft_diamond')
```

With FTB Teams + `syncWithFtbTeams`, these hit the team store. See [[KubeJS]] and [[FTB-integration]].

---

## Config note

Restriction flags are **stage definition data** (KubeJS / datapack). They are unrelated to `syncWithFtbTeams` in `config/chapters-common.toml` (that only chooses player vs team **storage** for unlocks).
