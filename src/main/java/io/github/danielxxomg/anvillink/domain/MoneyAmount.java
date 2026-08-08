// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.math.BigDecimal;

/**
 * A validated monetary value for one fixed-price activation. Pure domain — no Vault/Bukkit types.
 *
 * <p>Accepted only when finite, non-negative, and non-empty. Precision against a provider's {@code
 * fractionalDigits} is validated at the port boundary (repair-economy Scenario: Invalid precision
 * or value fails closed).
 */
public record MoneyAmount(BigDecimal value) {

  public MoneyAmount {
    if (value == null) {
      throw new IllegalArgumentException("amount must not be null");
    }
    if (value.signum() < 0) {
      throw new IllegalArgumentException("amount must be non-negative: " + value);
    }
    try {
      value.doubleValue(); // throws NumberFormatException on NaN/Infinity
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("amount must be finite: " + value);
    }
  }

  /** Parses a decimal string (e.g. config "25.00") into a validated amount. */
  public static MoneyAmount of(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("amount must not be null");
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("amount must not be empty");
    }
    return new MoneyAmount(new BigDecimal(trimmed));
  }

  /** Wraps an already-parsed {@link BigDecimal}. */
  public static MoneyAmount of(BigDecimal value) {
    return new MoneyAmount(value);
  }

  /**
   * True when {@code value} can be represented exactly with at most {@code fractionalDigits}
   * decimal places ({@code -1} means unlimited, per Vault's contract).
   */
  public boolean representableAt(int fractionalDigits) {
    if (fractionalDigits < 0) {
      return true;
    }
    return value.scale() <= fractionalDigits;
  }
}
