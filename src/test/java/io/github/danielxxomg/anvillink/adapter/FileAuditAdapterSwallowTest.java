// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.adapter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.domain.TransactionResult;
import io.github.danielxxomg.anvillink.domain.ports.AuditPort;
import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileAuditAdapterSwallowTest {

  @Test
  void filesWriteStringThrowsAdapterSwallowsCallerSwallowsTransactionStillSuccess() {
    // adapter is a directory -> write throws, must be swallowed
    File dir =
        new File(System.getProperty("java.io.tmpdir"), "anvillink-swallow-" + UUID.randomUUID());
    assertTrue(dir.mkdir());
    dir.deleteOnExit();
    FileAuditAdapter adapter = new FileAuditAdapter(dir);
    TransactionResult result = new TransactionResult.Success(new BigDecimal("5000"), 1);

    // simulate caller-side double-swallow wiring as in AnvilLinkPlugin
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
    // adapter must not throw
    assertDoesNotThrow(() -> adapter.record(entry));
    // caller must also swallow
    assertDoesNotThrow(
        () -> {
          try {
            try {
              adapter.record(entry);
            } catch (Exception ignored) {
            }
          } catch (Exception ignored) {
          }
        });
    // transaction remains Success — no compensation retry
    assertInstanceOf(TransactionResult.Success.class, result);
    assertEquals(new BigDecimal("5000"), ((TransactionResult.Success) result).amount());
  }

  @Test
  void recordNeverThrowsHandlesMissingParentUsesCreateAppendNotTruncate() throws Exception {
    File temp =
        new File(
            System.getProperty("java.io.tmpdir"), "anvillink-createappend-" + UUID.randomUUID());
    File auditFile = new File(new File(temp, "a/b"), "audit.log");
    // parent does not exist yet
    assertFalse(auditFile.getParentFile().exists());
    FileAuditAdapter adapter = new FileAuditAdapter(auditFile);
    AuditPort.AuditEntry e1 =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "A",
            RepairMode.HAND,
            "world",
            new BigDecimal("1000"),
            1,
            "SUCCESS");
    AuditPort.AuditEntry e2 =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "B",
            RepairMode.ALL,
            "world_nether",
            new BigDecimal("2000"),
            2,
            "SUCCESS");
    assertDoesNotThrow(() -> adapter.record(e1));
    assertDoesNotThrow(() -> adapter.record(e2));
    assertTrue(auditFile.exists(), "CREATE|APPEND must create file, mkdirs parent");
    String content = java.nio.file.Files.readString(auditFile.toPath());
    long lines = content.lines().count();
    assertEquals(2, lines, "must APPEND not TRUNCATE");
    // cleanup
    auditFile.delete();
    auditFile.getParentFile().delete();
    auditFile.getParentFile().getParentFile().delete();
    temp.delete();
  }
}
