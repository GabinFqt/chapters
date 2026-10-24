# Troubleshooting

## Nothing is locked

- The Chapters jar is in `mods` on the server. For multiplayer JEI, the client needs it too. **1.x** for Minecraft 1.21, **2.x** for Minecraft 26.
- KubeJS: the script is in `kubejs/server_scripts/`, `ServerEvents.loaded` ran, and the log shows no script error. See [[KubeJS]].
- `/reload` and `/chapters reload` do not rerun `ServerEvents.loaded`. `/chapters reload` only re-reads datapack JSON. Use `/kubejs reload server-scripts` or a restart, then `/chapters check <player> <stage>`.
- Datapack: the pack is enabled, `/reload` succeeded, and the file is `data/<namespace>/chapters/stages/<name>.json`. The stage id is `<namespace>:<name>`.
- The behaviour you expected is a flag you set to `false`. Defaults are all on. See [[Restriction-flags]].

## It worked, then an edit did nothing

KubeJS does not re-run `ServerEvents.loaded` on `/reload` or `/chapters reload`.

Reload the scripts (`/kubejs reload server-scripts` or a restart), then `/chapters check` again. Use `/chapters reload` after a datapack JSON edit.

## They have a stage, and another item is still locked

Owning `mypack:tier_early` opens only what that stage lists. Emerald stays locked if it is listed on `mypack:tier_mid` only.

If the item is listed on **several** stages, **any one** of those stages unlocks it. Check `/chapters list <player>` and compare the ids with `defineStage` (or with the datapack file name).

A KubeJS `defineStage` with the same id as a datapack file **replaces** the JSON. The item may no longer be on the stage.

## JEI and the game disagree

- `hideInJei: false` leaves it visible. Pickup, crafting, or the inventory drop can still be on.
- Gaining a stage that lists the content shows it in JEI and opens the gameplay together.
- Client and server jars differ, or a sync was missed: reconnect, or `/chapters add` then `remove` once. See [[Limits]].

## `/give` keeps a locked item

`dropFromInventory: false` turns the inventory drop off. Pickup and crafting can still be blocked. Set it back to `true` (the default) if `/give` and loot should be removed.

## A locked fluid can still be placed

`preventTransfer: false` leaves buckets and tanks alone. The default is `true`.

A pipe can place or move it even when the flag is `true`. See [[Limits]].

## A locked dimension can still be entered

`preventTravel: false` on that dimension leaves travel open. The default is `true`.

If the player is already inside and also locked out of the Overworld, Chapters does not move them. See [[Limits]].

## Party stages look wrong

- `syncWithFtbTeams` in `config/chapters-common.toml` defaults to `true`.
- Joining a party **unions** stages onto the party. Leaving copies the party stages onto the player, then the two sides diverge. See [[FTB-integration]].
- `/chapters reload` does not wipe team stages.

## Mekanism chemicals do nothing

- Mekanism is not installed: the lines are kept, and nothing is indexed.
- Minecraft **26** (Chapters **2.x**): chemical gating is not in the jar. Items, fluids, recipes, and dimensions still lock.
- A tube can move a chemical that the player cannot. `preventTransfer: false` also turns the player gate off.

## JEI is stale

Reconnect. Keep the same Chapters line on client and server. Toggle `/chapters add` and `remove` once after a big edit. `hideInJei: false` is a visibility choice, not a full unlock.

## Client crash with KubeJS (Better Advanced Tooltips)

On Minecraft **26.1.2**, an older KubeJS copy of Better Advanced Tooltips can crash the client. Use a Chapters **2.x** build that bundles a fixed copy, or a standalone fixed build. This does not apply to the 1.21 line.
