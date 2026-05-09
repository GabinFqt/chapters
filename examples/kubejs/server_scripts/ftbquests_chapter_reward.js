// Chapters + FTB Quests "Custom Reward" sample.
//
// FTB Quests already ships a "Stage Reward" type that grants a chapter
// natively when Chapters is installed (Chapters registers itself as the
// FTB Library stage provider, so any stage id passed by Stage Reward is
// granted as a Chapters chapter). Use this Custom Reward path only when
// you need conditional logic — for example: only grant the chapter if
// the player is at a certain XP level, or grant a different chapter
// based on player stages.
//
// Setup:
//   1. In FTB Quests, create a "Custom Reward" on the quest you want.
//   2. Right-click the reward and "Copy ID" — paste it below in place of
//      the example ID.
//   3. Reload server scripts: /kubejs reload server-scripts
//
// Notes:
//   - When FTB Teams is installed, granting a chapter via PlayerStages
//     automatically writes to the player's current team's TEAM_STAGES,
//     so every party member receives the unlock.
//   - You can also give items, XP, etc. on the same event — see the FTB
//     docs for the full Custom Reward API.

console.info('[chapters] ftbquests_chapter_reward.js loaded')

// Replace this with the ID of YOUR Custom Reward (right-click → Copy ID).
const CUSTOM_REWARD_ID = '0000000000000000'

if (typeof FTBQuestsEvents !== 'undefined') {
  FTBQuestsEvents.customReward(CUSTOM_REWARD_ID, (event) => {
    const stages = PlayerStages.of(event.player)

    // Example: grant a tier 2 chapter only if the player already unlocked tier 1.
    if (stages.has('mypack:tier1')) {
      stages.add('mypack:tier2')
      event.player.tell('Tier 2 unlocked!')
    } else {
      // Otherwise grant tier 1 first.
      stages.add('mypack:tier1')
      event.player.tell('Tier 1 unlocked!')
    }
  })
} else {
  console.warn('[chapters] FTB Quests is not installed; ftbquests_chapter_reward.js is a no-op')
}
