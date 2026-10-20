# JEI & limitations

## What JEI does when it is installed

Chapters syncs owned stages plus lock / JEI indices to the client so **JEI listings match what this player may use now**:

- Ingredients and outputs tied to locked content are hidden when the `jei` flag is active for that content.
- Any recipe whose **id** you locked is hidden in JEI for that player as well (when `jei` remains true).
- When you unlock a stage, listings catch up automatically (stage delta + index sync).

If JEI is not installed, progression still works server-side; you just don’t get the filtered recipe viewer.

Works on both **Minecraft 1.21** (Chapters **1.x**) and **Minecraft 26** (Chapters **2.x**).

---

## The `jei` flag vs gameplay

Restriction flags are documented in [[Restriction-flags]]. Short version for JEI:

| `jei` value | Meaning |
| --- | --- |
| `true` (default) | This stage contributes to JEI hiding for that content while the player is still locked |
| `false` | This stage does **not** ask JEI to hide the content; other flags (`pickup`, `craft`, …) can still gate gameplay |

Unlock rule (same as gameplay): owning **any** stage that lists the content unlocks **everything**, including JEI visibility. Chapters does **not** keep an item hidden in JEI after you unlocked it via a `jei: false` stage.

While still locked, hide happens only if **at least one** defining stage had `jei: true`.

### Practical patterns (KubeJS)

**Show in JEI, still hard-lock gameplay** (quest tease):

```js
ChaptersEvents.defineStage(
  'mypack:tease_netherite',
  ['minecraft:netherite_ingot'],
  { jei: false }
)
```

**Hide in JEI and block craft, but allow carrying** (`inventory: false`):

```js
ChaptersEvents.defineStage(
  'mypack:hold_diamond',
  ['minecraft:diamond'],
  { inventory: false, craft: true, jei: true }
)
```

More recipes: [[Restriction-flags]] · [[KubeJS]].

---

## Vanilla crafting grid vs other machines

Chapters stops **blocked recipe ids and craft-gated outputs on the vanilla 2×2 / 3×3 crafting grid** (`CraftingMenu` + craft undo): if a recipe or result is gated with `craft: true` and the player lacks a defining stage, the result should not craft there.

Other stations (**smithing, mod machines**, etc.) **are not guarded the same way** today. Someone might craft an item elsewhere even if **JEI** hides the vanilla recipe entry. Combine with item / fluid locks, `inventory` drops, or quest-gated workshop access if you need a hard barrier.

Recipe-id locks (`"recipes": ["minecraft:diamond_pickaxe"]`) only affect the vanilla grid + JEI recipe hide for that id. They do not rewrite every mod’s machine recipes.

---

## Fluids and buckets in JEI

When a fluid is JEI-hidden, Chapters also hides the corresponding filled **bucket** item in JEI when it can resolve that mapping. Gameplay bucket place/fill follows the `transfer` flag (see [[Restriction-flags]]), not `jei`.

---

## Mekanism + JEI

With **JEI**, **Chapters**, and **Mekanism** together (when chemical compat is active), Mekanism chemical-heavy recipes are wired into hiding logic so JEI doesn’t spotlight locked chemistry prematurely.

On **Minecraft 26** (Chapters **2.x**), chemical gating is not compiled in yet until Mekanism has a matching release. Item / fluid / recipe locks still apply.

---

## Sync / dedicated servers

JEI filtering needs the **client** to have Chapters installed and to receive:

1. The player’s effective owned stages.
2. The lock indices and JEI-filtered indices from the server (datapack + KubeJS definitions).

Mismatch jars client/server, or a failed sync after a big `/chapters reload`, usually shows as “JEI still shows locked stuff” or the opposite. Reconnect or toggle a stage with `/chapters add` / `remove` to force a refresh. See [[Troubleshooting]].

---

## Useful commands while tuning

| Command | Purpose |
| --- | --- |
| `/chapters list <player>` | See stages on a player |
| `/chapters reload` | Refresh Chapters’ internal caches after datapack / script tweaks |
| `/reload` | Full datapack reload (vanilla/op) |
