# Stagecraft (NeoForge 1.21.1)

Stagecraft is a progression mod inspired by GameStages + ItemStages.

It lets you:
- lock items behind named stages
- lock fluids (tags, ids, whole mods) behind named stages
- lock Mekanism **chemicals** (unified gas / infusion / slurry / pigment registry) behind named stages when Mekanism is installed
- prevent pickup of locked items
- block crafting of locked items
- auto-drop locked items from the player inventory (every second while online, and when a stage is removed or definitions reload)
- push client stage changes so **JEI** can hide locked entries (optional mod)

## Commands

- `/stagecraft add <player> <stage>`
- `/stagecraft remove <player> <stage>`
- `/stagecraft list <player>`
- `/stagecraft check <player> <stage>`
- `/stagecraft reload`

## Quick test (in-game)

The jar does **not** ship preset stages: add your own JSON under [`Datapack stages`](#datapack-stages), or copy [`examples/kubejs/`](examples/kubejs/) into your instance’s `kubejs/` folder (it registers `stagecraft:script_pickups` and related runtime stages).

With a datapack stage id like `myproject:tutorial` (file `data/myproject/stagecraft/stages/tutorial.json`):

1. `/reload`
2. `/stagecraft check Dev/myproject:tutorial`
3. `/give Dev minecraft:netherite_ingot 1` (if that item is gated in your JSON) → should be blocked until you `/stagecraft add Dev myproject:tutorial`

## Datapack stages

Put JSON files at:

`data/<namespace>/stagecraft/stages/<stage_id>.json`

Example:

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
  ]
}
```

Rules:
- `items`: `#` prefixes are **item tags**; `@` prefixes lock every **item** in that namespace **and** every **fluid** and every **Mekanism chemical** in the same namespace (you can still use `fluid_namespaces` / `chemical_namespaces` for fluids-only or chemicals-only additions); other strings are item ids.
- Optional top-level `namespaces`: same as `@mod` entries — applies to **items, fluids, and Mekanism chemicals** together.
- Optional `fluid_namespaces`: **extra** namespaces that only affect fluids (additive).
- Optional `chemical_namespaces`: **extra** namespaces that only affect Mekanism chemicals (additive).
- Optional `fluids`: `#` prefixes are **fluid tags**; `@` prefixes are mods (every fluid in that namespace); other strings are fluid ids.
- Optional `chemicals` (Mekanism): `#` prefixes are **chemical tags** on Mekanism’s chemical registry; `@` prefixes are mods (every chemical in that namespace); other strings are chemical ids (e.g. `mekanism:hydrogen`).
- If an ingredient appears in any stage definition, a player needs at least one matching stage **among every definition that mentions that ingredient**: for items — use/pickup/crafting; for fluids — transferring with tanks or buckets/vanilla placement (NeoForge fluid util); for chemicals — Mekanism `ChemicalUtils` insert/extract paths when a **server player** is in scope (container slot handling) and the transfer is **not** `AutomationType.EXTERNAL` (tube/pipe automation is not gated the same way).

**Mekanism:** optional dependency; without Mekanism, `chemicals` / `chemical_namespaces` entries are accepted in JSON but build no index until the mod is present.

## KubeJS integration

Stagecraft registers a KubeJS plugin and bindings:

- `StagecraftEvents.defineStage(stageId, ...entries)` — item-style strings (`@mod` also locks that mod’s fluids and Mekanism chemicals), plus `fluid:…` and `chemical:…` overrides in the same call. Definitions are applied **once per server tick** after your script runs so many `defineStage` calls in one `ServerEvents.loaded` callback batch into a single reload (indices are rebuilt once).
- `PlayerStages.of(player).add(stageId)`
- `PlayerStages.of(player).remove(stageId)`
- `PlayerStages.of(player).has(stageId)`
- `PlayerStages.of(player).get()`

Example script:

```js
ServerEvents.loaded(event => {
  StagecraftEvents.defineStage(
    'mypack:tier1',
    'minecraft:netherite_ingot',
    '#minecraft:swords'
  )
})

ServerEvents.commandRegistry(event => {
  // Example usage in your own command callbacks:
  // PlayerStages.of(player).add('mypack:tier1')
})
```

## Recipe viewer (JEI)

Stagecraft integrates optionally with **JEI**: when client stage payloads arrive, locked item / fluid / Mekanism chemical ids are recomputed and hidden in JEI (ingredients + output-focused recipes; Mekanism chemicals use `TYPE_CHEMICAL` when both mods are present), with unlock reconciliation via `includeHidden()` on focus lookups.

## Development

Requirements:
- Java 21
- NeoForge 21.1.x

Build:

```bash
./gradlew build
```

Example KubeJS scripts for local testing live under [`examples/kubejs/`](examples/kubejs/) (copy into your instance’s `kubejs/` folder).

## Releases

Tag source releases as `v0.1.0`, `v0.2.0`, … matching `mod_version` in [`gradle.properties`](gradle.properties). Attach the built jar from `build/libs/` (after `./gradlew build`) to the GitHub Release.

## License

MIT — see [LICENSE](LICENSE).

## Contributing

Issues and pull requests are welcome. Run `./gradlew build` before submitting.
