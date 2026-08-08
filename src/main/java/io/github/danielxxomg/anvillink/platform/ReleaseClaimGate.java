// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.platform;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Evidence gate for platform-compatibility claim: unit + MockBukkit + fake-provider pass is not
 * sufficient. Real Paper smoke and real Vault-provider evidence must be present before release
 * claims.
 */
public final class ReleaseClaimGate {

  private ReleaseClaimGate() {}

  public static boolean isRealProviderEvidencePresent(Path evidenceFile) {
    if (evidenceFile == null) return false;
    try {
      if (!Files.isRegularFile(evidenceFile)) return false;
      String text = Files.readString(evidenceFile);
      return text.contains("\"result\"")
          && text.contains("\"pass\"")
          && text.contains("EssentialsX");
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean claimBlockedWhenEvidenceMissing(Path evidenceFile) {
    return !isRealProviderEvidencePresent(evidenceFile);
  }

  public static void requireRealProviderOrBlock(Path evidenceFile) {
    if (claimBlockedWhenEvidenceMissing(evidenceFile)) {
      throw new IllegalStateException(
          "Release claim blocked: real Vault-provider evidence missing at " + evidenceFile);
    }
  }
}
