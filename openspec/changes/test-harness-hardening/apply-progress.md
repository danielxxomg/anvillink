# Apply progress: test-harness-hardening Slice 1+2 — Artifact + Bootstrap (bugs #1 #2 #3)

## Completed Slice 1 tasks

### Fase 1: Shadow

- [x] 1.1 RED `ShadowRelocationContractTest` `libs/kyori/legacy/LegacyComponentSerializer` present (9 classes) — `relocatedLegacySerializerPresentAndMinimizeDidNotStrip` via JarFile, fails if minimize() stripped legacy.
- [x] 1.2 GREEN `build.gradle.kts` `relocate(net.kyori→...libs.kyori)`+`implementation(legacy)` keeps `minimize()` — already correct (9f190b6), now gated.
- [x] 1.3 RED same JAR `! grep -q "^net/kyori"` zero unrelocated — `zeroUnrelocatedNetKyoriRemains` via JarFile.
- [x] 1.4 RED same JAR no `org/bukkit`/`net/minecraft`, major61, `plugin.yml` `${version}`+`1.13` — `noHostApisPackaged`, `prodBytecodeIsMajor61ExcludingLibs`, `pluginYmlVersionResolvedAndApiVersion113`.
- [x] 1.5 GREEN `build.yml` `grep -q libs/kyori/legacy || exit 1`+`! grep -q "^net/kyori" && exit 1` — Verify JAR now fails closed on legacy absence + unrelocated net/kyori + host APIs.
- [x] 1.6 Verify gate+`jar tf ... | grep legacy` — `clean test spotlessCheck build` green; JarFile gate passes; 557K jar ships 318 relocated kyori classes.

### Fase 2: Shipped-config

- [x] 2.1 RED `ShippedConfigRoundTripTest` `@TempDir File` copies `src/main/resources/config.yml`→`FileConfigurationPort(File)`; synthetic fails — `shippedConfigViaRealFileParsesInlineCommentsAndDefaults` via real File I/O.
- [x] 2.2 GREEN `activationEnabled==true`, `priceHand==12000.00`, `priceAll==25000.00`, `targetDistance==8`, `worlds:` absent — asserted against shipped file.
- [x] 2.3 RED `stripInlineComment` `"a # b"` kept, `12000.00 # comment` stripped — `quotedHashPreserved_unquotedHashStripped` + `singleQuotedHashPreserved`.
- [x] 2.4 GREEN `FileConfigurationPort` quote-aware `'`/`"` before `#` — already correct (2be2df1), now gated with shipped-file + synthetic cases.
- [x] 2.5 RED `MiniMessagePortTest` `NoSuchMethodError`→`catch(Throwable)` raw `withPlaceholders` — `render_fallbackOnThrowable_returnsRawWithPlaceholders` asserts Throwable catch + functional fallback.
- [x] 2.6 GREEN `MiniMessageMessagePort` `catch(Throwable)` not `Exception` — source already catches Throwable, gated by source-level assertion in test.

### Fase 3: CI

- [x] 3.1 `smoke.yml` Paper `1.21.5-114/JDK21` `continue-on-error:false` keep `26.x/J25` probe — added Paper 1.21.5 build 114 JDK21 mandatory row before 26.x probe.
- [x] 3.2 `evidence.json` add `Paper 1.21.5-114`/`jdkMajor25` `pass` SHA256 deferred — added Paper 1.21.5/114/JDK21 pass row (SHA256 deferred to real smoke, 666… placeholder).
- [x] 3.3 Verify gate+`grep -r "org.bukkit" domain` empty — 0 domain violations; gate green.

## Completed Slice 2 tasks — Bootstrap (bug #3)

### Fase 4: PluginBootstrapTest

- [x] 4.1 RED `MockBukkit.mock()+MockBukkit.load(AnvilLinkPlugin.class)`→`getDataFolder()` exists — `bootstrap_loadCreatesDataFolderAndConfig` proves `onEnable saveDefaultConfig` creates `getDataFolder()/config.yml`, PDC namespace `danielxxomg:anvillink_repair_sign` permanent; `AnvilLinkPlugin` de-finalized for MockBukkit proxy (ByteBuddy cannot subclass final).
- [x] 4.2 GREEN `onEnable` wires `FileConfigurationPort(new File(getDataFolder(),"config.yml"))`; PDC `danielxxomg:anvillink_repair_sign` — covered by 4.1 bootstrap; shipped `config.yml` via `getDataFolder()` carries inline comments and parses activationEnabled true.
- [x] 4.3 RED `callEvent(SignChangeEvent(...,"[repair]","HAND"))`→`block.getState()` PDC+`BLUE`+`update(true,false)` — `signChangeViaCallEvent_freshTileStateWritesPdcAndBlue` via `server.getPluginManager().callEvent` through real classloader; asserts `TileState` has PDC, `Sign.getColor()==BLUE`, canonical `getLine(0)=="[repair]"` / `getLine(1)=="HAND"` survive coalescing.
- [x] 4.4 GREEN `fresh=block.getState()` after `setLine`→`TileState` `wrote=true` — fresh-state path proven by 4.3; `allModeWritesPdcAndBlue` covers ALL variant.
- [x] 4.5 RED fallback `if (!wrote && state instanceof TileState)` `fresh` not TileState via `state` — `signChange_fallbackWhenFreshNotTileState_writesToOriginalState` via `Proxy Block` where first `getState()` is TileState, second is non-TileState; asserts fallback writes PDC+BLUE and `update(true,false)`.
- [x] 4.6 GREEN proxy-state `update(true,false)` fallback — proven by 4.5 `stale.updated` assertion.
- [x] 4.7 RED both non-TileState→no PDC — `signChange_bothNonTileState_noPdc` where both `getState()` are non-TileState; no TileState exists to hold PDC (listener writes nowhere).

### Fase 5: Vault+audit

- [x] 5.1 RED `PlayerInteractEvent` via `callEvent` `Proxy` Vault `fractionalDigits` 0/2/-1 — `playerInteract_fractionalDigitsMatrix_zeroNoWithdraw_twoAndMinusOneWithdraw` parameterizes fd 0/2/-1 via `Proxy Economy`; `fd=0` rejects scale 2 `ValidatedPrice` before withdraw, `fd=2`/`-1` withdraw `12000.00`.
- [x] 5.2 GREEN `0` no withdraw; `2`/`-1` withdraw — proven by 5.1 matrix assertions on `withdrawals` list, repaired `Damageable`, and audit existence.
- [x] 5.3 RED tampered text→`InvalidResponse` no charge/audit — `playerInteract_tamperedTextFailClosedNoChargeNoAudit` via `callEvent` after tampering `Sign.setLine("[tampered]")`; asserts `withdrawals.isEmpty()`, no audit file, no repair.
- [x] 5.4 RED only `Success(non-zero)`→`ISO_INSTANT|uuid|name|HAND|world|toPlainString|count|SUCCESS` to temp `audit.log` (`CREATE|APPEND`,`mkdirs()`) — proven by fractionalDigits success audit assertions (8 fields, `Instant.parse`, `toPlainString` no `E`, `count`, `SUCCESS`, `worldName` exact) and `playerInteract_worldNameSeamAndAuditAppend` second success appends; `auditAdapter_toPlainStringAndMkdirs` proves `mkdirs()` and `CREATE|APPEND`.
- [x] 5.5 GREEN `FileAuditAdapter` swallows `IOException`, `toPlainString` — `FileAuditAdapter` double-swallow proven by `dirAsFile` swallow, caller double-swallow already in `AnvilLinkPlugin.onPlayerInteract`; `toPlainString` via `BigDecimal("12000.00")` trailing zeros preserved.
- [x] 5.6 `OffHand` single, `InsufficientFunds`/`NoProvider`/`NoEligibleItems` via `callEvent` no Vault — `playerInteract_offHandFilteredNoCharge` via `EquipmentSlot.OFF_HAND` filtered by `InteractionFilter`; `InsufficientFunds` via `FAILURE Insufficient funds`, `NoProvider` via absent `Economy`, `NoEligibleItems` via undamaged sword gated zero (all via `callEvent`).
- [x] 5.7 edit/break without `manage`→cancelled, PDC unchanged — `editAndBreakWithoutManage_viaCallEvent_cancelledPdcUnchanged` via `callEvent(SignChangeEvent)` + `callEvent(BlockBreakEvent)` with intruder lacking `manage`; asserts cancelled and PDC remains.
- [x] 5.8 Verify `PluginBootstrapTest` gate+`jar tf ... | grep -q libs/kyori/legacy` — `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test --tests "*PluginBootstrapTest*" && ./gradlew clean test spotlessCheck build` both green; `jar tf | grep -q anvillink/libs/kyori/adventure/text/serializer/legacy/LegacyComponentSerializer` hits; `! grep "^net/kyori"` and `! grep "^org/bukkit"` still green.

## Production change

- `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` — `public final class` → `public class` (non-final) to allow MockBukkit 4.110 `MockBukkit.load(AnvilLinkPlugin.class)` ByteBuddy proxy. No behavioral change; `final` was not a contract, MockBukkit must subclass the plugin type. Reversible.

## Gates

- [x] `jar tf | grep -q anvillink/libs/kyori/adventure/text/serializer/legacy/LegacyComponentSerializer` — legacy present
- [x] `! grep -q "^net/kyori"` — zero unrelocated
- [x] `! grep -q "org/bukkit\|net/minecraft"` — not packaged
- [x] Shipped `config.yml` via `File` — activationEnabled true, inline comments stripped
- [x] `catch(Throwable)` fallback — proven via source-level + functional test
- [x] `MockBukkit.load(AnvilLinkPlugin.class)`+`callEvent` both TileState+Vault 0/2/-1+temp `audit.log` — all 15 PluginBootstrapTest PASSED
- [x] domain zero `org.bukkit.*` — 0 matches
- [x] `clean test spotlessCheck build` — BUILD SUCCESSFUL

## Verify

- `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test --tests "*PluginBootstrapTest*"` — 15 PASSED, 0 FAILED
- `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` — BUILD SUCCESSFUL 5s 12 tasks
- `jar tf build/libs/anvillink-0.3.1.jar | grep anvillink/libs/kyori | wc -l` — 318 classes, LegacyComponentSerializer present
- `jar tf ... | grep "^net/kyori"` — 0
- `jar tf ... | grep "^org/bukkit"` — 0

## Remaining

- None — Slice 2 Bootstrap done. Ready for verify. No `repair-economy`/`repair-signs` spec delta (harness-only).
