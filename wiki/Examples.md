# Examples

**KubeJS-first.** Works with **Chapters** on **Minecraft 1.21** and **26**. Pick stage ids to match your pack (`mypack:…`). Grant with `PlayerStages.of(player).add('…')` or `/chapters add <player> …`.

Starter scripts:

- [examples/kubejs on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs)
- [examples/kubejs on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs)

Optional datapack twins live under `examples/datapack/tutorial` on the same branches ([[Stages-datapack]]).

---

## 1. One item behind a stage

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('tutorial:intro_nether', ['minecraft:netherite_ingot'])
})
```

`/chapters check Steve tutorial:intro_nether` → `false`. After `/chapters add Steve tutorial:intro_nether` (or `PlayerStages.of(player).add(...)`) the ingot works normally again.

---

## 2. Three tiers

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('tutorial:tier_early', ['minecraft:diamond'])
  ChaptersEvents.defineStage('tutorial:tier_mid', ['minecraft:emerald'])
  ChaptersEvents.defineStage('tutorial:tier_late', ['minecraft:golden_apple'])
})
```

With **none** of those stages, the player cannot keep any of those items. With **only** `tier_early`, diamond is allowed; emerald and golden apple stay locked until their stages too.

---

## 3. Tag vs entire mod

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:gated', ['#minecraft:swords', '@create'])
})
```

| Goal | Entry |
| --- | --- |
| All vanilla swords | `'#minecraft:swords'` |
| Broad lock for **Create** | `'@create'` (items + related fluid/chem rules for `@` — see [[KubeJS]]) |

Fluid-only or chemical-only: use `'fluid:@modid'` / `'chemical:@modid'` (or datapack `fluid_namespaces`).

---

## 4. Water and lava

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('tutorial:fluides_base', [
    'fluid:minecraft:water',
    'fluid:minecraft:lava'
  ])
})
```

---

## 5. Lock one recipe

Vanilla grid + JEI: [[JEI-and-limitations]].

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('tutorial:recipe_pickaxe', [
    'recipe:minecraft:diamond_pickaxe'
  ])
})
```

---

## 6. Mekanism hydrogen (optional)

No Mekanism → lines are accepted but **no chemicals** are indexed. On Minecraft **26**, chemical compat is compiled out until Mekanism ships.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('tutorial:mek_h2', ['chemical:mekanism:hydrogen'])
})
```

GUI transfers vs unattended automation can differ. Factor that into hardcore packs.

---

## 7. Granular restriction flags

Full guide: [[Restriction-flags]].

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage(
    'tutorial:granular_emerald',
    [
      'minecraft:copper_ingot',
      { id: 'minecraft:raw_copper', preventPickup: false }
    ],
    { preventPickup: true, dropFromInventory: false, preventCraft: true, hideInJei: false }
  )
})
```

Without the stage:

| Item | Pickup | Keep in inventory | Craft (vanilla grid) | JEI |
| --- | --- | --- | --- | --- |
| Copper ingot | Blocked | Kept (`dropFromInventory: false`) | Blocked | Visible |
| Raw copper | Allowed (`preventPickup: false`) | Kept | Blocked | Visible |

### Soft unlock reminder

If another stage also lists `minecraft:copper_ingot`, owning **either** stage unlocks copper completely (all flags). Do not use an all-false flags stage to “soften” another pack’s lock.

### Grant from a quest script

```js
// e.g. FTB Quests custom reward, advancement reward, …
PlayerStages.of(player).add('tutorial:granular_emerald')
player.tell(Text.green('Copper chapter unlocked!'))
```

---

## 8. Dimension gate

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('tutorial:intro_dims', [
    'dimension:minecraft:the_nether',
    'dimension:minecraft:the_end'
  ])
})
```

---

## Try it locally

Paste an examples `server_scripts` file into **`kubejs/server_scripts/`**, restart / reload KubeJS as you usually do, then `/chapters check` your test player.
