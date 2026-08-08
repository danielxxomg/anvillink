# Delta for repair-economy

## MODIFIED Requirements

### Requirement: Valid per-mode configured price
The plugin MUST require `price.hand` and `price.all` under `price:`; each MUST be finite, non-negative, `>= 10,000`, and representable at `economy.fractionalDigits()` without rounding. One flat amount per activation MUST be selected by `RepairMode` after PDC validation. Bare `price: <scalar>` MUST be rejected. Invalid MUST fail closed: `activationEnabled=false` at startup; invalid reload MUST retain prior snapshot with no partial apply and report operator failure. Floor and precision MUST be enforced at parse (`FileConfigurationPort`) and domain (`MoneyAmount`/`ValidatedPrice`); per-mode precision MUST be validated at activation time.
(Previously: single flat `price` with finite/non-negative/precision checks only.)

#### Scenario: HAND charges price.hand
- GIVEN PDC `HAND` and valid `price.hand=12000.00`
- WHEN non-empty HAND plan succeeds
- THEN one withdrawal of `12000.00` is requested

#### Scenario: ALL charges price.all
- GIVEN PDC `ALL` and valid `price.all=25000.00`
- WHEN non-empty ALL plan succeeds
- THEN one withdrawal of `25000.00` is requested

#### Scenario: Flat scalar rejected
- GIVEN `price: 25.00` as bare scalar
- WHEN loaded at startup
- THEN `activationEnabled=false`, no repair; on reload prior snapshot retained and failure reported

#### Scenario: Missing per-mode price rejected
- GIVEN `price.hand` or `price.all` absent
- WHEN loaded at startup or reload
- THEN invalid fail-closed, no withdrawal

#### Scenario: Below-floor rejected
- GIVEN `price.hand=9999.99` or `price.all=5000`
- WHEN parsed or `ValidatedPrice.of`/`MoneyAmount.of` invoked
- THEN rejected, no withdrawal or repair

#### Scenario: Per-mode invalid precision fails closed
- GIVEN `price.hand=10000.001` with `fractionalDigits=2`
- WHEN HAND activation attempted
- THEN no withdrawal/repair, configuration failure reported; other mode's validity MUST NOT bypass it

### Requirement: Single withdrawal and failed-payment handling
The plugin MUST request at most one withdrawal after non-empty plan validation using the mode-selected price and MUST require `transactionSuccess()` before mutation. Failed withdrawals MUST NOT trigger a second attempt.
(Previously: single flat price; now mode-selected price.)

#### Scenario: Single withdrawal uses selected price
- GIVEN non-empty `HAND` plan with `price.hand=15000`
- WHEN withdrawal attempted
- THEN amount equals `price.hand` and no retry on failure

## ADDED Requirements

### Requirement: Transaction success carries repaired count
`TransactionResult.Success` MUST carry `amount` (`BigDecimal` withdrawn) and `repairedCount` (slots repaired). Empty plan MUST yield `Success(ZERO)` with no charge. `{count}` consumers MUST use `repairedCount`.

#### Scenario: Success carries amount and count
- GIVEN 3 eligible slots, `price.all=20000`
- WHEN withdrawal and apply succeed
- THEN `Success(amount=20000, repairedCount=3)`

#### Scenario: Empty plan yields zero success
- GIVEN empty/ineligible plan
- WHEN evaluated
- THEN `Success(ZERO)` with no withdrawal or feedback

### Requirement: Success feedback presentation
`repair-success` MUST be rendered only on `Success` with `amount != ZERO` after successful apply. `{count}` MUST be `repairedCount.toString()` and `{price}` MUST be `amount.toPlainString()` exact. Prior error keys MUST remain unchanged. `feedback.enabled=false` MUST suppress sound, particles, and message. Feedback MUST run on server thread via `SchedulerPort`/`FeedbackPort`; failure MUST be swallowed with no retry, deposit, or restoration.
#### Scenario: Paid success renders repair-success
- GIVEN `feedback.enabled=true` and `Success(20000, 4)`
- WHEN dispatched on server thread
- THEN `repair-success` rendered via `MessagePort` with `{count}=4` `{price}=20000`

#### Scenario: No feedback on zero or failure
- GIVEN `Success(ZERO)` or any failure
- WHEN completed
- THEN no sound/particles/`repair-success`

#### Scenario: Disabled feedback is silent
- GIVEN `feedback.enabled=false` and paid `Success`
- WHEN completed
- THEN no sound/particles/message

#### Scenario: Feedback failure never affects transaction
- GIVEN paid `Success` where rendering throws
- WHEN feedback attempted
- THEN swallowed, transaction stays `Success`, no compensation

#### Scenario: Error messages unchanged
- GIVEN any failure path
- WHEN messaging
- THEN prior keys/templates used; `repair-success` not emitted
