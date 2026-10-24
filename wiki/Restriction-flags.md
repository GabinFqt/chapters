# Restriction flags

Flags choose **which** behaviours stay on while the player does not have the stage. If you omit them, every flag is `true`: the lock is full. That is what you want for almost every stage.

Set a flag to `false` to turn that one behaviour off. Flags do not imply each other. `dropFromInventory: false` leaves pickup and crafting blocked.

KubeJS:

```js
ChaptersEvents.defineStage(stageId, entries, restrictions)
```

Datapack: a `"restrictions"` object, and the same keys on one entry. See [[Stages-datapack]].

---

## Two patterns that cover most packs

### Show it in JEI, keep it unusable

Quest teaser. Pickup, inventory drop, and vanilla crafting stay on.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage(
    'mypack:tease_netherite',
    ['minecraft:netherite_ingot'],
    { hideInJei: false }
  )
})
```

### Let them hold it, still no crafting

`/give` and loot stay in the inventory. The vanilla grid still refuses it. JEI still shows it.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage(
    'mypack:soft_copper',
    ['minecraft:copper_ingot'],
    {
      preventPickup: true,
      dropFromInventory: false,
      preventCraft: true,
      hideInJei: false
    }
  )
})
```

Turn `preventPickup` off too if ground loot should be pickable.

---

## Flag reference

| Flag | Applies to | While locked and `true` |
| --- | --- | --- |
| `preventPickup` | Items | No picking the item up off the ground |
| `dropFromInventory` | Items, and filled buckets of a locked fluid | Dropped from the inventory, about once a second, and again when the stage is removed |
| `preventCraft` | Items, recipe ids | The vanilla crafting grid will not produce it |
| `hideInJei` | Items, fluids, chemicals, recipes | Hidden in JEI |
| `preventTransfer` | Fluids, chemicals | The player cannot place, fill, or transfer it (buckets and tanks) |
| `preventTravel` | Dimensions | Travel is cancelled, and a player already inside is sent back to the Overworld |

Other machines and pipes: [[Limits]].

---

## Read this once

A lock is a list of stages that mention a given item, fluid, recipe, or dimension.

While the player has **none** of those stages, a behaviour is blocked if **any** of the stages has that flag on.

As soon as the player has **any one** of those stages, the content is fully open. Every flag stops applying, including JEI.

So:

- One stage is enough to unlock.
- While still locked, one stage with the flag on is enough to block.
- A second stage that lists the same id with flags set to `false` does **not** soften the first stage. It adds another key that unlocks everything.

```js
// Anyone who receives mypack:docs unlocks diamond,
// including packs that locked it for real.
ChaptersEvents.defineStage(
  'mypack:docs',
  ['minecraft:diamond'],
  {
    preventPickup: false,
    dropFromInventory: false,
    preventCraft: false,
    hideInJei: false,
    preventTransfer: false,
    preventTravel: false
  }
)
```

Leave the id out if you do not want it to be an unlock. Use a different stage id per pack when two packs gate the same item on purpose.

---

## Which flags actually run

Every entry accepts every flag name. Handlers ignore the ones that are not theirs.

| Entry | Flags that do something |
| --- | --- |
| Item (`'minecraft:…'`, `'#tag'`, `'@mod'`) | `preventPickup`, `dropFromInventory`, `preventCraft`, `hideInJei` |
| Fluid (`'fluid:…'`) | `preventTransfer`, `hideInJei`. Filled buckets follow the fluid for the inventory drop and for JEI |
| Chemical (`'chemical:…'`, 1.21 with Mekanism) | `preventTransfer`, `hideInJei` |
| Recipe (`'recipe:…'`) | `preventCraft`, `hideInJei` |
| Dimension (`'dimension:…'`) | `preventTravel` |

`preventTravel` on an item does nothing. `preventPickup` on a dimension does nothing. A dimension with `preventTravel: false` is still an unlock key, and travel stays allowed.

---

## Stage defaults and one-off entries

String entries inherit the third argument. An object starts from those defaults and overrides the keys you set.

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

Copper ingots: visible, stay in the inventory, cannot be picked up, cannot be crafted on the vanilla grid. Raw copper: same, and it **can** be picked up.

Inside **one** stage, tags and `@mod` expand first. An explicit id written after them replaces the flags for that id:

```js
ChaptersEvents.defineStage('mypack:planks', [
  '#minecraft:planks',
  { id: 'minecraft:oak_planks', preventPickup: false }
])
```

Oak planks can be picked up. The other planks keep the stage defaults (all on, here).

Across **different** stages, flags still combine while locked, and owning either stage unlocks the id completely.

---

## Recipe id and item craft

```js
// The diamond item cannot be crafted on the vanilla grid.
ChaptersEvents.defineStage('mypack:no_diamond', ['minecraft:diamond'])

// This recipe id cannot craft. The pickaxe item is free if obtained another way.
ChaptersEvents.defineStage('mypack:no_pick_recipe', ['recipe:minecraft:diamond_pickaxe'])
```

`preventCraft: false` leaves the vanilla grid alone. JEI can still hide the entry when `hideInJei` is true.

---

## Fluids

```js
ChaptersEvents.defineStage('mypack:fluids', [
  'fluid:minecraft:lava',
  { id: 'fluid:minecraft:water', preventTransfer: false, hideInJei: true }
])
```

Lava, with the defaults: the player cannot place it or move it, and JEI hides it. Water, here: JEI hides it, and Chapters does not block buckets or tanks. Both are still unlock keys.

Pipes are outside this flag. See [[Limits]].

---

## Where this is stored

Flags live in the stage definition (KubeJS or datapack). They are unrelated to `syncWithFtbTeams` in `config/chapters-common.toml`, which only chooses player storage or team storage for **who has the stage**.
