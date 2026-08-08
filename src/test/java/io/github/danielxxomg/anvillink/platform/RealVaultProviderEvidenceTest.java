// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.platform;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 7.12/7.13/7.15 RED: pins official EssentialsX provider path. Real Vault+EssentialsX wiring
 * requires a live Paper runtime (deferred to Phase 8/manual release). This test RED-documents the
 * evidence gate: until the pinned evidence file exists, release claims are blocked.
 */
class RealVaultProviderEvidenceTest {

  @Test
  void realVaultProviderEvidence_gatedUntilManualRun() throws Exception {
    Path evidence = Path.of("compatibility", "evidence.json");
    if (!Files.isRegularFile(evidence)) {
      assertTrue(
          ReleaseClaimGate.claimBlockedWhenEvidenceMissing(evidence),
          "missing — real Vault+EssentialsX not yet verified; claim correctly blocked");
      return;
    }
    // Phase 8: compatibility evidence present with mandatory rows passing. Provider is pinned in
    // docs/real-provider-pin.md; ReleaseClaimGate's legacy EssentialsX string check is superseded
    // by CompatibilityEvidence gating for matrix certification. Verify mandatory rows pass.
    List<CompatibilityEvidence.Row> rows = CompatibilityEvidence.read(evidence);
    assertTrue(
        CompatibilityEvidence.allMandatoryPass(rows), "mandatory compatibility rows must pass");
    assertTrue(
        Files.isRegularFile(Path.of("docs", "real-provider-pin.md")),
        "provider pin doc must exist alongside compatibility evidence");
  }

  @Test
  void pinnedEssentialsXMetadata_documented() throws Exception {
    Path doc = Path.of("docs", "real-provider-pin.md");
    if (!Files.isRegularFile(doc)) {
      // RED: document must exist before Phase 8 green — for now assert gated
      assertTrue(
          ReleaseClaimGate.claimBlockedWhenEvidenceMissing(
              Path.of("compatibility", "evidence.json")),
          "real-provider pin doc missing → claim blocked (deferred to Phase 8)");
      return;
    }
    String text = Files.readString(doc);
    assertTrue(text.contains("EssentialsX"), "pin doc must name EssentialsX");
    assertTrue(text.toLowerCase().contains("gpl"), "pin must record GPL-2.0 license");
    assertTrue(text.contains("SHA-256") || text.contains("sha256"), "pin must record checksum");
  }
}
