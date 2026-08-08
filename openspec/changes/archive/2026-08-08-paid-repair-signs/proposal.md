# Proposal: AnvilLink — paid repair signs for Minecraft servers

## Intent

Server operators need a small, trustworthy way to sell in-game equipment repair through authorized interactive signs: a canonical blue `[repair]` sign charging a fixed configured price per successful activation via Vault. The niche is filled only by abandoned or All-Rights-Reserved legacy plugins, GUI-based competitors, or GPL repair plugins with a different product contract — none is a maintained, sign-based, fixed-price, public-API-only fit.

## Target users and situations

- **Server operators/admins**: install a paid repair point without a GUI or economy-specific dependency; manage who can create/use/repair.
- **Players**: right-click a repair sign to fix `HAND` or `ALL` equipment for a flat fee.

## Current gap

Legacy `[repair]` plugins are from 2013–2019, mostly All-Rights-Reserved, untested on current Paper, and lack a fixed-price paid-repair contract. Current paid repair plugins are GUI- or command-based. No maintained sign-based paid-repair plugin with Vault, PDC identity, and an honest support-tier policy exists.

## Outcome

A released `AnvilLink` plugin: authorized sign creation, canonical rendering, bounded equipment repair, fail-closed Vault economy, protected sign lifecycle, and CI-evidenced support matrix — distributed on GitHub under GPL-3.0-or-later.

## Scope

### In Scope (MVP)
- Blue `[repair]` line 1; line 2 uppercased `HAND` (main-hand slot) or `ALL` (main, off hand, helmet, chestplate, leggings, boots — never storage).
- Authorization: `create`, `use`, `manage` permissions; edit/break registered signs requires manage; admin inspect/re-render.
- PDC-backed sign identity (permanent namespace, decoupled from display brand), `[repair]` text never trusted alone.
- Vault economy (soft dependency): no eligible damaged repairable item → no charge; missing provider / transaction failure → fail closed.
- Fixed configured price per successful activation; item-meta `setDamage(0)` for damaged repairable `Damageable` metas.
- Reloadable messages/defaults; MiniMessage presentation; unit + MockBukkit + real Paper smoke tests.

### Out of Scope / Non-Goals
- GUI/anvil replacement, full-inventory repair, XP/material modes, command-only repair as a second product.
- Per-item or per-durability pricing, databases, remote APIs, NMS.
- Folia certification, legacy sign migration, marketplace integrations, final trademark/domain clearance.

### Adjacent follow-ups (deferred)
1. Structured audit log for sign/economy/refund failures.
2. Optional per-world enable/permission/price policy.
3. Folia support as a separate compatibility slice once schedulers/economy policy are designed and tested.

## Capabilities

> Contract for sdd-spec. No `openspec/specs/` exist yet (greenfield).

### New Capabilities
- `repair-signs`: sign creation/identity/authorization, `HAND`/`ALL` parsing, blue rendering, protected lifecycle, admin inspect/re-render.
- `repair-economy`: Vault gateway, fail-closed transactions, fixed-per-activation pricing, refund/snapshot policy, no-eligible-item-no-charge.
- `equipment-repair`: bounded equipment-target resolution, damaged-repairable `Damageable` planning and `setDamage(0)` apply.
- `platform-compatibility`: Java/Paper toolchain, PDC namespace, support-tier policy, CI smoke-test matrix advertisement.

### Modified Capabilities
- None (greenfield; no existing specs).

## Approach

**Approved build floor: Paper 1.18.2 / Java 17 (user-approved decision).** Compatibility-first single artifact per exploration Approach [1]: compile Paper API + VaultAPI `compileOnly` (never shaded) against Paper 1.18.2, emit Java 17 bytecode (`--release 17`) via a pinned Gradle toolchain, use only public Bukkit/Paper APIs common to the 1.18.2 floor (avoid newer conveniences such as side-aware `SignSide`), and test representative older/middle/newest-and-current Java-21 lines plus a Java-25 Paper 26.x smoke job before advertising beyond the 1.18.2-tested range. Pure domain core (parser/plan/result types, no Bukkit); Paper adapter (events/PDC/equipment/messages); Vault `EconomyGateway` adapter; Gradle Kotlin DSL + Java toolchains; JUnit 5 + MockBukkit. One release stream, conventional `plugin.yml`, PDC identity as source of truth.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/` | New | Pinned Java 17 toolchain with `--release 17`, Paper 1.18.2 API + VaultAPI `compileOnly`, JUnit 5, MockBukkit, formatting |
| `src/main/resources/plugin.yml`, `config.yml` | New | Stable metadata, permissions, Vault soft-depend, defaults, reloadable messages |
| `src/main/java/<stable-namespace>/` | New | Domain core + Paper/economy adapters, listeners, presentation |
| `src/test/java/`, `src/test/resources/` | New | Parser/plan unit tests, MockBukkit lifecycle/event/fake-economy tests, compatibility fixtures |
| `.github/workflows/` | New | Build checks + real Paper smoke-test matrix |
| `README.md`, `LICENSE` (GPL-3.0-or-later), `CHANGELOG.md`, release metadata | New | Brand, setup, support-tier table, SemVer releases, non-goals |
| `openspec/config.yaml` | Modified | Replace undetected-stack/testing placeholders once floor + commands are chosen |

## Compatibility / Support-tier policy

Build floor is Paper 1.18.2 / Java 17 (approved). Honest, evidence-based language still governs advertisement — no future-version guarantees, and certification ≠ verification ≠ experiment.

| Platform | Status language |
|----------|----------------|
| Paper | Certified for the exact CI-tested version range; the tested range (anchored at the 1.18.2 floor) is advertised only from passing smoke jobs |
| Spigot, Purpur | Verified (compatible forks) once separately smoke-tested, not certified, not promised |
| Folia | Experimental until dedicated scheduler/economy matrix passes |
| Paper 26.x (Java 25) | Not certified until a Java-25 smoke job passes |

The 1.18.2 floor is the compile/API anchor; the advertised tested upper bound comes ONLY from passing CI evidence, not from compile success or assumptions. No claim of equal certification across platforms or unsupported future versions.

## Brand / SEO positioning (GitHub)

- Stable technical namespace independent of the display brand; `AnvilLink` is provisional pending trademark/domain/marketplace clearance.
- GitHub descriptor: **AnvilLink: paid repair signs for Minecraft servers**.
- One clear H1, benefit-first README, support-tier table, permission/economy/setup sections. Avoid keyword stuffing (−10% GEO penalty). Answer-first structure, FAQ block, and `SoftwareApplication` schema on the project page improve both classic SEO and AI-engine citation.

## Versioning / release principles

- Plugin follows **SemVer** (`MAJOR.MINOR.PATCH`); incompatible behavior, permissions, or PDC schema bumps MAJOR.
- Minecraft compatibility is a **separate matrix** (`Paper a.b.c–x.y.z tested`), generated from CI evidence, not from SemVer.
- Paper and Vault APIs are `compileOnly` and never shaded. Releases are GitHub-only initially.

## Security / economy / sign-lifecycle implications

- **Security:** PDC identity is the boundary; visible text is projection only. Tampered/invalid records are rejected or admin-re-rendered; never derive identity from `[repair]` text alone.
- **Economy non-atomicity:** Vault withdrawal and inventory mutation are separate systems. Snapshot/plan all mutations, withdraw once, apply on server thread; on unexpected apply failure restore snapshots and attempt a compensating deposit; log unreconciled refunds as high-severity operational events. Missing provider or transaction failure fails closed — never free repair.
- **Sign lifecycle:** separate create/use/manage permissions; creation authorization stored in PDC; edit/break of registered signs requires manage; admin inspect/re-render path for tampered signs.
- **Duplicate interactions:** `PlayerInteractEvent` may fire per hand; filter the interaction hand and define explicit `HAND`/`ALL` slot contract to prevent double-charge.

## Rollback Plan

Greenfield: revert the merge/PR, delete `src/` Gradle/plugin artifacts, restore `openspec/config.yaml` placeholders, remove `.github/workflows` smoke jobs. No persisted server data exists yet, so no data migration is needed. If released before revert, revert the GitHub release/tag and publish a `CHANGELOG.md` withdrawal note.

## Dependencies

- VaultAPI 1.7 (`compileOnly`, soft runtime dependency — fail-closed if absent).
- Paper API (`compileOnly`, not shaded).
- Vault economy provider at runtime (required for any charge; absent → diagnostic fail-closed state).

## Success Criteria

- [ ] Authorized `[repair]` sign creation renders blue canonical line 1; line 2 uppercased `HAND`/`ALL`; record in PDC under permanent namespace.
- [ ] `HAND` repairs main-hand only; `ALL` repairs the six equipment slots, never storage.
- [ ] No eligible damaged repairable item → no charge. Missing Vault/provider or failed transaction → fail closed, no repair.
- [ ] Fixed configured price charged once per successful activation; item metas `setDamage(0)` only after successful withdrawal.
- [ ] Unauthorized players cannot create/edit/break repair signs; manage authority required for edit/break; admin inspect/re-render works on tampered signs.
- [ ] Duplicate per-hand interactions do not double-charge.
- [ ] Support-tier matrix advertised only from passing CI smoke jobs; never claims equal certification or unsupported future versions.
- [ ] Unit, MockBukkit, and real Paper smoke tests pass for the advertised range; GPL-3.0-or-later license + GitHub release present.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Compatibility drift (Paper 26.x → Java 25; APIs evolve) | Med | Advertise only CI-tested range; Java-25 smoke job before 26.x claims; no future-version guarantees; floor locked at 1.18.2/Java 17 |
| Vault non-atomicity / refund gaps | Med | Snapshot/plan, withdraw-once, apply-on-server-thread, compensating deposit + high-sev log on apply failure |
| Duplicate per-hand double-charge | Med | Filter interaction hand; explicit `HAND`/`ALL` slot contract; deterministic before action |
| Sign tampering / identity loss | Med | PDC identity (not text), rejected tampered records, admin re-render, permissions for edit/break |
| Economy precision / provider rounding | Low–Med | Finite non-negative price parsing; fake-provider tests + one real economy integration |
| MockBukkit older-branch weakness vs real behavior | Med | Real Paper smoke tests required for release claims; mocks are supplemental |
| Legal/brand uncertainty (trademark/domain/marketplace) | Med | Stable namespace decoupled from `AnvilLink`; no copying from ARR/no-license legacy; GPL-3.0-or-later; do not invent clearance |
| Greenfield delivery load vs review budget | Med | The 1,200-line review budget applies **per immutable review candidate/slice**, not to the aggregate greenfield product. The aggregate implementation MAY exceed 1,200 lines only through independently testable auto-chain slices; the task plan targets **≤ 400 lines per slice**. Slices stay independently testable/reviewable — never one monolithic diff. |

## Proposal question round

Interactive proposal shaping was **attempted before finalization**. This change was launched with "User-approved product decisions to carry forward without reinterpretation," which pre-answered the four product/business questions the exploration left unresolved, plus the one remaining technical question resolved in a follow-up. All open items are now explicit approved decisions — no defaults carried forward silently.

**Approved decisions carried forward (no reinterpretation):**
1. **Target semantics** — `HAND` = main-hand slot only; `ALL` = main hand, off hand, helmet, chestplate, leggings, boots; never storage. ✅ approved.
2. **Sign lifecycle** — front-side MVP; separate create/use/manage permissions; edit/break registered signs requires manage; admin inspect/re-render included; auto-wax is adjacent, not assumed. ✅ approved.
3. **Compatibility promise** — Paper certified, Spigot/Purpur verified, Folia experimental; floor chosen by build approach; no equal-certification or future-version claims. ✅ approved.
4. **Identity & distribution** — provisional brand `AnvilLink`, stable namespace decoupled from display name, GPL-3.0-or-later, GitHub-only initial publication, commercial parallel-licensing option preserved, trademark rights separate. ✅ approved.
5. **Build compatibility floor** — **Paper 1.18.2 / Java 17** selected (exploration Approach [1]); `compileOnly` Paper 1.18.2 API + VaultAPI, `--release 17` bytecode, widest tested single-jar range. ✅ approved.

No open questions remain. If any assumption above is wrong, correct it before moving to specs.
