# Proposal: test-harness-hardening

> **Non-BREAKING harness** — no behavior/server/config change. Stays `0.3.1`; bump `0.3.2` only if Slice 2 asserts a new mandatory gate (deferred to tasks). Hardens the harness so the three v0.3.0 gens (Paper 1.21.5-114 / Java 25) regressions fail in CI before they fail on a host.

## Intent
Collapse the gap that let three v0.3.0 regressions slip: tests ran on a different classpath, synthetic-file I/O, and stale-`TileState` doubles — none matched the shipped artifact or Paper 1.21.5 host lifecycle. Root pattern: **synthetic unit contract ≠ shipped artifact contract ≠ host classpath ≠ host world lifecycle**. Make `clean build` fail on gens bugs before any server runs (#1 config, #2 shadow), then prove the plugin-bootstrap + sign-coalescing path (#3) through the real classloader.

## Target users
- **Maintainers** shipping a release JAR that enforces its own shadow/relocation + config-parse contracts on every build.
- **Contributors** needing a Docker-free single-`ubuntu-latest` TDD loop that stays honest about where MockBukkit `TileState` fidelity ends and a real-Paper fallback begins.

## Current gap
- `FileConfigurationPortTest` synthetic-only — never fed shipped `config.yml` via real `File` I/O; `stripInlineComment` (2be2df1) unverified against shipped defaults until regression.
- No `jar tf` assertion that relocated `libs/kyori/legacy` survives `minimize()` and zero unrelocated `net/kyori` remains — the 9f190b6 fix (533K→557K, 9 legacy classes) is unprotected.
- No `MockBukkit.load(AnvilLink)` — `SignIntegrationTest` calls `listener.onSignChange` directly; PDC `danielxxomg:anvillink_repair_sign`, `onEnable` wiring, and post-event coalescing never exercised end-to-end.
- `FakeSign.copy()==this` hid the stale-`TileState` bug; MockBukkit `WorldMock`/`SignMock` don't model Paper 1.21.5 copy-on-write.
- Paper 1.21.5-114 is a `continue-on-error:true` probe, not a mandatory `evidence.json` row.

## Outcome
- `clean build` enforces shadow/relocation + shipped-config contracts as artifact gates (no host Paper, no Docker).
- A plugin-bootstrap `MockBukkit.load` path proves PDC + BLUE + `ValidatedPrice` + `Vault` withdraw + `audit.log` survive `SignChangeEvent`→`PlayerInteract` through the real classloader — fresh-`TileState` path and non-`TileState` fallback included.
- Paper 1.21.5-114 is a mandatory evidence row; the MockBukkit `TileState` ceiling is documented as residual risk with explicit fallback to Paper Test Framework / Docker.

## Scope
### In Scope
- **Slice 1:** `ShadowRelocationContractTest` (release JAR `jar tf` — `libs/kyori/legacy` present, 0 unrelocated `net/kyori`, no `org/bukkit`/`net/minecraft`, prod bytecode major 61), `ShippedConfigRoundTripTest` (`FileConfigurationPort(File)` on shipped `config.yml` via real File I/O, not synthetic), `MiniMessageMessagePort` `Throwable`-fallback smoke, Paper 1.21.5-114 promoted to mandatory `smoke.yml` + `evidence.json` row.
- **Slice 2:** `PluginBootstrapTest` via `MockBukkit.mock()` + `MockBukkit.load(AnvilLink.class)` + `pluginManager.callEvent(SignChangeEvent)→PlayerInteract` end-to-end — PDC namespace, BLUE, `ValidatedPrice`, `Vault` mock matrix (`fractionalDigits` 0/2/-1), `audit.log` temp, fresh-`TileState` + non-`TileState` fallback.
- TDD red-first per `test-driven-development` skill.

### Out of Scope
- Paper Test Framework (`paper-test` BootstrapContext) — follow-up if Slice 2 leaves a measured blind spot.
- Docker / `rcon` smoke — stays an evidence gate (Slice 1 promotion), not a TDD tier.
- Any `repair-economy`/`repair-signs` requirement delta; no new domain capability.

### Adjacent follow-ups
- Paper Test Framework proposal if residual drift after Slice 2.
- `@Tag("smoke")` split only if a non-mock host tier lands.
- Truthful `evidence.json` SHA256, blocked on a real Paper 1.21.5-114 smoke run.

## Capabilities
### New Capabilities
- None.
### Modified Capabilities
- `platform-compatibility`: harness guarantees — released JAR MUST carry relocated `libs/kyori/legacy` and zero unrelocated `net/kyori`; shipped `config.yml` MUST parse via real File I/O on every `processResources` change; `MockBukkit.load` MUST exercise `SignChangeEvent`→`PlayerInteract` through the real classloader. Test-harness requirements only; no domain spec delta.

## Approach
**Baseline: Approach A (MockBukkit-only + artifact contract)** — lowest-cost path catching #1 and #2 as pure artifact gates (no host Paper), and narrowing #3 via `MockBukkit.load` plugin-bootstrap, explicitly carrying the residual `TileState`-fidelity risk rather than hiding it. Paper Test Framework (B) is a follow-up if Slice 2 drifts; Docker smoke (C) stays an evidence gate via Slice 1 promotion. Two independently-shippable slices respect the 4R/1200-line budget; Slice 1 covers both packaging/config regressions with zero Docker.

## Affected Areas
| Area | Impact | Description |
|------|--------|-------------|
| `build.gradle.kts` / `smoke.yml` | Modified | `Verify JAR` greps `libs/kyori/legacy` + fails on unrelocated `net/kyori`; mandatory Paper 1.21.5-114 row |
| `compatibility/evidence.json` | Modified | Add mandatory Paper 1.21.5-114 / JDK21 row (SHA256 deferred to real smoke) |
| `src/test/.../ShadowRelocationContractTest.java` | New | Release JAR artifact contract |
| `src/test/.../ShippedConfigRoundTripTest.java` | New | Real File I/O on shipped `config.yml` |
| `src/test/.../PluginBootstrapTest.java` | New (Slice 2) | `MockBukkit.load(AnvilLink)` end-to-end |
| `openspec/specs/platform-compatibility/spec.md` | Modified | Harness-guarantee requirement deltas |

## Compatibility / Support-tier policy
- **Platform:** unchanged — Paper 1.18.2 `compileOnly` floor, Java 17 bytecode (major 61, `BytecodeFloorTest`), Adventure 4.11.0 relocated `net.kyori → anvillink.libs.kyori`, `api-version 1.13`, public APIs only; no shaded Paper/Vault.
- **Versioning:** non-BREAKING; stays `0.3.1`. Bump `0.3.2` only if a new mandatory gate is asserted (deferred to tasks).
- **Support tier:** Paper-certified adds Paper 1.21.5-114 (JDK21) mandatory smoke row; Paper 26.x/J25 remains non-certified probe. Spigot/Purpur verified-only, Folia experimental — unchanged.

## Risks
| Risk | Likelihood | Mitigation |
|------|------------|------------|
| MockBukkit `TileState` fidelity ceiling — `SignMock.copy()==this` still hides stale-vs-fresh | High | Keep fresh + non-`TileState` fallback paths; document residual; promote Paper Test Framework follow-up if drift measured |
| `minimize()` re-strips `adventure-legacy` on future tuning | Med | Slice 1 mandatory `jar tf` gate + pin `adventure-legacy` as explicit `implementation` |
| Dual Paper API classpath duplicates `org.bukkit` | Low | Assert `testRuntimeClasspath` has no split `paper-api` |
| `evidence.json` SHA256 placeholders block truthful 1.21.5-114 row | High | Defer truthful SHA256 to real smoke; mark row shape mandatory, values populated on first green |
| Scope creep into domain spec | Low | No `repair-economy`/`repair-signs` delta; no new domain fields |

## Rollback Plan
Non-BREAKING — revert the three new test classes, `build.gradle.kts` `Verify JAR` grep, `smoke.yml` Paper 1.21.5-114 row, and `evidence.json` row. No shipped artifact, `plugin.yml`, or `config.yml` change to undo. Slices 1 and 2 are independently revertible.

## Dependencies
- `paper-api-test 1.21.11` + MockBukkit 4.110.0 (existing) — no new host runtime.
- `shadowJar`/`processResources` artifacts existing — tests inspect the assembled JAR.
- No new Gradle plugins, no Docker, no Paper Test Framework in this change.

## Success Criteria
- [ ] `ShippedConfigRoundTripTest` parses shipped `config.yml` via `FileConfigurationPort(File)` real File I/O — `activationEnabled==true`, `priceHand=12000.00`, `priceAll=25000.00`, `worlds` commented-out, inline comments stripped, quoted `#` preserved.
- [ ] `ShadowRelocationContractTest` — release JAR: `libs/kyori/legacy` present, 0 unrelocated `net/kyori`, no `org/bukkit`/`net/minecraft`, prod bytecode major 61, `plugin.yml` `${version}` expanded + `api-version 1.13`.
- [ ] `MiniMessageMessagePort` `catch (Throwable)` fallback returns raw `withPlaceholders` on simulated `LegacyComponentSerializer` absence.
- [ ] Paper 1.21.5-114 / JDK21 mandatory `smoke.yml` row (`continue-on-error:false`) + `evidence.json` row (SHA256 deferred).
- [ ] `PluginBootstrapTest` (Slice 2) `MockBukkit.load(AnvilLink)` — `getDataFolder` exists, PDC `danielxxomg:anvillink_repair_sign`, BLUE, `ValidatedPrice`, `Vault` mock matrix (0/2/-1), `audit.log` temp, fresh + fallback `TileState` paths.
- [ ] `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` green; `jar tf | grep libs/kyori/legacy` hits; domain zero `org.bukkit.*`.
