# Examples

Each block is one pack decision. Stage ids are yours: rename `mypack:` to your pack.

Grant a stage with `/chapters add <player> <stage>`, with `PlayerStages.of(player).add('…')`, or with an FTB Quests Stage Reward ([[FTB-integration]]).

Scripts go in `kubejs/server_scripts/`. Reload with `/kubejs reload server-scripts` (or a restart). `ServerEvents.loaded` does not run on `/reload` or `/chapters reload`.

Repo copies: [KubeJS on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs) · [on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/kubejs).

---

## One item

Lock netherite until the stage exists.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:netherite', ['minecraft:netherite_ingot'])
})
```

Until the player has `mypack:netherite`, the ingot is hidden in JEI, cannot be picked up, is dropped from the inventory, and will not craft on the vanilla grid.

```
/chapters add Steve mypack:netherite
```

---

## A whole mod

Lock Create, and keep the Nether closed, until the iron age.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:iron_age', [
    '@create',
    'dimension:minecraft:the_nether'
  ])
})
```

`@create` covers that mod's items and fluids (and Mekanism chemicals, on 1.21). Dimensions need their own line. Create machines can still craft other unlocked items: see [[Limits]].

---

## Three tiers

Three items, three stages. Owning the early stage does not open the later items.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:tier_early', ['minecraft:diamond'])
  ChaptersEvents.defineStage('mypack:tier_mid', ['minecraft:emerald'])
  ChaptersEvents.defineStage('mypack:tier_late', ['minecraft:golden_apple'])
})
```

With none of them, all three items are locked. With only `mypack:tier_early`, diamond works. Emerald and the golden apple stay locked.

If you list the **same** item on two stages, owning **either** stage unlocks it completely. See [[Restriction-flags]].

---

## A tag

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:swords', ['#minecraft:swords'])
})
```

Every item in `#minecraft:swords` at the time the stage is loaded. `/chapters reload` picks up a datapack tag change. A tag added by a KubeJS script needs that script to reload first.

---

## A fluid

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:no_lava', ['fluid:minecraft:lava'])
})
```

The player cannot place lava or move it with a bucket or a tank. JEI hides it. A pipe can still move it ([[Limits]]).

Fluid only, without the mod's items: `'fluid:@create'`. The bare `'@create'` form locks items and fluids together.

---

## One recipe

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:no_diamond_pick', [
    'recipe:minecraft:diamond_pickaxe'
  ])
})
```

That recipe id will not craft on the vanilla grid, and JEI hides it. A diamond pickaxe obtained another way still works. To lock the item too, add `'minecraft:diamond_pickaxe'` on this stage or another one.

Other machines are not covered by a recipe lock. See [[Limits]].

---

## Dimensions

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:late_dims', [
    'dimension:minecraft:the_nether',
    'dimension:minecraft:the_end'
  ])
})
```

Portals and `/tp` are cancelled. A player already inside is sent to the Overworld spawn on login, on respawn, and when the stage is removed. If the Overworld is locked too, they stay where they are (so two locked dimensions cannot bounce forever).

`'dimension:@aether'` locks every loaded dimension from that namespace. An item `@mod` line does not.

---

## Mekanism hydrogen

Minecraft **1.21** only, with Mekanism installed. On Minecraft **26** the line is accepted and locks nothing.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('mypack:hydrogen', ['chemical:mekanism:hydrogen'])
})
```

The player cannot move that chemical by hand. A tube still can. See [[Limits]].

---

## Visible in JEI, still locked

The default hides the item. This one leaves it in JEI so a quest can point at it. Pickup, the inventory drop, and vanilla crafting stay on.

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage(
    'mypack:tease_netherite',
    ['minecraft:netherite_ingot'],
    { hideInJei: false }
  )
})
```

More mixes (holdable, not craftable, one item inside a tag): [[Restriction-flags]].

---

## Grant from a script

Built-in FTB Quests Stage Reward does not need this. Use a script when the grant depends on something else.

```js
PlayerStages.of(player).add('mypack:iron_age')

if (PlayerStages.of(player).has('mypack:tier_early')) {
  PlayerStages.of(player).add('mypack:tier_mid')
}
```

`player` comes from the KubeJS event you are in. A full FTB Custom Reward sample is on [[FTB-integration]].
