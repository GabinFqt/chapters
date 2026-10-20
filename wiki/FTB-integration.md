# FTB Library / FTB Teams / FTB Quests

Chapters auto-detects [FTB Library], [FTB Teams], and [FTB Quests] at runtime. Drop the FTB jars into the same instance as Chapters and the integration boots itself. If any of them is missing, the corresponding feature is silently skipped (Chapters falls back to per-player attachment storage).

Works on both **Minecraft 1.21** (Chapters **1.x**) and **Minecraft 26** (Chapters **2.x**) when matching FTB builds are installed.

[FTB Library]: https://www.curseforge.com/minecraft/mc-mods/ftb-library-forge
[FTB Teams]: https://www.curseforge.com/minecraft/mc-mods/ftb-teams-forge
[FTB Quests]: https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge

---

## Config: `syncWithFtbTeams`

Team-wide unlocks with FTB Teams are **on by default**. To run Chapters in **standalone** mode while FTB Teams is still installed (per-player attachment only), set in `config/chapters-common.toml`:

```toml
[ftb]
	syncWithFtbTeams = true   # default
	# syncWithFtbTeams = false
```

| Setting | FTB Teams installed | Where unlocks are stored |
| --- | --- | --- |
| `true` (default) | Yes | Team property `TEAM_STAGES` (shared with party members) |
| `false` | Yes or no | Per-player Chapters attachment (`PLAYER_STAGES`) |
| (any) | No | Per-player attachment |

**What still works when `syncWithFtbTeams = false`:**

- FTB Library stage provider (Stage Reward / Stage Task / “Stage Required”).
- `/chapters`, KubeJS `PlayerStages`, datapack definitions, JEI sync.
- Writes go to the **per-player** store even if Teams is present.

**What changes when `true` + Teams loaded:**

- Reads and writes use the current team’s `TEAM_STAGES`.
- Party members share unlocks; inventory auditor and JEI refresh run for online members when the team property changes.
- Mid-session flipping the config without a restart can leave unlocks on the “wrong” store until you migrate manually. Prefer setting it before a serious world.

Restriction **flags** (pickup / JEI / …) are unrelated to this config; they live in stage JSON / KubeJS. See [[Restriction-flags]].

---

## What you get out of the box

When **FTB Library** is loaded, Chapters registers itself as the active stage provider via FTB Library's `StageHelper`. From that point on:

| FTB Quests feature | What it does with Chapters | Setup |
| --- | --- | --- |
| **Stage Reward** | Grants a chapter id to the claimer on quest completion | Add a *Stage* reward, type the stage id (e.g. `mypack:tier2`) |
| **Stage Task** | Marks the task complete when the player has the chapter | Add a *Stage* task, type the stage id |
| **"Stage Required"** field on a quest/chapter | Gates the quest/chapter behind a chapter id | Edit the quest/chapter, set "Stage Required" |

Stage ids are just chapter ids: no special prefix, no namespace mapping. Prefer defining them with KubeJS (`ChaptersEvents.defineStage('mypack:tier2', …)`); datapack JSON under `data/<namespace>/chapters/stages/` uses the same ids.

---

## Team-wide unlocks (with FTB Teams + sync on)

When **FTB Teams** is loaded **and** `syncWithFtbTeams` is true, chapter unlocks are **team-scoped** via FTB Teams' built-in `TEAM_STAGES` property.

### Day-to-day behaviour

- Every member of a `PartyTeam` shares unlocks instantly. Claiming a Stage Reward, running `/chapters add`, calling `PlayerStages.of(player).add(...)` from KubeJS: all write to the team store and trigger the inventory auditor + JEI hide/reveal for every online member.
- A solo player has their own personal team (`PlayerTeam`), so unlocks stay tied to them until they join a party.
- Everything routes through the same path: commands, KubeJS, FTB Quests Stage Reward, auditor, JEI sync, and the FTB Teams GUI converge on `TEAM_STAGES`.

### Party join (merge)

When a player **joins** a party, Chapters **unions**:

1. Stages already on the party,
2. Stages on the player’s previous team (usually their personal `PlayerTeam`),
3. Any leftover stages still on the per-player attachment,

…and writes that set onto the **party**. Everyone online in the party then sees the combined unlocks.

### Party leave (snapshot)

When a player **leaves** a party, Chapters copies the party’s current stage set onto their **new personal team**, then stops syncing with the party. Further unlocks on either side stay separate.

### Legacy migration (first login after adding Teams)

The first time a player logs in after FTB Teams is added to a pre-existing world, Chapters migrates non-empty per-player attachment unlocks into their **personal** `TEAM_STAGES`. **Party teams are skipped** during that migration so personal progress is not dumped into a shared party by accident.

If FTB Teams is **absent**, or `syncWithFtbTeams` is false, the per-player attachment store is used unchanged.

---

## Conditional rewards via Custom Reward + KubeJS

FTB Quests' **Custom Reward** is the escape hatch when you want logic on top of a chapter grant (one of several stages, conditional on current state, etc.). Pair it with KubeJS:

```js
// kubejs/server_scripts/ftbquests_chapter_reward.js
FTBQuestsEvents.customReward('mypack:grant_tier2', event => {
  const player = event.player
  const stage = 'mypack:tier2'

  if (PlayerStages.of(player).has(stage)) return

  PlayerStages.of(player).add(stage)
  player.tell(Text.green(`Unlocked ${stage}!`))
})
```

In the quest editor, add a **Custom** reward and set the id to `mypack:grant_tier2`. The reward routes through Chapters' writer (team or attachment according to `syncWithFtbTeams`), so with team sync on, a party member claiming it shares the unlock with the rest of the party.

Sample script:

- [on branch `26`](https://github.com/GabinFqt/chapters/blob/26/examples/kubejs/server_scripts/ftbquests_chapter_reward.js)
- [on `main`](https://github.com/GabinFqt/chapters/blob/main/examples/kubejs/server_scripts/ftbquests_chapter_reward.js)

---

## Notes for pack authors

- Team sync is opt-in by **mod presence** plus the `syncWithFtbTeams` config (default `true`). Authors who don't ship FTB get the original per-player behaviour.
- FTB Library, FTB Teams, FTB Quests are all declared as `optional` dependencies. None are required to load Chapters.
- If you only need Stage Reward / Stage Task / "Stage Required", install **FTB Library + FTB Quests**. FTB Teams is only required for team-wide unlock sharing.
- Chapters' own `/chapters reload` only refreshes stage **definitions**; it does not touch FTB Teams' stored team stages. To wipe team stages use FTB Teams' own commands.
- Restriction flags and multi-stage OR unlock rules are the same whether storage is personal or team-scoped. See [[Restriction-flags]] and [[Stages-datapack]].
