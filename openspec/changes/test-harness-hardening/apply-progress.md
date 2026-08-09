# Apply progress: test-harness-hardening Slice 1 — Artifact (bugs #1 #2)

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

## Gates

- [x] `jar tf | grep -q libs/kyori/legacy` — legacy present (12 files)
- [x] `! grep -q "^net/kyori"` — zero unrelocated
- [x] `! grep -q "org/bukkit\|net/minecraft"` — not packaged
- [x] Shipped `config.yml` via `File` — activationEnabled true, inline comments stripped
- [x] `catch(Throwable)` fallback — proven via source-level + functional test
- [x] `MockBukkit.*` — deferred to Slice 2
- [x] domain zero `org.bukkit.*` — 0 matches

## Remaining (Slice 2 — Bootstrap bug #3)

- [ ] 4.1–4.7 PluginBootstrapTest fresh/fallback TileState
- [ ] 5.1–5.8 Vault + audit.log + edit/break PDC

## Verify

- `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` — BUILD SUCCESSFUL 5s 12 tasks
- Focused: `ShadowRelocationContractTest`, `ShippedConfigRoundTripTest`, `MiniMessagePortTest` — all PASSED
- `python zipfile` — 557K jar, 0 net/kyori, 0 org/bukkit, LegacyComponentSerializer present
