# Tasks: AnvilLink — Paid Repair Signs

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated aggregate changed lines (phases 1–8) | 2,745 |
| Estimated aggregate including publication (phase 9) | 2,805 |
| 400-line budget risk | High (per immutable slice) |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 → PR 5 → PR 6 → PR 7 → PR 8 → PR 9 |
| Delivery strategy | auto-chain |
| Chain strategy | pending (user decision required) |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

The 1,200-line review budget applies **per immutable review candidate/slice**, not to the aggregate greenfield product. The aggregate implementation (2,745 lines across phases 1–8; 2,805 including gated publication in phase 9) exceeds 1,200 only through independently testable auto-chain slices; every slice targets **≤ 400 lines**.

**Publication gate**: Apply MAY prepare build artifacts, CI metadata, and release documentation locally. Apply MUST NOT create a GitHub release, tag, push, commit, or PR without explicit user authorization and native delivery gates. Phase 9 is BLOCKED until the user explicitly authorizes publication.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Gradle scaffold, plugin metadata, domain value types, bytecode floor proof | PR 1 | `./gradlew test` | N/A — pure domain, no server | `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `src/main/resources/`, `src/main/java/.../domain/{RepairMode,EquipmentSlotId,MoneyAmount,SignRecord}.java`, `src/test/java/.../domain/`, `LICENSE`, `CHANGELOG.md` |
| 2 | Parser, planner, transaction result types | PR 2 | `./gradlew test` | N/A — pure domain, no server | `src/main/java/.../domain/{SignParser,RepairPlanner,PlannedSlot,RepairPlan,TransactionResult,ValidatedPrice}.java`, `src/test/java/.../domain/{SignParser,RepairPlanner,TransactionResult}Test.java` |
| 3 | Ports, activation + compensation use case | PR 3 | `./gradlew test` | N/A — pure domain, no server | `src/main/java/.../{application,domain/RepairActivation}.java`, `src/test/java/.../domain/{RepairActivation,Compensation}Test.java` |
| 4 | PDC identity, sign lifecycle adapter | PR 4 | `./gradlew test` | Start Paper 1.20.6, place sign, verify PDC | `src/main/java/.../adapter/{PdcSignIdentity,SignLifecycleListener}.java`, `src/test/java/.../adapter/{PdcSignIdentity,SignLifecycleListener}Test.java` |
| 5 | Interaction filter, Vault gateway, equipment adapter | PR 5 | `./gradlew test` | Start Paper 1.20.6, right-click sign, verify charge | `src/main/java/.../adapter/{InteractionFilter,VaultEconomyGateway,BukkitEquipmentPort}.java`, `src/test/java/.../adapter/{InteractionFilter,VaultEconomyGateway,BukkitEquipmentPort}Test.java` |
| 6 | Config, MiniMessage, scheduler, admin commands, entrypoint | PR 6 | `./gradlew test` | Start Paper 1.20.6, reload config, verify message | `src/main/java/.../{adapter/{FileConfigurationPort,MiniMessageMessagePort,BukkitSchedulerAdapter,AdminCommandHandler},config,presentation,entrypoint/AnvilLinkPlugin}.java`, `src/test/java/.../adapter/{FileConfigurationPort,AdminCommandHandler,MiniMessageMessagePort}Test.java` |
| 7 | MockBukkit integration, real Vault provider, SemVer separation | PR 7 | `./gradlew test` | Start Paper 1.20.6, full sign lifecycle E2E + real EssentialsX economy | `src/test/java/.../integration/` |
| 8 | Evidence schema, CI, docs | PR 8 | `./gradlew build` + CI matrix | CI smoke: Paper 1.18.2/1.20.6/1.21.11, Spigot 1.20.6, Purpur 1.20.6, Paper 26.x probe | `compatibility/`, `.github/workflows/`, `README.md`, `CHANGELOG.md` updates, `openspec/config.yaml` updates |
| 9 | GitHub release publication (USER-AUTHORIZED) | GATED | N/A — requires user authorization | N/A — publication gate | Delete GitHub release/tag if created in error |

---

## Phase 1: Gradle Scaffold, Plugin Metadata, Domain Value Types, Bytecode Floor Proof — PR 1 (~370 lines)

> **Slice 1 apply notes (2026-08-06)**: All Phase 1 tasks complete. Authored changed lines = **831** (19 files; wrapper scripts/binary + generated build outputs excluded), above the ~370 forecast — see apply-progress.md for the honest reconciliation (descriptor/bytecode tests, SnakeYAML test dep, foojay resolver, GPL header add ~460 lines vs forecast). Stale pin corrected: MockBukkit is **not** 3.x today (see libs.versions.toml note; unused in Phase 1, corrected to 4.x line when Phase 7 lands). JUnit BOM coordinate is `org.junit:junit-bom` (not `junit-jupiter-bom`). `version` placeholder expansion confirmed in the shadow JAR. Bytecode-floor assertion scoped to our classes (relocated Adventure libs ship their own bytecode).

- [x] 1.1 Generate Gradle 8.x wrapper pinned to distribution URL; create `settings.gradle.kts` with `rootProject.name = "anvillink"`
- [x] 1.2 Create `gradle/libs.versions.toml`: Paper API 1.18.2, VaultAPI 1.7, JUnit 5.10, MockBukkit 3.x, adventure-text-minimessage 4.11.0
- [x] 1.3 Create `build.gradle.kts`: Java 17 toolchain, `--release 17`, `compileOnly` Paper/Vault, JUnit 5 + MockBukkit test deps, spotless formatting, Shadow JAR with Adventure relocation
- [x] 1.4 Add `LICENSE` (GPL-3.0-or-later) and `CHANGELOG.md` (initial unreleased entry)
- [x] 1.5 Create `src/main/resources/plugin.yml`: name AnvilLink, main entrypoint, api-version 1.13, softdepend [Vault], permissions (anvillink.create, anvillink.use, anvillink.manage)
- [x] 1.6 RED test `PluginDescriptorTest`: parse built `plugin.yml`, assert exact `softdepend: [Vault]`, name=`AnvilLink`, api-version present, all three permission nodes declared — platform-compliance Scenario: Host APIs are not packaged (soft-depend metadata assertion)
- [x] 1.7 Create `src/main/resources/config.yml`: default price 25.00, messages block with MiniMessage templates, admin.target-distance default 8
- [x] 1.8 RED test `RepairModeTest`: parse "HAND"→HAND, "ALL"→ALL, "hand"→HAND, "repair"→null
- [x] 1.9 GREEN: implement `RepairMode` enum with `parse(String)` returning Optional
- [x] 1.10 RED test `EquipmentSlotIdTest`: ordered slots for HAND/ALL, verify no storage slots — equipment-repair Scenarios: HAND excludes other, ALL excludes storage
- [x] 1.11 GREEN: implement `EquipmentSlotId` enum with `HAND_SLOTS` and `ALL_SLOTS` constants
- [x] 1.12 RED test `MoneyAmountTest`: finite non-negative accepted, negative/infinite rejected, scale validation — repair-economy Scenario: Invalid precision or value fails closed
- [x] 1.13 GREEN: implement `MoneyAmount` value object wrapping BigDecimal with factory validation
- [x] 1.14 RED test `SignRecordTest`: PDC byte-array layout magic|schema|mode|creator|authorized-create, roundtrip encode/decode
- [x] 1.15 GREEN: implement `SignRecord` with `toBytes()`/`fromBytes()` for versioned PDC serialization
- [x] 1.16 RED test `BytecodeFloorTest`: run produced JAR under a newer JDK (e.g. 21), mechanically inspect class major version = 61 (Java 17) via `javap -v`; fail if bytecode version drifts above 61 — platform-compliance Scenario: Newer build JDK still targets the floor
- [x] 1.17 Verify: `./gradlew test` passes; `./gradlew build` produces JAR; `./gradlew spotlessCheck` clean; JAR contains no Paper/Vault classes; bytecode major version = 61; plugin.yml has `softdepend: [Vault]`

## Phase 2: Parser, Planner, Transaction Types — PR 2 (~315 lines)

> **Slice 2 apply notes (2026-08-07) — partial, budget-constrained**: 2.1–2.4 complete (SignParser + RepairPlanner family) = 343 authored lines (7 prod + 2 test files). 2.5–2.10 deferred to slice 2b to stay ≤400 native limit (TransactionResult 112 lines, ValidatedPrice 103 lines, ports ~100 lines would exceed). All RED→GREEN verified; domain remains Bukkit/Vault-free.

- [x] 2.1 RED test `SignParserTest`: case-insensitive "[repair]" on line 1, "HAND"/"ALL" on line 2, wrong location rejected, empty/invalid rejected — repair-signs Scenario: Case-insensitive canonical creation, Wrong location is rejected
- [x] 2.2 GREEN: implement `SignParser` with `parse(String line1, String line2)` returning Optional\<ParseResult\>
- [x] 2.3 RED test `RepairPlannerTest`: HAND→main-hand only, ALL→six slots, excludes storage, skips empty/undamaged/unbreakable/non-Damageable — equipment-repair Scenarios: Mixed targets select only eligible, No eligible target creates no plan, Repeated planning is stable
- [x] 2.4 GREEN: implement `RepairPlanner` with `plan(mode, equipmentProvider)` returning RepairPlan with ordered PlannedSlot list + snapshots
- [x] 2.5 RED test `TransactionResultTest`: success with amount, fail-closed types (no-provider, insufficient-funds, invalid-response, apply-failure), compensation outcomes — repair-economy Scenario: Single withdrawal contract
- [x] 2.6 GREEN: implement `TransactionResult` sealed hierarchy (Success, NoProvider, InsufficientFunds, InvalidResponse, ApplyFailure, CompensationSuccess, CompensationFailed, RestorationFailed)
- [x] 2.7 RED test `ValidatedPriceTest`: finite non-negative accepted, negative/infinite/precision-overflow rejected, BigDecimal scale check against fractionalDigits
- [x] 2.8 GREEN: implement `ValidatedPrice` factory in domain with provider-precision validation
- [x] 2.9 Define application port interfaces: `SignPort`, `EquipmentPort`, `EconomyPort`, `SchedulerPort`, `ConfigurationPort`, `MessagePort`, `OperationalReporter` — neutral IDs, BigDecimal, opaque snapshots
- [x] 2.10 Verify: `./gradlew test` — all domain tests green, zero Bukkit/Vault imports in `domain` package

## Phase 3: Ports, Activation + Compensation Use Case (TDD) — PR 3 (~335 lines)

- [x] 3.1 RED test `RepairActivationTest` (no eligible): validate sign, plan empty, no Vault call, no charge — repair-economy Scenario: Undamaged or ineligible equipment is free
- [x] 3.2 RED test `RepairActivationTest` (no provider): valid plan, missing Vault → fail closed — repair-economy Scenario: Provider is unavailable
- [x] 3.3 RED test `RepairActivationTest` (insufficient funds): single withdrawal fails, no second attempt, no repair — repair-economy Scenario: Insufficient funds do not repair
- [x] 3.4 RED test `RepairActivationTest` (text alone): visible text matches but PDC incomplete → no charge — repair-signs Scenario: Visible text alone has no authority
- [x] 3.5 RED test `RepairActivationTest` (flat charge): non-empty ALL plan, price 25.00, success → exactly one withdrawal of 25.00 — repair-economy Scenario: One successful activation has one flat charge
- [x] 3.6 RED test `RepairActivationTest` (payment failure preserves): failed payment → no item mutated — equipment-repair Scenario: Payment failure preserves equipment
- [x] 3.7 RED test `CompensationTest` (success restore): withdrawal + partial mutation + restore success + deposit success → no net charge — repair-economy Scenario: Successful compensation restores payment state
- [x] 3.8 RED test `CompensationTest` (deposit fails): mutation + restore + deposit fail → high-sev event, player notified — repair-economy Scenario: Failed compensation is observable
- [x] 3.9 RED test `CompensationTest` (restore fails): mutation + restore fails → deposit attempted, high-sev evidence, no retry — repair-economy Scenario: Restoration failure is terminal and observable
- [x] 3.10 GREEN: implement `RepairActivation` use case — validate sign/PDC, check permissions, plan equipment, validate price, call economy port, apply repair on scheduler via `EquipmentPort` snapshot restoration, handle all failure/compensation paths with compensating deposit via `EconomyPort`
- [x] 3.11 Verify: `./gradlew test` — all activation/compensation RED tests now green

## Phase 4: PDC Identity, Sign Lifecycle Adapter — PR 4 (~360 lines)

- [x] 4.1 RED test `PdcSignIdentityTest`: encode/decode BYTE_ARRAY, roundtrip through TileState mock — platform-compliance Scenario: Stable namespace (display-brand change preserves identity)
- [x] 4.2 RED test `PdcSignIdentityTest` (malformed): malformed bytes fail closed, missing creator/marker fails closed — repair-signs Scenario: Missing creation authorization fails closed
- [x] 4.3 GREEN: implement `PdcSignIdentity` adapter — read/write `danielxxomg:anvillink_repair_sign` PDC key, versioned BYTE_ARRAY, permanent namespace decoupled from display brand
- [x] 4.4 RED test `SignLifecycleListenerTest` (create authorized): permitted player, valid front [repair]+HAND → blue text, PDC written
- [x] 4.5 RED test `SignLifecycleListenerTest` (create unauthorized): no create permission → cancelled, no PDC — repair-signs Scenario: Unauthorized creation is rejected
- [x] 4.6 RED test `SignLifecycleListenerTest` (break unauthorized): registered sign, no manage → cancelled, PDC unchanged — repair-signs Scenario: Unauthorized edit or break is cancelled
- [x] 4.7 RED test `SignLifecycleListenerTest` (edit by manager): manage permission → proceeds, but text remains tampered until rerender
- [x] 4.8 GREEN: implement `SignLifecycleListener` — SignChangeEvent (create), BlockBreakEvent (break), PDC check, permission gate
- [x] 4.9 RED test `PdcNamespacePermanenceTest`: simulate display-brand rename → PDC namespace/key/schema unchanged, existing signs remain valid — platform-compliance Scenario: Display-brand change preserves identity
- [x] 4.10 Verify: `./gradlew test` — PDC and lifecycle tests green

## Phase 5: Interaction Filter, Vault Gateway, Equipment Adapter — PR 5 (~380 lines)

- [x] 5.1 RED test `InteractionFilterTest`: main-hand event→proceed, off-hand event→ignored, non-right-click→ignored — repair-signs Scenario: Duplicate events do not double-charge
- [x] 5.2 GREEN: implement `InteractionFilter` — check EquipmentSlot.HAND, block off-hand duplicate in activation listener
- [x] 5.3 RED test `VaultEconomyGatewayTest` (withdraw success): valid plan, provider returns success → TransactionResult.Success with amount — repair-economy Scenario: One successful activation has one flat charge
- [x] 5.4 RED test `VaultEconomyGatewayTest` (withdraw fail): provider returns !transactionSuccess → no mutation, no deposit — repair-economy Scenario: Insufficient funds do not repair
- [x] 5.5 RED test `VaultEconomyGatewayTest` (missing provider): no Economy registered → TransactionResult.NoProvider — repair-economy Scenario: Provider is unavailable
- [x] 5.6 RED test `VaultEconomyGatewayTest` (invalid response): success=true but amount mismatch → deposit once for finite withdrawn, otherwise severe evidence — economy response validation
- [x] 5.7 RED test `VaultEconomyGatewayTest` (fractionalDigits): price scale exceeds provider fractionalDigits → rejected — repair-economy Scenario: Invalid precision or value fails closed
- [x] 5.8 RED test `VaultEconomyGatewayTest` (compensation adapter): provider deposit/refund call succeeds/fails as delegated by use case — economy adapter contract for compensation
- [x] 5.9 GREEN: implement `VaultEconomyGateway` — EconomyPort: withdraw-once, BigDecimal conversion, provider response validation, delegate deposit/refund calls. Ownership: adapter only, no orchestration or snapshot restoration
- [x] 5.10 RED test `BukkitEquipmentPortTest`: resolve HAND/ALL from PlayerInventory, apply setDamage(0), restore from snapshot — equipment-repair Scenarios: Payment failure preserves equipment, Apply failure restores snapshots
- [x] 5.11 GREEN: implement `BukkitEquipmentPort` — read Bukkit PlayerInventory slots, setDamage(0), snapshot restore. Ownership: equipment port owns snapshot restoration
- [x] 5.12 Verify: `./gradlew test` — interaction, economy, equipment tests green

## Phase 6: Config, MiniMessage, Scheduler, Admin, Entrypoint — PR 6 (~285 lines)

- [x] 6.1 RED test `FileConfigurationPortTest`: valid reload → atomically swap; invalid reload → retain prior, report failure; invalid startup → disable activation — repair-signs Scenarios: Valid reload changes presentation, Invalid reload fails deterministically, Invalid initial configuration fails closed
- [x] 6.2 GREEN: implement `FileConfigurationPort` with AtomicReference\<ConfigSnapshot\> swap
- [x] 6.3 RED test `MiniMessagePortTest`: supply a configured MiniMessage template with placeholder, call `MessagePort.render()`, assert returned String matches expected Bukkit-facing output; assert return type is `String` and no Adventure types (`Component`, `TagResolver`, etc.) appear in the port's public API signature — repair-signs Scenario: Valid reload changes presentation (MiniMessage renders through isolated/relocated implementation)
- [x] 6.4 GREEN: implement `MiniMessageMessagePort` — shade Adventure 4.11.0, expose String through port, no relocated type crosses port boundary, signs use Bukkit strings/color not MiniMessage
- [x] 6.5 GREEN: implement `BukkitSchedulerAdapter` — delegate to BukkitScheduler, reject Folia via `io.papermc.paper.threadedregions` detection
- [x] 6.6 RED test `AdminCommandTest`: inspect valid→report validity, inspect tampered→report tampered, rerender valid→restore canonical text, rerender invalid identity→reject — repair-signs Scenarios: Tampered text is rejected, Valid identity is re-rendered
- [x] 6.7 GREEN: implement `/anvillink inspect|rerender` — player/manage permission, line-of-sight targeting, configurable distance 1–32 (default 8)
- [x] 6.8 GREEN: implement `AnvilLinkPlugin` entrypoint — onEnable wires adapters, registers listeners, loads config; onDisable cleanup
- [x] 6.9 Verify: `./gradlew test` — config, MiniMessage, admin, entrypoint tests green

## Phase 7: MockBukkit Integration, Real Vault Provider, SemVer — PR 7 (~390 lines)

- [ ] 7.1 MockBukkit E2E: create sign with permission → blue text + PDC, right-click repairs HAND, verify one charge — repair-signs Scenarios: Case-insensitive canonical creation + end-to-end
- [ ] 7.2 MockBukkit: create without permission → no PDC, no record — repair-signs Scenario: Unauthorized creation is rejected
- [ ] 7.3 MockBukkit: edit/break registered sign without manage → cancelled, PDC unchanged — repair-signs Scenario: Unauthorized edit or break is cancelled
- [ ] 7.4 MockBukkit: tampered text activation → fail closed, no charge — repair-signs Scenario: Tampered text is rejected
- [ ] 7.5 MockBukkit: ALL mode repairs six slots, storage untouched — equipment-repair Scenario: ALL excludes storage
- [ ] 7.6 MockBukkit: no eligible items → no Vault call — repair-economy Scenario: Undamaged or ineligible equipment is free
- [ ] 7.7 MockBukkit: insufficient funds → no repair, items unchanged — repair-economy Scenario: Insufficient funds do not repair
- [ ] 7.8 MockBukkit: duplicate hand events → one activation, one charge — repair-signs Scenario: Duplicate events do not double-charge
- [ ] 7.9 MockBukkit: Vault absent → fail closed diagnostic — repair-economy Scenario: Provider is unavailable
- [ ] 7.10 MockBukkit: admin inspect/rerender on tampered sign → restores canonical — repair-signs Scenario: Valid identity is re-rendered
- [ ] 7.11 MockBukkit: reload valid→swaps; reload invalid→retains prior — repair-signs Scenarios: Valid reload changes presentation, Invalid reload fails deterministically
- [ ] 7.12 RED test `RealVaultProviderSetup`: verify and pin official EssentialsX release compatible with selected Paper runtime; record source URL, exact version, SHA-256 checksum, and GPL-2.0 license. No standalone/custom/fake provider fallback satisfies this task
- [ ] 7.13 RED test `VaultProviderIntegrationTest`: wire official Vault + pinned official EssentialsX Economy on real Paper runtime, verify withdrawal + repair end-to-end — platform-compliance Scenario: Incomplete evidence blocks release claims (real Vault-provider path required)
- [ ] 7.14 RED test `MissingRealProviderBlocksClaimTest`: given unit + MockBukkit + fake-provider pass but real Vault-provider evidence is missing, verify release claim is blocked — platform-compliance negative test: missing/failed real-provider evidence prohibits the corresponding release claim
- [ ] 7.15 GREEN: implement real Vault-provider integration test using pinned official EssentialsX Economy on real Paper runtime; assert claim blocked when evidence missing
- [ ] 7.16 RED test `SemVerSeparationTest`: verify SemVer value and compatibility matrix are published as separate claims, matrix update does not bump version — platform-compliance Scenario: Version and tested range stay distinct
- [ ] 7.17 GREEN: implement SemVer/matrix separation validation in build metadata
- [ ] 7.18 Verify: `./gradlew test` — all integration, real-provider, negative-evidence, and SemVer tests green

## Phase 8: Evidence Schema, CI, Docs — PR 8 (~310 lines)

- [ ] 8.1 Create `compatibility/evidence.json` schema: `{distribution, version, build, serverSha256, jdkMajor, testSuite, result}` per row
- [ ] 8.2 RED test `CompatibilityEvidenceSchemaTest`: validate evidence.json structure, mandatory rows (Paper 1.18.2/J17, 1.20.6/J21, 1.21.11/J21, Spigot 1.20.6/J21, Purpur 1.20.6/J21) must pass; probe row (Paper 26.x/J25) may fail without blocking certified ranges — platform-compliance Scenario: Missing Paper runtime evidence blocks certification
- [ ] 8.3 RED test `EvidenceGatedSupportTest`: Paper certified only when all mandatory Paper rows pass; Spigot/Purpur verified only after separate smoke; Folia experimental; Paper 26.x uncertified until Java 25 job passes; probe failure does not block unrelated certified claims — platform-compliance Scenario: Support labels follow their evidence, Paper 26.x requires Java 25 evidence
- [ ] 8.4 GREEN: implement evidence validation — mandatory rows gate certification; probe rows are informational; failed/missing probe does not prohibit certified ranges
- [ ] 8.5 Create `.github/workflows/build.yml`: Gradle build + test on push/PR
- [ ] 8.6 Create `.github/workflows/smoke.yml`: matrix — mandatory (Paper 1.18.2/J17, Paper 1.20.6/J21, Paper 1.21.11/J21, Spigot 1.20.6/J21, Purpur 1.20.6/J21) + probe (Paper 26.x/J25, continue-on-error)
- [ ] 8.7 Write `README.md`: H1, benefit paragraph, quick-start, support-tier table, permissions, config, FAQ, `SoftwareApplication` schema
- [ ] 8.8 Update `CHANGELOG.md` with all features for initial release
- [ ] 8.9 Update `openspec/config.yaml` testing section: runner available=true, command=`./gradlew test`, framework=JUnit 5, unit=JUnit 5, integration=MockBukkit, linter=`./gradlew spotlessCheck`, formatter=`./gradlew spotlessApply`
- [ ] 8.10 Verify: `./gradlew build` produces release JAR; CI matrix mandatory rows pass; JAR inspection confirms no Paper/Vault classes, plugin.yml present, PDC namespace correct; all artifacts remain local (no commit/push/tag/PR/release)

## Phase 9: GitHub Release Publication — USER-AUTHORIZED GATE — (~60 lines)

> **BLOCKED**: This phase MUST NOT execute during apply. It requires explicit user authorization after all prior phases pass and all native delivery/review gates clear. Apply only prepares release-ready artifacts; this phase creates the public release.

- [ ] 9.1 **BLOCKED — requires user authorization**: Create GitHub release with tag, containing: downloadable plugin JAR, source archive, GPL-3.0-or-later LICENSE, SHA-256 checksum, `compatibility/evidence.json`, and release notes from CHANGELOG — platform-compliance Scenario: Distribution criterion is satisfied (mapped here, not to README/CHANGELOG)
- [ ] 9.2 **BLOCKED — requires user authorization**: Verify GitHub release exists with all artifacts downloadable, checksum matches local build, evidence.json included, GPL license present
- [ ] 9.3 Verify: release readiness check passes locally; actual publication deferred to user-authorized delivery action

---

## Architecture Ownership Contract

| Concern | Owner | Rationale |
|---------|-------|-----------|
| Activation orchestration | `RepairActivation` (use case) | Single transaction boundary |
| Compensating deposit | `RepairActivation` via `EconomyPort` | Use case decides when to refund |
| Snapshot restoration | `BukkitEquipmentPort` via `EquipmentPort` | Equipment port owns inventory state |
| Provider withdraw/deposit/response | `VaultEconomyGateway` via `EconomyPort` | Adapter only, no orchestration |
| Price validation | `ValidatedPrice` (domain) | Pure domain, no Vault dependency |

---

## Scenario Traceability Matrix

| # | Spec | Requirement | Scenario | Task(s) |
|---|------|-------------|----------|---------|
| 1 | repair-signs | Exact authorized creation | Case-insensitive canonical creation | 2.1, 7.1 |
| 2 | repair-signs | Exact authorized creation | Unauthorized creation is rejected | 4.5, 7.2 |
| 3 | repair-signs | Exact authorized creation | Wrong location is rejected | 2.1 |
| 4 | repair-signs | Permanent PDC identity | Visible text alone has no authority | 3.4 |
| 5 | repair-signs | Permanent PDC identity | Missing creation authorization fails closed | 4.2 |
| 6 | repair-signs | Reloadable messages | Valid reload changes presentation | 6.1, 6.3, 7.11 |
| 7 | repair-signs | Reloadable messages | Invalid reload fails deterministically | 6.1, 7.11 |
| 8 | repair-signs | Reloadable messages | Invalid initial configuration fails closed | 6.1 |
| 9 | repair-signs | Tamper detection | Tampered text is rejected | 6.6, 7.4 |
| 10 | repair-signs | Tamper detection | Valid identity is re-rendered | 6.6, 7.10 |
| 11 | repair-signs | Protected lifecycle | Unauthorized edit or break is cancelled | 4.6, 7.3 |
| 12 | repair-signs | Duplicate-hand filtering | Duplicate events do not double-charge | 5.1, 7.8 |
| 13 | repair-economy | Valid fixed price | One flat charge | 3.5, 5.3 |
| 14 | repair-economy | Valid fixed price | Invalid precision or value fails closed | 1.12, 5.7 |
| 15 | repair-economy | No eligible target | Undamaged or ineligible equipment is free | 3.1, 7.6 |
| 16 | repair-economy | Vault/provider absence | Provider is unavailable | 3.2, 5.5, 7.9 |
| 17 | repair-economy | Single withdrawal | Insufficient funds do not repair | 3.3, 5.4, 7.7 |
| 18 | repair-economy | Compensation | Successful compensation restores payment state | 3.7, 5.8 |
| 19 | repair-economy | Compensation | Failed compensation is observable | 3.8, 5.8 |
| 20 | repair-economy | Compensation | Restoration failure is terminal and observable | 3.9, 5.8 |
| 21 | equipment-repair | Exact target modes | HAND excludes other equipment | 1.10 |
| 22 | equipment-repair | Exact target modes | ALL excludes storage | 1.10, 7.5 |
| 23 | equipment-repair | Eligibility | Mixed targets select only eligible items | 2.3 |
| 24 | equipment-repair | Eligibility | No eligible target creates no plan | 2.3, 3.1 |
| 25 | equipment-repair | Deterministic planning | Repeated planning is stable | 2.3 |
| 26 | equipment-repair | Payment-gated application | Payment failure preserves equipment | 3.6 |
| 27 | equipment-repair | Payment-gated application | Apply failure restores snapshots | 5.10 |
| 28 | platform | Compatibility floor | Newer build JDK still targets the floor | 1.3, 1.16, 1.17 |
| 29 | platform | Single-JAR boundary | Host APIs are not packaged | 1.6, 1.17 |
| 30 | platform | Stable namespace | Display-brand change preserves identity | 4.1, 4.9 |
| 31 | platform | SemVer separate | Version and tested range stay distinct | 7.16 |
| 32 | platform | Evidence-gated tiers | Missing Paper runtime evidence blocks certification | 8.2 |
| 33 | platform | Evidence-gated tiers | Support labels follow their evidence | 8.3 |
| 34 | platform | Evidence-gated tiers | Paper 26.x requires Java 25 evidence | 8.3 |
| 35 | platform | Verification layers | Incomplete evidence blocks release claims | 7.13, 7.14, 8.2 |
| 36 | platform | GPL + release | Distribution criterion is satisfied | 9.1, 9.2 |
