// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.platform;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SemVerSeparationTest {

  @Test
  void versionAndMatrixAreSeparateClaims(@TempDir Path temp) throws Exception {
    Path props = temp.resolve("gradle.properties");
    Files.writeString(props, "version=0.1.0-SNAPSHOT\n", StandardCharsets.UTF_8);
    Path matrix = temp.resolve("matrix.json");
    Files.writeString(matrix, "{\"paper\":\"1.18.2-1.21.11\"}", StandardCharsets.UTF_8);
    String v1 = SemVerSupportMatrix.versionFromGradleProperties(props);
    String m1 = SemVerSupportMatrix.readCompatibilityMatrix(matrix);
    assertEquals("0.1.0-SNAPSHOT", v1);
    assertTrue(m1.contains("1.18.2"));
    // update matrix without bumping version
    Files.writeString(
        matrix, "{\"paper\":\"1.18.2-1.21.11, Spigot 1.20.6\"}", StandardCharsets.UTF_8);
    String v2 = SemVerSupportMatrix.versionFromGradleProperties(props);
    String m2 = SemVerSupportMatrix.readCompatibilityMatrix(matrix);
    assertTrue(
        SemVerSupportMatrix.matrixUpdateDoesNotBumpVersion(v1, v2),
        "matrix update must not bump SemVer");
    assertNotEquals(m1, m2);
    assertEquals(v1, v2);
  }

  @Test
  void matrixFileSeparateFromVersionFile(@TempDir Path temp) throws Exception {
    Path props = temp.resolve("gradle.properties");
    Files.writeString(props, "version=1.2.3\n", StandardCharsets.UTF_8);
    Path matrix = temp.resolve("compatibility.json");
    Files.writeString(matrix, "{\"rows\":[]}", StandardCharsets.UTF_8);
    assertNotEquals(props, matrix);
    assertNotEquals(
        Files.readString(props, StandardCharsets.UTF_8),
        Files.readString(matrix, StandardCharsets.UTF_8));
  }
}
