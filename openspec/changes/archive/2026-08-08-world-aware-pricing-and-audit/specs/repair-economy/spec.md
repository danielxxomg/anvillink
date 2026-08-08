# Delta for repair-economy

## MODIFIED Requirements

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

## ADDED Requirements

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
