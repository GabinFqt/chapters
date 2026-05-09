console.info('[chapters] examples/kubejs/server_scripts/main.js loaded')

ServerEvents.loaded((event) => {
  ChaptersEvents.defineStage(
    'chapters:script_pickups',
    [
      'minecraft:diamond',
      'minecraft:emerald',
      'minecraft:golden_apple',
      'minecraft:gold_ingot',
      'minecraft:apple',
    ],
  )
  ChaptersEvents.defineStage('chapters:script_vanilla_all', ['@minecraft'])
  // Recipe id lock example (unlock with `/chapters add <player> chapters:script_recipes`):
  // ChaptersEvents.defineStage('chapters:script_recipes', ['recipe:minecraft:diamond_pickaxe'])

  // ChaptersEvents.defineStage('chapters:script_mek_hydrogen', ['chemical:mekanism:hydrogen'])
})
