// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.domain.ports.AuditPort;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileAuditAdapterFormatTest {

  @Test
  void thousandAndMillionUseToPlainString(@TempDir File temp) throws Exception {
    File auditFile = new File(temp, "audit.log");
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);

    AuditPort.AuditEntry e1 =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("1000"),
            1,
            "SUCCESS");
    AuditPort.AuditEntry e2 =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.ALL,
            "world_nether",
            new BigDecimal("1000000"),
            4,
            "SUCCESS");
    adapter.record(e1);
    adapter.record(e2);

    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(2, lines.size());
    String p1 = lines.get(0).split("\\|", -1)[5];
    String p2 = lines.get(1).split("\\|", -1)[5];
    assertEquals("1000", p1, "toPlainString 1000 not 1E3");
    assertEquals("1000000", p2, "toPlainString 1000000");
    assertFalse(p1.contains("E"));
    assertFalse(p2.contains("E"));
  }

  @Test
  void modeLiteralAndRepairedCountAndSuccess(@TempDir File temp) throws Exception {
    File auditFile = new File(temp, "audit.log");
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);

    AuditPort.AuditEntry eHand =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "A",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    AuditPort.AuditEntry eAll =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "B",
            RepairMode.ALL,
            "world",
            new BigDecimal("25000"),
            6,
            "SUCCESS");
    adapter.record(eHand);
    adapter.record(eAll);

    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals("HAND", lines.get(0).split("\\|", -1)[3]);
    assertEquals("ALL", lines.get(1).split("\\|", -1)[3]);
    assertEquals("1", lines.get(0).split("\\|", -1)[6]);
    assertEquals("6", lines.get(1).split("\\|", -1)[6]);
    assertEquals("SUCCESS", lines.get(0).split("\\|", -1)[7]);
    assertEquals("SUCCESS", lines.get(1).split("\\|", -1)[7]);
  }

  @Test
  void scientificBigDecimalRenderedPlain(@TempDir File temp) throws Exception {
    File auditFile = new File(temp, "audit.log");
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);
    AuditPort.AuditEntry e =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("1E+5"),
            1,
            "SUCCESS");
    adapter.record(e);
    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    String price = lines.get(0).split("\\|", -1)[5];
    assertEquals("100000", price);
  }
}
