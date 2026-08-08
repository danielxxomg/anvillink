# Changelog

All notable changes to AnvilLink will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] — BREAKING per-mode pricing + success feedback (MAJOR)

### BREAKING

- `config.yml` `price: 25.00` scalar removed — replaced by mandatory `price.hand` (`>= 10_000`) + `price.all` (`>= 10_000`) nested block under `price:` header. Bare scalar, missing `price.hand`, missing `price.all`, or below-floor values fail-closed (`activationEnabled=false` at startup, `ReloadOutcome.Failure` retaining prior on reload). Migration: edit existing `config.yml` to `price.hand: 12000.00` / `price.all: 25000.00`.
- `ConfigurationPort.ConfigSnapshot` now `priceHand`/`priceAll` + `feedbackEnabled`/`feedbackSound`/`feedbackParticles` (global defaults: enabled=true, sound=BLOCK_ANVIL_USE, particles=CRIT).
- `TransactionResult.Success` now `Success(BigDecimal amount, int repairedCount)` — `repairedCount` for `{count}`, `amount.toPlainString()` for `{price}`.

### Added

- Per-mode floor `MoneyAmount.MIN_PRICE=10_000` mirrored in `ValidatedPrice`; parser and domain both enforce.
- `RepairActivation` mode selector: `priceHand`/`priceAll` switched on PDC `RepairMode` before `ValidatedPrice.of(selected, fractionalDigits)`; per-mode precision fail-closed, empty plan `Success(ZERO,0)` no withdrawal, `repairedCount=planned.size()`.
- `FeedbackPort` pure port `play(SignPort.PlayerId, BigDecimal, int)` + `BukkitFeedbackAdapter` (server-thread via `SchedulerPort`, swallowed, never touches economy) wired in `AnvilLinkPlugin` gated on `amount != ZERO`.
- `messages.repair-success: "<green>Repaired {count} items for {price}.</green>"` and `feedback:` block in `config.yml`; disabled feedback is silent, throw is swallowed.
- `PricePerModeE2ETest` + `FeedbackE2ETest` (MockBukkit 4.110) covering per-mode withdrawals, scalar rejection, reload retention, paid HAND/ALL count/price, zero/disabled/throw-silence.

## [Unreleased] — AnvilLink initial release

### Added

- Gradle scaffold: pinned wrapper (8.14.3), version catalog, Java 17 toolchain
  with `--release 17` bytecode floor (class major version 61), Spotless
  formatting, Shadow JAR with Adventure 4.11.0 relocation.
- Plugin metadata: `plugin.yml` with stable identity (`AnvilLink`,
  `io.github.danielxxomg.anvillink.entrypoint.AnvilLinkPlugin`), `api-version: 1.13`,
  `softdepend: [Vault]`, and `anvillink.create` / `anvillink.use` /
  `anvillink.manage` permissions; commands `anvillink` (inspect, rerender, reload).
- Default configuration: fixed price `25.00`, MiniMessage message templates,
  `admin.target-distance: 8` (1–32, reloadable with atomic swap).
- Domain value types: `RepairMode`, `EquipmentSlotId`, `MoneyAmount`,
  `SignRecord` (versioned PDC byte layout) — no Bukkit/Vault dependencies.
- Parser and planner: `SignParser` (`[repair]` + `HAND|ALL`), `RepairPlanner`
  (HAND→main-hand, ALL→six equipment slots, never storage; only Damageable
  positive-damage breakable items), `RepairPlan`/`PlannedSlot`/`EquipmentView`/
  `ItemView`/`ItemSnapshot`.
- Ports: `SignPort`, `EquipmentPort`, `EconomyPort`, `SchedulerPort`,
  `ConfigurationPort`, `MessagePort`, `OperationalReporter` (neutral IDs,
  BigDecimal, opaque snapshots; domain has no Bukkit/Vault/Adventure imports).
- Transaction types: `TransactionResult` (sealed: success, no-provider,
  insufficient-funds, invalid-response, apply-failure, compensation outcomes)
  and `ValidatedPrice` (finite non-negative, scale vs `fractionalDigits`).
- Activation use case: `RepairActivation` — PDC/tamper/permission gate, plan,
  validated price, single Vault withdrawal, scheduler-bound apply, snapshot
  restoration + compensating deposit; payment/apply failures are terminal and
  observable.
- PDC identity: `danielxxomg:anvillink_repair_sign` BYTE_ARRAY
  (magic|schema=1|mode|creator UUID|authorized-create) — permanent namespace
  independent of display brand.
- Adapters: `PdcSignIdentity`, `SignLifecycleListener` (create/break, blue
  canonical `[repair]`/`HAND|ALL` text, `anvillink.create|manage` gate);
  `InteractionFilter` (main-hand + right-click only); `VaultEconomyGateway`
  (BigDecimal↔double exactness, single withdrawal, response validation, one
  compensating deposit); `BukkitEquipmentPort` (inventory read, setDamage(0),
  snapshot restore); `FileConfigurationPort` (AtomicReference swap, YAML
  validation); `MiniMessageMessagePort` (String through port, Adventure
  relocated to `anvillink.libs.kyori`); `BukkitSchedulerAdapter` (Folia
  rejection); `AdminCommandHandler` (`inspect|rerender|reload`).
- Entrypoint: `AnvilLinkPlugin` wiring adapters, listeners, config, scheduler,
  Vault, and Paper lifecycle.
- MockBukkit integration: full sign lifecycle E2E (create, permission,
  tamper, ALL/storage, no-eligible, insufficient-funds, duplicate-hand,
  Vault-absent, inspect/rerender, reload).
- Platform gates: real Vault-provider evidence via `ReleaseClaimGate`
  (EssentialsX), `SemVerSupportMatrix` (version vs matrix separate),
  `CompatibilityEvidence` + `compatibility/evidence.json` (mandatory + probe
  rows), CI `build.yml` + `smoke.yml` (mandatory 5 + probe Paper 26.x probe
  as continue-on-error).
- Documentation: `README.md` (H1, benefit, quick-start, support-tier table,
  permissions, config, FAQ, `SoftwareApplication` JSON-LD), `LICENSE`
  (GPL-3.0-or-later).

### Notes

- Java 17 bytecode floor (major 61) for production; tests run on JDK 21
  (MockBukkit 4.110.0). Paper 1.18.2 `compileOnly` + Paper 1.21.11 test
  runtime; Paper/Vault never shaded.
- Evidence-gated tiers: Paper certified only when all mandatory Paper rows
  pass; Spigot/Purpur verified after separate smoke; Folia experimental;
  Paper 26.x uncertified until Java 25 probe passes. Probe failure does not
  block certified ranges. SemVer and compatibility matrix are separate claims.

