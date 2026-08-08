// AnvilLink — paid repair signs for Minecraft servers
// Slice 1 (PR 1): Gradle scaffold, plugin metadata, domain value types, bytecode floor proof.
rootProject.name = "anvillink"

// Auto-provision the Java 17 toolchain (foojay resolver) so a fresh machine
// without a preinstalled JDK 17 still hits the bytecode floor.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
