import net.fabricmc.loom.build.nesting.NestableJarGenerationTask

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.mod.publish)
    `maven-publish`
}

val modVersion = property("mod_version") as String
val mavenGroup = property("maven_group") as String
val archivesBaseNameProp = property("archives_base_name") as String

version = "${modVersion}+mc.${libs.versions.minecraft.get()}"
group = mavenGroup
base {
    archivesName.set(archivesBaseNameProp)
}

repositories {
    maven("https://jitpack.io")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.terraformersmc.com")
    mavenLocal()
}

loom {
    splitEnvironmentSourceSets()
    accessWidenerPath.set(file("src/main/resources/jsonpatcher.accesswidener"))

    runs {
        create("testmodClient") {
            client()
            displayName = "Testmod Client"
            sourceSet = "test"
            runDirectory = layout.projectDirectory.dir("run/test_client")
        }
        create("testmodServer") {
            server()
            displayName = "Testmod Server"
            sourceSet = "test"
            runDirectory = layout.projectDirectory.dir("run/test_server")
        }
        named("server") {
            runDirectory = layout.projectDirectory.dir("run/server")
        }
        named("client") {
            runDirectory = layout.projectDirectory.dir("run/client")
        }
    }

    mods {
        register("jsonpatcher_testmod") {
            sourceSet(sourceSets["test"])
        }
        register("jsonpatcher") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runs.configureEach {
        vmArg("-Djsonpatcher.log.level=debug")
    }
}

configurations.register("langInclude") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
    }

    // MC ships ASM, so we can't
    exclude(group = "org.ow2.asm")
}

val mainSourceSet = sourceSets.named("main")
val clientSourceSet = sourceSets.named("client")
val testSourceSet = sourceSets.named("test")

testSourceSet.configure {
    runtimeClasspath += mainSourceSet.get().runtimeClasspath
    runtimeClasspath += clientSourceSet.get().runtimeClasspath
    compileClasspath += mainSourceSet.get().compileClasspath
    compileClasspath += clientSourceSet.get().compileClasspath
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    implementation(libs.fabric.api)

    implementation(libs.lang.runtime)
    implementation(libs.lang.compiler)
    implementation(libs.lang.parser)
    implementation(libs.lang.stdlib)
    "langInclude"(libs.lang.runtime)
    "langInclude"(libs.lang.compiler)
    "langInclude"(libs.lang.parser)
    "langInclude"(libs.lang.stdlib)

    // Config
    implementation(libs.config.toolkit)
    include(libs.config.toolkit)
    annotationProcessor(libs.config.toolkit)
    "clientAnnotationProcessor"(libs.config.toolkit)
    compileOnly(libs.modmenu)
    "localRuntime"(libs.modmenu)

    // Make testmod depend on the client source set. Main is handled by gradle automatically.
    testImplementation(clientSourceSet.get().output)
}

tasks.withType<ProcessResources>().configureEach {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    withSourcesJar()
}

tasks.named<Jar>("jar") {
    from("LICENSE") {
        rename { "${it}_${archivesBaseNameProp}" }
    }
}

// Hack to JiJ transitively: we make a configuration with our JiJ mods and the internal loom configuration,
// and then we make the task process the combined configuration.
// This works around some access control issues.
val allInclude by configurations.creating {
    extendsFrom(configurations["langInclude"], configurations["includeInternal"])
}
tasks.named<NestableJarGenerationTask>("processIncludeJars") {
    from(allInclude)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
    }
}

publishMods {
    file.set(tasks.jar.flatMap { it.archiveFile })
    displayName.set("v$modVersion [${libs.versions.minecraft.get()}]")
    changelog.set(
        providers.fileContents(
            layout.projectDirectory.file("changelog/$modVersion+${libs.versions.minecraft.get()}.md")
        ).asText
    )
    type.set(providers.environmentVariable("RELEASE_TYPE").map { me.modmuss50.mpp.ReleaseType.of(it) })
    modLoaders.add("fabric")
    dryRun = providers.gradleProperty("publish_dry_run").isPresent

    modrinth {
        projectId.set("qlXnlZLG")
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.add(libs.versions.minecraft.get())
    }

    curseforge {
        projectId.set("936929")
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        client.set(true)
        server.set(true)
        minecraftVersions.add(libs.versions.minecraft.get())
    }

    github {
        repository.set("MattiDragon/JsonPatcher")
        accessToken.set(providers.environmentVariable("GITHUB_TOKEN"))
        commitish.set(providers.environmentVariable("GITHUB_BRANCH"))
        tagName.set(version.map { it.replace('+', '-') })
    }
}
