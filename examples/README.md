# Chapters examples

Copy these into your game or dev instance as needed.

## KubeJS

[`kubejs/server_scripts/main.js`](kubejs/server_scripts/main.js) defines several **`chapters:`** stages at load for local progression tests (mixed items plus an optional **`@minecraft`** stage).

Place the repo’s `kubejs/` tree under your save or instance `kubejs/` folder.

## Datapack

[`datapack/tutorial/`](datapack/tutorial/) is a standalone datapack: copy the whole **`tutorial`** folder into `saves/<world>/datapacks/`, enable it for the world, then `/reload`.

It registers stages such as **`tutorial:intro_nether`**, **`tutorial:tier_early`** … **`tutorial:tier_late`**, **`tutorial:fluides_base`**, **`tutorial:recipe_pickaxe`**.

Details and snippets are mirrored on the **[GitHub Wiki](https://github.com/GabinFqt/chapters/wiki)** (English).
