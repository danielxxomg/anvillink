// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain.ports;

import java.math.BigDecimal;
import java.util.UUID;

/** Pure-domain economy port. No Vault types. */
public interface EconomyPort {
  sealed interface Withdrawal
      permits Withdrawal.Success,
          Withdrawal.NoProvider,
          Withdrawal.InsufficientFunds,
          Withdrawal.InvalidResponse {
    record Success(BigDecimal amountWithdrawn) implements Withdrawal {}

    record NoProvider() implements Withdrawal {}

    record InsufficientFunds() implements Withdrawal {}

    record InvalidResponse(String reason) implements Withdrawal {}
  }

  sealed interface Deposit permits Deposit.Success, Deposit.Failure {
    record Success(BigDecimal amountDeposited) implements Deposit {}

    record Failure(String reason) implements Deposit {}
  }

  Withdrawal withdraw(UUID playerId, BigDecimal amount);

  Deposit deposit(UUID playerId, BigDecimal amount);

  int fractionalDigits();
}
