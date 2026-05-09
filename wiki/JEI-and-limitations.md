# JEI & limitations

## What JEI does when it is installed

Chapters pushes your chapter state to your client so **JEI listings match “what this player may use now”**:

- Ingredients and outputs tied to locked content are hidden where appropriate.
- Any recipe whose **id** you locked is hidden in JEI for that player as well.
- When you unlock a stage, listings catch up automatically.

If JEI is not installed, progression still works server-side — you just don’t get the filtered recipe viewer.

---

## Vanilla crafting grid vs other machines

Chapters stops **blocked recipe ids on the vanilla 2×2 / 3×3 crafting grid** (`CraftingMenu`): if a recipe row is gated and the player lacks the stage, the result shouldn’t craft there.

Other stations (**smithing, mod machines**, etc.) **are not guarded the same way** today. Someone might craft an item elsewhere even if **JEI** hides the vanilla recipe entry. Combine with extra **item** / **fluid** locks or quest-gated workshop access if you need a hard barrier.

---

## Mekanism + JEI

With **JEI**, **Chapters**, and **Mekanism** together, Mekanism chemical-heavy recipes are wired into hiding logic so JEI doesn’t spotlight locked chemistry prematurely.

---

## Useful commands while tuning

| Command | Purpose |
| --- | --- |
| `/chapters list <player>` | See stages on a player |
| `/chapters reload` | Refresh Chapters’ internal caches after datapack / script tweaks |
| `/reload` | Full datapack reload (vanilla/op) |
