// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import java.math.BigDecimal;
import java.util.Map;

/** Pure-domain config port. No Bukkit config types. */
public interface ConfigurationPort {
  record ConfigSnapshot(
      BigDecimal price,
      int targetDistance,
      Map<String, String> messages,
      boolean activationEnabled) {}

  ConfigSnapshot current();

  ReloadOutcome reload();

  sealed interface ReloadOutcome permits ReloadOutcome.Success, ReloadOutcome.Failure {
    record Success(ConfigSnapshot snapshot) implements ReloadOutcome {}

    record Failure(String reason, ConfigSnapshot retained) implements ReloadOutcome {}
  }
}
