// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyAmountTest {

  @Test
  void acceptsZeroLowAndDefaultPrices() {
    // Slice 1 BREAKING: floor relaxed to >=0 (MIN_PRICE removed)
    assertEquals(new BigDecimal("0"), MoneyAmount.of("0").value());
    assertEquals(new BigDecimal("100"), MoneyAmount.of("100").value());
    assertEquals(new BigDecimal("12000.00"), MoneyAmount.of("12000.00").value());
    assertEquals(new BigDecimal("25000.00"), MoneyAmount.of("25000.00").value());
    assertEquals(new BigDecimal("0"), MoneyAmount.of(new BigDecimal("0")).value());
    assertEquals(new BigDecimal("100"), MoneyAmount.of(new BigDecimal("100")).value());
  }

  @Test
  void rejectsNegativeAmounts() {
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("-1"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("-5"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("-0.01"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of(new BigDecimal("-1")));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of(new BigDecimal("-5")));
  }

  @Test
  void minPriceFieldAbsent() {
    // MIN_PRICE must be removed entirely
    boolean hasMinPrice = false;
    for (var f : MoneyAmount.class.getDeclaredFields()) {
      if (f.getName().equals("MIN_PRICE")) {
        hasMinPrice = true;
        break;
      }
    }
    if (hasMinPrice) {
      throw new AssertionError("MoneyAmount.MIN_PRICE must be absent");
    }
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
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of((BigDecimal) null));
  }

  @Test
  void representableAtUnchanged() {
    MoneyAmount a = MoneyAmount.of("12000.00");
    assertEquals(true, a.representableAt(2));
    assertEquals(false, MoneyAmount.of("10000.001").representableAt(2));
    assertEquals(true, MoneyAmount.of("10000.001").representableAt(3));
    assertEquals(true, MoneyAmount.of("0").representableAt(2));
    assertEquals(true, MoneyAmount.of("100").representableAt(2));
  }
}
