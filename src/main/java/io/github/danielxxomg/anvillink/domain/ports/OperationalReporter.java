// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import java.math.BigDecimal;
import java.util.List;

/** Pure-domain operational reporter. No logging framework types. */
public interface OperationalReporter {
  enum Severity {
    HIGH,
    INFO
  }

  void report(Severity severity, String code, EventContext context);

  record EventContext(
      String signId,
      String playerId,
      BigDecimal price,
      BigDecimal withdrawn,
      List<String> mutatedSlots,
      List<String> restoredSlots,
      List<String> unresolvedSlots,
      String refundOutcome,
      String reason) {}
}
