# Changelog

All notable changes to AnvilLink will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.1] — 2026-08-08 — fix inline comments + Paper 1.21.5 Adventure + sign blue (PATCH)

### Fixed

- `FileConfigurationPort` failed on shipped defaults (`price.hand: 12000.00 # mandatory >= 0`) because inline `# comment` was fed to `BigDecimal` — added quote-aware `stripInlineComment` (` #` outside quotes) for `price.*`, `worlds.*`, `feedback.*`, `admin.target-distance`, `messages.*`; quoted `"a # b"` preserved. Also reports `activationEnabled=false` no longer occurs for valid shipped config on Paper 1.21.5-114/Java 25 where `gens:` hot.
- `MiniMessageMessagePort.render` threw `NoSuchMethodError: LegacyComponentSerializer.serialize(Component)` on Paper 1.21.5 (host Adventure 4.17+ vs shaded 4.11.0) — shaded `adventure-text-serializer-legacy`/`gson` were excluded from relocation so `Component` types mismatched; also `catch Exception` missed `Error`. Now relocates all `net.kyori` (removed `exclude gson/legacy`), adds `implementation(net.kyori:adventure-text-serializer-legacy)`, adsorbed into `libs.kyori` and guarded with `catch Throwable` fallback to raw string. Shadow JAR now includes 9 `legacy/` classes (557K vs 533K).
- `SignLifecycleListener` lost PDC + `DyeColor.BLUE` on Paper 1.21.5 because it wrote to the stale `BlockState` snapshot before `event.setLine`; now mirrors `[repair]`/`HAND|ALL` onto fresh `event.getBlock().getState()` `TileState` before `update(true,false)`. New `FileConfigurationPortTest` cases: `defaultConfigInlineComments_accepted`, `inlineCommentOnPrice_isStripped`, etc.

## [0.3.0] — 2026-08-08 — BREAKING per-world pricing + floor >=0 + audit log (MAJOR)

### BREAKING

- Floor relaxed `MoneyAmount.MIN_PRICE=10_000` → `>=0` (finite non-negative `representableAt(fractionalDigits)` at activation). `0` and `100` now accepted when representable; `12000/25000` defaults unchanged but `100` no longer rejected. Migration: operators relying on 10k rejection should enforce own minimum; `config.yml` values remain valid.
- `config.yml` adds optional `worlds:` map: each `worlds.<world>` MAY contain `hand` and/or `all` (partial — missing key falls back to global `price.hand`/`price.all`). Present values MUST be finite `>=0`. Lookup via `player.getWorld().getName()` exact case-sensitive; unknown/null/empty `worldName` → global. Present malformed (unparseable/negative/non-finite) `worlds.<world>.hand/all` fails whole file closed (retain prior). Per-world scale validated at activation via `ValidatedPrice.of(effective, fractionalDigits)` fail-closed no withdrawal.
- `ConfigurationPort.ConfigSnapshot` gains `Map<String, WorldPrice> worldPrices` (`WorldPrice(hand,all)` nullable per-field, unmodifiable defensive copy, `AtomicReference` swap). `RepairActivation.activate(SignId, UUID, String worldName)` seam `String` only (no `org.bukkit.*`), overload `activate(id,uuid)` delegates `worldName=null`.

### Added

- New capability `audit-log`: fixed `plugins/AnvilLink/audit.log` (`mkdirs`+`CREATE|APPEND`) appended ONLY on paid `Success(amount!=ZERO)` with fields `ISO_INSTANT|uuid|name|HAND/ALL|world|toPlainString|count|SUCCESS`; zero/empty and all failures not audited. `AuditPort` pure + `FileAuditAdapter` swallowed `IOException`; caller double-swallow → never affects transaction/compensation/feedback. File is unbounded, no auto-rotation — operator rotates by renaming/deleting, plugin recreates on next paid success; cleartext `UUID`+`name` retained, operator owns GDPR retention.
- `FileAuditAdapter` format tests + `AuditE2ETest`/`WorldAwarePricingE2ETest`/`WorldAwarePricingE2ETestFull` (MockBukkit 4.110), `FileConfigurationPortWorldsTest`/`RepairActivationWorldTest`; `config.yml` header fixes `v0.3.0` BREAKING worlds + fixed audit path/rotation/privacy note.

## [0.2.0] — 2026-08-08 — BREAKING per-mode pricing + success feedback (MAJOR)

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

## [0.1.0] — 2026-08-08 — AnvilLink initial release

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

