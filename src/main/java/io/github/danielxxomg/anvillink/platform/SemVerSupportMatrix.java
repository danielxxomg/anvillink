// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * SemVer is independent of the compatibility matrix (platform spec: Version and tested range stay
 * distinct). Matrix updates must not bump the SemVer value; version source and matrix source are
 * separate files.
 *
 * <p>v0.2.0 BREAKING: per-mode pricing floor 10k (price.hand/price.all mandatory >= 10000, scalar
 * {@code price: 25.00} invalid). Compatibility matrix unchanged — still Paper 1.18.2 (388/J17)
 * certified, same 5 mandatory + probe rows in compatibility/evidence.json (see
 * CompatibilityEvidence); floor note does not alter host/build support.
 */
public final class SemVerSupportMatrix {

  private SemVerSupportMatrix() {}

  public static String versionFromGradleProperties(Path gradleProperties) throws IOException {
    Properties p = new Properties();
    try (var in = Files.newBufferedReader(gradleProperties, StandardCharsets.UTF_8)) {
      p.load(in);
    }
    String v = p.getProperty("version");
    if (v == null || v.isBlank())
      throw new IllegalStateException("version missing in " + gradleProperties);
    return v.trim();
  }

  public static String readCompatibilityMatrix(Path matrixFile) throws IOException {
    if (!Files.isRegularFile(matrixFile))
      throw new IllegalStateException("matrix missing: " + matrixFile);
    return Files.readString(matrixFile, StandardCharsets.UTF_8);
  }

  public static boolean matrixUpdateDoesNotBumpVersion(String versionBefore, String versionAfter) {
    return versionBefore != null && versionBefore.equals(versionAfter);
  }
}
