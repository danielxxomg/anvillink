// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.platform;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MissingRealProviderBlocksClaimTest {

  @Test
  void missingEvidence_blocksReleaseClaim(@TempDir Path temp) throws Exception {
    Path missing = temp.resolve("missing.json");
    assertTrue(ReleaseClaimGate.claimBlockedWhenEvidenceMissing(missing));
    assertFalse(ReleaseClaimGate.isRealProviderEvidencePresent(missing));
    assertThrows(
        IllegalStateException.class, () -> ReleaseClaimGate.requireRealProviderOrBlock(missing));
  }

  @Test
  void fakeOnlyEvidence_blocksReleaseClaim(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("evidence.json");
    Files.writeString(
        file, "{\"distribution\":\"Paper\",\"result\":\"pass\"}", StandardCharsets.UTF_8);
    assertTrue(
        ReleaseClaimGate.claimBlockedWhenEvidenceMissing(file),
        "unit/MockBukkit/fake-provider pass without EssentialsX must not satisfy the claim");
  }

  @Test
  void realVaultProviderEvidence_satisfiesReleaseClaim(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("evidence.json");
    Files.writeString(
        file,
        "{\"distribution\":\"Paper\",\"result\":\"pass\",\"provider\":\"EssentialsX\",\"version\":\"2.20.1\"}",
        StandardCharsets.UTF_8);
    assertTrue(ReleaseClaimGate.isRealProviderEvidencePresent(file));
    assertFalse(ReleaseClaimGate.claimBlockedWhenEvidenceMissing(file));
    assertDoesNotThrow(() -> ReleaseClaimGate.requireRealProviderOrBlock(file));
  }
}
