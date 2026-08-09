```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:90bc6a41f40ffe561a7ec19dfb951faa0f02be48ebe48fae4e324444d65435d1
verdict: pass
blockers: 0
critical_findings: 0
requirements: 3/3
scenarios: 10/10
test_command: 'GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build jacocoTestCoverageVerification'
test_exit_code: 0
test_output_hash: sha256:e4af66be136b6375fa13c7f5d7155f1ddcb967e620823202aef55a1e17c9491e
build_command: 'GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build jacocoTestCoverageVerification'
build_exit_code: 0
build_output_hash: sha256:e4af66be136b6375fa13c7f5d7155f1ddcb967e620823202aef55a1e17c9491e
```

## Verification Report

**Change**: `test-harness-hardening`
**Version**: `0.3.1`
**Mode**: Standard
**Branch**: `feat/anvillink/slice-1-scaffold`

### Completeness

| Metric | Value |
|---|---:|
| Tasks total | 33 |
| Tasks complete | 33 |
| Tasks incomplete | 0 |

Native SDD status was `verify: ready`; proposal, specs, design, tasks, and apply-progress were all present. The two landed slices are complete.

### Build & Tests Execution

**Canonical command (single execution)**:

```text
GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build jacocoTestCoverageVerification
```

**Build**: ✅ Passed — `BUILD SUCCESSFUL in 5s`; 13 actionable tasks executed.

**Tests**: ✅ 245 passed / ❌ 0 failed / ⚠️ 0 skipped across 43 test suites. The change-focused suites passed: `ShadowRelocationContractTest` 5/5, `ShippedConfigRoundTripTest` 4/4, `MiniMessagePortTest` 4/4, and `PluginBootstrapTest` 15/15.

The single command emitted 61 Java deprecation/removal warnings and Gradle's deprecated-feature warning, but no test or build failure. `spotlessCheck` and `jacocoTestCoverageVerification` also passed. Coverage thresholds configured by the build (75% domain package and 55% bundle instruction coverage) were verified; exact percentages were not emitted because this command does not run `jacocoTestReport`.

The output preimage for the single execution is bound as `sha256:e4af66be136b6375fa13c7f5d7155f1ddcb967e620823202aef55a1e17c9491e` for both test and build fields because the requested gate was one combined invocation.

### Artifact Inspection

`build/libs/anvillink-0.3.1.jar` is 569,602 bytes. `mise x java@21.0.2 -- jar tf build/libs/anvillink-0.3.1.jar` listed 413 entries, including 318 relocated Kyori classes and 14 entries in the legacy serializer package. `LegacyComponentSerializer.class` is present; unrelocated `net/kyori`, `org/bukkit`, and `net/minecraft/server` entries are all zero. The passing descriptor contract also confirmed resolved version `0.3.1`, `api-version: 1.13`, and the permanent plugin main class.

### Spec Compliance Matrix

| Requirement | Scenario | Passing test / evidence | Result |
|---|---|---|---|
| Released-JAR relocation completeness | Legacy serializer survives `minimize()` and relocation | `ShadowRelocationContractTest > relocatedLegacySerializerPresentAndMinimizeDidNotStrip` | ✅ COMPLIANT |
| Released-JAR relocation completeness | No unrelocated Adventure remains | `ShadowRelocationContractTest > zeroUnrelocatedNetKyoriRemains` | ✅ COMPLIANT |
| Released-JAR relocation completeness | Host APIs not packaged and bytecode floor holds | `ShadowRelocationContractTest > noHostApisPackaged` + `prodBytecodeIsMajor61ExcludingLibs`; `BytecodeFloorTest` | ✅ COMPLIANT |
| Shipped-config File I/O parse | Shipped file with inline comments parses via `File` | `ShippedConfigRoundTripTest > shippedConfigViaRealFileParsesInlineCommentsAndDefaults` | ✅ COMPLIANT |
| Shipped-config File I/O parse | Quoted hash preserved, unquoted hash stripped | `ShippedConfigRoundTripTest > quotedHashPreserved_unquotedHashStripped` + `singleQuotedHashPreserved` | ✅ COMPLIANT |
| Shipped-config File I/O parse | Synthetic-string-only coverage is insufficient | The shipped-resource test copies `src/main/resources/config.yml` with `Files.writeString` to `@TempDir`, then loads `new FileConfigurationPort(File)`; no synthetic-only gate is relied upon | ✅ COMPLIANT |
| MockBukkit-load bootstrap end-to-end | `SignChangeEvent` through plugin classloader persists PDC identity and BLUE | `PluginBootstrapTest > signChangeViaCallEvent_freshTileStateWritesPdcAndBlue` | ✅ COMPLIANT |
| MockBukkit-load bootstrap end-to-end | Stale-vs-fresh `TileState` fallback is exercised | `PluginBootstrapTest > signChange_fallbackWhenFreshNotTileState_writesToOriginalState` | ✅ COMPLIANT |
| MockBukkit-load bootstrap end-to-end | `PlayerInteract` covers Vault 0/2/-1 and append-only audit | `PluginBootstrapTest > playerInteract_fractionalDigitsMatrix_zeroNoWithdraw_twoAndMinusOneWithdraw` + `playerInteract_worldNameSeamAndAuditAppend` | ✅ COMPLIANT |
| MockBukkit-load bootstrap end-to-end | Paper 1.21.5-114 evidence gate exists and is mandatory in CI | `CompatibilityEvidenceSchemaTest > evidenceJson_hasRequiredSchemaPerRow` + `mandatoryRows_mustPass_probeMayFailWithoutBlockingCertified`; `.github/workflows/smoke.yml` row is `continue-on-error: false` | ✅ COMPLIANT |

**Compliance summary**: 10/10 scenarios compliant with passing runtime tests or the required static CI/evidence checks.

Additional proposal acceptance passed: `MiniMessageMessagePort.java` catches `Throwable`, and `MiniMessagePortTest` passed its functional/source contract checks.

### Correctness (Static Evidence)

| Requirement | Status | Notes |
|---|---|---|
| Relocation and release artifact contract | ✅ Implemented | `build.gradle.kts` explicitly keeps `implementation(libs.adventure.legacy)`, `minimize()`, and one `relocate("net.kyori", "io.github.danielxxomg.anvillink.libs.kyori")`; the assembled JAR matches the contract. |
| Shipped configuration and throwable fallback | ✅ Implemented | `FileConfigurationPort.stripInlineComment` is quote-aware for single/double quotes; `MiniMessageMessagePort` catches `Throwable` and returns the substituted raw string on failure. |
| Plugin bootstrap and sign lifecycle | ✅ Implemented | `onEnable` wires `FileConfigurationPort` from the plugin data folder; `SignLifecycleListener` writes fresh `TileState` first and retains the original-state fallback; the event-bus path, Vault matrix, tamper fail-closed behavior, and audit file passed. |
| CI compatibility evidence | ✅ Implemented with residual evidence risk | Paper 1.21.5 build 114 is present as a non-optional smoke row and a passing evidence row, but its SHA is intentionally a placeholder pending a real host run. |
| Domain boundary | ✅ Implemented | No forbidden Bukkit/Paper/Vault/Adventure imports were found under `src/main/java/.../domain`. |

### Coherence (Design)

| Decision | Followed? | Notes |
|---|---|---|
| Artifact fidelity through ShadowJar plus JarFile assertions | ✅ Yes | The release JAR is inspected after `shadowJar`; relocation and host-API exclusions are green. |
| Lifecycle fidelity through `MockBukkit.load` and `callEvent` | ✅ Yes, with a bounded caveat | Fresh-state, bootstrap, and interaction paths use `MockBukkit.load`/`callEvent`; the isolated fallback test uses the planned proxy-state fixture and invokes the listener directly to force the stale-vs-fresh branch. |
| Isolation through shipped files, temp data, and a Vault proxy | ✅ Yes | No test-only production API was added. |
| Keep version `0.3.1` and preserve the production bytecode floor | ✅ Yes | Version remains `0.3.1`; production classes in the release JAR are major 61. |
| Production/domain code unchanged | ⚠️ Intentional deviation | `AnvilLinkPlugin` was made non-final so MockBukkit 4.110 ByteBuddy can subclass it for `MockBukkit.load`; apply-progress documents this as a reversible harness-enabling change. |

### Verification Authority

`gentle-ai review mode status` reported `receipt-driven development: off (decided by clone_local)` with global unset and clone-local off. `gentle-ai review status` reported a clean authority inventory with no entries. This report therefore makes no receipt, approval, or publication claim and records an ordinary Standard verification.

### Issues Found

**CRITICAL**: None.

**WARNING**:

1. `compatibility/evidence.json` uses the documented `6666...` placeholder for Paper 1.21.5 build 114; the row is structurally present and marked `pass`, but it is not a truthful server SHA until the mandatory host smoke runs.
2. The delta spec text mentions Paper 1.21.5-114/JDK 25 in its baseline/evidence wording, while the proposal, design, workflow, and evidence row use JDK 21. The repository's Paper 1.21.5 job is mandatory, but the SDD artifacts should be reconciled before archive.
3. `CompatibilityEvidence.paperCertified/allMandatoryPass` still programmatically requires only Paper 1.18.2, 1.20.6, and 1.21.11; it does not include the new 1.21.5-114 row. The workflow gate is mandatory, but the in-repo evidence-matrix gate is not fully promoted.
4. The stale-vs-fresh fallback test uses the intentional proxy-state fixture and direct listener invocation, not `pluginManager.callEvent`; the real classloader/event-bus path is covered for the fresh-state and interaction cases. This is the documented MockBukkit lifecycle ceiling, not a production failure.
5. `MiniMessagePortTest` verifies the `catch (Throwable)` source contract and normal fallback-shaped output, but does not inject a real serializer `NoSuchMethodError`/`Error`; the actual relocation contract and source catch are independently green.
6. The successful build still emits 61 deprecated/removal API warnings and Gradle 9-incompatibility warnings. They are non-blocking for this change.

**SUGGESTION**:

1. Replace the Paper 1.21.5-114 SHA placeholder after the real mandatory smoke run and bind the resulting evidence to the support claim.
2. Reconcile the JDK 21/JDK 25 wording and extend `CompatibilityEvidence` if the 1.21.5 row is intended to be a programmatic mandatory certification input.
3. If stronger lifecycle proof is required, route the proxy fallback through the MockBukkit event manager or add a Paper Test Framework follow-up; do not treat the proxy alone as proof of Paper copy-on-write behavior.

### Verdict

**PASS WITH WARNINGS**

All 33 tasks are complete, the single requested full Gradle gate is green, the assembled JAR satisfies the relocation/host/bytecode contract, and all 10 GWT scenarios have passing runtime or required static covering evidence. The warnings are residual evidence truthfulness, documentation/matrix enforcement, and MockBukkit fidelity concerns; they do not block this harness-only verification.
