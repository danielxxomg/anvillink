# Tasks: world-aware-pricing-and-audit

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated prod lines Slice 1 (pricing + floor) | ~280 |
| Estimated prod lines Slice 2 (audit) | ~150 |
| Aggregate prod lines | ~430 |
| Aggregate with tests/docs | ~850 |
| 400-line ideal risk | Medium (Slice 1 near limit) |
| 800-line hard gate | Not exceeded per slice (280 & 150 < 800) |
| Chained PRs recommended | Yes |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |
| Suggested split | PR 1 → PR 2 on tracker `feat/anvillink/slice-1-scaffold` |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Per-world pricing + floor `>=0` (transactional) | PR 1 `feat(pricing): world-aware price resolution` base=`feat/anvillink/slice-1-scaffold` | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew test --tests "*MoneyAmountTest*ValidatedPriceTest*FileConfigurationPortTest*RepairActivationTest"` | N/A — pure domain + file-temp; MockBukkit for activation withdraw path only, no server needed | `src/main/java/io/github/danielxxomg/anvillink/domain/MoneyAmount.java`, `domain/ValidatedPrice.java`, `domain/WorldPrice.java`, `domain/ports/ConfigurationPort.java`, `adapter/FileConfigurationPort.java`, `domain/RepairActivation.java`, `src/main/resources/config.yml` + pricing tests |
| 2 | Audit fixed append-only log (observability) | PR 2 `feat(audit): fixed audit.log for paid success` base=`PR 1 branch` | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew test --tests "*FileAuditAdapterTest*AuditE2ETest*WorldAwarePricingE2ETest"` | MockBukkit + temp `audit.log` (`Files.createTempDirectory`, `verify CREATE+APPEND`); no live Paper required — file I/O asserted | `src/main/java/io/github/danielxxomg/anvillink/domain/ports/AuditPort.java`, `adapter/FileAuditAdapter.java`, `entrypoint/AnvilLinkPlugin.java` (audit wiring only) + audit tests + `audit.log` creation |

## Slice 1 — Per-World Pricing (BREAKING: floor + worlds partial)

### Phase 1: Config & Domain floor relaxation

- [x] 1.1 RED `src/test/java/io/github/danielxxomg/anvillink/domain/MoneyAmountTest.java` — assert `0`, `100`, `12000` accepted, `-1`/`-5` rejected, `MIN_PRICE` absent, non-finite rejected, `representableAt` unchanged
- [x] 1.2 GREEN `src/main/java/io/github/danielxxomg/anvillink/domain/MoneyAmount.java` — remove `MIN_PRICE`, keep `signum<0` + `doubleValue` finite guard, `representableAt` untouched
- [x] 1.3 RED `src/test/java/io/github/danielxxomg/anvillink/domain/ValidatedPriceTest.java` — `0`/`100` with `fractionalDigits=2` passes, `100.001` fails, negative fails, null fails, `>=0` mirrors MoneyAmount
- [x] 1.4 GREEN `src/main/java/io/github/danielxxomg/anvillink/domain/ValidatedPrice.java` — replace `MIN_PRICE` check with `signum<0`/`>=0`, keep `representableAt` activation scale
- [x] 1.5 `src/main/java/io/github/danielxxomg/anvillink/domain/WorldPrice.java` — create `public record WorldPrice(BigDecimal hand, BigDecimal all)` pure domain (nullable per field for partial)
- [x] 1.6 `src/main/java/io/github/danielxxomg/anvillink/domain/ports/ConfigurationPort.java` — add `Map<String,WorldPrice> worldPrices` to `ConfigSnapshot`, `unmodifiableMap` copy, javadoc `>=0` + partial fallback
- [x] 1.7 RED `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortTest.java` (worlds) — hand-only `worlds.world.hand=5000` valid, all-only valid, both valid, empty `worlds:` valid, unknown subkey warns not invalid, negative/unparseable/non-finite per-world fails whole file, dup world last-wins warns, quoted `"world nether"` parses, missing subkey lenient (fallback)
- [x] 1.8 GREEN `src/main/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPort.java` — add `inWorlds`/`currentWorld` to line-scan, parse `worlds:` two-level indent, validate present `hand`/`all` via `new BigDecimal` + `signum<0` + `new MoneyAmount(v)` (`>=0`), unknown subkeys warn/ignore, dup last wins warn, build `HashMap` then `unmodifiableMap` before `AtomicReference` swap, whole-file `err("worlds.<name>.hand: <reason>")` retains prior
- [x] 1.9 `src/main/resources/config.yml` — add `worlds:` optional block with commented example (`world.hand: 5000` + `world_nether.all: 1000`), header `>=0` partial case-sensitive, defaults `price.hand/all` unchanged
- [x] 1.10 `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortReloadTest.java` — reload retains prior `worldPrices` on malformed per-world entry, success swaps atomically, no partial apply

### Phase 2: Transaction resolver (worldName seam)

- [x] 2.1 RED `src/test/java/io/github/danielxxomg/anvillink/domain/RepairActivationTest.java` (world-aware) — `world.get(world.hand)=5000` overrides HAND, `worldHandOnly` ALL falls back to global, unknown/case-mismatch/null/empty → global, `worldPrices.get` exact, missing null/empty never NPE
- [x] 2.2 GREEN `src/main/java/io/github/danielxxomg/anvillink/domain/RepairActivation.java` — add `activate(SignId,UUID,String worldName)` resolver `worldPrices.get(worldName)` else global, keep `activate(id,uuid)` overload delegating `worldName=null` for compat; no `org.bukkit.*`
- [x] 2.3 GREEN `src/main/java/io/github/danielxxomg/anvillink/domain/RepairActivation.java` — resolve `BigDecimal effective = (wp!=null ? (mode==HAND?wp.hand():wp.all()) : global)` with `null` per-field fallback to global before `ValidatedPrice.of(effective, fractionalDigits)`
- [x] 2.4 RED `src/test/java/io/github/danielxxomg/anvillink/domain/RepairActivationTest.java` (scale) — `worlds.world.hand=100.001` with `fractionalDigits=2` returns `InvalidResponse` no withdrawal, `world.hand=0` with non-empty plan requests withdrawal `0`, other mode valid not bypass
- [x] 2.5 GREEN `src/main/java/io/github/danielxxomg/anvillink/domain/RepairActivation.java` — `ValidatedPrice.of` per-activation on effective price, scale failure → `InvalidResponse("invalid-price:...")` fail-closed no withdrawal
- [x] 2.6 `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` (pricing seam) — capture `String worldName = player.getWorld().getName()` server-thread before `activation.activate(id, uuid, worldName)` in `onPlayerInteract`
- [x] 2.7 `src/test/java/io/github/danielxxomg/anvillink/adapter/WorldAwarePricingE2ETest.java` (MockBukkit) — HAND in `world` charges `5000` not `12000`, ALL in `world` charges global `25000`, nether ALL charges `1000`
- [x] 2.8 Verify Slice 1: `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck` green, domain `grep -r "org.bukkit\|net.minecraft\|Vault\|kyori" src/main/java/io/github/danielxxomg/anvillink/domain` empty

## Slice 2 — Audit Log (fixed append-only)

### Phase 3: Audit port/adapter + wiring

- [x] 3.1 `src/main/java/io/github/danielxxomg/anvillink/domain/ports/AuditPort.java` — create `interface AuditPort { record AuditEntry(Instant timestamp, UUID playerUuid, String playerName, RepairMode mode, String worldName, BigDecimal price, int repairedCount, String result) void record(AuditEntry e); }` pure domain
- [x] 3.2 RED `src/test/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapterTest.java` — single `Success(amount!=ZERO)` appends one line `ISO_INSTANT|uuid|name|HAND/ALL|world|toPlainString|count|SUCCESS`, `mkdirs()` creates parent, `toPlainString` not `1E3`, `IOException` swallowed, fixed path `audit.log` via `File` ctor
- [x] 3.3 GREEN `src/main/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapter.java` — `File auditFile` ctor (`new File(dataFolder,"audit.log")`), `record()` does `auditFile.getParentFile().mkdirs()` + `Files.writeString(path, line+"\n", CREATE, APPEND)` swallowed `IOException`, format `ISO_INSTANT` via `Instant.now()` + `|` fields + `toPlainString()`
- [x] 3.4 RED `src/test/java/io/github/danielxxomg/anvillink/entrypoint/AuditWiringTest.java` — `Success(ZERO)` and `InsufficientFunds`/`NoProvider`/`InvalidResponse` never call `AuditPort`, paid `Success` calls once after feedback, double-swallow outer try/catch, `worldName` from `player.getWorld().getName()` exact
- [x] 3.5 GREEN `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` — wire `FileAuditAdapter(new File(getDataFolder(),"audit.log"))`, capture `worldName`+`playerName` server-thread, call `audit.record(entry)` after `feedback.play` only when `s.amount()!=ZERO`, double-swallow `try{adapter.record} catch(Exception ignored){}` inside outer swallow
- [x] 3.6 RED `src/test/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapterSwallowTest.java` — `Files.writeString` throws → adapter swallows, caller swallows, transaction remains `Success`, no compensation retry
- [x] 3.7 GREEN `src/main/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapter.java` — ensure `record` never throws, handles missing parent, uses `CREATE|APPEND` not `TRUNCATE`
- [x] 3.8 `src/test/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapterFormatTest.java` — `1000` and `1000000` assert `toPlainString` in line, `HAND`/`ALL` literal, `repairedCount` exact, `SUCCESS` discriminator

### Phase 4: Integration / E2E + Docs / Quality (both slices)

- [x] 4.1 `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortIntegrationTest.java` — reload with `worlds.b.hand=-1` retains prior `worlds.a.hand=100`, valid partial `worlds.world.hand=5000` survives reload, unknown world still global after reload
- [x] 4.2 `src/test/java/io/github/danielxxomg/anvillink/e2e/AuditE2ETest.java` — MockBukkit paid HAND `world` `5000` count 1 audits one line, paid ALL `world_nether` audits, empty `Success(ZERO)` creates no file, `IOException` swallow keeps `Success`
- [x] 4.3 `src/test/java/io/github/danielxxomg/anvillink/e2e/WorldAwarePricingE2ETestFull.java` — full withdraw → apply → `Success(amount,count)` → feedback + audit order, per-world scale invalid `InvalidResponse` no withdraw
- [x] 4.4 Docs `src/main/resources/config.yml` header — document fixed `plugins/AnvilLink/audit.log` path, `CREATE+APPEND`+`mkdirs`, unbounded/manual rotation (rename/delete), privacy cleartext IDs + operator GDPR retention
- [x] 4.5 Quality `build.gradle.kts`/`gradle/libs.versions.toml` unchanged — verify `compileOnly` Paper/Vault not shaded, Adventure relocated `anvillink.libs.kyori` still excluded
- [x] 4.6 `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` — assert `major 61` via `BytecodeFloorTest`, `spotlessCheck` green, JaCoCo `jacocoTestReport` if available
- [x] 4.7 Domain purity gate `grep -R "org.bukkit\|net.minecraft\|org.bukkit.craftbukkit\|net.kyori\|com.github.MilkBowl\|ConfigurationSection" src/main/java/io/github/danielxxomg/anvillink/domain` → empty, `worldName` String only
- [x] 4.8 Rollback drill — verify PR 1 revert removes `WorldPrice`+`worldPrices`+`worldName` seam, global `>=0` floor restored via `MoneyAmount`; PR 2 revert deletes `AuditPort`/`FileAuditAdapter` + audit wiring, `audit.log` archived/deleted, no PDC migration

## Scenario Traceability Matrix

| Spec | Scenario | Tasks |
|------|----------|-------|
| repair-economy | HAND charges price.hand | 1.7, 2.2, 2.7, 4.3 |
| repair-economy | ALL charges price.all | 1.7, 2.2, 2.7, 4.3 |
| repair-economy | Flat scalar rejected | 1.7, 1.8, 4.1 |
| repair-economy | Missing per-mode price rejected | 1.7, 1.8, 4.1 |
| repair-economy | Below-floor rejected (now negative/non-finite) | 1.1, 1.2, 1.3, 1.4, 1.8 |
| repair-economy | Zero and low prices accepted when representable | 1.1, 1.2, 1.3, 1.4, 2.4, 2.5 |
| repair-economy | Per-mode invalid precision fails closed | 1.3, 1.4, 2.4, 2.5, 4.3 |
| repair-economy | World hand-only overrides hand, all falls back | 1.7, 1.8, 2.1, 2.2, 2.3, 2.7 |
| repair-economy | World all-only overrides all | 1.7, 1.8, 2.1, 2.2, 2.7 |
| repair-economy | Unknown world uses global both | 1.7, 2.1, 2.2, 4.1 |
| repair-economy | Case mismatch falls back to global | 1.7, 2.1, 2.2 |
| repair-economy | Null or empty worldName resolves to global | 2.1, 2.2, 3.4, 3.5 |
| repair-economy | Negative/unparseable per-world hand fails whole file closed | 1.7, 1.8, 1.10, 4.1 |
| repair-economy | Per-world invalid retains prior not other worlds | 1.10, 4.1 |
| repair-economy | World price scale invalid fails closed per-activation | 2.4, 2.5, 4.3 |
| repair-economy | Valid world price passes scale and withdraws | 2.4, 2.5, 2.7, 4.3 |
| audit-log | Paid HAND success audits one line correct fields | 3.1, 3.2, 3.3, 3.5, 4.2 |
| audit-log | Paid ALL success audits | 3.2, 3.3, 4.2 |
| audit-log | Zero and empty plan not audited | 3.4, 3.5, 4.2 |
| audit-log | Failures not audited | 3.4, 3.5, 4.2 |
| audit-log | Audit IOException swallowed transaction still Success | 3.2, 3.3, 3.6, 3.7, 4.2 |
| audit-log | Audit line uses toPlainString not scientific | 3.2, 3.3, 3.8 |

## Dependency Notes

- 1.1→1.2 → 1.3→1.4: floor `>=0` domain first, then parser may reuse `MoneyAmount` validation
- 1.5 `WorldPrice` before 1.6 `ConfigSnapshot.worldPrices` before 1.7/1.8 parser — record exists before map typed
- 1.6 → 1.8 → 1.10: snapshot shape before nested scan, scan before atomic retain-prior test
- 1.8 → 1.9: parser ready before `config.yml` example (docs match impl)
- Phase 1 green before Phase 2.1: `ValidatedPrice >=0` needed for resolver scale tests
- 2.2 resolver before 2.3 fallback null handling before 2.4 scale scenarios before 2.6 wiring — domain seam before entrypoint capture
- 3.1 `AuditPort` before 3.2/3.3 adapter before 3.4/3.5 wiring — pure contract first, then format/swallow, then double-swallow call site
- 2.6 `worldName` capture (Slice 1) before 3.5 audit needs same `worldName` — reuse string, no second `getWorld()` call
- Phase 4 after both slices: `4.1` reload atomics needs pricing done, `4.2` audit E2E needs adapter+wiring, `4.3` combined order needs both

## Verification Checklist

### Slice 1 — Pricing

- [x] `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test --tests "*MoneyAmountTest*ValidatedPriceTest*FileConfigurationPortTest*RepairActivationTest*WorldAwarePricingE2ETest"` all green
- [x] `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew spotlessCheck build` — `BytecodeFloorTest` major 61, no Bukkit in `domain/**`
- [x] Rollback: revert pricing files only, `audit.log` untouched, `worlds:` → unknown warn ignore
- [x] Coverage: `jacocoTestReport` pricing paths (global + per-world + fallback + scale)

### Slice 2 — Audit

- [x] `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test --tests "*FileAuditAdapterTest*Audit*E2ETest"` green, `audit.log` temp file has `CREATE|APPEND|mkdirs` + `toPlainString`
- [x] `spotlessCheck` + `build` green, fixed `plugins/AnvilLink/audit.log` only, no new deps, `shadowJar` exclusions hold
- [x] Rollback: remove `AuditPort`/`FileAuditAdapter` + wiring, delete/archive `audit.log`, pricing still works
- [x] Manual rotation: rename `audit.log` → next paid `Success` recreates, unbounded + GDPR note in config header
