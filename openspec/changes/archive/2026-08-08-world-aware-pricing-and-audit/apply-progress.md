# Apply Progress: world-aware-pricing-and-audit — Slice 1 + Slice 2

> Tracker `feat/anvillink/slice-1-scaffold` — feature-branch-chain auto-chain.
> Slice 1 PR 1: per-world pricing + floor >=0 (BREAKING). Slice 2 PR 2: fixed append-only audit.log (this slice).

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

## Completed Tasks Slice 2

### Phase 3: Audit port/adapter + wiring

- [x] 3.1 AuditPort pure `interface AuditPort { record AuditEntry(Instant timestamp, UUID playerUuid, String playerName, RepairMode mode, String worldName, BigDecimal price, int repairedCount, String result) void record(AuditEntry e); }`
- [x] 3.2 RED FileAuditAdapterTest — single Success(amount!=ZERO) appends one line ISO_INSTANT|uuid|name|HAND/ALL|world|toPlainString|count|SUCCESS, mkdirs creates parent, toPlainString not 1E3, IOException swallowed, fixed path audit.log via File ctor
- [x] 3.3 GREEN FileAuditAdapter — File auditFile ctor (new File(dataFolder,"audit.log")), record() does getParentFile().mkdirs() + Files.writeString(path, line+"\n", CREATE, APPEND) swallowed IOException, format ISO_INSTANT via Instant.now()
- [x] 3.4 RED AuditWiringTest — Success(ZERO) and InsufficientFunds/NoProvider/InvalidResponse never call AuditPort, paid Success calls once after feedback, double-swallow, worldName exact
- [x] 3.5 GREEN AnvilLinkPlugin — wire FileAuditAdapter(new File(getDataFolder(),"audit.log")), capture worldName+playerName server-thread, call audit.record(entry) after feedback.play only when s.amount()!=ZERO, double-swallow try{adapter.record} catch(Exception ignored)
- [x] 3.6 RED FileAuditAdapterSwallowTest — Files.writeString throws -> adapter swallows, caller swallows, transaction remains Success
- [x] 3.7 GREEN ensure record never throws, handles missing parent, uses CREATE|APPEND not TRUNCATE
- [x] 3.8 FormatTest — 1000 and 1000000 assert toPlainString in line

### Phase 4: Integration/E2E + Docs

- [x] 4.1 FileConfigurationPortIntegration — reload with worlds.b.hand=-1 retains prior worlds.a.hand=100
- [x] 4.2 AuditE2ETest MockBukkit — paid HAND world 5000 count1 audits one line, paid ALL world_nether, empty Success(ZERO) no file, IOException swallow
- [x] 4.3 WorldAwarePricingE2ETestFull — withdraw->apply->Success->feedback+audit order, per-world scale invalid InvalidResponse no withdraw
- [x] 4.4 Docs config.yml header — fixed audit.log path, CREATE+APPEND+mkdirs, unbounded/manual rotation, privacy
- [x] 4.5 Quality build unchanged — compileOnly Paper/Vault not shaded, Adventure relocated anvillink.libs.kyori still excluded
- [x] 4.6 clean test spotlessCheck build — major61 via BytecodeFloorTest, spotless green, JaCoCo coverageVerification
- [x] 4.7 Domain purity — grep empty domain Bukkit (org.bukkit/net.minecraft/net.kyori/MilkBowl/ConfigurationSection)
- [x] 4.8 Rollback drill — PR1 revert removes WorldPrice+worldPrices+worldName seam restores MIN_PRICE floor via MoneyAmount; PR2 revert deletes AuditPort/FileAuditAdapter+audit wiring, audit.log archived/deleted, no PDC migration

## Files Changed

| File | Action | What |
|------|--------|------|
| `src/main/java/io/github/danielxxomg/anvillink/domain/MoneyAmount.java` | Modified | Remove MIN_PRICE, keep signum<0+finite |
| `src/main/java/io/github/danielxxomg/anvillink/domain/ValidatedPrice.java` | Modified | signum<0 instead of MIN_PRICE |
| `src/main/java/io/github/danielxxomg/anvillink/domain/WorldPrice.java` | Created | Pure record hand,all nullable |
| `src/main/java/io/github/danielxxomg/anvillink/domain/ports/ConfigurationPort.java` | Modified | worldPrices added |
| `src/main/java/io/github/danielxxomg/anvillink/domain/ports/AuditPort.java` | Created | Pure AuditEntry+record |
| `src/main/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPort.java` | Modified | worlds: nested scan + validation |
| `src/main/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapter.java` | Created | fixed audit.log mkdirs CREATE+APPEND swallow |
| `src/main/java/io/github/danielxxomg/anvillink/domain/RepairActivation.java` | Modified | worldName seam + resolver |
| `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` | Modified | capture worldName+playerName, wire FileAuditAdapter, audit after feedback double-swallow |
| `src/main/resources/config.yml` | Modified | worlds: + audit fixed-path header |
| `src/test/java/io/github/danielxxomg/anvillink/domain/MoneyAmountTest.java` | Modified | >=0 suite |
| `src/test/java/io/github/danielxxomg/anvillink/domain/ValidatedPriceTest.java` | Modified | >=0 + 100.001 scale |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortTest.java` | Modified | negative not 9999 |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortWorldsTest.java` | Created | worlds RED suite |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapterTest.java` | Created | RED append/mkdirs/toPlainString/I IOException/fixed path |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapterSwallowTest.java` | Created | RED swallow no compensation |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapterFormatTest.java` | Created | 1000/1e5 toPlainString + HAND/ALL |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortIntegrationTest.java` | Created | retain prior a on bad b |
| `src/test/java/io/github/danielxxomg/anvillink/e2e/AuditE2ETest.java` | Created | MockBukkit paid/empty/swallow E2E |
| `src/test/java/io/github/danielxxomg/anvillink/e2e/WorldAwarePricingE2ETestFull.java` | Created | MockBukkit order + scale InvalidResponse no withdraw |
| `src/test/java/io/github/danielxxomg/anvillink/domain/RepairActivationWorldTest.java` | Created | world resolver + scale |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/WorldAwarePricingE2ETest.java` | Created | MockBukkit E2E pricing |
| `src/test/java/io/github/danielxxomg/anvillink/entrypoint/AuditWiringTest.java` | Created | wiring never-audit + double-swallow |
| `src/test/java/io/github/danielxxomg/anvillink/domain/RepairActivationTest.java` | Modified | StubCfg worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/AdminCommandTest.java` | Modified | ConfigSnapshot worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/BukkitFeedbackAdapterTest.java` | Modified | ConfigSnapshot worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/MiniMessagePortTest.java` | Modified | ConfigSnapshot worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/adapter/ErrorMessagesUnchangedTest.java` | Modified | ConfigSnapshot worldPrices |
| `src/test/java/io/github/danielxxomg/anvillink/integration/SignIntegrationTest.java` | Modified | ConfigSnapshot worldPrices + negative |

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew test --tests "*MoneyAmountTest*ValidatedPriceTest*FileConfigurationPortWorldsTest*RepairActivationWorldTest*WorldAwarePricingE2ETest"` PASS; `test --tests "*FileAuditAdapterTest*FileAuditAdapterSwallowTest*FileAuditAdapterFormatTest*FileConfigurationPortIntegrationTest*AuditE2ETest*WorldAwarePricingE2ETestFull*AuditWiringTest"` PASS |
| Full harness | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` PASS; jacocoTestReport + jacocoTestCoverageVerification PASS (domain 94%, bundle 74%/0.55); BytecodeFloorTest major 61 green; grep domain bukkit/kyori empty |
| Rollback boundary | Slice 1: 20 files above priced; Slice 2: AuditPort + FileAuditAdapter + entrypoint audit wiring + audit tests + e2e + config header — reverting slice 2 deletes those + audit.log, pricing still works |

## Verification

- `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` green (12 tasks)
- `grep -R "org.bukkit\|net.minecraft\|net.kyori\|MilkBowl" src/main/java/io/github/danielxxomg/anvillink/domain` empty
- bytecode major 61 via BytecodeFloorTest (shadowJar); host APIs not packaged
- `jacocoTestCoverageVerification` green; `shadowJar` Adventure relocate `anvillink.libs.kyori` still excluded

## Remaining

- None — both slices applied. Next: archive delta specs.

## Commits

- `feat(pricing): world-aware price resolution with floor >=0` on `feat/anvillink/slice-1-scaffold` (PR1)
- Slice 2 pending commit `feat(audit): fixed audit.log for paid success` (this slice)
