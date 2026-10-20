# Datapack stages

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
| `restrictions` | Optional stage-wide defaults for lock behaviours (all `true` if omitted). See below. |

List entries may be plain strings (legacy) or objects with an `id` plus per-entry overrides, e.g. `{ "id": "minecraft:emerald", "jei": false }`.

### Restriction flags

Every gated content id inherits these behaviours while the player lacks the stage. Defaults are all **`true`** (same as older Chapters builds). Set a flag to `false` on the stage (`restrictions`) or on one entry to disable that behaviour for that content.

| Flag | Effect |
| --- | --- |
| `pickup` | Block picking the item up from the world |
| `inventory` | Auto-drop the item from the player inventory |
| `craft` | Block crafting the item (or a locked recipe id) on the vanilla crafting grid |
| `jei` | Hide the ingredient / recipe in JEI |
| `transfer` | Block fluid / chemical tank transfers (and related bucket place/fill) |
| `travel` | Block travel into a locked dimension |

Example: lock diamond fully, but keep emerald visible in JEI and allow carrying it:

```json
{
  "restrictions": {
    "pickup": true,
    "inventory": true,
    "craft": true,
    "jei": true
  },
  "items": [
    "minecraft:diamond",
    { "id": "minecraft:emerald", "jei": false, "inventory": false }
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

If **different** stage JSON files (or KubeJS `defineStage` calls) all mention the same item, fluid, recipe, or dimension, the player must satisfy **every** such definition: they need at least one allowed stage **from each** file that lists that thing. That way two packs can both add rules without silently undoing each other.

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

A working **`pack.mcmeta`** plus several example stages:

- Minecraft **26**: [examples/datapack/tutorial on `26`](https://github.com/GabinFqt/chapters/tree/26/examples/datapack/tutorial)
- Minecraft **1.21**: [examples/datapack/tutorial on `main`](https://github.com/GabinFqt/chapters/tree/main/examples/datapack/tutorial)

Copy the whole `tutorial` folder into `saves/<your world>/datapacks/`.
