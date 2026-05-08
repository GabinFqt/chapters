plugins {
    java
    id("net.neoforged.moddev") version "2.0.74"
}

group = property("mod_group_id")!!
version = property("mod_version")!!

base {
    archivesName.set(property("mod_id").toString())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.latvian.dev/releases") {
        content {
            includeGroup("dev.latvian.mods")
            includeGroup("dev.latvian.apps")
        }
    }
    maven("https://maven.blamejared.com") {
        content {
            includeGroup("mezz.jei")
        }
    }
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.rtyley")
        }
    }
}

neoForge {
    version = property("neo_version").toString()

    parchment {
        minecraftVersion = property("minecraft_version").toString()
        mappingsVersion = "2024.11.17"
    }

    mods {
        create(property("mod_id").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }
}

dependencies {
    compileOnly("dev.latvian.mods:kubejs-neoforge:${property("kubejs_version")}")
    runtimeOnly("dev.latvian.mods:kubejs-neoforge:${property("kubejs_version")}")
    runtimeOnly("dev.latvian.mods:rhino:${property("rhino_version")}")
    val minecraftVersion = property("minecraft_version").toString()
    val jeiVersion = property("jei_version").toString()
    compileOnly("mezz.jei:jei-$minecraftVersion-neoforge:$jeiVersion")
    runtimeOnly("mezz.jei:jei-$minecraftVersion-neoforge:$jeiVersion")
}

tasks.withType<ProcessResources>().configureEach {
    val properties = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "neo_version" to project.property("neo_version"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_version" to project.property("mod_version"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description")
    )

    inputs.properties(properties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(properties)
    }
}
