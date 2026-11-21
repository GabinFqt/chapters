import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

plugins {
    java
    eclipse
    id("net.neoforged.moddev") version "2.0.74"
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
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
            includeGroup("net.mezzdev.config")
        }
    }
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.rtyley")
        }
    }
    maven("https://modmaven.dev/") {
        content {
            includeGroup("mekanism")
        }
    }
    maven("https://maven.architectury.dev/") {
        content {
            includeGroup("dev.architectury")
        }
    }
    maven("https://maven.ftb.dev/releases") {
        content {
            includeGroup("dev.ftb.mods")
        }
    }
}

// Dev-only GameTests and the client harness. Not part of the published jar (jar uses source set main).
val e2eSourceSet = sourceSets.create("e2e") {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

// Big datapack used only by the perf runs. Kept out of the normal GameTest and client runs.
val generateE2ePerfPack = tasks.register("generateE2ePerfPack") {
    val out = layout.buildDirectory.dir("generated/e2e-perf")
    outputs.dir(out)
    doLast {
        val root = out.get().asFile
        val items = 2000
        val recipes = 5000
        val bulk = 1500
        fun write(path: String, text: String) {
            val file = root.resolve(path)
            file.parentFile.mkdirs()
            file.writeText(text)
        }
        val tag = StringBuilder()
        tag.append("{\n  \"values\": [\n")
        for (i in 0 until bulk) {
            tag.append("    \"chapters_e2e:perf_").append(i).append("\"")
            if (i < bulk - 1) tag.append(",")
            tag.append("\n")
        }
        tag.append("  ]\n}\n")
        write("data/chapters_e2e/tags/item/perf_bulk.json", tag.toString())
        val stageItems = StringBuilder()
        stageItems.append("{\n  \"items\": [\n    \"#chapters_e2e:perf_bulk\"\n  ]\n}\n")
        write("data/chapters/chapters/stages/e2e_perf_bulk.json", stageItems.toString())
        for (s in 0 until 40) {
            val body = StringBuilder()
            body.append("{\n  \"items\": [\n")
            for (i in 0 until 50) {
                val id = (s * 50 + i) % items
                body.append("    \"chapters_e2e:perf_").append(id).append("\"")
                if (i < 49) body.append(",")
                body.append("\n")
            }
            body.append("  ]\n}\n")
            write("data/chapters/chapters/stages/e2e_perf_$s.json", body.toString())
        }
        for (r in 0 until recipes) {
            val input = r % items
            val output = (r + 1) % items
            write(
                "data/chapters_e2e/recipe/perf_$r.json",
                """
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": [{ "item": "chapters_e2e:perf_$input" }],
                  "result": { "id": "chapters_e2e:perf_$output", "count": 1 }
                }
                """.trimIndent() + "\n"
            )
        }
    }
}

val e2ePerfSourceSet = sourceSets.create("e2eperf") {
    resources.srcDir(layout.buildDirectory.dir("generated/e2e-perf"))
}
tasks.named("processE2eperfResources") { dependsOn(generateE2ePerfPack) }

neoForge {
    version = property("neo_version").toString()

    parchment {
        minecraftVersion = property("minecraft_version").toString()
        mappingsVersion = "2024.11.17"
    }

    // Assigned inside mods {} (that block runs immediately) and read by the runs below.
    var chaptersMod: net.neoforged.moddevgradle.dsl.ModModel? = null
    var e2eMod: net.neoforged.moddevgradle.dsl.ModModel? = null
    var e2ePerfMod: net.neoforged.moddevgradle.dsl.ModModel? = null

    mods {
        chaptersMod = create(property("mod_id").toString()) {
            sourceSet(sourceSets.main.get())
        }
        e2eMod = create("chapters_e2e") {
            sourceSet(e2eSourceSet)
        }
        e2ePerfMod = create("chapters_e2e_perf") {
            sourceSet(e2ePerfSourceSet)
        }
    }

    val chaptersOnly = setOf(chaptersMod!!)
    val withE2e = setOf(chaptersMod!!, e2eMod!!)
    val withPerf = setOf(chaptersMod!!, e2eMod!!, e2ePerfMod!!)

    unitTest {
        enable()
        testedMod = mods.getByName(property("mod_id").toString())
        // Logic unit tests do not need JEI / KubeJS / FTB on the classpath at runtime.
        loadedMods.set(setOf(mods.getByName(property("mod_id").toString())))
    }

    runs {
        create("client") {
            client()
            programArguments.addAll("--username", "Alice")
            loadedMods.set(chaptersOnly)
        }
        create("clientBob") {
            client()
            programArguments.addAll("--username", "Bob")
            gameDirectory = file("run-bob")
            loadedMods.set(chaptersOnly)
        }
        create("server") {
            server()
            loadedMods.set(chaptersOnly)
        }
        // Dedicated server with -Dneoforge.gameTestServer=true (see Minecraft server Main).
        create("gameTestServer") {
            server()
            gameDirectory = file("run-gametest")
            systemProperty("neoforge.gameTestServer", "true")
            systemProperty("neoforge.enabledGameTestNamespaces", "chapters")
            loadedMods.set(withE2e)
        }
        create("clientE2e") {
            client()
            gameDirectory = file("run-e2e")
            programArguments.addAll("--username", "E2E")
            systemProperty("chapters.e2e", "true")
            logLevel.set(org.slf4j.event.Level.DEBUG)
            loadedMods.set(withE2e)
        }
        // Same server as gameTestServer, but only the perf namespace and the big pack.
        create("perfGameTestServer") {
            server()
            gameDirectory = file("run-perf")
            systemProperty("neoforge.gameTestServer", "true")
            systemProperty("neoforge.enabledGameTestNamespaces", "chapters_perf")
            systemProperty("chapters.e2e.perf", "true")
            loadedMods.set(withPerf)
        }
        create("clientPerf") {
            client()
            gameDirectory = file("run-client-perf")
            programArguments.addAll("--username", "Perf")
            systemProperty("chapters.e2e", "true")
            systemProperty("chapters.e2e.perf", "true")
            logLevel.set(org.slf4j.event.Level.DEBUG)
            loadedMods.set(withPerf)
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // Opt-in benchmark: ./gradlew test --tests '*PerfBenchmarkTest' -Pchapters.perf=true -Pchapters.perf.label=before
    val perf = providers.gradleProperty("chapters.perf").orNull == "true"
    systemProperty("chapters.perf", perf.toString())
    if (perf) {
        maxHeapSize = "2g"
        outputs.upToDateWhen { false }
        systemProperty("chapters.perf.label", providers.gradleProperty("chapters.perf.label").getOrElse("run"))
        systemProperty("chapters.perf.out", layout.buildDirectory.dir("perf").get().asFile.absolutePath)
    }
}

// Optional runtime mods stay available for runClient/runServer, but unit tests only need Chapters.
listOf("testRuntimeClasspath", "testCompileClasspath").forEach { configName ->
    configurations.named(configName).configure {
        exclude(group = "mezz.jei")
        exclude(group = "net.mezzdev.config")
        exclude(group = "dev.latvian.mods")
        exclude(group = "dev.ftb.mods")
        exclude(group = "dev.architectury")
        exclude(group = "mekanism")
    }
}

/**
 * Symlink each run directory's `kubejs/` to the canonical `kubejs/` at the project root
 * so KubeJS scripts (server_scripts, startup_scripts, client_scripts, config, assets, data)
 * stay in sync between the dedicated server, Alice's client, and Bob's client.
 */
val linkKubejs by tasks.registering {
    group = "chapters dev"
    description = "Symlink run/kubejs and run-bob/kubejs to the shared project-root kubejs/ folder."

    val rootKubejs = layout.projectDirectory.dir("kubejs").asFile.toPath()
    val targets = listOf(
        layout.projectDirectory.dir("run").asFile.toPath().resolve("kubejs"),
        layout.projectDirectory.dir("run-bob").asFile.toPath().resolve("kubejs"),
    )

    doLast {
        Files.createDirectories(rootKubejs)
        val rootAbs = rootKubejs.toAbsolutePath().normalize()
        for (link in targets) {
            Files.createDirectories(link.parent)
            val expectedRelative: Path = link.parent.relativize(rootKubejs)

            // Windows + WSL-copied reparse points: exists()/isSymbolicLink() can disagree; always clear first if needed.
            val alreadyOk = try {
                Files.isSymbolicLink(link) && Files.readSymbolicLink(link) == expectedRelative
            } catch (_: Exception) {
                false
            }
            if (alreadyOk) {
                continue
            }

            fun removeLinkOrDir(path: Path) {
                if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)) {
                    return
                }
                if (Files.isDirectory(path) && !Files.isSymbolicLink(path)) {
                    path.toFile().deleteRecursively()
                } else {
                    Files.deleteIfExists(path)
                }
            }

            try {
                removeLinkOrDir(link)
            } catch (e: Exception) {
                logger.warn("Could not clear $link before linking: ${e.message}")
            }

            var linked = false
            try {
                Files.createSymbolicLink(link, expectedRelative)
                logger.lifecycle("Linked $link -> $expectedRelative")
                linked = true
            } catch (e: Exception) {
                // Windows often blocks symlinks without Developer Mode; junctions work without elevation.
                if (System.getProperty("os.name").lowercase().contains("windows")) {
                    try {
                        removeLinkOrDir(link)
                        val code = ProcessBuilder("cmd", "/c", "mklink", "/J", link.toAbsolutePath().toString(), rootAbs.toString())
                            .redirectErrorStream(true)
                            .start()
                            .also { it.inputStream.bufferedReader().use { r -> r.lines().forEach { line -> logger.lifecycle(line) } } }
                            .waitFor()
                        if (code == 0) {
                            logger.lifecycle("Junction $link -> $rootAbs")
                            linked = true
                        }
                    } catch (junctionError: Exception) {
                        logger.warn("Junction fallback failed for $link: ${junctionError.message}")
                    }
                }
                if (!linked) {
                    logger.warn("Could not link $link (${e.message}). Ensure $link points at project kubejs/.")
                }
            }
        }
    }
}

tasks.matching { it.name == "runClient" || it.name == "runClientBob" || it.name == "runServer" }
    .configureEach { dependsOn(linkKubejs) }

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("dev.latvian.mods:kubejs-neoforge:${property("kubejs_version")}")
    runtimeOnly("dev.latvian.mods:kubejs-neoforge:${property("kubejs_version")}")
    runtimeOnly("dev.latvian.mods:rhino:${property("rhino_version")}")
    val minecraftVersion = property("minecraft_version").toString()
    val jeiVersion = property("jei_version").toString()
    compileOnly("mezz.jei:jei-$minecraftVersion-neoforge:$jeiVersion")
    runtimeOnly("mezz.jei:jei-$minecraftVersion-neoforge:$jeiVersion")
    // JEI 19.71+ requires this mod, but declares the Maven dependency optional, so dev runs must ask for it.
    runtimeOnly("net.mezzdev.config:mezz_config_gui-$minecraftVersion-neoforge:mc$minecraftVersion-0.7.1")

    val mekanismVersion = property("mekanism_version").toString()
    compileOnly("mekanism:Mekanism:$mekanismVersion:api")

    val architecturyVersion = property("architectury_version").toString()
    val ftbLibraryVersion = property("ftb_library_version").toString()
    val ftbTeamsVersion = property("ftb_teams_version").toString()
    val ftbQuestsVersion = property("ftb_quests_version").toString()
    compileOnly("dev.architectury:architectury-neoforge:$architecturyVersion")
    compileOnly("dev.ftb.mods:ftb-library-neoforge:$ftbLibraryVersion")
    compileOnly("dev.ftb.mods:ftb-teams-neoforge:$ftbTeamsVersion")
    runtimeOnly("dev.architectury:architectury-neoforge:$architecturyVersion")
    runtimeOnly("dev.ftb.mods:ftb-library-neoforge:$ftbLibraryVersion")
    runtimeOnly("dev.ftb.mods:ftb-teams-neoforge:$ftbTeamsVersion")
    runtimeOnly("dev.ftb.mods:ftb-quests-neoforge:$ftbQuestsVersion")
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

publishMods {
    file = tasks.jar.flatMap { it.archiveFile }
    type = STABLE
    modLoaders.add("neoforge")
    displayName = "${property("mod_name")} ${property("mod_version")}"

    val changelogFile = rootProject.file("CHANGELOG.md")
    changelog = if (changelogFile.exists()) {
        changelogFile.readText().substringBefore("\n---\n").trim()
    } else {
        "Release ${property("mod_version")}"
    }

    val modrinthProjectId = findProperty("modrinth_project_id")?.toString()?.takeIf { it.isNotBlank() }
    if (modrinthProjectId != null) {
        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = modrinthProjectId
            minecraftVersions.add(property("minecraft_version").toString())
            // FTB mods are CurseForge-only (no Modrinth projects).
            optional { slug = "jei" }
            optional { slug = "kubejs" }
            optional { slug = "mekanism" }
        }
    }

    val curseforgeProjectId = findProperty("curseforge_project_id")?.toString()?.takeIf { it.isNotBlank() }
    if (curseforgeProjectId != null) {
        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = curseforgeProjectId
            minecraftVersions.add(property("minecraft_version").toString())
            // Required by CurseForge's environment version group.
            client = true
            server = true
            optional { slug = "jei" }
            optional { slug = "kubejs" }
            optional { slug = "mekanism" }
            optional { slug = "ftb-library-forge" }
            optional { slug = "ftb-teams-forge" }
            optional { slug = "ftb-quests-forge" }
        }
    }
}
