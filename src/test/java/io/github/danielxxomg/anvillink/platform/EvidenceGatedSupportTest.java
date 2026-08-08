// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.platform;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 8.3 RED: Paper certified only when all mandatory Paper rows pass; Spigot/Purpur verified only
 * after separate smoke; Folia experimental; Paper 26.x uncertified until Java 25 job passes; probe
 * failure does not block unrelated certified claims. Support labels follow their evidence, Paper
 * 26.x requires Java 25 evidence.
 */
class EvidenceGatedSupportTest {

  @Test
  void paperCertified_onlyWhenAllMandatoryPaperRowsPass() {
    List<CompatibilityEvidence.Row> rows =
        List.of(mandatoryPaper("1.18.2", 17), mandatoryPaper("1.20.6", 21));
    assertFalse(
        CompatibilityEvidence.paperCertified(rows),
        "needs all mandatory Paper rows (missing 1.21.11)");
    List<CompatibilityEvidence.Row> full = mandatoryPaperSet();
    assertTrue(CompatibilityEvidence.paperCertified(full));
  }

  @Test
  void spigotVerified_onlyAfterSeparateSmoke() {
    List<CompatibilityEvidence.Row> paperOnly = mandatoryPaperSet();
    assertFalse(
        CompatibilityEvidence.spigotVerified(paperOnly),
        "Spigot not verified without its own smoke");
    List<CompatibilityEvidence.Row> withSpigot = with(paperOnly, spigotPass());
    assertTrue(CompatibilityEvidence.spigotVerified(withSpigot));
  }

  @Test
  void purpurVerified_onlyAfterSeparateSmoke() {
    assertFalse(CompatibilityEvidence.purpurVerified(mandatoryPaperSet()));
    assertTrue(CompatibilityEvidence.purpurVerified(with(mandatoryPaperSet(), purpurPass())));
  }

  @Test
  void folia_alwaysExperimental() {
    assertEquals("experimental", CompatibilityEvidence.foliaTier());
    assertEquals("experimental", CompatibilityEvidence.foliaTier(mandatoryPaperSet()));
  }

  @Test
  void paper26RequiresJava25_probeFailDoesNotBlockCertifiedRanges() {
    List<CompatibilityEvidence.Row> certified =
        List.of(
            mandatoryPaper("1.18.2", 17),
            mandatoryPaper("1.20.6", 21),
            mandatoryPaper("1.21.11", 21),
            spigotPass(),
            purpurPass(),
            new CompatibilityEvidence.Row(
                "Paper",
                "26.2",
                "102",
                "5555555555555555555555555555555555555555555555555555555555555555",
                25,
                "smoke",
                "fail"));
    assertFalse(CompatibilityEvidence.paper26Certified(certified), "probe fail → 26.x uncertified");
    assertTrue(
        CompatibilityEvidence.paperCertified(certified),
        "probe fail must not block certified Paper");
    assertTrue(CompatibilityEvidence.spigotVerified(certified));
    List<CompatibilityEvidence.Row> withProbePass =
        with(
            mandatoryPaperSet(),
            new CompatibilityEvidence.Row(
                "Paper",
                "26.2",
                "102",
                "5555555555555555555555555555555555555555555555555555555555555555",
                25,
                "smoke",
                "pass"));
    assertTrue(CompatibilityEvidence.paper26Certified(withProbePass));
  }

  @Test
  void supportLabels_followEvidence() {
    List<CompatibilityEvidence.Row> allMandatory = mandatoryPaperSetWithSpigotPurpur();
    assertEquals("certified", CompatibilityEvidence.paperTier(allMandatory));
    assertEquals("verified", CompatibilityEvidence.spigotTier(allMandatory));
    assertEquals("verified", CompatibilityEvidence.purpurTier(allMandatory));
    assertEquals("experimental", CompatibilityEvidence.foliaTier(allMandatory));
    assertEquals("uncertified", CompatibilityEvidence.paper26Tier(allMandatory));
  }

  private static CompatibilityEvidence.Row mandatoryPaper(String version, int jdk) {
    String sha = "a".repeat(64);
    String build = version.equals("1.18.2") ? "388" : version.equals("1.20.6") ? "151" : "132";
    return new CompatibilityEvidence.Row("Paper", version, build, sha, jdk, "smoke", "pass");
  }

  private static List<CompatibilityEvidence.Row> mandatoryPaperSet() {
    return List.of(
        mandatoryPaper("1.18.2", 17), mandatoryPaper("1.20.6", 21), mandatoryPaper("1.21.11", 21));
  }

  private static List<CompatibilityEvidence.Row> mandatoryPaperSetWithSpigotPurpur() {
    return List.of(
        mandatoryPaper("1.18.2", 17),
        mandatoryPaper("1.20.6", 21),
        mandatoryPaper("1.21.11", 21),
        spigotPass(),
        purpurPass());
  }

  private static CompatibilityEvidence.Row spigotPass() {
    return new CompatibilityEvidence.Row(
        "Spigot", "1.20.6", "BuildTools #200 --rev 1.20.6", "b".repeat(64), 21, "smoke", "pass");
  }

  private static CompatibilityEvidence.Row purpurPass() {
    return new CompatibilityEvidence.Row(
        "Purpur", "1.20.6", "2233", "c".repeat(64), 21, "smoke", "pass");
  }

  private static List<CompatibilityEvidence.Row> with(
      List<CompatibilityEvidence.Row> base, CompatibilityEvidence.Row row) {
    return java.util.stream.Stream.concat(base.stream(), java.util.stream.Stream.of(row)).toList();
  }
}
