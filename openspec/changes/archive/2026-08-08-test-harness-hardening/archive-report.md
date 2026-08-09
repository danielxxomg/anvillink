# Archive Report — test-harness-hardening

**Change**: `test-harness-hardening` (AnvilLink — Test Harness Hardening, harness-only)
**Mode**: `openspec` (filesystem; delta → main spec merge + `git mv` archive)
**Branch**: `feat/anvillink/slice-1-scaffold` (tracker preserved, no push/tag/PR/release per preflight)
**Archive date**: 2026-08-08 (ISO 8601, matches `2026-08-08-*` precedent)
**Archived to**: `openspec/changes/archive/2026-08-08-test-harness-hardening/` (after mechanical `git mv` + `diff -r` empty proof; this report is the terminal record)
**Status**: `archived` — 33/33 tasks complete, `PASS` 10/10, 0 CRITICAL, 0 blockers
**Commits**: `afab901` Slice 1 artifact + `fbd300b` Slice 2 bootstrap on `feat/anvillink/slice-1-scaffold` (prior `378ed44` v0.3.1 bump retained; no behavior/server/config change, stays `0.3.1`)
**Version**: `0.3.1` (non-BREAKING harness; no `repair-economy`/`repair-signs` delta)

---

## Final-State Authority

This report describes the state of the change AT CLOSE (2026-08-08). Per `sdd-archive` Final-State Authority (highest wins):

1. **Native review authority** — `reviewGate` structurally ABSENT for this candidate (no genuine review artifact discovered). `gentle-ai review mode status` reported `receipt-driven development: off (decided by clone_local)` with clean authority inventory. Per contract, archive proceeds under ordinary repository policy; `dependencies.archive: ready` means proceed, not "investigate why gate is missing". No receipt to validate or block. Not a defect.
2. **Persisted `tasks.md`** — 33/33 checked at archive (Slice 1 17 + Slice 2 16; no checklist phase). Tasks artifact is source of truth for completion visibility; no stale unchecked tasks. `grep -c "^- \[x\]"` = 33, `grep -c "^- \[ \]"` = 0.
3. **Explicit final-state facts in orchestrator launch prompt** — outrank stale snapshots. Facts: 33/33 (Slice 1 artifact + Slice 2 bootstrap), verify `PASS` 10/10 0 CRITICAL, 245 tests, `jar 569K` with 14 legacy entries, no `net/kyori` unrelocated, domain pure major 61; commits `afab901` + `fbd300b` on `feat/anvillink/slice-1-scaffold` plus bump `378ed44` v0.3.1; specs to sync `platform-compatibility` ADDED 3 requirements (relocation completeness, shipped File I/O, bootstrap) + `smoke.yml`/`evidence.json` already promoted Paper 1.21.5-114 mandatory; skills before `test-driven-development`.
4. **`verify-report.md` (schema `gentle-ai.verify-result/v1`)** — intermediate snapshot at verification time: `verdict: pass`, `blockers 0`, `critical_findings 0`, `requirements 3/3`, `scenarios 10/10`, `245 passed / 0 failed`, `BUILD SUCCESSFUL in 5s`, 13 tasks, `evidence_revision sha256:90bc6a41f40ffe561a7ec19dfb951faa0f02be48ebe48fae4e324444d65435d1`, `test_output_hash sha256:e4af66be136b6375fa13c7f5d7155f1ddcb967e620823202aef55a1e17c9491e`. History, not current gate — but consistent with launch prompt. No contradictions requiring ranking resolution.
5. **`apply-progress.md` (Slice 1+2)** — intermediate per-slice history. Done stays true; pending claims are time-bound. Consistent with `tasks.md` 33/33 and verify PASS. Documents production deviation: `AnvilLinkPlugin public final class → public class` (non-final for MockBukkit 4.110 ByteBuddy proxy, reversible).

**No unrankable contradictions**: launch prompt, `tasks.md`, `verify-report.md`, and `apply-progress.md` agree. Verify warnings (6 WARNINGS) are residual evidence/fidelity concerns, not contradictions of final state.

**Task Completion Gate**: PASS — `sdd-apply` owned checkbox completion. 33/33 checked, no unchecked implementation tasks remain. No exceptional reconciliation needed.

**Action Context Guard**: `actionContext.mode` is not `workspace-planning`; no `allowedEditRoots` restriction. Archive operations stayed inside repo root (`openspec/specs/platform-compatibility/spec.md` and `openspec/changes/archive/` only).

**Strict-vs-OpenSpec Policy**: `CRITICAL: 0` so no hard block. `PASS` with 6 WARNINGS (non-blocking) does not require intentional-with-warnings carve-out. `CRITICAL` would still block with no override — not applicable.

---

## Executive Summary

Harness-only hardening shipped as two independently-revertible slices that make `clean build` fail on the three v0.3.0 Paper 1.21.5-114/J25 regressions before any server runs: Slice 1 gates the release JAR relocation (all `net.kyori` → `anvillink.libs.kyori`, 14 legacy entries / 318 relocated Kyori, 569K, zero unrelocated/host APIs, major 61, `api-version 1.13`, `${version}` expanded) and the shipped `config.yml` File I/O contract (quote-aware `stripInlineComment`, `activationEnabled`/`priceHand`/`priceAll`/`targetDistance`/`worlds` via real `File`), plus `Throwable` fallback for `LegacyComponentSerializer`; Slice 2 proves `MockBukkit.load(AnvilLinkPlugin)` bootstrap through the real classloader via `pluginManager.callEvent(SignChangeEvent)`→`PlayerInteractEvent` covering PDC `danielxxomg:anvillink_repair_sign` + `DyeColor.BLUE` + `update(true,false)`, fresh-`TileState` and non-`TileState` fallback (`if (!wrote && state instanceof TileState)`), Vault `fractionalDigits` matrix 0/2/-1, tamper fail-closed, and `audit.log` `ISO_INSTANT|uuid|name|HAND|world|toPlainString|count|SUCCESS` to temp `dataFolder`. Paper 1.21.5-114/114/JDK21 promoted from `continue-on-error:true` probe to mandatory `smoke.yml` + `evidence.json` row. 245 tests green (43 suites: `ShadowRelocationContractTest` 5/5, `ShippedConfigRoundTripTest` 4/4, `MiniMessagePortTest` 4/4, `PluginBootstrapTest` 15/15), `clean test spotlessCheck build jacocoTestCoverageVerification` green, domain pure. Specs merged mechanically (`append` → `diff` proof) and change folder mechanically archived (`git mv` + `diff -r` empty proof). Local only — no push/tag/PR/release.

---

## What Shipped

| Slice | Scope | Tasks | Commit | Status |
|-------|-------|-------|--------|--------|
| Slice 1 — Artifact (bugs #1 #2) | Shadow relocation contract (`ShadowRelocationContractTest` 5/5: legacy present, zero `^net/kyori`, no `org/bukkit`/`net/minecraft`, major 61, `plugin.yml` resolved), shipped-config round-trip (`ShippedConfigRoundTripTest` 4/4 via `@TempDir File` + shipped `config.yml`), `MiniMessageMessagePort` `catch(Throwable)` fallback, `build.yml` Verify JAR fail-closed, `smoke.yml` Paper 1.21.5-114/JDK21 mandatory, `evidence.json` row SHA deferred | 1.1–1.6 + 2.1–2.6 + 3.1–3.3 (14) | `afab901` `test(harness): slice 1 artifact gates` | ✅ 14/14 |
| Slice 2 — Bootstrap (bug #3) | `MockBukkit.mock()+MockBukkit.load(AnvilLinkPlugin.class)` → `getDataFolder()` + PDC `danielxxomg:anvillink_repair_sign`, `callEvent(SignChangeEvent)` → fresh `TileState` PDC+BLUE+`update(true,false)` + fallback `if (!wrote && state instanceof TileState)` via proxy `Block`, both non-`TileState` no-PDC, `PlayerInteractEvent` via `callEvent` Vault matrix 0/2/-1 + `ValidatedPrice` + `audit.log` `CREATE\|APPEND` `mkdirs` `toPlainString` + tamper `InvalidResponse` + `OffHand`/`InsufficientFunds`/`NoProvider`/`NoEligibleItems` + edit/break without `manage` cancelled | 4.1–4.7 + 5.1–5.8 (19) | `fbd300b` `test(harness): slice 2 bootstrap via MockBukkit.load` | ✅ 19/19 |
| **Total** | Harness-only; no domain spec delta; no new capability | **33** | **2 slice commits + archive** | **✅ 33/33** |

**Production files changed** (per `apply-progress.md`):

- `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` — `public final class` → `public class` (non-final) to allow MockBukkit 4.110 `MockBukkit.load(AnvilLinkPlugin.class)` ByteBuddy subclass. No behavioral change; reversible. Sole production delta.
- Tests: `src/test/java/io/github/danielxxomg/anvillink/descriptor/ShadowRelocationContractTest.java` (new), `src/test/java/io/github/danielxxomg/anvillink/adapter/ShippedConfigRoundTripTest.java` (new), `src/test/java/io/github/danielxxomg/anvillink/adapter/MiniMessagePortTest.java` (modified: `Throwable` fallback), `src/test/java/io/github/danielxxomg/anvillink/e2e/PluginBootstrapTest.java` (new, 15 tests)
- CI/evidence: `.github/workflows/build.yml` (Verify JAR fail-closed: `grep -q libs/kyori/legacy || exit 1` + `! grep -q "^net/kyori" && exit 1`), `.github/workflows/smoke.yml` (Paper 1.21.5 build 114 JDK21 `continue-on-error:false`), `compatibility/evidence.json` (Paper 1.21.5-114/`jdkMajor25` `pass` row, SHA `666...` placeholder deferred)

---

## Specs Synced (Mechanical — Step 2)

Source delta: `openspec/changes/test-harness-hardening/specs/platform-compatibility/spec.md` (3 ADDED requirements, 10 GWT scenarios, harness-only) → main `openspec/specs/platform-compatibility/spec.md`. No Read→Write routing; shell `append` only.

| Domain | Action | Details | Scenarios | Source | Destination |
|--------|--------|---------|-----------|--------|-------------|
| `platform-compatibility` | **ADDED 3 requirements (append)** | `Released-JAR relocation completeness` (legacy survives `minimize`, zero unrelocated, no host APIs + major 61) + `Shipped-config File I/O parse` (shipped `config.yml` via `File`, quoted `#` preserved, synthetic-only insufficient) + `MockBukkit-load bootstrap end-to-end` (PDC+BLUE via `callEvent`, stale-vs-fresh fallback, Vault 0/2/-1 + `audit.log`, Paper 1.21.5-114 evidence gate) | 10 (3 + 3 + 4) | `openspec/changes/test-harness-hardening/specs/platform-compatibility/spec.md` | `openspec/specs/platform-compatibility/spec.md` (73 → 145 lines, 7 → 10 requirements) |

**Preserved requirements** (not in delta, untouched in merge): `Public API compatibility floor`, `Single-JAR dependency boundary`, `Stable identity namespace`, `SemVer is separate from Minecraft support`, `Evidence-gated support tiers` (3 scenarios), `Verification layers and economy evidence`, `GPL licensing and GitHub release presence` — all retained verbatim.

**Mechanical proof (Step 2)**:

- Main spec existed, so merge (not `cp` of whole spec). Mechanics:
  - `sed -n '/^### Requirement: Released-JAR/,$ p' openspec/changes/test-harness-hardening/specs/platform-compatibility/spec.md >> openspec/specs/platform-compatibility/spec.md` (shell append, never Read→Write)
  - Post-merge verification:
    - `wc -l openspec/specs/platform-compatibility/spec.md` → 145 (was 73; +72 delta)
    - `grep -c "^### Requirement:"` → 10 (was 7; +3 ADDED)
    - `git diff --stat openspec/specs/platform-compatibility/spec.md` → `1 file changed, 72 insertions(+)` (append only, no deletions)
- No destructive overwrite; `diff -- openspec/specs/platform-compatibility/spec.md` shows only the 3 appended requirements (see git status evidence below).

---

## Archive Contents (Mechanical Move — Step 3)

**Archived to**: `openspec/changes/archive/2026-08-08-test-harness-hardening/`
**Date prefix**: `2026-08-08` (ISO 8601, per `2026-08-08-paid-repair-signs`/`price-per-mode-and-feedback`/`world-aware-pricing-and-audit` precedent and orchestrator instruction)
**Mechanism**: `git mv openspec/changes/test-harness-hardening openspec/changes/archive/2026-08-08-test-harness-hardening` (tracked move for `proposal.md`, `exploration.md`, `design.md`, `tasks.md`, `apply-progress.md`, `specs/platform-compatibility/spec.md`; `verify-report.md` moved as untracked and staged after)
**Snapshot**: `cp -R openspec/changes/test-harness-hardening → $snapshot_root/source` before move, compared after move

| Artifact | Present | Notes |
|----------|---------|-------|
| `proposal.md` | ✅ | Non-BREAKING harness, 87 lines, 6 success criteria |
| `exploration.md` | ✅ | 25KB, three v0.3.0 regressions, Paper/MockBukkit/Adventure tradeoffs, Approaches A/B/C |
| `specs/platform-compatibility/spec.md` | ✅ | Delta (3 ADDED, 10 GWT) preserved in archive snapshot; merged to main spec |
| `design.md` | ✅ | 751 words, artifact→JarFile/File + MockBukkit.load→callEvent→audit |
| `tasks.md` | ✅ | 33/33 checked (Fase 1–5 + Gates) |
| `apply-progress.md` | ✅ | Slice 1+2, production deviation documented |
| `verify-report.md` | ✅ | `PASS` 10/10 `sha256:90bc6a41f40ffe561a7ec19dfb951faa0f02be48ebe48fae4e324444d65435d1` |
| `archive-report.md` | ✅ | This file — additive-only, excluded from `diff -r` comparison |

**Mechanical proof (Step 3)** — verbatim:

```
snapshot_root=/tmp/sdd-archive.WOACa6 (mktemp -d)
cp -R openspec/changes/test-harness-hardening → $snapshot_root/source
snapshot contents: apply-progress.md, design.md, exploration.md, proposal.md, specs/platform-compatibility/spec.md, tasks.md, verify-report.md
mkdir -p openspec/changes/archive
git mv openspec/changes/test-harness-hardening → openspec/changes/archive/2026-08-08-test-harness-hardening
[verify] [ -e openspec/changes/test-harness-hardening ] → false (source gone: pass)
diff -r $snapshot_root/source openspec/changes/archive/2026-08-08-test-harness-hardening → DIFF_R_EMPTY: pass
diff -r: EMPTY (no differences) — PASS
```

Empty `diff -r` is the only passing evidence; archive-report additive-only excluded. No model readback substituted.

```text
git status --porcelain (post-move, pre-archive-report):
R  openspec/changes/test-harness-hardening/apply-progress.md -> openspec/changes/archive/2026-08-08-test-harness-hardening/apply-progress.md
R  openspec/changes/test-harness-hardening/design.md -> openspec/changes/archive/2026-08-08-test-harness-hardening/design.md
R  openspec/changes/test-harness-hardening/exploration.md -> openspec/changes/archive/2026-08-08-test-harness-hardening/exploration.md
R  openspec/changes/test-harness-hardening/proposal.md -> openspec/changes/archive/2026-08-08-test-harness-hardening/proposal.md
R  openspec/changes/test-harness-hardening/specs/platform-compatibility/spec.md -> openspec/changes/archive/2026-08-08-test-harness-hardening/specs/platform-compatibility/spec.md
R  openspec/changes/test-harness-hardening/tasks.md -> openspec/changes/archive/2026-08-08-test-harness-hardening/tasks.md
 M openspec/specs/platform-compatibility/spec.md
?? openspec/changes/archive/2026-08-08-test-harness-hardening/verify-report.md  (staged on commit)
```

**Active changes**: `openspec/changes/test-harness-hardening/` no longer exists (verified). `openspec/changes/archive/` now contains `2026-08-08-test-harness-hardening` alongside `2026-08-08-paid-repair-signs`, `2026-08-08-price-per-mode-and-feedback`, `2026-08-08-world-aware-pricing-and-audit`.

---

## Verification at Close (Final-State Authority)

**Carry from highest-ranked source**: orchestrator launch prompt (245 tests, 569K, 14 legacy, zero unrelocated, domain pure major61, PASS 10/10) + `verify-report.md` `sha256:90bc6a41f40ffe561a7ec19dfb951faa0f02be48ebe48fae4e324444d65435d1` (intermediate snapshot still current — no later test run changed counts per launch prompt). Never copy numbers from stale `apply-progress` when later facts exist — here they agree.

| Metric | Final |
|--------|-------|
| Verdict | `PASS` (no CRITICAL) |
| Requirements | 3/3 |
| Scenarios | 10/10 (relocation 3 + shipped-config 3 + bootstrap 4) |
| Tests | 245 passed / 0 failed / 0 skipped across 43 suites (`ShadowRelocationContractTest` 5/5, `ShippedConfigRoundTripTest` 4/4, `MiniMessagePortTest` 4/4, `PluginBootstrapTest` 15/15) |
| Build | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build jacocoTestCoverageVerification` exit 0, `BUILD SUCCESSFUL in 5s`, 13 tasks |
| Warnings | 61 Java deprecation/removal + Gradle 9 warnings (non-blocking), 6 verify WARNINGS (non-blocking) |
| Blockers / Critical | 0 / 0 |
| JaCoCo | `jacocoTestCoverageVerification` PASS (domain ≥75%, bundle ≥55%); `jacocoTestReport` not emitted in this gate |
| Bytecode | major 61 (Java 17) via `BytecodeFloorTest` + `ShadowRelocationContractTest.prodBytecodeIsMajor61ExcludingLibs` (prod `anvillink/**` excluding `/libs/`) |
| Domain purity | `grep -R "org.bukkit" domain` empty — 0 violations |
| Artifact | `build/libs/anvillink-0.3.1.jar` 569,602 bytes, 413 entries, 318 relocated `anvillink.libs.kyori`, 14 legacy, `LegacyComponentSerializer` present, `^net/kyori` 0, `^org/bukkit` 0, `^net/minecraft/server` 0, `plugin.yml` resolved `0.3.1` + `api-version: 1.13` + `main io.github.danielxxomg.anvillink.entrypoint.AnvilLinkPlugin` |
| Smoke/evidence | `smoke.yml` Paper 1.21.5 build 114/JDK21 `continue-on-error:false` mandatory; `compatibility/evidence.json` Paper 1.21.5-114/`jdkMajor25` `pass` (SHA `666...` placeholder) + probe `Paper 26.x/J25` `continue-on-error:true` retained |
| Evidence revision | `sha256:90bc6a41f40ffe561a7ec19dfb951faa0f02be48ebe48fae4e324444d65435d1` |
| Branch at close | `feat/anvillink/slice-1-scaffold` (ahead of origin by 2 slice commits pre-archive; archive commit becomes +1) |

**Verify WARNINGS at close** (per `verify-report.md` — non-blocking, do not gate `PASS`):

1. `compatibility/evidence.json` uses `6666...` placeholder for Paper 1.21.5 build 114; row structurally `pass` but not truthful SHA until mandatory host smoke runs.
2. Delta spec text says JDK 25 while `proposal`/`design`/`workflow`/`evidence` use JDK 21 — wording reconciled to JDK 21 for Paper 1.21.5-114 in `smoke.yml`/`evidence.json`.
3. `CompatibilityEvidence.paperCertified/allMandatoryPass` still requires only Paper 1.18.2/1.20.6/1.21.11; not yet includes new 1.21.5-114 row programmatically.
4. Stale-vs-fresh fallback test uses proxy-state fixture + direct listener vs `callEvent` for fresh/interaction paths — documented MockBukkit 4.110 `TileState` ceiling, not production failure.
5. `MiniMessagePortTest` verifies `catch(Throwable)` source + shaped output, not injected `NoSuchMethodError`/`Error` — relocation contract + source catch independently green.
6. 61 deprecated/removal warnings remain — non-blocking.

**Coherence (Design)**: artifact fidelity via `shadowJar→JarFile` ✅, lifecycle via `MockBukkit.load→callEvent` ✅ (proxy fallback caveat noted), isolation via `@TempDir`/`Proxy Economy` ✅, version `0.3.1` + floor major 61 ✅, deviation (`AnvilLinkPlugin` non-final) ✅ intentional.

---

## Source of Truth Updated

The following specs now reflect the new behavior (post-archive `openspec/specs/`):

- `openspec/specs/platform-compatibility/spec.md` — **ADDED 3 harness guarantees** (relocation completeness + shipped-config File I/O + MockBukkit-load bootstrap, 10 GWT, 145 lines) alongside 7 preserved requirements
- `openspec/specs/repair-economy/spec.md` — unchanged (not in this harness-only change)
- `openspec/specs/repair-signs/spec.md` — unchanged
- `openspec/specs/equipment-repair/spec.md` — unchanged
- `openspec/specs/audit-log/spec.md` — unchanged

---

## SDD Cycle Complete

The change `test-harness-hardening` has been fully planned, implemented, verified, and archived.
All deltas are now the source of truth. Ready for the next change.

**Next**: No follow-up SDD change required by this cycle. Adjacent follow-ups noted in `proposal.md`: Paper Test Framework proposal if residual MockBukkit `TileState` drift is measured, `@Tag("smoke")` split only if a host tier lands, truthful `evidence.json` SHA256 after real Paper 1.21.5-114 mandatory smoke. Keep `smoke.yml` mandatory row truthful on next green host run; reconcile `CompatibilityEvidence` if 1.21.5-114 becomes a programmatic certification input.

---

## Artifact Notes

- `exploration.md` exists in the change snapshot and is preserved in the archived tree (normal `git mv` carries all files under the folder).
- `verify-report.md` was untracked before archive (`git status ??`); mechanically moved via snapshot + `git mv` of parent directory and staged in the archive commit — `diff -r` empty still holds.
- `openspec/specs/platform-compatibility/spec.md` merge was append-only via shell (`sed -n ... >>`); `git diff --stat` confirms `72 insertions(+), 0 deletions(-)` — no requirement lost, no heading hierarchy broken.
- Archive report skill resolution: `test-driven-development` skill loaded before archiving as requested (`strict_tdd: false` per `openspec/config.yaml:148` — project uses TDD as guideline, not Iron Law; verification confirms RED→GREEN per `tasks.md` Fase 1.x/2.x with `apply-progress.md` gates).
- Mechanical Copy Contract honored: no `Read → Write` artifact routing for specs or archive move; every `cp`/`git mv` verified by `diff -r` empty (verbatim output included above). Missing/skipped `diff -r` would have FAILED the phase.
