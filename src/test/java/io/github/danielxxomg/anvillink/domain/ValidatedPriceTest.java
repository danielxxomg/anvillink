// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ValidatedPriceTest {

  @Test
  void acceptsFiniteNonNegativeWithCompatibleScale() {
    ValidatedPrice price = ValidatedPrice.of("12000.00", 2);
    assertEquals(new BigDecimal("12000.00"), price.amount().value());
    assertEquals(new BigDecimal("12000.00"), price.value());
  }

  @Test
  void acceptsFloorAndIntegerWithGenerousDigits() {
    assertEquals(new BigDecimal("10000"), ValidatedPrice.of("10000", 2).value());
    assertEquals(new BigDecimal("15000"), ValidatedPrice.of("15000", 2).value());
    // fractionalDigits=-1 means unlimited (Vault contract).
    assertEquals(new BigDecimal("10000.12345"), ValidatedPrice.of("10000.12345", -1).value());
  }

  @Test
  void rejectsNegative() {
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("-1", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("-0.01", 2));
  }

  @Test
  void rejectsNonFinite() {
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("NaN", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("Infinity", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("-Infinity", 2));
  }

  @Test
  void rejectsBelowFloor() {
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("9999.99", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("5000", 2));
    assertThrows(
        IllegalArgumentException.class, () -> ValidatedPrice.of(new BigDecimal("9999"), 2));
  }

  @Test
  void rejectsPrecisionOverflowAgainstFractionalDigits() {
    // 12000.001 needs 3 fractional digits but provider reports 2.
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("12000.001", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("10000.001", 0));
    assertThrows(
        IllegalArgumentException.class, () -> ValidatedPrice.of(new BigDecimal("10000.001"), 2));
  }

  @Test
  void scaleCheckUsesBigDecimalScaleAgainstFractionalDigits() {
    // Exactly at limit should pass; one beyond should fail.
    ValidatedPrice atLimit = ValidatedPrice.of("10000.00", 2);
    assertTrue(atLimit.amount().representableAt(2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("10000.000", 2));
  }

  @Test
  void rejectsNullOrBlank() {
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of((String) null, 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("   ", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of((BigDecimal) null, 2));
  }
}
