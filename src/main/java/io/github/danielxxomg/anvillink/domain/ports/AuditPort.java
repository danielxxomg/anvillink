// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import io.github.danielxxomg.anvillink.domain.RepairMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Pure-domain audit port for paid activations. No Bukkit/Vault/Adventure types. */
public interface AuditPort {

  record AuditEntry(
      Instant timestamp,
      UUID playerUuid,
      String playerName,
      RepairMode mode,
      String worldName,
      BigDecimal price,
      int repairedCount,
      String result) {}

  void record(AuditEntry entry);
}
