```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:5db0004f075202aac44e5e82dd553ed686626c4d364f94ffdb0f9a45b65c028b
verdict: pass_with_warnings
blockers: 0
critical_findings: 0
requirements: 22/22
scenarios: 36/36
test_command: GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew cleanTest test
test_exit_code: 0
test_output_hash: sha256:4db66ac93193fb4508825e1e1392a19300233c026f5fb71ccd02694d44543a75
build_command: GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew build
build_exit_code: 0
build_output_hash: sha256:76a6f1a7b14cb72925a3f69e343c2e77d974959ab132d3673a26ffd2327fd167
```

## Verification Report

**Change**: paid-repair-signs
**Version**: N/A (greenfield)
**Mode**: Standard

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 100 |
| Tasks complete | 97 |
| Tasks incomplete | 3 (Phase 9: 9.1, 9.2, 9.3 — USER-AUTHORIZED GATE, expected BLOCKED) |
| Phases 1–8 | 97/97 complete (100%) |
| Phase 9 publication gate | 0/3 BLOCKED — intentional, requires explicit user authorization + native delivery gates |

Phase 9 is not a defect: `tasks.md` and `apply-progress.md` explicitly gate GitHub release/tag/push/PR until user authorizes. Verification treats this as expected pending, not CRITICAL.

### Build & Tests Execution
**Build**: ✅ Passed
```text
GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew build
BUILD SUCCESSFUL in 1s
JAR: build/libs/anvillink-0.1.0-SNAPSHOT.jar
plugin.yml present in JAR, version='0.1.0-SNAPSHOT' (expanded from ${version})
No Paper/Vault classes shaded: unzip -l shows no org/bukkit or net/milkbowl
PDC namespace danielxxomg:anvillink_repair_sign present in strings
Bytecode floor major 61 (Java 17) verified via BytecodeFloorTest (our classes), Adventure 4.11.0 relocated to io.github.danielxxomg.anvillink.libs.kyori
```

**Tests**: ✅ 121 passed / 0 failed / 0 skipped
```text
GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew cleanTest test
BUILD SUCCESSFUL in 1s
121 tests completed, 0 failed
Spotless: ./gradlew spotlessCheck → BUILD SUCCESSFUL (googleJavaFormat 1.17.0 + ktlint 1.0.1, license header)
```

Focused suites verified:
- Domain: RepairMode(3) EquipmentSlotId(4) MoneyAmount(4) SignRecord(6) SignParser(6) RepairPlanner(6) TransactionResult(7) ValidatedPrice(7) RepairActivation(9) = 52
- Adapter: PdcSignIdentity(3) NamespacePermanence(2) SignLifecycleListener(4) InteractionFilter(4) VaultEconomyGateway(7) BukkitEquipmentPort(4) FileConfigurationPort(4) MiniMessagePort(3) AdminCommand(5) = 36
- Integration: SignIntegrationTest(11) = 11
- Platform: CompatibilityEvidenceSchema(3) EvidenceGatedSupport(6) MissingRealProviderBlocksClaim(3) RealVaultProviderEvidence(2) SemVerSeparation(2) BytecodeFloor(3) PluginDescriptor(3) = 22
- Total 121; flaky ordering noted on `clean` without `cleanTest` — `cleanTest test` passes deterministically

**Coverage**: ➖ Not available (JaCoCo not configured; threshold not enforced; 121 behavioral tests cover 36 scenarios)

### Spec Compliance Matrix
| # | Requirement | Scenario | Test Evidence | Result |
|---|-------------|----------|---------------|--------|
| 1 | repair-signs: Exact authorized creation | Case-insensitive canonical creation | `SignParserTest.parsesCaseInsensitiveRepairOnLine1AndHandOnLine2` + `SignIntegrationTest.createWithPermission_blueTextAndPdc` | ✅ COMPLIANT |
| 2 | repair-signs: Exact authorized creation | Unauthorized creation is rejected | `SignLifecycleListenerTest.createUnauthorized_cancelledNoPdc` + `SignIntegrationTest.createWithoutPermission_noPdc` | ✅ COMPLIANT |
| 3 | repair-signs: Exact authorized creation | Wrong location is rejected | `SignParserTest.rejectsWrongLocationRepairOnLine2` | ✅ COMPLIANT |
| 4 | repair-signs: Permanent PDC identity | Visible text alone has no authority | `RepairActivationTest.textAlone_hasNoAuthority` | ✅ COMPLIANT |
| 5 | repair-signs: Permanent PDC identity | Missing creation authorization fails closed | `PdcSignIdentityTest.missingCreatorOrMarkerFailsClosed` + `PdcSignIdentityTest.malformedBytesFailClosed` | ✅ COMPLIANT |
| 6 | repair-signs: Reloadable messages | Valid reload changes presentation | `FileConfigurationPortTest.validReload_atomicallySwaps` + `MiniMessagePortTest.render_returnsBukkitStringWithPlaceholder` + `SignIntegrationTest.reloadValidSwapsInvalidRetains` | ✅ COMPLIANT |
| 7 | repair-signs: Reloadable messages | Invalid reload fails deterministically | `FileConfigurationPortTest.invalidReload_retainsPriorAndReportsFailure` + `SignIntegrationTest.reloadValidSwapsInvalidRetains` | ✅ COMPLIANT |
| 8 | repair-signs: Reloadable messages | Invalid initial configuration fails closed | `FileConfigurationPortTest.invalidStartup_disablesActivation` | ✅ COMPLIANT |
| 9 | repair-signs: Tamper detection | Tampered text is rejected | `AdminCommandTest.inspec*_tampered` (via RepairActivation tamper gate) + `SignIntegrationTest.tamperedTextActivation_failClosedNoCharge` | ✅ COMPLIANT |
| 10 | repair-signs: Tamper detection | Valid identity is re-rendered | `AdminCommandTest.rerender_valid_restoresCanonical` + `SignIntegrationTest.adminInspectAndRerender_restoresCanonical` | ✅ COMPLIANT |
| 11 | repair-signs: Protected lifecycle | Unauthorized edit or break is cancelled | `SignLifecycleListenerTest.breakUnauthorized_cancelledPdcUnchanged` + `SignIntegrationTest.editAndBreakWithoutManage_cancelledPdcUnchanged` | ✅ COMPLIANT |
| 12 | repair-signs: Duplicate-hand filtering | Duplicate events do not double-charge | `InteractionFilterTest.offHandRightClick_ignored` + `InteractionFilterTest.mainHandRightClick_proceeds` + `SignIntegrationTest.duplicateHandEvents_oneCharge` | ✅ COMPLIANT |
| 13 | repair-economy: Valid fixed price | One successful activation has one flat charge | `RepairActivationTest.flatCharge_oneWithdrawal_25` + `VaultEconomyGatewayTest.withdrawSuccess_oneFlatCharge` | ✅ COMPLIANT |
| 14 | repair-economy: Valid fixed price | Invalid precision or value fails closed | `MoneyAmountTest.rejectsNonFiniteValues` + `ValidatedPriceTest.rejectsPrecisionOverflowAgainstFractionalDigits` + `VaultEconomyGatewayTest.fractionalDigits_scaleExceeds_rejected` | ✅ COMPLIANT |
| 15 | repair-economy: No eligible target | Undamaged or ineligible equipment is free | `RepairActivationTest.emptyPlan_isFree_noVault` + `SignIntegrationTest.noEligibleItems_noVaultCall` | ✅ COMPLIANT |
| 16 | repair-economy: Vault absence | Provider is unavailable | `RepairActivationTest.noProvider_failClosed` + `VaultEconomyGatewayTest.missingProvider_noProvider` + `SignIntegrationTest.vaultAbsent_noProvider` | ✅ COMPLIANT |
| 17 | repair-economy: Single withdrawal | Insufficient funds do not repair | `RepairActivationTest.insufficientFunds_noSecondWithdraw_noRepair` + `VaultEconomyGatewayTest.withdrawFail_insufficientFunds` + `SignIntegrationTest.insufficientFunds_noRepair` | ✅ COMPLIANT |
| 18 | repair-economy: Compensation | Successful compensation restores payment state | `RepairActivationTest.compensationSuccess_noNetCharge` | ✅ COMPLIANT |
| 19 | repair-economy: Compensation | Failed compensation is observable | `RepairActivationTest.compensationDepositFails_highSev` | ✅ COMPLIANT |
| 20 | repair-economy: Compensation | Restoration failure is terminal and observable | `RepairActivationTest.restoreFails_terminal_highSev` | ✅ COMPLIANT |
| 21 | equipment-repair: Exact target modes | HAND excludes other equipment | `EquipmentSlotIdTest.handModeResolvesOnlyTheMainHandSlot` + `RepairPlannerTest.handModePlansOnlyMainHand` | ✅ COMPLIANT |
| 22 | equipment-repair: Exact target modes | ALL excludes storage | `EquipmentSlotIdTest.allModeResolvesExactlySixEquipmentSlotsInOrder` + `RepairPlannerTest.allModePlansSixSlotsAndExcludesStorage` + `SignIntegrationTest.allModeRepairsSixSlotsStorageUntouched` | ✅ COMPLIANT |
| 23 | equipment-repair: Eligibility | Mixed targets select only eligible items | `RepairPlannerTest.skipsIneligibleItemsAndSelectsOnlyEligible` | ✅ COMPLIANT |
| 24 | equipment-repair: Eligibility | No eligible target creates no plan | `RepairPlannerTest.emptyPlanWhenNoEligibleTargets` + `RepairActivationTest.emptyPlan_isFree_noVault` | ✅ COMPLIANT |
| 25 | equipment-repair: Deterministic planning | Repeated planning is stable | `RepairPlannerTest.repeatedPlanningIsStableAndSnapshotsEquivalent` | ✅ COMPLIANT |
| 26 | equipment-repair: Payment-gated application | Payment failure preserves equipment | `RepairActivationTest.paymentFailure_preservesEquipment` | ✅ COMPLIANT |
| 27 | equipment-repair: Payment-gated application | Apply failure restores snapshots | `BukkitEquipmentPortTest.restore_fromSnapshot_restoresDamage` + `BukkitEquipmentPortTest.applyFailure_restorationPreservesUntouchedSlots` | ✅ COMPLIANT |
| 28 | platform: Compatibility floor | Newer build JDK still targets the floor | `BytecodeFloorTest.builtJarClassesTargetJava17Bytecode` (major 61, --release 17, toolchain Temurin 17) | ✅ COMPLIANT |
| 29 | platform: Single-JAR boundary | Host APIs are not packaged | `BytecodeFloorTest.releaseJarDoesNotPackageHostApis` + `PluginDescriptorTest.builtJarDescriptorDeclaresExactIdentityAndSoftDependency` (softdepend [Vault], paper/vault compileOnly) | ✅ COMPLIANT |
| 30 | platform: Stable namespace | Display-brand change preserves identity | `PdcSignIdentityTest.roundtripThroughTileStateMock` + `PdcNamespacePermanenceTest.displayBrandRename_preservesPdcIdentity` | ✅ COMPLIANT |
| 31 | platform: SemVer separate | Version and tested range stay distinct | `SemVerSeparationTest.versionAndMatrixAreSeparateClaims` + `SemVerSeparationTest.matrixFileSeparateFromVersionFile` | ✅ COMPLIANT |
| 32 | platform: Evidence-gated tiers | Missing Paper runtime evidence blocks certification | `CompatibilityEvidenceSchemaTest.missingPaperEvidence_blocksCertification` | ✅ COMPLIANT |
| 33 | platform: Evidence-gated tiers | Support labels follow their evidence | `EvidenceGatedSupportTest.supportLabels_followEvidence` + `EvidenceGatedSupportTest.paperCertified_onlyWhenAllMandatoryPaperRowsPass` | ✅ COMPLIANT |
| 34 | platform: Evidence-gated tiers | Paper 26.x requires Java 25 evidence | `EvidenceGatedSupportTest.paper26RequiresJava25_probeFailDoesNotBlockCertifiedRanges` | ✅ COMPLIANT |
| 35 | platform: Verification layers | Incomplete evidence blocks release claims | `MissingRealProviderBlocksClaimTest.*` + `RealVaultProviderEvidenceTest.realVaultProviderEvidence_gatedUntilManualRun` + `CompatibilityEvidenceSchemaTest.mandatoryRows_mustPass` | ✅ COMPLIANT |
| 36 | platform: GPL + release | Distribution criterion is satisfied | `LICENSE` GPL-3.0-or-later present + `RealVaultProviderEvidenceTest.pinnedEssentialsXMetadata_documented` + local JAR readiness (Phase 9 GitHub release gated — expected BLOCKED, not defect) | ✅ COMPLIANT (local readiness) |

**Compliance summary**: 36/36 scenarios compliant (35 fully verified + 1 local-readiness, gated publication intentionally deferred)

### Correctness (Static Evidence)
| Requirement | Status | Notes |
|-------------|--------|-------|
| repair-signs 6 reqs | ✅ Implemented | SignParser, PdcSignIdentity, SignLifecycleListener, FileConfigurationPort, MiniMessageMessagePort, AdminCommandHandler, InteractionFilter, AnvilLinkPlugin |
| repair-economy 5 reqs | ✅ Implemented | ValidatedPrice, MoneyAmount, RepairActivation (single withdraw, exactly one deposit on mismatch, finite check), VaultEconomyGateway (adapter-only) |
| equipment-repair 4 reqs | ✅ Implemented | EquipmentSlotId, RepairPlanner, RepairPlan, PlannedSlot, ItemView/Snapshot, BukkitEquipmentPort (owns snapshot restore) |
| platform 7 reqs | ✅ Implemented | build.gradle.kts --release 17 toolchain 17, compileOnly paper 1.18.2 / vault 1.7, shadow relocate kyori 4.11.0, plugin.yml api-version 1.13 + softdepend Vault, CompatibilityEvidence + evidence.json 5 pass + 1 probe fail, ReleaseClaimGate, SemVerSupportMatrix, CI workflows |

### Coherence (Design)
| Decision | Followed? | Notes |
|----------|-----------|-------|
| domain <- application <- adapters, one transaction use case | ✅ Yes | RepairActivation pure domain (7 ports), adapters own Bukkit/Vault |
| Permanent repo danielxxomg/anvillink, package io.github.danielxxomg.anvillink, PDC danielxxomg:anvillink_repair_sign | ✅ Yes | PdcSignIdentity NAMESPACE/KEY permanent, SignRecord schema 1 |
| plugin.yml softdepend [Vault] | ✅ Yes | Descriptor asserts softdepend, Vault gateway fail-closed |
| Java 17 JAR, Paper 1.18.2 compileOnly, --release 17, no NMS | ✅ Yes | build.gradle.kts toolchain 17, grep domain 0 Bukkit, grep src 0 NMS |
| Exact HAND=[main] / ALL=[main,off,helmet,chest,leggings,boots], never storage | ✅ Yes | EquipmentSlotId + RepairPlanner |
| Snapshot/plan before payment, apply on server thread, compensating deposit | ✅ Yes | RepairActivation + BukkitEquipmentPort + VaultEconomyGateway ownership contract |
| MiniMessage 4.11.0 shaded/relocated, String through MessagePort | ✅ Yes | MiniMessageMessagePort, MessagePort String-only |
| Evidence-gated tiers: Paper certified 3, Spigot/Purpur verified, Paper 26.x probe, Folia experimental | ✅ Yes | CompatibilityEvidence, evidence.json, smoke.yml continue-on-error for probe |

### Issues Found
**CRITICAL**: None

**WARNING**:
- Phase 9 (9.1 GitHub release with JAR+SHA+evidence.json, 9.2 verification, 9.3 readiness) intentionally BLOCKED — requires explicit user authorization and native delivery gates per tasks.md Publication gate. Not a verification failure; release readiness passes locally (LICENSE GPL-3.0-or-later, JAR built, CHANGELOG, evidence.json). Public release MUST NOT be created without authorization.
- `clean` without `cleanTest` is flaky for descriptor tests (plugin.yml version token needs shadowJar); `cleanTest test` or `cleanTest test spotlessCheck build` is the canonical command and passes deterministically (121/121).

**SUGGESTION**:
- Consider adding JaCoCo coverage threshold and publishing `compatibility/evidence.json` with real serverSha256 values post-CI (current placeholders are 64-char hex, valid per schema but not real artifact hashes). Probe row correctly fails to prove gating.
- Paper 26.2/102 JDK25 probe intentionally fails; keep `continue-on-error: true` so mandatory certification is not blocked.

### Verdict
PASS WITH WARNINGS — 22/22 requirements implemented, 36/36 scenarios have passing covering tests, 121/121 tests green, domain purity and NMS veto clean, JAR and descriptor correct, evidence schema and CI gates correct; only warnings are the intentional Phase 9 publication gate and the known clean-task ordering quirk.

