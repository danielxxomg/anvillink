// AnvilLink — paid repair signs for Minecraft servers
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.danielxxomg.anvillink.domain;

import java.math.BigDecimal;

/** Fail-closed transaction outcome for one flat-price activation. Pure domain, no Bukkit/Vault. */
public sealed interface TransactionResult
    permits TransactionResult.Success,
        TransactionResult.NoProvider,
        TransactionResult.InsufficientFunds,
        TransactionResult.InvalidResponse,
        TransactionResult.ApplyFailure,
        TransactionResult.CompensationSuccess,
        TransactionResult.CompensationFailed,
        TransactionResult.RestorationFailed {
  record Success(BigDecimal amount) implements TransactionResult {}

  record NoProvider() implements TransactionResult {}

  record InsufficientFunds() implements TransactionResult {}

  record InvalidResponse(String reason) implements TransactionResult {}

  record ApplyFailure(String reason) implements TransactionResult {}

  record CompensationSuccess(BigDecimal amount) implements TransactionResult {}

  record CompensationFailed(BigDecimal amount, String reason) implements TransactionResult {}

  record RestorationFailed(BigDecimal amount, String reason) implements TransactionResult {}
}
