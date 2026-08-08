// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

/**
 * platform-compliance Scenario: Newer build JDK still targets the floor.
 *
 * <p>Phase 1 proof that the built plugin JAR targets Java 17 bytecode (class major version 61) and
 * that the descriptor is present. The check runs under the JVM executing the test suite (Java 17
 * via the pinned toolchain — or anything newer); the assertion is on the JAR contents, so a build
 * running on a newer JDK still proves the bytecode floor. When a Java 21/25 runtime is used for the
 * test JVM (Phase 8 matrix), this test mechanically inspects the JAR with {@code javap -v}
 * equivalent: reading the class file header bytes.
 */
class BytecodeFloorTest {

  private static final int JAVA_17_MAJOR = 61;

  @Test
  void builtJarContainsPluginDescriptor() throws IOException {
    Path jar = locateReleaseJar();
    assertTrue(Files.isRegularFile(jar), "release JAR not found at " + jar);
    try (JarFile jf = new JarFile(jar.toFile())) {
      assertTrue(jf.getEntry("plugin.yml") != null, "plugin.yml must be packaged");
    }
  }

  @Test
  void builtJarClassesTargetJava17Bytecode() throws IOException {
    Path jar = locateReleaseJar();
    try (JarFile jf = new JarFile(jar.toFile())) {
      jf.stream()
          .filter(e -> e.getName().endsWith(".class"))
          // Only OUR classes prove the floor. Relocated libraries
          // (io/.../libs/) ship their own (older) bytecode and are
          // intentionally not part of the floor assertion.
          .filter(e -> e.getName().startsWith("io/github/danielxxomg/anvillink/"))
          .filter(e -> !e.getName().contains("/libs/"))
          .forEach(
              e -> {
                try (var in = jf.getInputStream(e)) {
                  byte[] header = in.readNBytes(8);
                  // magic CAFEBABE + minor(2) + major(2)
                  int major = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                  assertEquals(
                      JAVA_17_MAJOR, major, "class " + e.getName() + " must be Java 17 bytecode");
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              });
    }
  }

  @Test
  void releaseJarDoesNotPackageHostApis() throws IOException {
    Path jar = locateReleaseJar();
    try (JarFile jf = new JarFile(jar.toFile())) {
      jf.stream()
          .map(e -> e.getName())
          .filter(n -> n.startsWith("org/bukkit/"))
          .forEach(
              n -> {
                throw new AssertionError("host API class packaged in release JAR: " + n);
              });
    }
  }

  private Path locateReleaseJar() throws IOException {
    // Resolve via the `anvillink` shadow JAR location: build/libs/anvillink-*.jar
    Path buildDir = Path.of(System.getProperty("user.dir"), "build", "libs");
    if (!Files.isDirectory(buildDir)) {
      throw new IllegalStateException("no build/libs dir; run ./gradlew build first");
    }
    try (var stream = Files.list(buildDir)) {
      return stream
          .filter(p -> p.getFileName().toString().startsWith("anvillink-"))
          .filter(p -> p.getFileName().toString().endsWith(".jar"))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("no anvillink JAR in build/libs"));
    }
  }
}
