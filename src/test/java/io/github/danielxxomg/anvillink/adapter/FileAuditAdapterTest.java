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

class FileAuditAdapterTest {

  @Test
  void singleSuccessAppendsOneLineWithCorrectFields(@TempDir File temp) throws Exception {
    File auditFile = new File(temp, "audit.log");
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);
    UUID uuid = UUID.randomUUID();
    Instant now = Instant.now();
    AuditPort.AuditEntry entry =
        new AuditPort.AuditEntry(
            now, uuid, "Steve", RepairMode.HAND, "world", new BigDecimal("5000"), 1, "SUCCESS");
    adapter.record(entry);

    assertTrue(auditFile.exists(), "audit.log must be created");
    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
    String line = lines.get(0);
    String[] parts = line.split("\\|", -1);
    assertEquals(
        8,
        parts.length,
        "format ISO_INSTANT|uuid|name|HAND/ALL|world|toPlainString|count|SUCCESS: " + line);
    // ISO_INSTANT parseable
    assertDoesNotThrow(() -> Instant.parse(parts[0]));
    assertEquals(uuid.toString(), parts[1]);
    assertEquals("Steve", parts[2]);
    assertEquals("HAND", parts[3]);
    assertEquals("world", parts[4]);
    assertEquals("5000", parts[5]);
    assertEquals("1", parts[6]);
    assertEquals("SUCCESS", parts[7]);
  }

  @Test
  void mkdirsCreatesParent(@TempDir File temp) throws Exception {
    File auditFile = new File(new File(temp, "nested/dir"), "audit.log");
    assertFalse(auditFile.getParentFile().exists());
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);
    AuditPort.AuditEntry entry =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Alex",
            RepairMode.ALL,
            "world_nether",
            new BigDecimal("25000"),
            4,
            "SUCCESS");
    adapter.record(entry);
    assertTrue(auditFile.getParentFile().exists(), "mkdirs must create parent");
    assertTrue(auditFile.exists());
    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
  }

  @Test
  void usesToPlainStringNotScientific(@TempDir File temp) throws Exception {
    File auditFile = new File(temp, "audit.log");
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);
    // 1E3 as BigDecimal would be "1E+3" via toString, but toPlainString is "1000"
    AuditPort.AuditEntry entry =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("1E3"),
            1,
            "SUCCESS");
    adapter.record(entry);
    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
    String[] parts = lines.get(0).split("\\|", -1);
    assertEquals("1000", parts[5], "must use toPlainString not 1E3");
    assertFalse(parts[5].contains("E"), "must not contain scientific notation");
  }

  @Test
  void ioExceptionSwallowedDoesNotThrow(@TempDir File temp) {
    // auditFile is a directory -> Files.writeString throws, must be swallowed
    File dir = new File(temp, "adir");
    assertTrue(dir.mkdir());
    FileAuditAdapter adapter = new FileAuditAdapter(dir);
    AuditPort.AuditEntry entry =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    assertDoesNotThrow(() -> adapter.record(entry));
  }

  @Test
  void fixedPathViaFileCtor(@TempDir File temp) throws Exception {
    File dataFolder = new File(temp, "AnvilLink");
    File auditFile = new File(dataFolder, "audit.log");
    assertEquals("audit.log", auditFile.getName());
    assertTrue(auditFile.getPath().endsWith("audit.log"));
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);
    AuditPort.AuditEntry entry =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    adapter.record(entry);
    assertTrue(auditFile.exists());
  }

  @Test
  void appendsNotTruncates(@TempDir File temp) throws Exception {
    File auditFile = new File(temp, "audit.log");
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);
    AuditPort.AuditEntry e1 =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "A",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    AuditPort.AuditEntry e2 =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "B",
            RepairMode.ALL,
            "world_nether",
            new BigDecimal("1000"),
            2,
            "SUCCESS");
    adapter.record(e1);
    adapter.record(e2);
    List<String> lines = Files.readAllLines(auditFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(2, lines.size(), "CREATE|APPEND must not TRUNCATE");
  }
}
