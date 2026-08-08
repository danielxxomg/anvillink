// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import java.math.BigDecimal;

/**
 * Pure-domain feedback port for post-commit success presentation. Never affects economy or
 * transaction outcome. Called only after committed {@code Success(non-zero)}.
 */
public interface FeedbackPort {
  /**
   * Present success feedback for a paid repair.
   *
   * @param playerId target player
   * @param amount withdrawn price; use {@code toPlainString()} for {price}
   * @param repairedCount repaired slots count; use for {count}
   */
  void play(SignPort.PlayerId playerId, BigDecimal amount, int repairedCount);
}
