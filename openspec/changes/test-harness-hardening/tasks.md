# Tasks: test-harness-hardening

## Forecast

| Slice | Lines | Risk |
|-------|-------|------|
| 1 artifact | ~180 | Low |
| 2 bootstrap | ~220 | Low |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

| Unit | Goal | PR | Test |
|------|------|----|------|
| 1 | Artifact | PR1 `feat/anvillink/slice-1-scaffold` | `mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build && jar tf build/libs/anvillink-*.jar | grep -q libs/kyori/legacy && ! jar tf ... | grep -q "^net/kyori"` |
| 2 | Bootstrap | PR2 on PR1 | `mise x java@21.0.2 -- ./gradlew clean test --tests "*PluginBootstrapTest*" && ./gradlew clean test spotlessCheck build` |

Gate `mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build`. Harness U1 JarFile/File I/O; U2 `MockBukkit.mock()+MockBukkit.load(AnvilLinkPlugin.class)`+`callEvent`+`audit.log`.

## Slice 1 — Artifact (bugs #1 #2)

### Fase 1: Shadow

- [x] 1.1 RED `ShadowRelocationContractTest` `libs/kyori/legacy/LegacyComponentSerializer` present (9 classes)
- [x] 1.2 GREEN `build.gradle.kts` `relocate(net.kyori→...libs.kyori)`+`implementation(legacy)` keeps `minimize()`
- [x] 1.3 RED same JAR `! grep -q "^net/kyori"` zero unrelocated
- [x] 1.4 RED same JAR no `org/bukkit`/`net/minecraft`, major61, `plugin.yml` `${version}`+`1.13`
- [x] 1.5 GREEN `build.yml` `grep -q libs/kyori/legacy || exit 1`+`! grep -q "^net/kyori" && exit 1`
- [x] 1.6 Verify gate+`jar tf ... | grep legacy`

### Fase 2: Shipped-config

- [x] 2.1 RED `ShippedConfigRoundTripTest` `@TempDir File` copies `src/main/resources/config.yml`→`FileConfigurationPort(File)`; synthetic fails
- [x] 2.2 GREEN `activationEnabled==true`, `priceHand==12000.00`, `priceAll==25000.00`, `targetDistance==8`, `worlds:` absent
- [x] 2.3 RED `stripInlineComment` `"a # b"` kept, `12000.00 # comment` stripped
- [x] 2.4 GREEN `FileConfigurationPort` quote-aware `'`/`"` before `#`
- [x] 2.5 RED `MiniMessagePortTest` `NoSuchMethodError`→`catch(Throwable)` raw `withPlaceholders`
- [x] 2.6 GREEN `MiniMessageMessagePort` `catch(Throwable)` not `Exception`

### Fase 3: CI

- [x] 3.1 `smoke.yml` Paper `1.21.5-114/JDK21` `continue-on-error:false` keep `26.x/J25` probe
- [x] 3.2 `evidence.json` add `Paper 1.21.5-114`/`jdkMajor25` `pass` SHA256 deferred
- [x] 3.3 Verify gate+`grep -r "org.bukkit" domain` empty

## Slice 2 — Bootstrap (bug #3)

### Fase 4: PluginBootstrapTest

- [ ] 4.1 RED `MockBukkit.mock()+MockBukkit.load(AnvilLinkPlugin.class)`→`getDataFolder()` exists
- [ ] 4.2 GREEN `onEnable` wires `FileConfigurationPort(new File(getDataFolder(),"config.yml"))`; PDC `danielxxomg:anvillink_repair_sign`
- [ ] 4.3 RED `callEvent(SignChangeEvent(...,"[repair]","HAND"))`→`block.getState()` PDC+`BLUE`+`update(true,false)`
- [ ] 4.4 GREEN `fresh=block.getState()` after `setLine`→`TileState` `wrote=true`
- [ ] 4.5 RED fallback `if (!wrote && state instanceof TileState)` `fresh` not TileState via `state`
- [ ] 4.6 GREEN proxy-state `update(true,false)` fallback
- [ ] 4.7 RED both non-TileState→no PDC

### Fase 5: Vault+audit

- [ ] 5.1 RED `PlayerInteractEvent` via `callEvent` `Proxy` Vault `fractionalDigits` 0/2/-1
- [ ] 5.2 GREEN `0` no withdraw; `2`/`-1` withdraw
- [ ] 5.3 RED tampered text→`InvalidResponse` no charge/audit
- [ ] 5.4 RED only `Success(non-zero)`→`ISO_INSTANT|uuid|name|HAND|world|toPlainString|count|SUCCESS` to temp `audit.log` (`CREATE|APPEND`,`mkdirs()`)
- [ ] 5.5 GREEN `FileAuditAdapter` swallows `IOException`, `toPlainString`
- [ ] 5.6 `OffHand` single, `InsufficientFunds`/`NoProvider`/`NoEligibleItems` via `callEvent` no Vault
- [ ] 5.7 edit/break without `manage`→cancelled, PDC unchanged
- [ ] 5.8 Verify `PluginBootstrapTest` gate+`jar tf ... | grep -q libs/kyori/legacy`

## Trace

| Spec | Scenario | Tasks |
|------|----------|-------|
| platform | Legacy minimize | 1.1,1.2,1.6 |
| platform | No unrelocated | 1.3,1.5 |
| platform | Shipped File I/O | 2.1,2.2 |
| platform | Quoted hash | 2.3,2.4 |
| platform | SignChange via load | 4.1,4.3,4.4 |
| platform | Fallback `!wrote && instanceof` | 4.5-4.7 |
| platform | Vault 0/2/-1+audit | 5.1,5.2,5.4,5.5 |
| platform | Evidence 1.21.5-114 | 3.1,3.2 |

## Gates

- [ ] `jar tf | grep -q libs/kyori/legacy`+`! grep -q "^net/kyori"`+`! grep -q "org/bukkit\|net/minecraft"`
- [ ] Shipped `config.yml` via `File`+`catch(Throwable)` fallback
- [ ] `MockBukkit.load(AnvilLinkPlugin.class)`+`callEvent` both TileState+Vault 0/2/-1+temp `audit.log`
