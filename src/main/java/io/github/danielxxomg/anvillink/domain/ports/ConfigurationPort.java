// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import java.math.BigDecimal;
import java.util.Map;

/** Pure-domain config port. No Bukkit config types. */
public interface ConfigurationPort {
  /**
   * Validated snapshot of plugin configuration.
   *
   * @param priceHand mandatory per-mode price for HAND (1 slot), must be >= 10_000
   * @param priceAll mandatory per-mode price for ALL (up to 6 slots), must be >= 10_000
   * @param targetDistance line-of-sight distance for admin targeting (1-32)
   * @param messages reloadable MiniMessage templates keyed by message id
   * @param activationEnabled false when startup config was invalid (fail-closed)
   * @param feedbackEnabled global feedback toggle, defaults to true
   * @param feedbackSound sound id for success feedback, defaults to BLOCK_ANVIL_USE
   * @param feedbackParticles particle id for success feedback, defaults to CRIT
   */
  record ConfigSnapshot(
      BigDecimal priceHand,
      BigDecimal priceAll,
      int targetDistance,
      Map<String, String> messages,
      boolean activationEnabled,
      boolean feedbackEnabled,
      String feedbackSound,
      String feedbackParticles) {}

  ConfigSnapshot current();

  ReloadOutcome reload();

  sealed interface ReloadOutcome permits ReloadOutcome.Success, ReloadOutcome.Failure {
    record Success(ConfigSnapshot snapshot) implements ReloadOutcome {}

    record Failure(String reason, ConfigSnapshot retained) implements ReloadOutcome {}
  }
}
