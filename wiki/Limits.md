# Limits

What Chapters actually stops, and what it leaves alone. Flag names are documented in [[Restriction-flags]].

## Vanilla crafting grid

Chapters blocks locked recipe ids and craft-locked items on the **vanilla crafting grid** (the 2x2 inventory grid and the 3x3 table).

These still craft:

- Smithing tables
- Create mechanical crafters and other mod crafters
- Furnaces, machines, and anything that is not that vanilla grid

JEI can hide the vanilla recipe while a machine still performs it. If the machine must stay closed, lock the **item** as well (so it is dropped from the inventory) or keep the player out of the machine with a quest.

A `recipe:` lock is that one recipe id. It does not rewrite every machine recipe that happens to output the same item.

## Pipes and automation

A player placing a bucket, filling a tank, or moving a fluid or chemical through a GUI is blocked when `preventTransfer` is on (the default).

A pipe, tube, or other insert that runs with no player attached still moves the fluid or chemical. Do not count on pipes to enforce a stage.

## JEI

JEI is optional. Without it, the server locks still run. You only lose the hidden list.

With JEI installed, and Chapters on the **client**, the client receives:

1. The stages this player has
2. The lock lists from the server (datapack + KubeJS)

Hidden when `hideInJei` is on and the player has none of the stages that list the content:

- The item, fluid, or chemical
- The filled bucket of a hidden fluid, when Chapters can resolve that bucket
- Any recipe whose id you locked

When the player gains a stage that lists the content, JEI shows it again. Chapters does not keep something hidden after that unlock, even if the stage was defined with `hideInJei: false`.

While still locked, JEI hides the content if **at least one** of its stages has `hideInJei: true`. A stage with `hideInJei: false` leaves it on screen and can still block pickup or crafting.

Mismatched jars, or a sync that did not land after `/chapters reload`, looks like "JEI shows locked items" or the reverse. Reconnect, or toggle `/chapters add` and `remove` once. See [[Troubleshooting]].

On **1.21**, with JEI, Chapters, and Mekanism together, chemical recipes are included in that hide. On **26**, chemical gating is not in the jar.

## Dimensions

Travel into a locked dimension is cancelled: portals, `/tp`, and other vanilla teleports.

A player who is already inside is sent to the **Overworld spawn** on login, on respawn, when they lose the stage, and when definitions reload.

If the Overworld is locked for them too, they are **not** moved. That avoids a loop. Unlock the Overworld, or do not lock it, if you still want the eject.

An item `@mod` line does not lock dimensions. Use `dimension:`.

## Mekanism chemicals

On Minecraft **1.21** (Chapters **1.x**), with Mekanism installed, `chemical:` entries gate player transfers and JEI. Without Mekanism, the lines are accepted and index nothing.

On Minecraft **26** (Chapters **2.x**), chemical gating is not compiled in. `chemical:` entries do nothing. Items, fluids, recipes, and dimensions still lock.

Tubes follow the pipe rule above.

## Who needs the mod installed

Server locks (pickup, drop, craft, travel, player fluid transfer) come from the **server** jar.

JEI hiding needs Chapters on the **client**, same major line as the server: **1.x** on Minecraft 1.21, **2.x** on Minecraft 26.
