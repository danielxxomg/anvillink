# Proposal: price-per-mode-and-feedback

## Intent
Replace flat `price: 25.00` with mandatory per-mode pricing (`price.hand` + `price.all`, each >= 10,000, no legacy fallback) and add global success feedback (anvil sound + particles + `repair-success` with `{count}`/`{price}`). Fixes floor, adds polish; transaction safety and Paper 1.18.2 / Java 17 floor unchanged.

## Target users and situations
- **Operators (Paper):** set HAND (1 slot) vs ALL (6 slots); tune global `feedback.*`. Validated at startup/reload.
- **Players:** get `{count}` + `{price}` confirmation only on paid success; existing errors unchanged.

## Current gap
- One price for both modes; no floor allows trivial prices.
- No success presentation; `TransactionResult.Success(BigDecimal)` lacks count.
- `ConfigSnapshot(BigDecimal price)` + `FileConfigurationPort` assume single price; feedback not configurable.

## Outcome
Price by `RepairMode` after PDC load, validated at parse + domain (fail-closed), one flat withdrawal preserved, `repair-success` only on `Success(non-zero)` on server thread; feedback never affects economy.

## Scope

### In Scope
- BREAKING: mandatory `price.hand` + `price.all`; flat `price: 25.00` INVALID → `activationEnabled=false` (fail-closed).
- Floor 10,000 per mode at `FileConfigurationPort` AND `ValidatedPrice`/`MoneyAmount`.
- `ConfigSnapshot` → `priceHand, priceAll BigDecimal` + global feedback fields.
- `RepairActivation` selects price by mode before `ValidatedPrice.of(selected, fractionalDigits)`.
- `TransactionResult` surfaces `repairedCount` for `{count}`; `{price}` via `BigDecimal.toPlainString`.
- New `FeedbackPort` (domain) + `BukkitFeedbackAdapter` (adapter-only) for global `feedback:` block.
- `repair-success` only on `Success(non-zero)`; all prior error keys unchanged.
- `repair-economy` spec delta; atomic reload retains prior valid snapshot.

### Out of Scope
- Per-item/per-durability pricing, per-mode feedback, DB/metrics, NMS, GUI, storage repair, Folia scheduler.

### Adjacent follow-ups
- Audit log for economy/feedback failures; per-world price policy.

## Capabilities

### New Capabilities
- None — feedback is presentation within existing capabilities.

### Modified Capabilities
- `repair-economy`: per-mode pricing (each >= 10,000), mode-selected single withdrawal, `repair-success` contract.

## Approach
Exploration Approach 1 (strict per-mode fields) + global feedback toggle — selected.

- **Config:** hand parser rejects bare `price:` scalar, requires `price.hand`/`price.all` under `price:` header, each >= 10,000. Global `feedback:` block + `messages.repair-success`.
- **Domain:** `ConfigSnapshot(priceHand, priceAll, ...)`. `MoneyAmount`/`ValidatedPrice` add `>= 10_000` invariant alongside existing finite/non-negative + `representableAt(fractionalDigits)` checks.
- **Selector:** `RepairActivation` switches on `rec.get().mode()` after tamper check, then `ValidatedPrice.of(selected, economy.fractionalDigits())`.
- **Feedback:** `FeedbackPort.play(PlayerId, count, price)` pure port. `BukkitFeedbackAdapter` runs via `SchedulerPort` on server thread, renders via `MessagePort` (MiniMessage 4.11.0), swallowed try/catch — never touches transaction.
- **Wiring:** `AnvilLinkPlugin` calls feedback only after apply success and `withdrawn != ZERO`.
- Invariants preserved: domain purity (`domain/**` no `org.bukkit.*`/`net.milkbowl.*`/`net.kyori.*`), single-withdrawal + compensation.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/config.yml` | Modified | `price.hand`/`price.all` + `feedback:` + `repair-success` |
| `domain/ports/ConfigurationPort.java` | Modified | `ConfigSnapshot(priceHand, priceAll, ...)` |
| `adapter/FileConfigurationPort.java` | Modified | Nested parse, reject flat, enforce >= 10_000 |
| `domain/MoneyAmount.java` + `ValidatedPrice.java` | Modified | Floor invariant |
| `domain/RepairActivation.java` | Modified | Mode price selector |
| `domain/TransactionResult.java` | Modified | `Success(amount, repairedCount)` |
| `domain/ports/FeedbackPort.java` | New | Pure port |
| `adapter/BukkitFeedbackAdapter.java` | New | Sound/particles/MiniMessage, fail-safe |
| `entrypoint/AnvilLinkPlugin.java` | Modified | Wire feedback, gate on non-zero success |
| `openspec/specs/repair-economy/spec.md` | Modified | Delta spec |

## Compatibility / Support-tier policy
**Paper 1.18.2 / Java 17, `compileOnly` Paper + VaultAPI, `--release 17`, public APIs only — unchanged.** BREAKING config = **MAJOR SemVer bump** (per `platform-compatibility`). Migration: old `price:` → must edit to `price.hand`/`price.all`; invalid startup = `activationEnabled=false` until fixed; invalid reload retains prior valid config atomically, reports operator failure, applies no partial values. No PDC namespace change.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Existing servers break on upgrade | High | Precise error (`missing price.hand/all`), MAJOR bump, release note, fail-closed |
| Feedback exception leaks into transaction | Low | Adapter try/catch, separate dispatch |
| Provider precision varies | Low | Validate selected price per activation |
| Domain purity violation | Low | Port pure, Bukkit only in adapter |
| Off-thread sound/particles | Med | `SchedulerPort.runOnServerThread` |

## Rollback Plan
Revert PR: restore `price: 25.00`, `ConfigSnapshot(price)`, single-price parser/validation/`RepairActivation`, delete `FeedbackPort`/`BukkitFeedbackAdapter`, revert spec delta. No persisted data migration. If released, revert tag + `CHANGELOG.md` withdrawal. Operators recover by restoring last valid `config.yml` and `/anvillink reload`.

## Dependencies
- VaultAPI 1.7 soft dep, `fractionalDigits()` global; Adventure MiniMessage 4.11.0; no new external services.

## Success Criteria
- [ ] Both `price.hand`/`price.all` required >= 10_000 at parse + domain; flat `price:` → startup `activationEnabled=false`; invalid reload retains prior.
- [ ] HAND charges `price.hand`, ALL charges `price.all`; one flat withdrawal per non-empty plan; precision validated per mode.
- [ ] `repair-success` only on `Success(non-zero)` with correct `{count}`/`{price}` (`toPlainString`); prior errors unchanged.
- [ ] `feedback.enabled=false` silences sound/particles/message; feedback failure never triggers compensation.
- [ ] Domain has no Bukkit/Vault/Adventure imports; single-withdrawal + compensation unchanged.
- [ ] `clean test spotlessCheck build` passes on Paper 1.18.2 floor.
