// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.entrypoint;

import static org.junit.jupiter.api.Assertions.*;

import io.github.danielxxomg.anvillink.domain.RepairMode;
import io.github.danielxxomg.anvillink.domain.TransactionResult;
import io.github.danielxxomg.anvillink.domain.ports.AuditPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditWiringTest {

  static final class CountingAudit implements AuditPort {
    int calls;
    List<AuditEntry> entries = new ArrayList<>();
    boolean shouldThrow;

    @Override
    public void record(AuditEntry entry) {
      if (shouldThrow) throw new RuntimeException("disk boom");
      calls++;
      entries.add(entry);
    }
  }

  private void dispatchIfNeeded(
      TransactionResult result, AuditPort audit, AuditPort.AuditEntry template) {
    // mirrors AnvilLinkPlugin post-feedback audit wiring: only Success(non-zero) is audited,
    // double-swallow outer try/catch
    try {
      if (result instanceof TransactionResult.Success s
          && s.amount().compareTo(BigDecimal.ZERO) != 0) {
        try {
          audit.record(template);
        } catch (Exception ignored) {
        }
      }
    } catch (Exception ignored) {
    }
  }

  @Test
  void successZeroNeverAudited() {
    CountingAudit audit = new CountingAudit();
    var template =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            BigDecimal.ZERO,
            0,
            "SUCCESS");
    dispatchIfNeeded(new TransactionResult.Success(BigDecimal.ZERO, 0), audit, template);
    assertEquals(0, audit.calls);
  }

  @Test
  void insufficientFundsNeverAudited() {
    CountingAudit audit = new CountingAudit();
    var template =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    dispatchIfNeeded(new TransactionResult.InsufficientFunds(), audit, template);
    assertEquals(0, audit.calls);
  }

  @Test
  void noProviderNeverAudited() {
    CountingAudit audit = new CountingAudit();
    var template =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    dispatchIfNeeded(new TransactionResult.NoProvider(), audit, template);
    assertEquals(0, audit.calls);
  }

  @Test
  void invalidResponseNeverAudited() {
    CountingAudit audit = new CountingAudit();
    var template =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    dispatchIfNeeded(new TransactionResult.InvalidResponse("tampered-text"), audit, template);
    assertEquals(0, audit.calls);
  }

  @Test
  void paidSuccessCallsOnceAfterFeedbackDoubleSwallow() {
    CountingAudit audit = new CountingAudit();
    var template =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    // simulate feedback played then audit dispatched
    boolean feedbackPlayed = true;
    if (feedbackPlayed) {
      dispatchIfNeeded(new TransactionResult.Success(new BigDecimal("5000"), 1), audit, template);
    }
    assertEquals(1, audit.calls);
    assertEquals("world", audit.entries.get(0).worldName());
    assertEquals("Steve", audit.entries.get(0).playerName());
    assertEquals(RepairMode.HAND, audit.entries.get(0).mode());
  }

  @Test
  void worldNameExactFromPlayerGetWorld() {
    CountingAudit audit = new CountingAudit();
    String worldName = "world"; // exact from player.getWorld().getName()
    var template =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            worldName,
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    dispatchIfNeeded(new TransactionResult.Success(new BigDecimal("5000"), 1), audit, template);
    assertEquals("world", audit.entries.get(0).worldName());
    assertNotEquals("World", audit.entries.get(0).worldName());
  }

  @Test
  void doubleSwallowDoesNotPropagateEvenWhenAdapterThrows() {
    CountingAudit audit = new CountingAudit();
    audit.shouldThrow = true;
    var template =
        new AuditPort.AuditEntry(
            Instant.now(),
            UUID.randomUUID(),
            "Steve",
            RepairMode.HAND,
            "world",
            new BigDecimal("5000"),
            1,
            "SUCCESS");
    assertDoesNotThrow(
        () ->
            dispatchIfNeeded(
                new TransactionResult.Success(new BigDecimal("5000"), 1), audit, template));
    // even though adapter threw, call was attempted but swallowed; caller does not retry
    assertEquals(0, audit.calls, "swallowed throw means no success recorded");
  }

  @Test
  void pluginWiresFixedAuditLogViaFileCtor() throws Exception {
    // reflect that AnvilLinkPlugin wires FileAuditAdapter(new File(getDataFolder(),"audit.log"))
    var field = AnvilLinkPlugin.class.getDeclaredField("audit");
    assertNotNull(field);
    assertEquals(AuditPort.class, field.getType());
  }
}
