# Stagecraft (NeoForge 1.21.1)

Stagecraft is a progression mod inspired by GameStages + ItemStages.

It lets you:
- lock items behind named stages
- lock fluids (tags, ids, whole mods) behind named stages
- prevent pickup of locked items
- block crafting of locked items
- auto-drop locked items from the player inventory (every second while online, and when a stage is removed or definitions reload)
- push client stage changes so recipe-viewer compat can hide locked entries

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
  ]
}
```

Rules:
- `items`: `#` prefixes are **item tags**; `@` prefixes lock every **item** in that namespace **and** every **fluid** in the same namespace (so you do not need `fluid:@…` for parity); other strings are item ids.
- Optional top-level `namespaces`: same as `@mod` entries — applies to **items and fluids** together.
- Optional `fluid_namespaces`: **extra** namespaces that only affect fluids (additive).
- Optional `fluids`: `#` prefixes are **fluid tags**; `@` prefixes are mods (every fluid in that namespace); other strings are fluid ids.
- If an ingredient appears in any stage definition, a player needs at least one matching stage **among every definition that mentions that ingredient**: for items — use/pickup/crafting; for fluids — transferring with tanks or buckets/vanilla placement (NeoForge fluid util).

## KubeJS integration

Stagecraft registers a KubeJS plugin and bindings:

- `StagecraftEvents.defineStage(stageId, ...entries)` — item-style strings (`@mod` also locks that mod’s fluids) and `fluid:…` overrides in the same call. Definitions are applied **once per server tick** after your script runs so many `defineStage` calls in one `ServerEvents.loaded` callback batch into a single reload (indices are rebuilt once).
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

## Recipe-viewer compat notes

Stagecraft includes optional JEI/REI/EMI compat modules.
When client stage payloads arrive, Stagecraft recomputes locked item ids and updates each viewer bridge.

**JEI:** recipes are masked **before** ingredients are removed so output-focus lookups work; unlock restores **all** transitioning ingredients first, then reapplies output recipes; reconcile uses `includeHidden()` on focus lookups so still-hidden recipes can be unhid. Recipe visibility for REI/EMI stacks is thinner today than JEI — open an issue if you want them brought to parity.

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
