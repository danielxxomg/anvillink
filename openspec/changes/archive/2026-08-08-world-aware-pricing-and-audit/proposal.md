# Proposal: world-aware-pricing-and-audit

> **BREAKING MAJOR — risky** — removes `MoneyAmount.MIN_PRICE=10000` (now `>=0`) + adds optional `worlds:` partial-override map. Existing `12000/25000` remain valid; `100` now passes. Rollback plan required.

## Intent
Price `HAND`/`ALL` per world with global fallback, relax floor `>=10000`→`>=0` for versatility over paternalism, and audit every paid activation. Unified change: both share `config.yml`/`ConfigurationPort`/`FileConfigurationPort`/`RepairActivation`/`AnvilLinkPlugin` + `worldName` seam — one SDD change, two internal slices.

## Target users
- Multi-world Paper 1.18.2 operators needing per-world sinks.
- Admins needing file trail of who paid what, where, when.

## Current gap
- Only global `price.hand/all` (v0.2.0); no per-world override.
- `MIN_PRICE=10000` blocks low prices.
- No `audit.log` beyond `HIGH` `OperationalReporter`.

## Outcome
- Optional `worlds:`: `worlds.<world>` may hold only `hand` or only `all`; missing → global. Lookup `player.getWorld().getName()` exact case-sensitive; unknown → global.
- Prices `>=0` finite non-negative `representableAt(fractionalDigits)` at activation; defaults `12000/25000` unchanged.
- Paid `Success(amount!=ZERO)` appends one line to fixed `plugins/AnvilLink/audit.log` (ISO instant, UUID, name, mode, world, `toPlainString()`, count, `SUCCESS`); never blocks (double-swallowed). Fixed path, no DB; unbounded — operator rotates by renaming/deleting; cleartext IDs, operator owns GDPR retention.

## Scope
### In Scope
- `worlds:` hand-rolled indent parse; unknown subkeys warned not invalid (enables partial fallback).
- Whole-file fail-closed on malformed `hand`/`all` (unparseable/negative/non-finite) — retain prior snapshot (consistent with global strict); unknown/valid-partial lenient, malformed inside existing entry invalidates whole file.
- Remove `MIN_PRICE`; `MoneyAmount`/`ValidatedPrice`→`>=0`; `ConfigSnapshot` adds `Map<String,WorldPrice> worldPrices` (`unmodifiableMap`, `AtomicReference` swap).
- `RepairActivation.activate(SignId,UUID,String worldName)` resolver `get(worldName)` else global → `ValidatedPrice.of`.
- Pure `AuditPort`+`FileAuditAdapter` fixed `audit.log`; `AnvilLinkPlugin` captures `worldName` server-thread, audits after feedback, double-swallowed.

### Out of Scope
- Configurable audit path, auto-rotation, DB, per-group pricing, world UUID.

### Adjacent follow-ups
- Async audit (`SchedulerPort.runAsync`/`Executor`, `onDisable` flush) — v1 sync swallow; offload later.
- `audit.maxBytes`/rotation; anonymized retention; world-UUID alias.

## Capabilities
### New Capabilities
- `audit-log`: append-only audit for paid activations (fixed path, best-effort, never blocks).
### Modified Capabilities
- `repair-economy`: per-world resolution + floor `>=0` (BREAKING vs `>=10000`); partial override + global fallback; whole-file fail-closed. `audit-log` MAY fold into `repair-economy`; separate preferred.

## Approach
**Pricing — Exploration Approach 1** (`Map<String,WorldPrice>`): `record WorldPrice(BigDecimal hand,BigDecimal all)` pure domain. `FileConfigurationPort` adds `inWorlds`/`currentWorld` to line-scan, validates present `hand`/`all` via `new BigDecimal`+`signum<0`+non-finite+`new MoneyAmount(v)` (now `>=0`). Missing subkey valid (fallback); dup world last wins warn. Rejected `PriceCatalog` premature couples to `fractionalDigits`. Hand-rolled avoids SnakeYAML 1.30/2.2 skew, single validation boundary. Tradeoff: extra branching vs minimal coupling.

Purity: `String worldName` into `activate(id,uuid,worldName)` from `AnvilLinkPlugin.onPlayerInteract` (`getWorld().getName()` server thread). No `org.bukkit.World` in domain; null/empty→global.

**Audit — Approach 1** (sync swallow): `AuditPort.record(AuditEntry{Instant,UUID,name,RepairMode,world,BigDecimal,int,String})`. `FileAuditAdapter(plugins/AnvilLink/audit.log)` does `Files.writeString(...,CREATE,APPEND)` swallowed; caller double-swallows. Tab `ISO_INSTANT|uuid|name|HAND/ALL|world|toPlainString|count|SUCCESS`; only `Success(non-zero)`. Simplest/hexagonal/testable never blocks; tradeoff tick stall on slow disk — v1 documents bound, queues async Executor follow-up. Rejected logger-based audit (mixes log, no structured guarantee).

## Affected Areas
| Area | Impact | Description |
|------|--------|-------------|
| `config.yml` | Modified | Add `worlds:` (`>=0`, partial, case-sensitive) |
| `domain/ports/ConfigurationPort.java` | Modified | `ConfigSnapshot` + `WorldPrice` map |
| `adapter/FileConfigurationPort.java` | Modified | Nested scan, `>=0`, partial/whole-file invalid |
| `domain/MoneyAmount.java` | Modified | Remove `MIN_PRICE`; `>=0` |
| `domain/ValidatedPrice.java` | Modified | Floor `>=0` |
| `domain/RepairActivation.java` | Modified | `activate(id,uuid,worldName)` + audit hook |
| `domain/ports/AuditPort.java` | New | Pure `AuditEntry` |
| `adapter/FileAuditAdapter.java` | New | Fixed `audit.log`, swallow, `mkdirs()` |
| `entrypoint/AnvilLinkPlugin.java` | Modified | Capture `worldName`, wire adapter, audit after feedback |
| `specs/repair-economy/spec.md` | Modified | Floor `>=0`, per-world, fail-closed |
| `specs/audit-log/spec.md` | New | Paid-success audit, fixed path, privacy |

## Compatibility / Platform / Support-tier policy
- **Platform:** Paper 1.18.2 / Java17 bytecode (`--release 17`, major61, `BytecodeFloorTest`), `compileOnly` Paper1.18.2+Vault1.7; Adventure4.11.0 relocated `anvillink.libs.kyori`; `api-version 1.13`; public APIs only.
- **Versioning:** BREAKING MAJOR `>=10000`→`>=0`+`worlds:` map. Migration: `12000/25000` valid; `100` now valid (was rejected); operators relying on rejection enforce own minimum. `price<0`/non-finite invalid. Versatility over paternalism.
- **Support tier:** `softdepend [Vault]` unchanged; Vault absence fail-closed.

## Risks
| Risk | Likelihood | Mitigation |
|------|------------|------------|
| World rename silent fallback | Med | Document exact match; re-key after rename |
| Parser fragility (indent/quotes) | Med | Tests for quoted `"world nether"`/dup/empty |
| One bad world invalidates file | Low | Error `worlds.<name>.hand: <reason>`; retain prior |
| Audit tick stall (sync) | Low/Med | Short line swallow; async follow-up |
| Unbounded `audit.log` | Med | Manual rotation (rename/delete) |
| GDPR cleartext UUID+name | Med | Operator owns retention; no scrubbing v1 |
| Domain purity regression | Low | `worldName` String; zero `org.bukkit.*` |

## Rollback Plan
**Risky BREAKING — rollback required.** Tag prior v0.2.0 spec+`config.yml`+`MIN_PRICE`. On regression revert `config.yml`,`ConfigurationPort`,`FileConfigurationPort`,`MoneyAmount`/`ValidatedPrice`,`RepairActivation`,`AuditPort`/`FileAuditAdapter`,`AnvilLinkPlugin`. `worlds:`→unknown warn ignore. Restore `MIN_PRICE=10000` per-world `<10000` then fail-closed. Delete/archive `audit.log`; no PDC migration.

## Dependencies
- VaultAPI1.7 `fractionalDigits()` for `representableAt`.
- JDK `Files`/`AtomicReference`; no new deps; `shadowJar` exclusions unchanged; `spotlessCheck` green.

## Success Criteria
- [ ] `worlds:` partial: only `hand`→world `hand`+global `all` (inverse); unknown→global; case-sensitive exact.
- [ ] `0`/`100`/`12000`/`25000` accepted when `representableAt`; negative/non-finite/unparseable per-world fails whole file closed (retain prior), never withdraws.
- [ ] `MIN_PRICE` removed; `>=0` at parse+domain+activation; `ValidatedPrice` mirrors.
- [ ] `activate(id,uuid,worldName)` resolves effective price; bad precision fails closed.
- [ ] Non-empty plan withdraws once; empty `Success(ZERO)` with no audit.
- [ ] Paid `Success(non-zero)` appends one `audit.log` line (ISO instant,UUID,name,mode,world,`toPlainString`,count,`SUCCESS`); failures/zero never audited; I/O swallowed.
- [ ] Fixed `plugins/AnvilLink/audit.log` (`CREATE+APPEND`+`mkdirs()`); rotation by rename/delete; unbounded+privacy/GDPR noted.
- [ ] `worldName` server thread; domain zero `org.bukkit.*`; `./gradlew clean test spotlessCheck build` green.
