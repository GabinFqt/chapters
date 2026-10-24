# Datapack stages

Optional alternative to **[[KubeJS]]** (recommended for most packs). Datapack JSON uses the same stage ids, list prefixes (`#`, `@`), and [[Restriction-flags]]. A KubeJS `defineStage` with the **same id** replaces the datapack definition.

## Folder layout

Inside your datapack ZIP or folder:

```
data/<namespace>/chapters/stages/<stage_name>.json
```

The in-game stage id becomes **`<namespace>:<stage_name>`** (same as the JSON filename without `.json`).

**Example:** `data/mypack/chapters/stages/iron_age.json` → stage **`mypack:iron_age`**.

Activate the datapack on your world (**Create / Edit → Data packs**), then **`/reload`**.

---

## Keys in each JSON file

Anything you don’t need can be omitted.

| Key | What it gates |
| --- | --- |
| `items` | Item ids (`minecraft:diamond`), item tags (**`#`**`minecraft:swords`), or an entire mod’s items (**`@`**`modid`). For **`@`** mod prefixes, fluids and Mekanism chemicals from that mod are also gated unless you only add extras via `fluid_namespaces` / `chemical_namespaces`. |
| `fluids` | Fluid ids, **`#`** fluid tags, or **`@`** every fluid from a mod |
| `chemicals` | Mekanism-only: chemical ids/tags/**`@`**mods. Harmless JSON if Mekanism is not installed. On **Minecraft 26**, chemical gating in Chapters **2.x** is compiled out until a Mekanism 26 build exists. |
| `recipes` | Recipe **registry ids**: `minecraft:diamond_pickaxe` (no **`#`** / **`@`**). Locks that recipe **on the vanilla crafting grid** when the player lacks a matching stage. See [[JEI-and-limitations]]. |
| `dimensions` | Dimension ids (`minecraft:the_nether`), **`#`** dimension tags, or **`@`** every loaded dimension from a mod. Travel into a locked dimension is cancelled; players already inside are returned to the Overworld. Item **`@`** does **not** lock dimensions. |
| `namespaces` | List of mod ids; same broad effect as adding **`@`** for items **and** fluids **and** chemicals for each |
| `fluid_namespaces` | Extra mod ids that only add fluid locks |
| `chemical_namespaces` | Extra mod ids that only add chemical locks |
| `restrictions` | Optional stage-wide defaults for lock behaviours (all `true` if omitted). Full guide: [[Restriction-flags]]. |

List entries may be plain strings (legacy) or objects with an `id` (or `item`) plus per-entry flag overrides, e.g. `{ "id": "minecraft:emerald", "hideInJei": false }`.

### Restriction flags (summary)

Defaults are all **`true`** (same as older Chapters builds). Set a flag to `false` on the stage (`restrictions`) or on one entry to disable that behaviour for that content. Flags are **independent**: `dropFromInventory: false` does not turn off `preventPickup` or `preventCraft`.

| Flag | Effect while the content is still locked |
| --- | --- |
| `preventPickup` | Block picking the item up from the world |
| `dropFromInventory` | Auto-drop the item from the player inventory |
| `preventCraft` | Block crafting the item (or a locked recipe id) on the vanilla crafting grid |
| `hideInJei` | Hide the ingredient / recipe in JEI |
| `preventTransfer` | Block fluid / chemical tank transfers (and related bucket place/fill) |
| `preventTravel` | Block travel into a locked dimension |

Only some flags matter for each list type (e.g. `preventTravel` only affects `dimensions`). For the full mental model, overrides vs tags, multi-stage OR, and recipes, see **[[Restriction-flags]]**.

Example: lock diamond fully, but keep emerald visible in JEI and allow carrying it:

```json
{
  "restrictions": {
    "preventPickup": true,
    "dropFromInventory": true,
    "preventCraft": true,
    "hideInJei": true
  },
  "items": [
    "minecraft:diamond",
    { "id": "minecraft:emerald", "hideInJei": false, "dropFromInventory": false }
  ]
}
```

## Prefix quick reference

| You write | Meaning |
| --- | --- |
| `minecraft:iron_ingot` | One specific id |
| `#minecraft:swords` | Everything in that tag (context: item / fluid / chemical / dimension list) |
| `@create` | Everything from that mod in that list’s category (and combined rules above for `items`) |

## When the same item appears in more than one file

If **different** stage JSON files (or KubeJS `defineStage` calls) all mention the same item, fluid, recipe, or dimension:

1. **Unlock** is OR: owning **any one** of those stages unlocks the content for **every** behaviour.
2. **While locked** (owns none): each behaviour is blocked if **any** listing enables that flag.

Consequence: a second pack cannot “soften” another pack’s lock by listing the same id with flags set to `false`. Listing an id at all still counts as a defining stage for unlock. Prefer distinct stage ids per pack when you need separate progression tracks.

Within a **single** stage file, an explicit id entry **overrides** flags that came from a tag or namespace expansion for that same id. Details: [[Restriction-flags]].

---

## Full example (one stage)

```json
{
  "items": [
    "minecraft:netherite_ingot",
    "#minecraft:swords",
    "minecraft:enchanted_book"
  ],
  "fluids": [
    "minecraft:lava",
    "#minecraft:water"
  ],
  "chemicals": [
    "mekanism:hydrogen",
    "#mekanism:gases"
  ],
  "recipes": [
    "minecraft:diamond_pickaxe"
  ],
  "dimensions": [
    "minecraft:the_nether",
    "minecraft:the_end"
  ]
}
```

## Template datapack to copy

A working **`pack.mcmeta`** plus several example stages (including granular flags):

- Minecraft **26**: [examples/datapack/tutorial on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/datapack/tutorial)
- Minecraft **1.21**: [examples/datapack/tutorial on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/datapack/tutorial)

Copy the whole `tutorial` folder into `saves/<your world>/datapacks/`.
