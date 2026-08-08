# Apply Progress — paid-repair-signs (AnvilLink)

**Slice**: 1 / Phase 1 — Gradle Scaffold, Plugin Metadata, Domain Value Types, Bytecode Floor Proof (PR 1)
**Status**: COMPLETE — ready for candidate checking / review of slice 1
**Date**: 2026-08-06
**Executed by**: sdd-apply sub-agent (slice 1, auto-chain / feature-branch-chain)

---

## Delivery context

| Field | Value |
|---|---|
| delivery_strategy | auto-chain |
| chain_strategy | feature-branch-chain (resolved by orchestrator prompt; tasks.md still says "pending") |
| work unit | `slice-1-scaffold-foundations` (runtime attempt acquired by orchestrator) |
| boundary | Slice 1 only: Phase 1 tasks 1.1–1.17. No later-phase domain/application/adapters. |
| max changed lines | 400 forecast (Phase 1) |
| size exception | none |
| Git/GitHub side effects | NONE (no commit, branch push, PR, tag, release, remote). Local branch `feat/anvillink/slice-1-scaffold` created for PR targeting; zero commits made. |

## Resolved delivery path

`Decision needed before apply: Yes` + `Chain strategy: pending` appear in tasks.md. The orchestrator
prompt resolved the delivery path explicitly: `delivery_strategy: auto-chain` and
`chain_strategy: feature-branch-chain`. That prompt is authoritative for this slice; no block
applied. Per feature-branch-chain: PR 1 (this slice) targets the tracker/feature branch; it must
never target `main` directly.

## Completed tasks (all Phase 1)

- [x] 1.1 Gradle wrapper pinned to 8.14.3 (`distributionUrl=...gradle-8.14.3-bin.zip`), `settings.gradle.kts` `rootProject.name = "anvillink"` + foojay toolchain resolver 0.8.0
- [x] 1.2 `gradle/libs.versions.toml`: Paper API 1.18.2-R0.1-SNAPSHOT, VaultAPI 1.7, JUnit 5.10.2, adventure-minimessage 4.11.0, spotless 6.25.0, shadow 8.3.6 (+ SnakeYAML 2.2 test-only)
- [x] 1.3 `build.gradle.kts`: Java 17 toolchain (Temurin), `--release 17`, compileOnly Paper/Vault, JUnit 5, spotless, Shadow with Adventure relocation; processResources + shadowJar expand `${version}`
- [x] 1.4 `LICENSE` (GPL-3.0-or-later) + `CHANGELOG.md` (Unreleased entry)
- [x] 1.5 `src/main/resources/plugin.yml`: name AnvilLink, main entrypoint, api-version 1.13, `softdepend: [Vault]`, permissions anvillink.create/use/manage
- [x] 1.6 RED `PluginDescriptorTest` → GREEN (reads built JAR descriptor: softdepend=[Vault], name=AnvilLink, api-version, 3 permission nodes, version resolved)
- [x] 1.7 `src/main/resources/config.yml`: price 25.00, messages block (MiniMessage templates), admin.target-distance 8
- [x] 1.8 RED `RepairModeTest` → GREEN
- [x] 1.9 `RepairMode` enum `parse(String): Optional<RepairMode>` (case-insensitive HAND/ALL)
- [x] 1.10 RED `EquipmentSlotIdTest` → GREEN (HAND=main-hand only; ALL=six slots; no storage)
- [x] 1.11 `EquipmentSlotId` enum `slotsFor(RepairMode)` + `STORAGE` (never included)
- [x] 1.12 RED `MoneyAmountTest` → GREEN (finite non-negative; rejects negative/NaN/Infinity/empty)
- [x] 1.13 `MoneyAmount` record wrapping BigDecimal with factory validation + `representableAt(fractionalDigits)`
- [x] 1.14 RED `SignRecordTest` → GREEN (byte layout magic|schema=1|mode|creator UUID|authorized-create; roundtrip; fail-closed on malformed/unknown schema/missing auth/unknown mode)
- [x] 1.15 `SignRecord` with `toBytes()`/`fromBytes()`
- [x] 1.16 RED `BytecodeFloorTest` → GREEN (reads release JAR class headers; our classes major=61; JAR has plugin.yml; no Paper/Vault classes)
- [x] 1.17 Verification commands (all pass, see evidence below)

## Files changed (slice 1 authored, additions)

| File | Action | Lines | What |
|------|--------|-------|------|
| `build.gradle.kts` | Created | 115 | Java 17 toolchain, --release 17, compileOnly Paper/Vault, Shadow+relocate, spotless, version expand |
| `settings.gradle.kts` | Created | 9 | rootProject anvillink + foojay resolver |
| `gradle/libs.versions.toml` | Created | 29 | version catalog (pins corrected — see deviations) |
| `gradle/wrapper/gradle-wrapper.properties` | Created | 7 | pinned 8.14.3-bin |
| `gradle/spotless/license-header.txt` | Created | 2 | GPL-3.0-or-later header |
| `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` | Generated | 251+ | Gradle wrapper (tooling, excluded from review budget) |
| `LICENSE` | Created | 19 | GPL-3.0-or-later (summary text) |
| `CHANGELOG.md` | Created | 22 | Keep-a-Changelog, Unreleased |
| `src/main/resources/plugin.yml` | Created | 19 | descriptor (version token expanded at build) |
| `src/main/resources/config.yml` | Created | 29 | price/messages/admin defaults |
| `src/main/java/.../domain/RepairMode.java` | Created | 35 | parse → Optional, stable ordinals |
| `src/main/java/.../domain/EquipmentSlotId.java` | Created | 36 | slotsFor + STORAGE |
| `src/main/java/.../domain/MoneyAmount.java` | Created | 57 | BigDecimal value object |
| `src/main/java/.../domain/SignRecord.java` | Created | 78 | versioned PDC byte layout |
| `src/test/java/.../domain/RepairModeTest.java` | Created | 31 | RED→GREEN |
| `src/test/java/.../domain/EquipmentSlotIdTest.java` | Created | 54 | RED→GREEN |
| `src/test/java/.../domain/MoneyAmountTest.java` | Created | 41 | RED→GREEN |
| `src/test/java/.../domain/SignRecordTest.java` | Created | 71 | RED→GREEN |
| `src/test/java/.../descriptor/PluginDescriptorTest.java` | Created | 86 | RED→GREEN (built-JAR descriptor) |
| `src/test/java/.../descriptor/BytecodeFloorTest.java` | Created | 91 | RED→GREEN (class major 61, host-API exclusion) |
| `openspec/changes/paid-repair-signs/tasks.md` | Updated | — | Phase 1 tasks marked `[x]` + apply notes |

**Authored changed lines (additions + deletions): 831** — excludes wrapper scripts/binary
(`gradlew` 251, `gradlew.bat`, `gradle-wrapper.jar`) and generated `build/` outputs. Exceeds the
Phase 1 ~370-line forecast; see Deviations for reconciliation.

## RED → GREEN evidence (contractually required behavior-first ordering)

| Test | RED (first run) | GREEN (final) | Notes |
|------|-----------------|---------------|-------|
| RepairModeTest | compile-blocked → then FAILED on missing `RepairMode` | PASS (3 tests) | RED observed once source compiled |
| EquipmentSlotIdTest | FAILED (missing type) | PASS (4 tests) | |
| MoneyAmountTest | FAILED (missing type) | PASS (4 tests) | |
| SignRecordTest | FAILED (missing type) | PASS (6 tests) | |
| PluginDescriptorTest | FAILED: `${version}` unresolved + Yaml missing → coordinate fix | PASS (3 tests) | saw real failure before green |
| BytecodeFloorTest | FAILED: no JAR → shadow ordering; then FAILED major=52 on relocated libs → scoped | PASS (3 tests) | honest RED; scoped to our classes |

## Verification commands and exact results (final, check-only)

```
./gradlew test            → BUILD SUCCESSFUL (23 tests, 0 failures)
./gradlew spotlessCheck   → BUILD SUCCESSFUL
./gradlew build           → BUILD SUCCESSFUL (produces build/libs/anvillink-0.1.0-SNAPSHOT.jar)
unzip -l anvillink-0.1.0-SNAPSHOT.jar | grep -E "org/bukkit/|net/milkbowl/" → NONE (no Paper/Vault classes)
unzip -p ... plugin.yml | grep softdepend → softdepend: [Vault]
javap -v .../RepairMode.class | grep "major version" → 61 (Java 17)
23 tests: RepairMode(3) EquipmentSlotId(4) MoneyAmount(4) SignRecord(6) PluginDescriptor(3) BytecodeFloor(3)
```

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command + result | `./gradlew test --tests '*domain*'` → all domain tests PASS (17) |
| Runtime harness command/scenario | `./gradlew build` + JAR inspection (javap major 61, no host classes, plugin.yml softdepend) — real artifact path; no server runtime needed in Phase 1 (pure domain + descriptor/bytecode floor) |
| Rollback boundary | Delete: `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradlew*`, `LICENSE`, `CHANGELOG.md`, `src/` (slice-1 files). Revert tasks.md `[x]` marks + apply-notes. No later-phase files exist yet, so rollback removes nothing unrelated. |

## Deviations from design / tasks

1. **MockBukkit pin stale (tasks 1.2, 1.3)**: "MockBukkit 3.x" does not exist as a resolvable current
   artifact. Verified: JitPack legacy `com.github.MockBukkit` 3.x tags stop ~3.133; project moved to
   `org.mockbukkit.mockbukkit:mockbukkit-v1.21` (PaperMC/Central), latest 4.110.0, compiled against
   Paper 1.21 API and needing Java 25 (JitPack v4.115.0 build fails — no Java 25). **No MockBukkit
   artifact targets Paper 1.18.2.** Phase 1 uses no MockBukkit, so the catalog pins a placeholder
   (`unused-in-slice-1`) with a correction note; the 4.x line gets pinned when Phase 7 lands. This is
   an honest stale-claim correction, not a silent substitution.
2. **JUnit BOM coordinate**: tasks/design say JUnit 5.10; the BOM artifact is `org.junit:junit-bom`
   (not `org.junit.jupiter:junit-jupiter-bom`). Fixed in catalog.
3. **Bytecode-floor assertion scope**: relocated Adventure library classes ship major 52 (Java 8).
   `BytecodeFloorTest` asserts major 61 for **our** classes (`io/github/danielxxomg/anvillink/`,
   excluding `/libs/`), which is the correct floor proof; shaded libs keep their own bytecode.
4. **plugin.yml version token**: `version: '${version}'` must be expanded by `shadowJar`
   (`filesMatching("plugin.yml") { expand(...) }`) — required for the descriptor test to pass.
5. **SnakeYAML test-only dep** added to parse plugin.yml in tests (not shaded).
6. **foojay toolchain resolver** added to settings for JDK 17 auto-provision (no system JDK 17).
7. **Line budget**: Phase 1 authored = 831 (not ~370). Forecast under-counted descriptor/bytecode
   tests (~177 lines), YAML parser, toolchain resolver, GPL header, config/plugin.yml details.
   Recorded honestly; chain strategy keeps this as one reviewable slice (PR 1), no size exception.
8. `entrypoint.AnvilLinkPlugin` is declared in plugin.yml (Phase 6 owns the class); Phase 1 JAR does
   not include it — acceptable for a Phase 1 scaffold (plugin loads diagnostically at Phase 6).

## Issues found

- JitPack cannot build MockBukkit v4.115.0 (needs Java 25, not provisioned) — the `org.mockbukkit`
  Central/PaperMC coordinates are the correct source; verified resolvable.
- No system JDK 17 on the machine; foojay resolver provisions Temurin 17 automatically (works).
- `./gradlew test` on a JDK-17 toolchain correctly catches bytecode drift; the relocated libs are the
  only non-17 classes in the JAR (expected for shaded Adventure).

## Remaining tasks (not in slice 1)

- [ ] Phase 2 (PR 2): SignParser, RepairPlanner, TransactionResult, ValidatedPrice, application ports
- [ ] Phase 3 (PR 3): RepairActivation + Compensation use cases (TDD)
- [ ] Phase 4 (PR 4): PDC identity + sign lifecycle adapters
- [ ] Phase 5 (PR 5): InteractionFilter, VaultEconomyGateway, BukkitEquipmentPort
- [ ] Phase 6 (PR 6): config/MiniMessage/scheduler/admin/entrypoint
- [ ] Phase 7 (PR 7): MockBukkit integration (pin corrected to 4.x), real Vault provider, SemVer
- [ ] Phase 8 (PR 8): evidence schema, CI, docs, openspec/config.yaml testing section update
- [ ] Phase 9 (PR 9): GitHub release publication — USER-AUTHORIZED GATE (blocked)

## Status (slice 1)

**17/17 Phase 1 tasks complete. Slice 1 ready for candidate checking.** Next recommended: sdd-verify
for slice 1, or chain to PR 2 planning once PR 1 is reviewed.

---

## Slice 2a — Parser + Planner (2.1–2.4) — 2026-08-07

**Work unit**: `slice-2-domain-logic` (compact, ≤400)
**Boundary**: 2.1–2.4 only (SignParser + RepairPlanner family); 2.5–2.10 → slice 2b
**Budget**: 343 src + ≤40 docs = ≤393 total (tasks 10 + apply-progress 40 + src 343)

- [x] 2.1 RED `SignParserTest` → GREEN (6 tests)
- [x] 2.2 `SignParser` `parse(line1,line2)` → `Optional<ParseResult>` (pure domain)
- [x] 2.3 RED `RepairPlannerTest` → GREEN (6 tests; HAND=1, ALL=6, no storage)
- [x] 2.4 `RepairPlanner` + `RepairPlan` + `PlannedSlot` + `EquipmentView` + `ItemView` + `ItemSnapshot`

**Slice 2a files (343 lines, 7 prod + 2 test)**:

| File | Lines | Note |
|------|-------|------|
| `domain/SignParser.java` | 32 | `[repair]`+HAND/ALL parser |
| `domain/ItemView.java` | 17 | slot view, no Bukkit |
| `domain/ItemSnapshot.java` | 9 | opaque snapshot |
| `domain/EquipmentView.java` | 10 | provider |
| `domain/PlannedSlot.java` | 6 | ordered slot |
| `domain/RepairPlan.java` | 24 | immutable plan |
| `domain/RepairPlanner.java` | 39 | eligibility filter |
| `domain/SignParserTest.java` | 58 | RED→GREEN 12 err→6 PASS |
| `domain/RepairPlannerTest.java` | 148 | RED→GREEN 35 err→6 PASS |

**Evidence**: `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew test` → 35 PASS (17+12+6), `spotlessCheck` PASS, `build` PASS, `grep Bukkit/Vault domain` → NO MATCH. Rollback: delete slice-2a 9 files + revert tasks 2.1–2.4.
