# Troubleshooting

## Locks never apply

- **Chapters** jar is in **`mods`** (client **and** server if multiplayer). Use **1.x** for Minecraft **1.21**, **2.x** for Minecraft **26**.
- **KubeJS:** script under `kubejs/server_scripts/`, `ServerEvents.loaded` ran, no script errors in the log. See [[KubeJS]].
- **Datapack (optional):** pack enabled, **`/reload`** ok, JSON under **`data/<namespace>/chapters/stages/*.json`**.
- You expect a behaviour that you turned **off** with a restriction flag (`pickup: false`, `transfer: false`, …). Defaults are all on; any explicit `false` disables that behaviour. See [[Restriction-flags]].

## Worked once, stopped after edits

**KubeJS** usually needs whatever restart / reload flow you normally use. **`ServerEvents.loaded`** does **not** rerun on every vanilla **`/reload`**.

Run **`/chapters reload`** after definition changes, then **`/chapters check`** your test player again.

## “They have the stage” but another stage’s item stays locked

Unlock is **OR** across every stage that lists that content: owning **one** defining stage should unlock it fully. If it stays locked:

- Confirm with `/chapters list` that the player really has a stage id that appears in **that** content’s definitions (datapack filename → `namespace:name`, or the id passed to `defineStage`).
- A KubeJS `defineStage` with the **same id** as a datapack file **replaces** the datapack definition; it does not merge. You may have removed the item from the runtime stage by accident.

If **two packs** both list the same item under different stage ids, the player needs **one** of those stages, not both. Details: [[Stages-datapack]] / [[Restriction-flags]].

## Item visible in JEI but still unusable (or the reverse)

- `jei: false` keeps the ingredient visible while other flags can still block pickup / craft / inventory.
- Owning any defining stage unlocks JEI **and** gameplay together. Chapters will not keep JEI hidden after a gameplay unlock.
- Client/server jar mismatch or a stale sync: reconnect or `/chapters add` / `remove` once. See [[JEI-and-limitations]].

## `/give` keeps a “locked” item

`inventory: false` disables the auto-drop auditor for that content. Pickup and craft can still be blocked. This is intentional for soft gates. Use `inventory: true` (default) if `/give` and loot should be purged.

## Fluid / bucket still placeable with a fluid stage

Check `transfer`. If you set `"transfer": false`, Chapters will not block bucket place/fill or tank transfers for that fluid (the stage can still be a defining unlock key). Defaults keep `transfer: true`.

## Dimension listed but travel works

`"travel": false` on that dimension entry disables the travel gate while leaving the dimension in the lock index. Defaults keep travel blocked.

## Party unlocks look wrong (FTB Teams)

- Confirm `syncWithFtbTeams` in `config/chapters-common.toml` (default `true`).
- Join **merges** personal + party stages onto the party; leave **snapshots** the party onto the new personal team. See [[FTB-integration]].
- `/chapters reload` does **not** wipe team-stored stages.

## Mekanism chemicals unaffected

- Mod missing → harmless JSON, zero chemicals indexed.
- On **Minecraft 26** (Chapters **2.x**), chemical compat is compiled out until Mekanism has a 26.1 release. Item / fluid / recipe locks still work.
- GUI transfers vs unattended automation can differ. Don’t rely on pipes alone mirroring survival clicking. `transfer: false` also disables Chapters’ chemical transfer gates when compat is on.

## Odd JEI

Reconnect; keep **matching Chapters jars** client/server; flip **`/chapters add/remove`** once to poke a fresh sync after big script edits. Remember `jei: false` is a visibility choice, not a full unlock.

## Client crash with KubeJS (Better Advanced Tooltips)

On Minecraft **26.1.2**, older KubeJS embeds of Better Advanced Tooltips can break. Prefer a Chapters **2.x** build that Jar-in-Jars a fixed BAT, or a standalone BAT / newer KubeJS. (Does not apply to the 1.21 line.)
