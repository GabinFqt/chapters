# FTB Quests and FTB Teams

Chapters notices [FTB Library](https://www.curseforge.com/minecraft/mc-mods/ftb-library-forge), [FTB Teams](https://www.curseforge.com/minecraft/mc-mods/ftb-teams-forge), and [FTB Quests](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge) when they are installed. Nothing to register. A missing mod just skips that part.

Works on Minecraft **1.21** (Chapters **1.x**) and **26** (Chapters **2.x**) with a matching FTB build.

For Stage Reward, Stage Task, and Stage Required you need **FTB Library** and **FTB Quests**. **FTB Teams** is only for shared party unlocks.

---

## Quests

Define the stage first ([[KubeJS]] or [[Stages-datapack]]). The id you type in Quests is that same id, for example `mypack:iron_age`.

| In FTB Quests | What happens | What you type |
| --- | --- | --- |
| **Stage Reward** | Completing the quest grants the stage | The stage id |
| **Stage Task** | The task completes once the player has the stage | The stage id |
| **Stage Required** on a quest or a quest chapter | That quest or chapter stays closed until the player has the stage | The stage id |

"Quest chapter" here is FTB's folder of quests. It is not a Chapters stage. The value you type into Stage Required is still a stage id like `mypack:iron_age`.

---

## Party sharing

On by default when FTB Teams is installed.

A party shares stages. One member claims a Stage Reward, runs `/chapters add`, or calls `PlayerStages.of(player).add(...)`, and the other online members get it too. JEI and the inventory drop update for them as well.

A solo player keeps stages on their own personal team until they join a party.

| Moment | What Chapters does |
| --- | --- |
| Player **joins** a party | The party gains the union of its stages and the stages the player had. Everyone online in the party sees the combined set |
| Player **leaves** a party | They keep a copy of the party's stages at that moment. Later grants on either side stay separate |
| First login after you **add** FTB Teams to an old world | Stages that player already had move onto their personal team. They are not dumped into a party |

`/chapters reload` re-reads datapack JSON. It does not clear stages stored on teams, and it does not re-run KubeJS. Use FTB Teams' own commands to wipe team stages.

### Turn party sharing off

`config/chapters-common.toml`:

```toml
[ftb]
	syncWithFtbTeams = false
```

The default is `true`. Set `false` **before** a world matters to you. Flipping it mid-save leaves existing unlocks on the store you just stopped reading.

With `false`, or with FTB Teams absent, each player has their own stages. Stage Reward, Stage Task, and Stage Required still work.

Restriction flags do not change with this config. See [[Restriction-flags]].

---

## Custom Reward

Use this when a Stage Reward is not enough: grant only if another stage is already there, pick between two stages, send a message.

In the quest editor, add a **Custom** reward and copy its id. Put that id in the script.

```js
// kubejs/server_scripts/ftbquests_chapter_reward.js
const CUSTOM_REWARD_ID = '0000000000000000' // paste the copied reward id

if (typeof FTBQuestsEvents !== 'undefined') {
  FTBQuestsEvents.customReward(CUSTOM_REWARD_ID, event => {
    const stages = PlayerStages.of(event.player)

    if (!stages.has('mypack:tier_early')) {
      stages.add('mypack:tier_early')
      event.player.tell('Early game unlocked.')
      return
    }

    stages.add('mypack:tier_mid')
    event.player.tell('Mid game unlocked.')
  })
}
```

With party sharing on, the grant still reaches the whole party.

Sample in the repo (same shape, placeholder id):

- [on `main`](https://github.com/GabinFqt/chapters/blob/main/examples/kubejs/server_scripts/ftbquests_chapter_reward.js)
- [on `26`](https://github.com/GabinFqt/chapters/blob/26/examples/kubejs/server_scripts/ftbquests_chapter_reward.js)
