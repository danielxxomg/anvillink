# Design: Test Harness Hardening

## Technical Approach

Keep `0.3.1` and production/domain code unchanged. Use the existing JUnit 5, ShadowJar, and MockBukkit 4.110 substrate. Slice 1 tests the release JAR and shipped configuration; Slice 2 loads `AnvilLinkPlugin` and drives Bukkit events. CI promotes Paper 1.21.5-114/JDK 25 from probe to mandatory evidence.

## Current vs Target Harness

```text
Current: JUnit -> direct listener/hand-built ports -> MockBukkit
         synthetic/temp fixtures + weak JAR grep -> optional host probe

Target:  Slice 1: shadowJar -> JarFile/jar tf + FileConfigurationPort(File)
         Slice 2: MockBukkit.mock -> load(AnvilLinkPlugin) -> callEvent
                   SignChange -> fresh TileState/PDC+BLUE -> PlayerInteract
                   -> ValidatedPrice -> Vault -> equipment -> audit.log
         CI: Paper 1.21.5-114/J25 mandatory -> compatibility/evidence.json
```

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|---|---|---|---|
| Artifact fidelity | JUnit `ShadowRelocationContractTest` plus strict CI listing checks; retain explicit `adventure-legacy` and `minimize()`. | Host-only smoke or Paper Test Framework | Catches the 9f190b6 relocation/minimize regression before a server starts. |
| Lifecycle fidelity | `MockBukkit.load(AnvilLinkPlugin.class)` and `pluginManager.callEvent`; proxy-state fixture for fallback. | Direct listener tests or Docker now | Exercises bootstrap/classloader wiring while keeping the change fast; the MockBukkit snapshot ceiling remains explicit. |
| Isolation | Copy shipped config, inspect the temporary data folder, and register a Proxy Vault `Economy`. | Production injection or domain changes | Tests existing adapters without test-only production APIs. |

## Data Flow

```text
SignChangeEvent lines
  -> event.setLine canonical values
  -> fresh = block.getState()
  -> fresh TileState: write PDC, BLUE, canonical lines, update(true,false), wrote=true
  -> if (!wrote && state instanceof TileState): write PDC, BLUE, update(true,false)
  -> PlayerInteractEvent
  -> sign load/front-text validation -> RepairActivation
  -> economy.fractionalDigits -> ValidatedPrice -> withdraw
  -> equipment apply -> Success -> FileAuditAdapter(audit.log)
```

## File Changes

| File | Action | Description |
|---|---|---|
| `src/test/java/io/github/danielxxomg/anvillink/descriptor/ShadowRelocationContractTest.java` | Create | Assert relocated legacy classes, zero `^net/kyori`, no host APIs, major 61, and resolved descriptor. |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/ShippedConfigRoundTripTest.java` | Create | Copy `src/main/resources/config.yml` through real `File` I/O; assert prices, defaults, worlds, and quoted `#`. |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/MiniMessagePortTest.java` | Modify | Add serializer/parse-failure fallback smoke. |
| `src/test/java/io/github/danielxxomg/anvillink/e2e/PluginBootstrapTest.java` | Create | Bootstrap, event-bus, fresh/fallback, Vault, repair, and audit assertions. |
| `.github/workflows/build.yml`, `.github/workflows/smoke.yml`, `compatibility/evidence.json` | Modify | Make artifact checks fail closed and add mandatory Paper 1.21.5-114/J25 evidence; retain Paper 26.x/J25 as a probe. |

## Interfaces / Contracts

The contract uses existing APIs: `FileConfigurationPort(File)`, `PdcSignIdentity.key()`, `DyeColor.BLUE`, `MockBukkit.load(AnvilLinkPlugin.class)`, `VaultEconomyGateway`, and `FileAuditAdapter`. Vault matrix: `fractionalDigits=0` rejects the scaled price without withdrawal; `2` and `-1` withdraw successfully. Only non-zero successes append the eight-field ISO audit line.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit/artifact | Shadow, bytecode, descriptor, config, MiniMessage fallback | RED first; `test` depends on `shadowJar`; inspect `JarFile` and temporary files. |
| Integration | Bootstrap and sign lifecycle | `MockBukkit.mock()`/`load`, register permissions/economy, call `SignChangeEvent` then `PlayerInteractEvent`; parameterize Vault and assert PDC, BLUE, repair, and temp `audit.log`. Exercise fresh TileState and exact `if (!wrote && state instanceof TileState)` fallback. |
| Host/CI | Certified matrix and artifact shell gate | `continue-on-error: false` for Paper 1.21.5-114/J25; add its evidence row only with a truthful SHA256. |

Run the canonical gate with `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build jacocoTestCoverageVerification`. Use the requested positive `jar tf | grep -q libs/kyori/legacy`; implement negativity as `! grep -q '^net/kyori'` over the listing because `grep -q -v` does not mean “zero matches.”

## Threat Matrix

| Boundary | Applicability | Safe/failure behavior | Planned RED test |
|---|---|---|---|
| Documentation-like paths | N/A — no executable-file classification | No behavior change | None |
| Git repository selection | N/A — CI uses checked-out workspace directly | No alternate repository/cwd selection | None |
| Commit state | N/A — no commit automation | No index mutation | None |
| Push state | N/A — no push automation | No remote write | None |
| PR commands | N/A — no PR command composition | No PR side effect | None |
| Artifact shell (`jar tf`/`grep`) | Applicable | Read-only listing; missing legacy or any `^net/kyori` makes the job fail | Shadow contract RED case for missing/unrelocated entries and CI gate |

## Migration / Rollout

No migration or feature flag. Ship Slice 1 independently, then Slice 2. Promote the Paper 1.21.5-114 row after a real green smoke run; keep Paper 26.x/J25 non-certified. No domain changes.

## Open Questions

- [ ] Populate the new evidence row’s truthful server SHA256 from the first mandatory smoke execution.
