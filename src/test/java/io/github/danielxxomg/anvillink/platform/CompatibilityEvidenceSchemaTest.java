// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.platform;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 8.2 RED: validate evidence.json structure, mandatory rows must pass; probe row (Paper 26.x/J25)
 * may fail without blocking certified ranges — Missing Paper runtime evidence blocks certification.
 */
class CompatibilityEvidenceSchemaTest {

  @Test
  void evidenceJson_hasRequiredSchemaPerRow() throws Exception {
    Path evidence = Path.of("compatibility", "evidence.json");
    assertTrue(Files.isRegularFile(evidence), "compatibility/evidence.json must exist");
    List<CompatibilityEvidence.Row> rows = CompatibilityEvidence.read(evidence);
    assertFalse(rows.isEmpty(), "evidence must contain rows");
    for (CompatibilityEvidence.Row r : rows) {
      assertNotNull(r.distribution());
      assertFalse(r.distribution().isBlank());
      assertNotNull(r.version());
      assertFalse(r.version().isBlank());
      assertNotNull(r.build());
      assertFalse(r.build().isBlank());
      assertNotNull(r.serverSha256());
      assertTrue(r.serverSha256().matches("[0-9a-fA-F]{64}"), "serverSha256 must be 64 hex");
      assertTrue(r.jdkMajor() > 0, "jdkMajor must be positive");
      assertNotNull(r.testSuite());
      assertFalse(r.testSuite().isBlank());
      assertTrue(
          r.result().equals("pass") || r.result().equals("fail") || r.result().equals("missing"),
          "result must be pass|fail|missing");
    }
  }

  @Test
  void mandatoryRows_mustPass_probeMayFailWithoutBlockingCertified() throws Exception {
    Path evidence = Path.of("compatibility", "evidence.json");
    List<CompatibilityEvidence.Row> rows = CompatibilityEvidence.read(evidence);
    // mandatory 5 must be pass
    assertTrue(CompatibilityEvidence.allMandatoryPass(rows), "all mandatory rows must pass");
    // probe Paper 26.x may be fail — certified Paper rows still pass
    assertTrue(
        CompatibilityEvidence.paperCertified(rows),
        "Paper certified when mandatory Paper rows pass even if probe fails");
    assertFalse(
        CompatibilityEvidence.paper26Certified(rows),
        "Paper 26.x uncertified until Java 25 job passes");
  }

  @Test
  void missingPaperEvidence_blocksCertification() throws Exception {
    Path tmp = Files.createTempFile("evidence-missing-", ".json");
    Files.writeString(
        tmp,
        "[{\"distribution\":\"Paper\",\"version\":\"1.21.11\",\"build\":\"132\","
            + "\"serverSha256\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
            + "\"jdkMajor\":21,\"testSuite\":\"smoke\",\"result\":\"pass\"}]");
    List<CompatibilityEvidence.Row> rows = CompatibilityEvidence.read(tmp);
    assertFalse(
        CompatibilityEvidence.paperCertified(rows),
        "missing Paper 1.18.2 and 1.20.6 evidence must block Paper certification");
    assertFalse(CompatibilityEvidence.allMandatoryPass(rows));
  }
}
