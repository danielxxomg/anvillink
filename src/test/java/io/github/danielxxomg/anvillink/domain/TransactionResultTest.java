// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionResultTest {

  @Test
  void successCarriesAmount() {
    TransactionResult result = new TransactionResult.Success(new BigDecimal("25.00"));
    assertInstanceOf(TransactionResult.Success.class, result);
    assertEquals(new BigDecimal("25.00"), ((TransactionResult.Success) result).amount());
    assertInstanceOf(TransactionResult.class, result);
  }

  @Test
  void noProviderIsFailClosed() {
    TransactionResult result = new TransactionResult.NoProvider();
    assertInstanceOf(TransactionResult.NoProvider.class, result);
    assertInstanceOf(TransactionResult.class, result);
  }

  @Test
  void insufficientFundsIsFailClosed() {
    TransactionResult result = new TransactionResult.InsufficientFunds();
    assertInstanceOf(TransactionResult.InsufficientFunds.class, result);
  }

  @Test
  void invalidResponseIsFailClosed() {
    TransactionResult result = new TransactionResult.InvalidResponse("mismatch");
    assertInstanceOf(TransactionResult.InvalidResponse.class, result);
    assertEquals("mismatch", ((TransactionResult.InvalidResponse) result).reason());
  }

  @Test
  void applyFailureIsFailClosed() {
    TransactionResult result = new TransactionResult.ApplyFailure("io-error");
    assertInstanceOf(TransactionResult.ApplyFailure.class, result);
  }

  @Test
  void compensationOutcomesAreDistinct() {
    TransactionResult cs = new TransactionResult.CompensationSuccess(new BigDecimal("25.00"));
    TransactionResult cf =
        new TransactionResult.CompensationFailed(new BigDecimal("25.00"), "deposit-failed");
    TransactionResult rf =
        new TransactionResult.RestorationFailed(new BigDecimal("25.00"), "restore-failed");
    assertInstanceOf(TransactionResult.CompensationSuccess.class, cs);
    assertInstanceOf(TransactionResult.CompensationFailed.class, cf);
    assertInstanceOf(TransactionResult.RestorationFailed.class, rf);
    assertTrue(cs instanceof TransactionResult);
    assertTrue(cf instanceof TransactionResult);
    assertTrue(rf instanceof TransactionResult);
  }

  @Test
  void sealedHierarchyPermitsExactlyEightSubtypes() {
    Class<?>[] permitted = TransactionResult.class.getPermittedSubclasses();
    assertEquals(8, permitted.length);
  }
}
