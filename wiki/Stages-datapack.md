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
| `chemicals` | Mekanism-only: chemical ids/tags/**`@`**mods. Harmless JSON if Mekanism is not installed. |
| `recipes` | Recipe **registry ids**: `minecraft:diamond_pickaxe` (no **`#`** / **`@`**). Locks that recipe **on the vanilla crafting grid** when the player lacks a matching stage plus other rules — see [[JEI-and-limitations]]. |
| `namespaces` | List of mod ids; same broad effect as adding **`@`** for items **and** fluids **and** chemicals for each |
| `fluid_namespaces` | Extra mod ids that only add fluid locks |
| `chemical_namespaces` | Extra mod ids that only add chemical locks |

## Prefix quick reference

| You write | Meaning |
| --- | --- |
| `minecraft:iron_ingot` | One specific id |
| `#minecraft:swords` | Everything in that tag (context: item / fluid / chemical list) |
| `@create` | Everything from that mod in that list’s category (and combined rules above for `items`) |

## When the same item appears in more than one file

If **different** stage JSON files (or KubeJS `defineStage` calls) all mention the same item, fluid, or recipe, the player must satisfy **every** such definition: they need at least one allowed stage **from each** file that lists that thing. That way two packs can both add rules without silently undoing each other.

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
  ]
}
```

## Template datapack to copy

A working **`pack.mcmeta`** plus several example stages lives here (copy the whole `tutorial` folder into `saves/<your world>/datapacks/`):

**[examples/datapack/tutorial on GitHub](https://github.com/GabinFqt/chapters/tree/main/examples/datapack/tutorial)**
