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
    assertEquals(new BigDecimal("12000.00"), MoneyAmount.of("12000.00").value());
    assertEquals(new BigDecimal("10000"), MoneyAmount.of("10000").value());
    assertEquals(new BigDecimal("25000.00"), MoneyAmount.of("25000.00").value());
  }

  @Test
  void acceptsFloorAt10000() {
    assertEquals(new BigDecimal("10000"), MoneyAmount.of("10000").value());
    assertEquals(new BigDecimal("10000.00"), MoneyAmount.of("10000.00").value());
    assertEquals(new BigDecimal("15000"), MoneyAmount.of("15000").value());
  }

  @Test
  void rejectsBelowFloor() {
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("9999.99"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("5000"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of("-1"));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of(new BigDecimal("9999.99")));
    assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of(new BigDecimal("0")));
  }

  @Test
  void representableAtUnchanged() {
    MoneyAmount a = MoneyAmount.of("10000.00");
    assertEquals(true, a.representableAt(2));
    assertEquals(false, MoneyAmount.of("10000.001").representableAt(2));
    assertEquals(true, MoneyAmount.of("10000.001").representableAt(3));
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
