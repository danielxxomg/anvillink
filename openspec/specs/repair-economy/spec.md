# repair-economy Specification

## Purpose

Define the fail-closed Vault transaction boundary for one fixed-price repair-sign activation.

## Requirements

### Requirement: Valid per-mode configured price
The plugin MUST require `price.hand` and `price.all` under `price:`; each MUST be finite, non-negative, `>= 10,000`, and representable at `economy.fractionalDigits()` without rounding. One flat amount per activation MUST be selected by `RepairMode` after PDC validation. Bare `price: <scalar>` MUST be rejected. Invalid MUST fail closed: `activationEnabled=false` at startup; invalid reload MUST retain prior snapshot with no partial apply and report operator failure. Floor and precision MUST be enforced at parse (`FileConfigurationPort`) and domain (`MoneyAmount`/`ValidatedPrice`); per-mode precision MUST be validated at activation time.

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

### Requirement: No eligible target means no charge
The plugin MUST finish target resolution and plan validation before calling Vault. An empty plan MUST produce no withdrawal, no mutation, and a no-eligible-item outcome.

#### Scenario: Undamaged or ineligible equipment is free
- GIVEN every resolved target is empty, undamaged, unbreakable, or otherwise ineligible
- WHEN the plan is evaluated
- THEN no Vault withdrawal occurs and all items remain unchanged

### Requirement: Vault and provider absence fail closed
Vault MUST be a soft runtime dependency, and the plugin MUST resolve a usable economy provider at transaction time. If Vault or the provider is absent or unusable, the plugin MUST perform no withdrawal and no repair.

#### Scenario: Provider is unavailable
- GIVEN a valid sign and non-empty repair plan but no usable Vault economy provider
- WHEN activation is attempted
- THEN no money is withdrawn, no item is changed, and a diagnostic failure is returned

### Requirement: Single withdrawal and failed-payment handling
The plugin MUST request at most one withdrawal after non-empty plan validation using the mode-selected price and MUST require `transactionSuccess()` before mutation. Failed withdrawals MUST NOT trigger a second attempt.

#### Scenario: Single withdrawal uses selected price
- GIVEN non-empty `HAND` plan with `price.hand=15000`
- WHEN withdrawal attempted
- THEN amount equals `price.hand` and no retry on failure

### Requirement: Compensating refund and unreconciled observability
After withdrawal, an unexpected apply failure MUST attempt restoration only from snapshots for slots mutated before that failure; untouched planned slots MUST remain untouched. It MUST trigger exactly one compensating deposit for that amount. A successful deposit MUST yield no net charge; a failed deposit MUST emit a high-severity unreconciled-refund operational event and report failure. If restoration fails, it MUST still attempt deposit, emit structured high-severity operator evidence identifying unresolved inventory and economy state, notify the player without claiming success, and MUST NOT automatically retry mutation or withdrawal in that activation.

#### Scenario: Successful compensation restores payment state
- GIVEN withdrawal succeeds, application mutates one or more planned slots, and restoration of those slots completes
- WHEN the compensating deposit succeeds for the withdrawn amount
- THEN only mutated slots are restored, untouched planned slots remain untouched, the player has no net charge, and activation reports failure

#### Scenario: Failed compensation is observable
- GIVEN withdrawal succeeds, application fails, restoration of mutated slots runs, and the compensating deposit fails
- WHEN recovery completes
- THEN a high-severity unreconciled-refund event is recorded and the player receives a failure outcome

#### Scenario: Restoration failure is terminal and observable
- GIVEN withdrawal succeeds, application fails, and restoration of a mutated slot also fails
- WHEN recovery completes
- THEN one compensating deposit is attempted, untouched planned slots remain untouched, unresolved inventory/economy state is emitted as structured high-severity operator evidence, the player is notified without a success claim, and no mutation or withdrawal retry occurs

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
