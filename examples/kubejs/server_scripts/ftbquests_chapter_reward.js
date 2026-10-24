// Chapters + FTB Quests Custom Reward sample.
//
// FTB Quests already has a Stage Reward. With Chapters installed, that
// reward grants a stage id directly. Use this Custom Reward only when
// the grant depends on something else.
//
// Setup:
//   1. In FTB Quests, create a Custom Reward on the quest.
//   2. Right-click the reward, Copy ID, and paste it below.
//   3. Reload server scripts: /kubejs reload server-scripts
//
// With FTB Teams and the default config, the whole party receives the stage.

console.info('[chapters] ftbquests_chapter_reward.js loaded')

// Paste the copied reward id.
const CUSTOM_REWARD_ID = '0000000000000000'

if (typeof FTBQuestsEvents !== 'undefined') {
  FTBQuestsEvents.customReward(CUSTOM_REWARD_ID, (event) => {
    const stages = PlayerStages.of(event.player)

    if (!stages.has('mypack:tier_early')) {
      stages.add('mypack:tier_early')
      event.player.tell('Early game unlocked.')
      return
    }

    stages.add('mypack:tier_mid')
    event.player.tell('Mid game unlocked.')
  })
} else {
  console.warn('[chapters] FTB Quests is not installed; ftbquests_chapter_reward.js is a no-op')
}
