// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Slice 1 artifact contract — Shadow relocation and descriptor.
 *
 * <p>RED 1.1: JAR must contain relocated legacy serializer (9+ classes) — fails if minimize()
 * stripped it. RED 1.3: zero unrelocated net/kyori. RED 1.4: no org/bukkit/net/minecraft, major 61
 * for prod classes, plugin.yml version resolved + api-version 1.13.
 */
class ShadowRelocationContractTest {

  private static final int JAVA_17_MAJOR = 61;

  @Test
  void relocatedLegacySerializerPresentAndMinimizeDidNotStrip() throws IOException {
    Path jar = locateReleaseJar();
    try (JarFile jf = new JarFile(jar.toFile())) {
      List<String> legacy =
          jf.stream()
              .map(e -> e.getName())
              .filter(n -> n.contains("libs/kyori/adventure/text/serializer/legacy/"))
              .collect(Collectors.toList());
      assertFalse(legacy.isEmpty(), "relocated legacy serializer missing — minimize() stripped it");
      // 9f190b6 ships 9 legacy classes (14 with inner classes); assert at least 9
      assertTrue(
          legacy.size() >= 9,
          "expected at least 9 relocated legacy classes, found " + legacy.size() + ": " + legacy);
      boolean hasSerializer =
          legacy.stream().anyMatch(n -> n.endsWith("LegacyComponentSerializer.class"));
      assertTrue(hasSerializer, "LegacyComponentSerializer.class must be present under libs/kyori");
    }
  }

  @Test
  void zeroUnrelocatedNetKyoriRemains() throws IOException {
    Path jar = locateReleaseJar();
    try (JarFile jf = new JarFile(jar.toFile())) {
      List<String> unrelocated =
          jf.stream()
              .map(e -> e.getName())
              .filter(n -> n.startsWith("net/kyori"))
              .collect(Collectors.toList());
      assertTrue(
          unrelocated.isEmpty(),
          "unrelocated net/kyori entries must be zero — found: " + unrelocated);
    }
  }

  @Test
  void noHostApisPackaged() throws IOException {
    Path jar = locateReleaseJar();
    try (JarFile jf = new JarFile(jar.toFile())) {
      List<String> bukkit =
          jf.stream()
              .map(e -> e.getName())
              .filter(n -> n.startsWith("org/bukkit/"))
              .collect(Collectors.toList());
      assertTrue(bukkit.isEmpty(), "host org/bukkit must not be packaged: " + bukkit);
      List<String> nms =
          jf.stream()
              .map(e -> e.getName())
              .filter(n -> n.startsWith("net/minecraft/server"))
              .collect(Collectors.toList());
      assertTrue(nms.isEmpty(), "host net/minecraft/server must not be packaged: " + nms);
    }
  }

  @Test
  void prodBytecodeIsMajor61ExcludingLibs() throws IOException {
    Path jar = locateReleaseJar();
    try (JarFile jf = new JarFile(jar.toFile())) {
      List<String> offenders =
          jf.stream()
              .filter(e -> e.getName().endsWith(".class"))
              .filter(e -> e.getName().startsWith("io/github/danielxxomg/anvillink/"))
              .filter(e -> !e.getName().contains("/libs/"))
              .filter(
                  e -> {
                    try (var in = jf.getInputStream(e)) {
                      byte[] h = in.readNBytes(8);
                      int major = ((h[6] & 0xFF) << 8) | (h[7] & 0xFF);
                      return major != JAVA_17_MAJOR;
                    } catch (IOException ex) {
                      throw new RuntimeException(ex);
                    }
                  })
              .map(e -> e.getName())
              .collect(Collectors.toList());
      assertTrue(offenders.isEmpty(), "prod classes must be major 61, offenders: " + offenders);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void pluginYmlVersionResolvedAndApiVersion113() throws IOException {
    Path jar = locateReleaseJar();
    try (JarFile jf = new JarFile(jar.toFile())) {
      var entry = jf.getEntry("plugin.yml");
      assertTrue(entry != null, "plugin.yml must be packaged");
      Map<String, Object> plugin;
      try (var in = jf.getInputStream(entry)) {
        plugin = new Yaml().load(in);
      }
      String version = String.valueOf(plugin.get("version"));
      assertFalse(version.contains("${"), "plugin.yml version must be resolved, got: " + version);
      assertFalse(version.isBlank(), "plugin.yml version must not be blank");
      assertEquals("1.13", String.valueOf(plugin.get("api-version")), "api-version must be 1.13");
      assertEquals("AnvilLink", plugin.get("name"));
      assertEquals(
          "io.github.danielxxomg.anvillink.entrypoint.AnvilLinkPlugin", plugin.get("main"));
    }
  }

  private Path locateReleaseJar() throws IOException {
    Path buildDir = Path.of(System.getProperty("user.dir"), "build", "libs");
    if (!Files.isDirectory(buildDir)) {
      throw new IllegalStateException("no build/libs dir; run ./gradlew shadowJar first");
    }
    try (var stream = Files.list(buildDir)) {
      return stream
          .filter(p -> p.getFileName().toString().startsWith("anvillink-"))
          .filter(p -> p.getFileName().toString().endsWith(".jar"))
          .filter(p -> !p.getFileName().toString().contains("-sources"))
          .filter(p -> !p.getFileName().toString().contains("-javadoc"))
          .sorted()
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("no anvillink JAR in build/libs"));
    }
  }
}
