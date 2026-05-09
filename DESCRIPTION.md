# Chapters

**Chapters** is a progression mod for **NeoForge 1.21.1** inspired by *GameStages* and *ItemStages*. It lets you split your modpack into named "chapters" (a.k.a. stages) and gate **items, fluids, Mekanism chemicals, and recipes** behind them — so players literally cannot interact with locked content until you flip the switch.

It is designed for pack authors who want a **single, well-integrated tool** to drive progression on modern NeoForge, with first-class support for **datapacks**, **KubeJS**, **JEI**, and **Mekanism**.

---

## What you can lock

- **Items** — by id, by tag (`#minecraft:swords`), or by entire mod (`@create`).
- **Fluids** — by id, by tag, or by mod. Buckets, vanilla placement, and NeoForge fluid utility transfers are all checked.
- **Mekanism chemicals** — gas / infusion / slurry / pigment, by id, tag, or mod. Insertion / extraction is gated on Mekanism's `ChemicalUtils` paths when a server player is in scope.
- **Recipes** — by recipe id (`minecraft:diamond_pickaxe`, KubeJS `recipe:…`, etc.). Vanilla crafting-grid recipes are blocked server-side until unlocked.
- **A whole mod at once** — `@modid` in a stage covers items + fluids + chemicals from that namespace in one line.

When a player has not unlocked a chapter yet, locked items also get **auto-dropped from their inventory** (every second while online, and whenever a stage is removed or definitions reload). No more "I sneaked the item into a shulker before the wipe".

---

## Recipe viewer integration (JEI)

When **JEI** is installed, Chapters pushes client-side stage updates so JEI can:

- hide locked **ingredients** entirely,
- hide **output-focused recipes** for locked items / fluids / Mekanism chemicals (using `TYPE_CHEMICAL` when both mods are present),
- hide any recipe matching a **locked recipe id** across recipe types,
- and reconcile back when a stage is granted (via `includeHidden()` on focus lookups).

The result: players only see what they can actually use right now.

---

## Commands

```
/chapters add <player> <stage>
/chapters remove <player> <stage>
/chapters list <player>
/chapters check <player> <stage>
/chapters reload
```

---

## Datapack stages

Drop JSON files at:

```
data/<namespace>/chapters/stages/<stage_id>.json
```

Example (`data/mypack/chapters/stages/tier1.json`):

```json
{
  "items": [
    "minecraft:netherite_ingot",
    "#minecraft:swords",
    "minecraft:enchanted_book"
  ],
  "fluids": [
    "minecraft:lava",
    "#minecraft:water",
    "@mymodfluids"
  ],
  "chemicals": [
    "mekanism:hydrogen",
    "#mekanism:gases",
    "@mekanism"
  ],
  "recipes": [
    "minecraft:diamond_pickaxe"
  ]
}
```

### Syntax cheatsheet

| Prefix | Meaning |
| --- | --- |
| `minecraft:foo` | Single id |
| `#namespace:tag` | Tag (works for items, fluids, chemicals) |
| `@modid` | Every item **and** fluid **and** Mekanism chemical from that mod |

Optional top-level keys: `namespaces` (same as `@mod`, but for the whole stage), `fluid_namespaces` (extra mods, fluids only), `chemical_namespaces` (extra mods, Mekanism chemicals only).

If an ingredient appears in **any** stage definition, a player needs **at least one matching stage among every definition that mentions it** — so multiple datapacks / KubeJS scripts can layer rules without fighting each other.

---

## KubeJS integration

Chapters registers a KubeJS plugin and bindings for runtime stage definitions and per-player checks:

```js
ServerEvents.loaded(event => {
  ChaptersEvents.defineStage(
    'mypack:tier1',
    [
      'minecraft:netherite_ingot',
      '#minecraft:swords',
      'recipe:minecraft:diamond_pickaxe',
      'fluid:minecraft:lava',
      'chemical:mekanism:hydrogen',
      '@create'
    ]
  )
})
```

Bindings:

- `ChaptersEvents.defineStage(stageId, entries)` — `entries` is a JS array of strings; multiple `defineStage` calls in the same tick are **batched into a single index rebuild**.
- `PlayerStages.of(player).add(stageId)`
- `PlayerStages.of(player).remove(stageId)`
- `PlayerStages.of(player).has(stageId)`
- `PlayerStages.of(player).get()` — returns the current stage list

---

## Quick start (in-game)

The jar **does not ship preset stages** — you write your own progression. The fastest way to verify it works:

1. Add a JSON datapack stage like `data/test/chapters/stages/tutorial.json` gating `minecraft:netherite_ingot`.
2. `/reload`
3. `/chapters check Dev test:tutorial` → should be `false`.
4. `/give Dev minecraft:netherite_ingot 1` → blocked / dropped.
5. `/chapters add Dev test:tutorial` → item now usable, JEI reveals it.

Sample KubeJS scripts live under [`examples/kubejs/`](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs) on the GitHub repo — copy them into your instance's `kubejs/` folder.

---

## Compatibility

| Mod | Status |
| --- | --- |
| **NeoForge 1.21.1** (21.1.x) | Required |
| **Java 21** | Required |
| **JEI** | Optional — locked content is hidden client-side when present |
| **KubeJS** | Optional — bindings registered automatically when present |
| **Mekanism** | Optional — chemicals can be locked when present; without Mekanism, `chemicals` / `chemical_namespaces` entries are accepted in JSON but build no index |

Other workstations (smithing tables, machines) are **not** covered by the recipe-blocking mixin yet — those still hide in JEI when locked client-side, but the server-side block currently targets the vanilla crafting grid (`CraftingMenu`).

---

## Source code & issues

- **GitHub:** [github.com/GabinFqt/chapters](https://github.com/GabinFqt/chapters)
- **Releases:** [github.com/GabinFqt/chapters/releases](https://github.com/GabinFqt/chapters/releases)
- **License:** MIT

Issues, suggestions, and pull requests are very welcome.
