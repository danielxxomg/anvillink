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
  void acceptsZeroAndLowPriceWithFractionalDigits2() {
    // Slice 1: >=0 replaces MIN_PRICE — 0 and small values pass
    assertEquals(new BigDecimal("0"), ValidatedPrice.of("0", 2).value());
    assertEquals(new BigDecimal("100"), ValidatedPrice.of("100", 2).value());
    assertEquals(new BigDecimal("0"), ValidatedPrice.of(new BigDecimal("0"), 2).value());
    assertEquals(new BigDecimal("100"), ValidatedPrice.of(new BigDecimal("100"), 2).value());
  }

  @Test
  void acceptsFiniteNonNegativeWithCompatibleScale() {
    ValidatedPrice price = ValidatedPrice.of("12000.00", 2);
    assertEquals(new BigDecimal("12000.00"), price.amount().value());
    assertEquals(new BigDecimal("12000.00"), price.value());
    // fractionalDigits=-1 means unlimited (Vault contract).
    assertEquals(new BigDecimal("12000.12345"), ValidatedPrice.of("12000.12345", -1).value());
  }

  @Test
  void rejectsNegative() {
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("-1", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("-0.01", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of(new BigDecimal("-1"), 2));
  }

  @Test
  void rejectsNonFinite() {
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("NaN", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("Infinity", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("-Infinity", 2));
  }

  @Test
  void rejectsPrecisionOverflow100_001WithFd2() {
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("100.001", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("12000.001", 2));
    assertThrows(
        IllegalArgumentException.class, () -> ValidatedPrice.of(new BigDecimal("100.001"), 2));
  }

  @Test
  void scaleCheckUsesBigDecimalScaleAgainstFractionalDigits() {
    // Exactly at limit should pass; one beyond should fail.
    ValidatedPrice atLimit = ValidatedPrice.of("100.00", 2);
    assertTrue(atLimit.amount().representableAt(2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("100.000", 2));
  }

  @Test
  void rejectsNullOrBlank() {
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of((String) null, 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of("   ", 2));
    assertThrows(IllegalArgumentException.class, () -> ValidatedPrice.of((BigDecimal) null, 2));
  }
}
