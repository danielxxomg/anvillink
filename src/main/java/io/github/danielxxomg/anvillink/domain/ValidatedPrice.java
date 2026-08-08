// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.math.BigDecimal;

/**
 * Validated fixed price for one activation, bound to provider precision. Pure domain — no
 * Vault/Bukkit. Finite/non-negative via {@link MoneyAmount}; scale via {@link
 * MoneyAmount#representableAt(int)} (-1 = unlimited).
 */
public record ValidatedPrice(MoneyAmount amount, int fractionalDigits) {
  public ValidatedPrice {
    if (amount == null) {
      throw new IllegalArgumentException("amount must not be null");
    }
    if (amount.value().signum() < 0) {
      throw new IllegalArgumentException("amount must be non-negative: " + amount.value());
    }
    if (!amount.representableAt(fractionalDigits)) {
      throw new IllegalArgumentException(
          "amount scale "
              + amount.value().scale()
              + " exceeds fractionalDigits "
              + fractionalDigits
              + ": "
              + amount.value());
    }
  }

  public BigDecimal value() {
    return amount.value();
  }

  public static ValidatedPrice of(String raw, int fractionalDigits) {
    return new ValidatedPrice(MoneyAmount.of(raw), fractionalDigits);
  }

  public static ValidatedPrice of(BigDecimal value, int fractionalDigits) {
    if (value == null) {
      throw new IllegalArgumentException("amount must not be null");
    }
    return new ValidatedPrice(MoneyAmount.of(value), fractionalDigits);
  }
}
