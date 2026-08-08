// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * platform-compliance Scenario: Host APIs are not packaged (soft-depend metadata assertion). Parses
 * the {@code plugin.yml} packaged inside the assembled release JAR (build/libs/anvillink-*.jar) and
 * asserts the exact descriptor contract: name, main, resolved version, api-version, softdepend,
 * permissions.
 */
class PluginDescriptorTest {

  @Test
  @SuppressWarnings("unchecked")
  void builtJarDescriptorDeclaresExactIdentityAndSoftDependency() throws Exception {
    Map<String, Object> plugin = descriptorFromReleaseJar();
    assertEquals("AnvilLink", plugin.get("name"));
    assertEquals("io.github.danielxxomg.anvillink.entrypoint.AnvilLinkPlugin", plugin.get("main"));
    assertTrue(plugin.get("api-version") != null, "api-version must be present");
    assertEquals("1.13", String.valueOf(plugin.get("api-version")));

    // softdepend must be exactly [Vault]
    Object softdepend = plugin.get("softdepend");
    assertTrue(softdepend instanceof List, "softdepend must be a list");
    assertEquals(List.of("Vault"), softdepend);
  }

  @Test
  @SuppressWarnings("unchecked")
  void builtJarDescriptorDeclaresAllThreePermissionNodes() throws Exception {
    Map<String, Object> plugin = descriptorFromReleaseJar();
    Map<String, Object> permissions = (Map<String, Object>) plugin.get("permissions");
    assertTrue(permissions != null, "permissions block must exist");
    for (String node : new String[] {"anvillink.create", "anvillink.use", "anvillink.manage"}) {
      assertTrue(permissions.containsKey(node), "permission node " + node + " must be declared");
    }
  }

  @Test
  void builtJarDescriptorVersionIsResolved() throws Exception {
    // The `version: '${version}'` token must be replaced at build time.
    Map<String, Object> plugin = descriptorFromReleaseJar();
    String version = String.valueOf(plugin.get("version"));
    assertTrue(
        !version.isEmpty() && !version.contains("${"),
        "plugin.yml version must be resolved, got: " + version);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> descriptorFromReleaseJar() throws IOException {
    Path jar = locateReleaseJar();
    try (JarFile jf = new JarFile(jar.toFile())) {
      var entry = jf.getEntry("plugin.yml");
      assertTrue(entry != null, "plugin.yml must be packaged in the release JAR");
      try (var in = jf.getInputStream(entry)) {
        return new Yaml().load(in);
      }
    }
  }

  private Path locateReleaseJar() throws IOException {
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
