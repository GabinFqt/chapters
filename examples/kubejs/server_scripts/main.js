console.info('[stagecraft] examples/kubejs/server_scripts/main.js loaded')

ServerEvents.loaded(event => {
  StagecraftEvents.defineStage(
    'stagecraft:script_pickups',
    [
      'minecraft:diamond',
      'minecraft:emerald',
      'minecraft:golden_apple',
      'minecraft:gold_ingot',
      'minecraft:apple',
    ],
  )
  StagecraftEvents.defineStage('stagecraft:script_vanilla_all', ['@minecraft'])
  // Recipe id lock example (unlock with `/stagecraft add <player> stagecraft:script_recipes`):
  // StagecraftEvents.defineStage('stagecraft:script_recipes', ['recipe:minecraft:diamond_pickaxe'])
  // With Mekanism: optional explicit chemicals (or rely on @namespace above for mekanism:* chemicals too):
  // StagecraftEvents.defineStage('stagecraft:script_mek_hydrogen', ['chemical:mekanism:hydrogen'])
})
