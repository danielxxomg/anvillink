# Exploration: price-per-mode-and-feedback

## Intent
Unified change: mandatory per-mode pricing (`price.hand` + `price.all`, no legacy `price:` fallback, minimum 10,000 per mode) + global configurable feedback polish (anvil sound + particles + `repair-success` with `{count}`/`{price}`).

## Current State

**Config/validation today:**
- `src/main/resources/config.yml:7` = `price: 25.00` flat. `FileConfigurationPort` parses `price:` top-level via manual line scan (avoids SnakeYAML skew), `BigDecimal(priceRaw)`, rejects negative, range-checks `admin.target-distance` 1-32, swaps `AtomicReference<ConfigSnapshot>` only after validation; invalid reload retains prior, invalid startup `new ConfigSnapshot(ZERO,8,empty,false)` disables activation (`AnvilLinkPlugin:53` warns).
- `ConfigurationPort.ConfigSnapshot(BigDecimal price, int targetDistance, Map messages, boolean activationEnabled)` — single price.
- `MoneyAmount(BigDecimal)` rejects null/negative/non-finite, `representableAt(fractionalDigits)` = `scale <= fractionalDigits` (`-1` unlimited). `ValidatedPrice` enforces scale at port boundary.
- `RepairActivation.activate()` loads `SignPort`, checks `anvillink.use`, validates `frontText` vs PDC `mode`, calls `ValidatedPrice.of(cfg.price(), economy.fractionalDigits())` fail-closed, plans `RepairPlanner(repairMode, view)`, if empty returns `Success(ZERO)` no Vault call, else single `economy.withdraw(player, amount)` then `scheduler.runOnServerThread(apply...)`; compensation via `EconomyPort.deposit` + `EquipmentPort.restore` + `OperationalReporter`.
- `VaultEconomyGateway` is global: `fractionalDigits()` delegates to `Economy.fractionalDigits()`, `withdraw/deposit` do `BigDecimal->double` finite check + precision-loss guard.

**Feedback today:** none. `MessagePort` (`MiniMessageMessagePort` 4.11.0) exposes `String render(key, placeholders)` via `ConfigurationPort` messages; no `repair-success`, no sound/particles. `TransactionResult.Success(BigDecimal amount)` carries only amount (no count).

**Domain purity:** `domain/**` imports no `org.bukkit.*`/`net.milkbowl.*`/`net.kyori.*`/`ConfigurationSection` — enforced by `AGENTS.md`. PDC key `danielxxomg:anvillink_repair_sign` permanent.

## Affected Areas

- `src/main/resources/config.yml` — BREAKING replace `price: 25.00` with mandatory `price.hand` + `price.all` (each >= 10000), add global `feedback:` block, keep existing error keys unchanged.
- `src/main/java/.../domain/ports/ConfigurationPort.java` — `ConfigSnapshot` to `priceHand, priceAll BigDecimal` + optional feedback fields; `activationEnabled` still gates both.
- `src/main/java/.../adapter/FileConfigurationPort.java` — extend hand-rolled parser to read nested `price.hand`/`price.all` under `price:` header, reject flat `price:`, require both, enforce >= 10000 per mode.
- `src/main/java/.../domain/ValidatedPrice.java` / `MoneyAmount.java` — add minimum-floor enforcement (10_000) — recommended at domain layer too (defense in depth).
- `src/main/java/.../domain/RepairActivation.java` — select price by `RepairMode` before `ValidatedPrice.of(selected, economy.fractionalDigits())`; single flat withdrawal per mode.
- `src/main/java/.../domain/TransactionResult.java` — consider `Success(amount, repairedCount)` or keep count in entrypoint; decision deferred to design.
- `src/main/java/.../domain/ports/FeedbackPort.java` *(new)* — pure domain port with PlayerId, neutral context.
- `src/main/java/.../adapter/BukkitFeedbackAdapter.java` *(new)* — adapter-only: reads feedback config, on server thread plays sound + particles + renders `repair-success`; failures swallowed, never throws into `RepairActivation`.
- `src/main/java/.../entrypoint/AnvilLinkPlugin.java` — wire new FeedbackPort, call only on `Success(non-zero)` after apply success.
- `openspec/specs/repair-economy/spec.md` — delta: per-mode pricing language, each mode >= 10_000.

## Approaches

**1. Strict per-mode ValidatedPrice (recommended)** — mandatory `price.hand` + `price.all`, no legacy. ConfigSnapshot explicit fields; FileConfigurationPort rejects `price: <scalar>` and requires both subkeys; `ValidatedPrice.of(priceForMode, fractionalDigits)` after mode resolved; minimum 10_000 enforced at parser + domain. RepairActivation switch is selector.

**2. Shared validator with single-entry abstraction (alternative)** — PriceCatalog(Map) or PricePolicy built at config load. Rejected: couples config load to runtime provider precision; adds indirection for only two modes.

**3. Global feedback toggle (recommended)** — `feedback:` block with enabled/sound/particles + `repair-success` template; BukkitFeedbackAdapter no-ops when disabled, otherwise plays anvil sound + particles; failures swallowed.

## Recommendation

- **Pricing:** Approach 1 — explicit fields, no fallback, document as MAJOR bump. `RepairActivation` selects price after `rec.get().mode()` known.
- **Feedback:** Global defaults, presentation-only. FeedbackPort (domain) + BukkitFeedbackAdapter (adapter) + scheduler dispatch; called only on `Success(amount != ZERO)` after apply; wrapped in try/catch.

## Risks
- Transaction boundary leak if feedback throws or does Vault work.
- Config migration: old flat `price:` becomes invalid; activation-disabled until manual edit; needs release note + precise error messages.
- Provider precision per mode: fractionalDigits global, each mode validated independently at activation.
- Scheduler threading: sound/particles require server thread.
- Domain purity: Sound/Particle/Location must stay in adapter only.

## Ready for Proposal
Yes — pricing selector, feedback isolation, and all file boundaries identified; approach 1 + global configurable feedback is minimal spec-compliant design.
