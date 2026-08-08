# repair-economy Specification

## Purpose

Define the fail-closed Vault transaction boundary for one fixed-price repair-sign activation.

## Requirements

### Requirement: Valid per-mode configured price

`price.hand` and `price.all` under `price:` MUST each be finite, non-negative `>=0` and representable at `economy.fractionalDigits()` without rounding. Bare `price: <scalar>` MUST be rejected. Invalid MUST fail closed: `activationEnabled=false` at startup; reload retains prior, no partial apply, report failure. Floor/precision MUST be enforced at parse and domain; precision at activation via `ValidatedPrice.of`.
(Previously `>=10_000` via `MIN_PRICE`; now `>=0`, `MIN_PRICE` removed. Defaults `12000/25000` unchanged.)

#### Scenario: HAND charges price.hand
- GIVEN PDC `HAND` and `price.hand=12000.00` representable
- WHEN non-empty HAND plan succeeds
- THEN one withdrawal of `12000.00` MUST be requested

#### Scenario: ALL charges price.all
- GIVEN PDC `ALL` and `price.all=25000.00` representable
- WHEN non-empty ALL plan succeeds
- THEN one withdrawal of `25000.00` MUST be requested

#### Scenario: Flat scalar rejected
- GIVEN `price: 25.00` as bare scalar
- WHEN loaded
- THEN it MUST fail closed, no withdrawal

#### Scenario: Missing per-mode price rejected
- GIVEN `price.hand` or `price.all` absent
- WHEN loaded
- THEN it MUST fail closed, no withdrawal

#### Scenario: Below-floor rejected (now negative/non-finite)
- GIVEN `price.hand=-1` or non-finite
- WHEN parsed or `ValidatedPrice.of` invoked
- THEN it MUST be rejected, no withdrawal

#### Scenario: Zero and low prices accepted when representable
- GIVEN `price.hand=0` or `100` with `fractionalDigits=2`
- WHEN parsed and validated
- THEN it MUST be accepted (previously rejected)

#### Scenario: Per-mode invalid precision fails closed
- GIVEN `price.hand=100.001` with `fractionalDigits=2`
- WHEN HAND activation attempted
- THEN no withdrawal MUST occur; other mode MUST NOT bypass

### Requirement: Per-world price overrides with global fallback

`worlds:` is optional in `config.yml`. Each `worlds.<world>` MAY contain `hand` and/or `all`; missing key MUST fallback to global. Present values MUST be finite `>=0` (floor at parse; scale at activation). Unknown subkeys MUST be warned/ignored. Lookup MUST be `player.getWorld().getName()` exact `Map.get`; unknown→global. Null/empty `worldName`→global; no normalization. Present unparseable/negative/non-finite MUST fail whole-file (`activationEnabled=false` at startup, retain prior on reload, no partial apply, `worlds.<name>.hand: <reason>`). Valid partial is lenient; scale failure fails that activation with no withdrawal.

#### Scenario: World hand-only overrides hand, all falls back to global
- GIVEN `worlds.world.hand=5000`, `price.all=25000`
- WHEN HAND and ALL in `world`
- THEN HAND MUST charge `5000`, ALL MUST charge `25000`

#### Scenario: World all-only overrides all
- GIVEN `worlds.nether.all=1000`
- WHEN ALL in `nether`
- THEN it MUST charge `1000`; HAND in `nether` MUST charge global

#### Scenario: Unknown world uses global both
- GIVEN no entry for `world_the_end`
- WHEN activation with `worldName=world_the_end`
- THEN both modes MUST resolve to global

#### Scenario: Case mismatch falls back to global
- GIVEN `worlds.world.hand=5000`
- WHEN `worldName=World`
- THEN it MUST resolve to global

#### Scenario: Null or empty worldName resolves to global
- GIVEN `worldName=null` or `""`
- WHEN activation attempted
- THEN it MUST resolve to global

#### Scenario: Negative or unparseable per-world hand fails whole file closed
- GIVEN `worlds.world.hand=-5` or `abc` or non-finite
- WHEN file loaded
- THEN load MUST fail closed, retain prior on reload, no withdrawal

#### Scenario: Per-world invalid retains prior and does not affect other worlds
- GIVEN prior `worlds.a.hand=100` valid; reload adds `worlds.b.hand=-1`
- WHEN reload fails
- THEN `worlds.a.hand=100` MUST remain; no partial apply

### Requirement: World-aware effective price resolution at activation

`RepairActivation.activate(SignId, UUID, String worldName)` MUST resolve effective via `worldPrices.get(worldName)` exact (present key else global; null/empty/unknown→global) before `ValidatedPrice.of(effective,fractionalDigits)`. Scale failure MUST fail closed per-activation with no withdrawal. Domain stays pure — `String` only, no `org.bukkit.*`.

#### Scenario: World price scale invalid fails closed per-activation
- GIVEN `worlds.world.hand=100.001` with `fractionalDigits=2`
- WHEN HAND activation in `world`
- THEN it MUST fail closed, no withdrawal

#### Scenario: Valid world price passes scale and withdraws
- GIVEN `worlds.world.hand=0` representable and non-empty HAND plan
- WHEN activation in `world`
- THEN withdrawal of `0` MUST be requested

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
