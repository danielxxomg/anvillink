# repair-economy Specification

## Purpose

Define the fail-closed Vault transaction boundary for one fixed-price repair-sign activation.

## Requirements

### Requirement: Valid fixed configured price
The plugin MUST accept a configured price only when it is finite, non-negative, and representable at the provider-supported precision without implicit rounding. It MUST preserve the configured monetary value and use one flat amount per activation, independent of item count, slot count, or damage.

#### Scenario: One successful activation has one flat charge
- GIVEN a non-empty `ALL` plan and a valid configured price of `25.00`
- WHEN withdrawal and repair application both succeed
- THEN exactly one withdrawal of `25.00` is requested

#### Scenario: Invalid precision or value fails closed
- GIVEN the price is negative, non-finite, or cannot be represented at provider precision without rounding
- WHEN an activation is attempted
- THEN no withdrawal or repair occurs and a configuration failure is reported

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
The plugin MUST request at most one withdrawal after validating a non-empty plan and MUST require `transactionSuccess()` before item mutation. Insufficient funds and every other failed withdrawal MUST leave the plan unapplied and MUST NOT trigger a second withdrawal.

#### Scenario: Insufficient funds do not repair
- GIVEN a non-empty plan and a provider response identifying insufficient funds
- WHEN the single withdrawal is attempted
- THEN no item is mutated and no additional withdrawal is requested

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
