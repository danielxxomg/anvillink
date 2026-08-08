# Apply Progress: world-aware-pricing-and-audit — Slice 1 Per-World Pricing

> Slice 1 (auto-chain PR 1) on tracker `feat/anvillink/slice-1-scaffold` — feature-branch-chain.
> Scope: BREAKING floor >=10000 -> >=0 + optional worlds: partial overrides with global fallback.

## Completed Tasks Slice 1

### Phase 1: Config & Domain floor relaxation
- [x] 1.1 RED MoneyAmountTest — 0,100,12000 accepted, -1/-5 rejected, MIN_PRICE absent, non-finite rejected
- [x] 1.2 GREEN MoneyAmount — remove MIN_PRICE, keep signum<0 + finite guard, representableAt untouched
- [x] 1.3 RED ValidatedPriceTest — 0/100 passes, 100.001 fails, negative fails
- [x] 1.4 GREEN ValidatedPrice — replace MIN_PRICE with signum<0 check
- [x] 1.5 WorldPrice pure record `hand,all` nullable per-field
- [x] 1.6 ConfigurationPort — add Map<String,WorldPrice> worldPrices unmodifiableMap to ConfigSnapshot
- [x] 1.7 RED FileConfigurationPort worlds — hand-only/all-only/both/empty valid, unknown subkey warns, negative/unparseable/non-finite fails whole file, dup last-wins warns, quoted "world nether", missing subkey lenient
- [x] 1.8 GREEN FileConfigurationPort — inWorlds/currentWorld, two-level indent, validate present hand/all via BigDecimal+signum<0+MoneyAmount >=0, unknown subkeys warn, dup last wins, unmodifiableMap before AtomicReference swap, err("worlds.<name>.hand: <reason>") whole-file fail retains prior
- [x] 1.9 config.yml — add worlds: optional block with commented example (world.hand 5000 + world_nether.all 1000), header >=0 partial case-sensitive
- [x] 1.10 Reload retain prior test — covered in FileConfigurationPortWorldsTest (worldsHandReloadFailsWholeFileRetainsPrior + reloadRetainsWorldPricesOnMalformedPerWorld)

### Phase 2: Transaction resolver (worldName seam)
- [x] 2.1 RED RepairActivation world-aware — world.get(world.hand) overrides HAND, worldHandOnly ALL fallback global, unknown/case-mismatch/null/empty -> global
- [x] 2.2 GREEN RepairActivation — add activate(SignId,UUID,String worldName) resolver worldPrices.get(worldName) else global, keep overload activate(id,uuid) delegating worldName=null
- [x] 2.3 GREEN partial fallback null per-field -> global before ValidatedPrice
- [x] 2.4 RED scale — worlds.world.hand=100.001 fd2 InvalidResponse no withdrawal
- [x] 2.5 GREEN ValidatedPrice per activation effective price
- [x] 2.6 AnvilLinkPlugin — capture worldName = player.getWorld().getName() server-thread before activate(id, uuid, worldName)
- [x] 2.7 WorldAwarePricingE2ETest MockBukkit — HAND in world charges 5000 not 12000 etc
- [x] 2.8 Verify Slice 1 — clean test spotlessCheck green, domain purity empty

## Files Changed
| File | Action | What |
|------|--------|------|
| `src/main/java/io/github/danielxxomg/anvillink/domain/MoneyAmount.java` | Modified | Remove MIN_PRICE, keep signum<0+finite |
| `src/main/java/io/github/danielxxomg/anvillink/domain/ValidatedPrice.java` | Modified | signum<0 instead of MIN_PRICE |
| `src/main/java/io/github/danielxxomg/anvillink/domain/WorldPrice.java` | Created | Pure record hand,all nullable |
| `src/main/java/io/github/danielxxomg/anvillink/domain/ports/ConfigurationPort.java` | Modified | worldPrices added |
| `src/main/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPort.java` | Modified | worlds: nested scan + validation |
| `src/main/java/io/github/danielxxomg/anvillink/domain/RepairActivation.java` | Modified | worldName seam + resolver |
| `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` | Modified | capture worldName |
| `src/main/resources/config.yml` | Modified | worlds: optional commented example |
| `src/test/java/io/github/danielxxomg/anvillink/domain/MoneyAmountTest.java` | Modified | >=0 suite |
| `src/test/java/io/github/danielxxomg/anvillink/domain/ValidatedPriceTest.java` | Modified | >=0 + 100.001 scale |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortTest.java` | Modified | negative not 9999 |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortWorldsTest.java` | Created | worlds RED suite |
| `src/test/java/io/github/danielxxomg/anvillink/domain/RepairActivationWorldTest.java` | Created | world resolver + scale |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/WorldAwarePricingE2ETest.java` | Created | MockBukkit E2E |
| `src/test/java/io/github/danielxxomg/anvillink/domain/RepairActivationTest.java` | Modified | StubCfg worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/AdminCommandTest.java` | Modified | ConfigSnapshot worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/BukkitFeedbackAdapterTest.java` | Modified | ConfigSnapshot worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/MiniMessagePortTest.java` | Modified | ConfigSnapshot worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/ErrorMessagesUnchangedTest.java` | Modified | ConfigSnapshot worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/integration/SignIntegrationTest.java` | Modified | ConfigSnapshot worldPrices + negative |

## Work Unit Evidence
| Evidence | Value |
|---|---|
| Focused test command | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew test --tests "io.github.danielxxomg.anvillink.domain.MoneyAmountTest" --tests "io.github.danielxxomg.anvillink.domain.ValidatedPriceTest" --tests "io.github.danielxxomg.anvillink.adapter.FileConfigurationPortWorldsTest" --tests "io.github.danielxxomg.anvillink.domain.RepairActivationWorldTest" --tests "io.github.danielxxomg.anvillink.adapter.WorldAwarePricingE2ETest"` PASS (5 suites) |
| Full harness | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` PASS; jacocoTestReport + jacocoTestCoverageVerification PASS (domain 0.75, bundle 0.55); grep domain bukkit empty |
| Rollback boundary | The 20 files above; reverting removes WorldPrice+worldPrices+worldName seam, restores MIN_PRICE floor via MoneyAmount; worlds: -> unknown warn ignore; audit.log untouched |

## Verification
- `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` green
- `grep -R "org.bukkit\|net.minecraft" src/main/java/io/github/danielxxomg/anvillink/domain` empty
- bytecode major 61 via BytecodeFloorTest (shadowJar)

## Remaining
- Slice 2 audit (Phase 3/4) not in scope this PR

## Commit
- `feat(pricing): world-aware price resolution with floor >=0` on `feat/anvillink/slice-1-scaffold` (20 files, +956/-86)

