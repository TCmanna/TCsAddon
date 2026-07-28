import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("fabric-loom")
    kotlin("jvm")
    `maven-publish`
}

group = property("maven_group")!!
version = property("mod_version")!!

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.architectury.dev")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
    modImplementation("com.github.odtheking:odin:${property("odin_version")}")

    modImplementation("com.github.stivais:Commodore:${property("commodore_version")}")

    property("minecraft_lwjgl_version").let { lwjglVersion ->
        modImplementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion")

        listOf("windows", "linux", "macos", "macos-arm64").forEach { os ->
            modImplementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-$os")
        }
    }

    modCompileOnly("com.github.Noamm9:NoammAddons:1.2.0:cheat")
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-fabric:21.11.814")
    compileOnly("com.github.skies-starred.library:library-1.21.11:009")
    compileOnly(files("libs/odin-client-0.2.3-r1+1.21.11.jar"))
}

loom {
    runConfigs.named("client") {
        isIdeConfigGenerated = true
        vmArgs.addAll(
            arrayOf(
                "-Dmixin.debug.export=true",
                "-Ddevauth.enabled=true",
                "-Ddevauth.account=main"
            )
        )
    }

    runConfigs.named("server") {
        isIdeConfigGenerated = false
    }
}

afterEvaluate {
    loom.runs.named("client") {
        vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
    }
}

tasks {
    processResources {
        outputs.upToDateWhen { false }
        filesMatching("fabric.mod.json") {
            expand(getProperties())
        }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xlambdas=class") //Commodore
        }
    }

    compileJava {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)

    withSourcesJar()
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}