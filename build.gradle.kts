import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom")
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
    maven("https://api.modrinth.com/maven")
    maven("https://maven.starred.foo/releases")
    maven("https://maven.starred.foo/snapshots")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
    implementation("maven.modrinth:odin:${property("odin_version")}")

    implementation("com.github.stivais:Commodore:${property("commodore_version")}")

    property("minecraft_lwjgl_version").let { lwjglVersion ->
        implementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion")

        listOf("windows", "linux", "macos", "macos-arm64").forEach { os ->
            implementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-$os")
        }
    }

    compileOnly("com.github.Noamm9:NoammAddons:1.2.3:cheat")
    compileOnly("me.shedaniel:RoughlyEnoughItems-fabric:26.1.819")
    compileOnly("com.github.skies-starred.library:library-26.1:010")
    compileOnly("foo.starred:snowbird:011+26.1")
    compileOnly(files("libs/odin-client-0.2.3-r1+26.1.jar"))
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
            jvmTarget = JvmTarget.JVM_25
            freeCompilerArgs.addAll(listOf("-Xlambdas=class")) //Commodore
        }
    }

    compileJava {
        sourceCompatibility = "25"
        targetCompatibility = "25"
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)

    withSourcesJar()
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}