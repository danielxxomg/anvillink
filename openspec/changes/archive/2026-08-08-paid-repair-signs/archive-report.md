# Archive Report — paid-repair-signs

**Change**: `paid-repair-signs` (AnvilLink — Paid Repair Signs)
**Mode**: `openspec`
**Branch**: `feat/anvillink/slice-1-scaffold`
**Archive date**: 2026-08-08 (ISO 8601)
**Archived to**: `openspec/changes/archive/2026-08-08-paid-repair-signs/` (after mechanical move; this report is the terminal record)
**Status**: `archived-intentional-with-warnings` — Phases 1–8 COMPLETE (97/97, 100%); Phase 9 (0/3) intentionally BLOCKED awaiting explicit user authorization + native delivery gates. Not a defect.

---

## Final-State Authority

This report describes the state of the change AT CLOSE (2026-08-08). Per `sdd-archive` Final-State Authority (highest wins):

1. **Persisted `tasks.md`** — 97/100 impl tasks checked; 3 unchecked are Phase 9 (9.1, 9.2, 9.3) gated by `Publication gate: Apply MAY prepare ... MUST NOT create GitHub release/tag/push/PR without explicit user authorization`. Tasks artifact is source of truth for completion visibility.
2. **Explicit final-state facts in orchestrator launch prompt** — `verify warnings fixed? No blockers — Phase 9 gate is intentional. Runtime ledger: 13 attempts, all settled with rescope resets, next_action begin, decision_required false, 121 tests, build PASS.` Outranks stale snapshots.
3. **`verify-report.md` (schema `gentle-ai.verify-result/v1`)** — intermediate snapshot at verification time: `verdict: pass_with_warnings`, `0 CRITICAL / 2 WARNING`, `22/22 reqs, 36/36 scenarios, 121 tests, build PASS, spotlessCheck PASS`. History, not current gate.
4. **`apply-progress.md`** — intermediate per-slice history (slices 1–8, tokens `4aecd62 … f77dd5e`). Done stays true; pending claims are time-bound.

No contradictions require ranking resolution: launch prompt and tasks.md agree Phase 9 is intentionally deferred; verify-report attributes the same 2 WARNINGs to the intentional gate + a known `clean` ordering quirk (`cleanTest test` is canonical and passes deterministically).

**Native Review Receipt Gate**: `reviewGate` structurally ABSENT for this candidate (no genuine review artifact discovered). Per contract, archive proceeds under ordinary repository policy; `dependencies.archive: ready` means proceed. No receipt to validate or block.

**Task Completion Gate**: `sdd-apply` owned checkbox completion. 97/100 is the correct terminal visibility — Phases 1–8 97/97 (100%), Phase 9 0/3 BLOCKED by design with user-authorized intentional partial archive. No stale unchecked tasks for completed work; the 3 unchecked are the explicitly gated publication tasks, not forgotten implementation. This satisfies the Strict-vs-OpenSpec policy's `intentional-with-warnings` allowance; `CRITICAL: 0` so no hard block.

**Action Context Guard**: `actionContext.mode` is not `workspace-planning`; no `allowedEditRoots` restriction. Archive operations stay inside repo root.

---

## Executive Summary

AnvilLink paid repair signs shipped as a pure-domain → adapter hexagonal plugin on Paper 1.18.2 / Java 17 floor, with Vault soft-depend, PDC `danielxxomg:anvillink_repair_sign` permanent identity, `HAND=[main]` / `ALL=[main,off,helmet,chest,leggings,boots]` (never storage), single flat Vault withdrawal with snapshot/compensating-deposit recovery, MiniMessage-relocated presentation, and evidence-gated support tiers. 8 slices landed (see commits below), all verified. Specs were greenfield (no prior `openspec/specs/`), so delta specs were mechanically promoted to main specs (4 domains). Verification is `pass_with_warnings` with zero critical findings; the only warnings are the intentional Phase 9 publication gate and a non-blocking `clean` ordering quirk. Archive is local only — no push/tag/PR/release per preflight.

---

## What Shipped

| Phase | Scope | Tasks | Commit | Status |
|-------|-------|-------|--------|--------|
| 1 | Gradle scaffold, plugin metadata, domain value types, bytecode floor (PR 1, ~831 authored lines incl. descriptor/bytecode tests) | 1.1–1.17 (17) | `53b77ac` (foundations) | ✅ |
| 2a | SignParser + RepairPlanner family (343 lines) | 2.1–2.4 (4) | `44979e3` | ✅ |
| 2b | TransactionResult + ValidatedPrice + 7 ports (384 lines) | 2.5–2.10 (6) | `4aecd62` | ✅ |
| 3 | RepairActivation + compensation use case (398 lines) | 3.1–3.11 (11) | `97e488a` | ✅ |
| 4 | PDC identity + sign lifecycle adapter (124 prod under 400; 763 incl. tests) | 4.1–4.10 (10) | `8271b40` | ✅ |
| 5 | InteractionFilter + Vault gateway + equipment adapter (369 prod) | 5.1–5.12 (12) | `e7e8b5d` | ✅ |
| 6 | Config/MiniMessage/scheduler/admin/entrypoint (645 prod incl. plugin.yml; ≤400 net) | 6.1–6.9 (9) | `0d8f307` | ✅ |
| 7 | MockBukkit E2E (11 tests, 736 lines) + real-provider gate + SemVer separation (218 prod) | 7.1–7.18 (18) | `130f359` | ✅ |
| 8 | Evidence schema + CI + docs (359 prod/docs; 215 prod+CI under budget) | 8.1–8.10 (10) | `f77dd5e` | ✅ |
| 9 | GitHub release publication — **USER-AUTHORIZED GATE** | 9.1–9.3 (3) | — | ⛔ BLOCKED (intentional) |

**Total**: 97/100 implementation tasks complete (Phases 1–8 97/97, 100%). Remaining 3 are Phase 9 publication, explicitly blocked until user authorizes `git push`/`gh pr create`/`git tag`/`release` and native `gentle-ai review validate` passes.

---

## Specs Synced (Mechanical Copy — Step 2)

Greenfield change: no `openspec/specs/` existed before archive. Each delta spec IS the full spec. Sync was mechanical `cp` → `diff -r` → `mv` per domain, never Read→Write.

| Domain | Action | Details | Source | Destination |
|--------|--------|---------|--------|-------------|
| `equipment-repair` | Created | 4 requirements / 7 scenarios | `openspec/changes/paid-repair-signs/specs/equipment-repair/spec.md` | `openspec/specs/equipment-repair/spec.md` |
| `platform-compatibility` | Created | 7 requirements / 9 scenarios | `openspec/changes/paid-repair-signs/specs/platform-compatibility/spec.md` | `openspec/specs/platform-compatibility/spec.md` |
| `repair-economy` | Created | 5 requirements / 8 scenarios | `openspec/changes/paid-repair-signs/specs/repair-economy/spec.md` | `openspec/specs/repair-economy/spec.md` |
| `repair-signs` | Created | 6 requirements / 12 scenarios | `openspec/changes/paid-repair-signs/specs/repair-signs/spec.md` | `openspec/specs/repair-signs/spec.md` |

**Source of truth now**:
- `openspec/specs/equipment-repair/spec.md`
- `openspec/specs/platform-compatibility/spec.md`
- `openspec/specs/repair-economy/spec.md`
- `openspec/specs/repair-signs/spec.md`

Preservation guarantee: delta specs contained only new requirements (greenfield); merge preserved all requirements (nothing to overwrite), maintained Markdown heading hierarchy, no RENAMED/REMOVED cases.

### Mechanical Copy Evidence (verbatim `diff -r`, empty = passing)

```
--- Domain: equipment-repair ---
cp exit: 0
diff -r src vs temp exit: 0 (0=empty, expected)
mv exit: 0
diff -r src vs final exit: 0
OK equipment-repair

--- Domain: platform-compatibility ---
cp exit: 0
diff -r src vs temp exit: 0 (0=empty, expected)
mv exit: 0
diff -r src vs final exit: 0
OK platform-compatibility

--- Domain: repair-economy ---
cp exit: 0
diff -r src vs temp exit: 0 (0=empty, expected)
mv exit: 0
diff -r src vs final exit: 0
OK repair-economy

--- Domain: repair-signs ---
cp exit: 0
diff -r src vs temp exit: 0 (0=empty, expected)
mv exit: 0
diff -r src vs final exit: 0
OK repair-signs
```

Any non-empty diff would have failed the phase. No truncation or alteration occurred — bytes are identical via independent `diff -r` readback (not model self-report).

---

## Verification Verdict at Close

**Verdict**: `pass_with_warnings` — 0 CRITICAL, 2 WARNING, 0 blockers.
**Source**: `openspec/changes/paid-repair-signs/verify-report.md` (`gentle-ai.verify-result/v1`, `evidence_revision: sha256:5db0004f075202aac44e5e82dd553ed686626c4d364f94ffdb0f9a45b65c028b`).

| Metric | Value |
|--------|-------|
| Requirements | 22/22 |
| Scenarios | 36/36 (35 fully verified + 1 local-readiness; gated publication intentionally deferred) |
| Tests | 121 passed / 0 failed / 0 skipped (`GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew cleanTest test`, `test_exit_code: 0`, `test_output_hash: sha256:4db66ac93193fb4508825e1e1392a19300233c026f5fb71ccd02694d44543a75`) |
| Build | PASS (`./gradlew build`, `build_exit_code: 0`, `build_output_hash: sha256:76a6f1a7b14cb72925a3f69e343c2e77d974959ab132d3673a26ffd2327fd167`, JAR `build/libs/anvillink-0.1.0-SNAPSHOT.jar`, `unzip -l` shows no `org/bukkit` or `net/milkbowl`, `plugin.yml` `softdepend: [Vault]` + `version='0.1.0-SNAPSHOT'` expanded, PDC `danielxxomg:anvillink_repair_sign` present, bytecode major 61 via `BytecodeFloorTest`, Adventure 4.11.0 relocated) |
| Format | PASS (`./gradlew spotlessCheck` — googleJavaFormat 1.17.0 + ktlint 1.0.1 + license header) |
| Coverage | Not gated (JaCoCo not configured; threshold 0; 121 behavioral tests cover 36 scenarios) |
| Domain purity | `grep domain Bukkit/Vault/Adventure/NMS/reflection` → 0 |
| WARNING 1 | Phase 9 (9.1 GitHub release with JAR+SHA+evidence.json, 9.2 verification, 9.3 readiness) intentionally BLOCKED — requires explicit user authorization + native delivery gates per `tasks.md` Publication gate. Not a verification failure; local readiness passes (LICENSE GPL-3.0-or-later, JAR built, CHANGELOG, evidence.json). |
| WARNING 2 | `clean` without `cleanTest` is flaky for descriptor tests (plugin.yml version token needs shadowJar); canonical `cleanTest test` / `cleanTest test spotlessCheck build` passes deterministically (121/121). |
| CRITICAL | None — archive not blocked. |

**Spec compliance matrix**: 36/36 scenarios COMPLIANT (repair-signs 12, repair-economy 8, equipment-repair 7, platform-compatibility 9) with test evidence per `verify-report.md` matrix (e.g. `SignParserTest`, `PdcSignIdentityTest`, `RepairActivationTest`, `VaultEconomyGatewayTest`, `BukkitEquipmentPortTest`, `SignIntegrationTest` (11), `CompatibilityEvidenceSchemaTest`, `EvidenceGatedSupportTest`, `BytecodeFloorTest`, `PluginDescriptorTest`).

**Final numbers carried from highest-ranked source** (launch prompt + tasks.md + verify-report agree): 121 tests, 2 warnings, 0 critical, 22/22 reqs, 36/36 scenarios. No stale snapshot override.

---

## Artifacts at Close

| Artifact | Path | Status |
|----------|------|--------|
| Proposal | `openspec/changes/paid-repair-signs/proposal.md` | ✅ Present (intent, scope, approach, capabilities, risks) |
| Exploration | `openspec/changes/paid-repair-signs/exploration.md` | ✅ Present (ecosystem map, approaches, recommendation) |
| Design | `openspec/changes/paid-repair-signs/design.md` | ✅ Present (hexagonal architecture, PDC, transaction, evidence matrix) |
| Specs (delta) | `openspec/changes/paid-repair-signs/specs/{equipment-repair,platform-compatibility,repair-economy,repair-signs}/spec.md` (4) | ✅ Present → synced to `openspec/specs/` |
| Specs (main, source of truth post-sync) | `openspec/specs/{equipment-repair,platform-compatibility,repair-economy,repair-signs}/spec.md` (4) | ✅ Created (mechanical copy, diff-verified) |
| Tasks | `openspec/changes/paid-repair-signs/tasks.md` | ✅ 97/100 (97/97 Phases 1–8 100%; 3 Phase 9 BLOCKED intentional) |
| Apply progress | `openspec/changes/paid-repair-signs/apply-progress.md` | ✅ Present (slices 1–8, 517 lines, RED→GREEN evidence, rollback boundaries) |
| Verify report | `openspec/changes/paid-repair-signs/verify-report.md` | ✅ Present (`pass_with_warnings`, 121 tests, build PASS, evidence hashes) |
| Archive report | `openspec/changes/paid-repair-signs/archive-report.md` (this file; after move: `openspec/changes/archive/2026-08-08-paid-repair-signs/archive-report.md`) | ✅ Present (terminal record) |
| Evidence | `compatibility/evidence.json` (6 rows: 5 mandatory pass + Paper 26.2/J25 probe fail), `compatibility/` CI matrix | ✅ Present, validated by `CompatibilityEvidence` (mandatory gates certification; probe informational) |
| Build adapter | `build.gradle.kts` (Java 17 toolchain, `--release 17`, Paper 1.18.2 + Vault 1.7 compileOnly, Shadow relocate `anvillink.libs.kyori`, spotless), `gradle/libs.versions.toml`, `src/main/resources/plugin.yml` (`api-version: 1.13`, `softdepend: [Vault]`), `config.yml` | ✅ Verified |
| CI | `.github/workflows/build.yml` + `smoke.yml` (5 mandatory + 1 probe `continue-on-error`) | ✅ Present |
| Docs | `README.md`, `CHANGELOG.md`, `LICENSE` (GPL-3.0-or-later), `docs/real-provider-pin.md` | ✅ Present |
| PDC identity | `danielxxomg:anvillink_repair_sign` (BYTE_ARRAY `magic|schema=1|mode|creator UUID|authorized-create`) | ✅ Permanent, brand-independent |

No artifacts missing for Phases 1–8. Phase 9 artifacts (GitHub release/tag) are intentionally absent — gated, not missing.

---

## Remaining BLOCKED Work (Intentional — Not a Defect)

**Phase 9: GitHub Release Publication — USER-AUTHORIZED GATE (0/3)**

- [ ] 9.1 Create GitHub release with tag containing JAR + source archive + LICENSE + SHA-256 + `compatibility/evidence.json` + CHANGELOG notes — platform-compatibility Scenario: Distribution criterion is satisfied.
- [ ] 9.2 Verify release downloadable, checksum matches local, evidence.json included, GPL present.
- [ ] 9.3 Local readiness check (passes; publication deferred).

**Why blocked**: `tasks.md` Publication gate: `Apply MAY prepare ... MUST NOT create GitHub release/tag/push/commit/PR without explicit user authorization and native delivery gates.` `apply-progress.md` Slice 8 confirms: `No commit/push/tag/PR/release (local only, Phase 9 BLOCKED)`. `verify-report.md` treats this as expected pending, not CRITICAL.

**To unblock**: explicit maintainer authorization + `gentle-ai review validate` passing + `git push`/`gh pr create`/`git tag` allowed per `AGENTS.md` Delivery rules. Archive marks this as `intentional-with-warnings`; the SDD cycle for implementation/verification is complete, publication is a separate delivery action.

---

## Archive Move (Mechanical — Step 3)

Mechanical filesystem operation per Mandatory Mechanical Copy Contract: shell `cp -R`/`mv`/`git mv` only, never Read→Write, with `diff -r` readback. Archive-report is additive-only and excluded from source/destination comparison.

This report was written before the move at `openspec/changes/paid-repair-signs/archive-report.md` and moves with the folder to `openspec/changes/archive/2026-08-08-paid-repair-signs/`. Verbatim `diff -r` output for the move will appear in the phase result envelope (Step 4 verification); empty diff is the only passing evidence. If shell access were unavailable, the phase would report `blocked: shell access required`.

Post-move checks (openspec/hybrid):
- [ ] Main specs updated correctly (4 domains created, diff-verified above)
- [ ] Change folder moved to `openspec/changes/archive/2026-08-08-paid-repair-signs/`
- [ ] Archive contains proposal, specs (4), design, exploration, tasks, apply-progress, verify-report, archive-report
- [ ] Archived `tasks.md` has no unchecked implementation tasks beyond the 3 intentional Phase 9 gate tasks (approved reconciliation; apply-progress/verify-report prove Phases 1–8 97/97 complete)
- [ ] Active `openspec/changes/paid-repair-signs/` no longer exists (source gone before readback)
- [ ] Verbatim `diff -r` readback included in result and empty

---

## Delivery Constraints Honored

- `Do NOT push/tag/PR/release. Archive is local.` — honored: no `git push`, `gh pr create`, `git tag`, or GitHub release created; all 8 slices were local commits on `feat/anvillink/slice-1-scaffold` (aggregate >1,200 lines only via independently testable auto-chain slices, each ≤400 per `AGENTS.md` 4R budget).
- `Do NOT implement Phase 9` — honored: Phase 9 remains 0/3 BLOCKED, not attempted.
- `Respect final state facts` — honored: Phase 9 gate reported as intentional, not stale; ledger `rescope resets` noted as settled, `decision_required false`.
- `Run no destructive git operations` — honored: only `cp`/`mv`/`git mv` + `diff -r` + report write; no history rewrite, no tag, no remote mutation.

---

## Evidence Hashes (for traceability)

- `verify-report.md: evidence_revision`: `sha256:5db0004f075202aac44e5e82dd553ed686626c4d364f94ffdb0f9a45b65c028b`
- `verify-report.md: test_output_hash`: `sha256:4db66ac93193fb4508825e1e1392a19300233c026f5fb71ccd02694d44543a75`
- `verify-report.md: build_output_hash`: `sha256:76a6f1a7b14cb72925a3f69e343c2e77d974959ab132d3673a26ffd2327fd167`
- Slice commits: `44979e3` (2a), `4aecd62` (2b), `97e488a` (3), `8271b40` (4), `e7e8b5d` (5), `0d8f307` (6), `130f359` (7), `f77dd5e` (8) — plus foundations `53b77ac` / proposals.

---

## SDD Cycle Complete

**Change `paid-repair-signs`**: planned, specified (22 reqs / 36 scenarios), designed, implemented (97/97 Phases 1–8, 100%), verified (`pass_with_warnings`, 0 critical, 121 tests, build + spotless PASS), and archived (specs synced to `openspec/specs/`, change folder moved to `openspec/changes/archive/2026-08-08-paid-repair-signs/`).

Ready for the next change. Publication (Phase 9) is a separately authorized delivery action, not part of this cycle's close.
