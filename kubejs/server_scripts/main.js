console.info('[chapters] server_scripts/main.js loaded')

const CHAPTERS_JUKEBOX_DIAMOND_PICKAXE = 'chapters:jukebox_diamond_pickaxe'

ServerEvents.recipes(event => {
  // Cheap test recipe: diamond pickaxe from jukeboxes (gated by stage below).
  event
    .shaped('minecraft:diamond_pickaxe', ['JJJ', ' S ', ' S '], {
      J: 'minecraft:jukebox',
      S: 'minecraft:stick',
    })
    .id(CHAPTERS_JUKEBOX_DIAMOND_PICKAXE)
})

ServerEvents.loaded(event => {
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
  ChaptersEvents.defineStage('chapters:script_vanilla_all', ['@minecraft', '@mekanism'])

  // Mekanism example (needs Mekanism): hydrogen is gated until `/chapters add <player> chapters:script_mek_hydrogen`
  ChaptersEvents.defineStage(
    'chapters:script_mek_hydrogen',
    ['chemical:mekanism:hydrogen'],
  )
  ChaptersEvents.defineStage('chapters:jukebox_pickaxe_stage', [
    `recipe:${CHAPTERS_JUKEBOX_DIAMOND_PICKAXE}`,
  ])
})
