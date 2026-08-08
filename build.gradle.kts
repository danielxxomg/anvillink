// AnvilLink — paid repair signs for Minecraft servers
// Slice 1 (PR 1). Build floor: Paper API 1.18.2 / Java 17 (approved decision).
plugins {
    java
    `java-library`
    id("com.gradleup.shadow") version "8.3.6"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "io.github.danielxxomg"
version = "0.1.0-SNAPSHOT"
description = "AnvilLink: paid repair signs for Minecraft servers"

repositories {
    mavenCentral()
    // Paper API + MockBukkit (Phase 7+) publish here; keep first-class.
    maven("https://repo.papermc.io/repository/maven-public/")
    // VaultAPI 1.7 is published via JitPack (MilkBowl/VaultAPI).
    maven("https://jitpack.io/")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.ADOPTIUM
    }
    withSourcesJar()
}

// Compile floor: --release 17 (bytecode major 61) regardless of the JDK running the build.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

// Expand ${version} in plugin.yml (and any other resource tokens) at build time.
tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version.toString())
    }
}

// shadowJar consumes raw source resources; expand the token there too.
tasks.shadowJar {
    filesMatching("plugin.yml") {
        expand("version" to project.version.toString())
    }
}

dependencies {
    // Host APIs: compileOnly, NEVER shaded into the plugin JAR.
    compileOnly(libs.paper.api)
    compileOnly(libs.vault.api)

    // Adventure (MiniMessage) IS shaded/relocated (design: pin 4.11.0, relocate).
    implementation(libs.adventure.minimessage)

    // Tests: JUnit 5 platform. MockBukkit arrives in Phase 7 (pin corrected then).
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.snakeyaml) // parse plugin.yml in descriptor tests (test-only, never shaded)
    testImplementation(libs.paper.api)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Descriptor + bytecode-floor tests inspect the assembled release JAR.
    dependsOn(tasks.shadowJar)
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Shadow: produce the release JAR; relocate Adventure so no host Adventure is assumed.
tasks.shadowJar {
    archiveBaseName.set("anvillink")
    archiveClassifier.set("")
    minimize()
    relocate("net.kyori", "io.github.danielxxomg.anvillink.libs.kyori") {
        exclude("net.kyori.adventure.text.serializer.gson.**")
        exclude("net.kyori.adventure.text.serializer.legacy.**")
    }
    // Host APIs must never be packaged.
    dependencies {
        exclude(dependency("io.papermc.paper:paper-api"))
        exclude(dependency("com.github.MilkBowl:VaultAPI"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    enabled = false // Shadow JAR is the artifact; avoid an empty thin JAR.
}

// Formatting / static checks (source-mutating normalization must finish BEFORE final verification).
spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat("1.17.0")
        licenseHeaderFile(rootProject.file("gradle/spotless/license-header.txt"))
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.0.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
