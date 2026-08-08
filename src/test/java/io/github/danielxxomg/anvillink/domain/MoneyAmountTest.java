// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyAmountTest {

  @Test
  void acceptsFiniteNonNegativeAmounts() {
    // repair-economy Scenario: Valid fixed configured price.
    assertEquals(new BigDecimal("25.00"), MoneyAmount.of("25.00").value());
    assertEquals(BigDecimal.ZERO, MoneyAmount.of("0").value());
    assertEquals(new BigDecimal("0.01"), MoneyAmount.of("0.01").value());
  }

  @Test
  void rejectsNegativeAmounts() {
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("-1"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("-0.01"));
  }

  @Test
  void rejectsNonFiniteValues() {
    // repair-economy Scenario: Invalid precision or value fails closed.
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("NaN"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("Infinity"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("-Infinity"));
  }

  @Test
  void rejectsNullAndEmpty() {
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of((String) null));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of(""));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("   "));
  }
}
