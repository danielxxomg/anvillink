# Apply Progress — price-per-mode-and-feedback (Slice 1 + Slice 2)

Slice 1 — Pricing: Transactional Core — COMPLETE
Slice 2 — Feedback: Presentation — COMPLETE

## Completed Tasks (Slice 1)
- [x] 1.1 ConfigurationPort.ConfigSnapshot — replaced single price with priceHand/priceAll + feedback fields, Javadoc
- [x] 1.2 RED MoneyAmountTest — failing floor tests added (10000 passes, 9999.99/5000/-1 rejected)
- [x] 1.3 GREEN MoneyAmount — MIN_PRICE=10000 enforcement in compact constructor
- [x] 1.4 GREEN ValidatedPrice — mirror floor after MoneyAmount.of
- [x] 1.5 config.yml BREAKING — price.hand 12000.00 / price.all 25000.00 + feedback block + repair-success
- [x] 1.6 RED FileConfigurationPortTest — bare scalar rejected, missing hand/all, below-floor, empty price all fail-closed
- [x] 1.7 GREEN FileConfigurationPort — nested scan under price: header, scalar rejected, both required, MIN_PRICE enforced, feedback parsing with defaults, atomic swap only on success
- [x] 2.1 TransactionResult.Success — Success(BigDecimal amount, int repairedCount) with validation
- [x] 2.2 RED TransactionResultTest — Success carries amount+count, zero, equality, null/negative rejected
- [x] 2.3 RepairActivation — mode selector priceHand/priceAll before ValidatedPrice, per-mode precision fail-closed, Success(ZERO,0) on empty, repairedCount=planned.size() on apply
- [x] 2.4 RED RepairActivationTest — HAND withdraws price.hand, ALL withdraws price.all, hand precision overflow fail-closed, cross-mode not polluted, empty zero, 3-slot count, single withdrawal
- [x] 2.5 AnvilLinkPlugin minimal — verified no price() call sites remain, success branch intact (feedback deferred to Slice 2)
- [x] 2.6 GREEN Phase 2 REDs pass — domain purity verified (0 Bukkit/Vault/Adventure imports in domain/**)
- [x] 2.7 Verify — GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build GREEN; bytecode major 61; PDC unchanged

## Completed Tasks (Slice 2)
- [x] 3.1 FeedbackPort — pure `void play(SignPort.PlayerId, BigDecimal, int)` no Bukkit/Adventure/Vault imports
- [x] 3.2 RED BukkitFeedbackAdapterTest — failing: disabled no-op, enabled renders count=4 price=20000 via toPlainString, throw swallowed, SchedulerPort dispatch
- [x] 3.3 BukkitFeedbackAdapter — ctor (ConfigurationPort, MessagePort, SchedulerPort, Function<UUID,Player>); play checks feedbackEnabled early return else runOnServerThread -> render repair-success {count}/{price} via toPlainString, string-based sound (BLOCK_ANVIL_USE) + particle (CRIT) safe defaults, swallowed
- [x] 3.4 AnvilLinkPlugin wiring — `FeedbackPort feedback = new BukkitFeedbackAdapter(configPort, messagePort, scheduler, Bukkit::getPlayer)`; after Success s gate `if (s.amount().compareTo(ZERO)!=0) try { feedback.play(PlayerId, amount, repairedCount) } catch(Exception ignored) {}`; empty Success and failures never trigger
- [x] 3.5 config.yml feedback ensure — `feedback: enabled/sound/particles` + `messages.repair-success` already present from Slice 1, verified
- [x] 3.6 RED FileConfigurationPortTest feedback — failing: enabled=false snapshot, missing block defaults to enabled, repair-success absent still succeeds, invalid reload retains prior feedback together with prices (no partial apply), valid reload swaps feedback atomically
- [x] 3.7 GREEN — 3.2/3.6 pass, domain purity `grep -R "org.bukkit\|net.milkbowl\|net.kyori" src/main/java/.../domain` => 0 hits (adapter allowed Bukkit)
- [x] 4.1 FileConfigurationPortTest reload atomics — valid->scalar/missing/below-floor => ReloadOutcome.Failure retains prior (both prices + feedback unchanged); valid reload swaps atomically
- [x] 4.2 RepairActivationTest per-mode precision parametric — fractionalDigits=2 hand 10000.001 fails closed, all pollution check still passes (covered in Slice 1 2.4 + E2E)
- [x] 4.3 E2E FeedbackE2ETest (MockBukkit 4.110) — paid HAND one render {count}==1 {price}==hand.toPlainString via gateway amount, paid ALL 3 slots count 3, zero no render, disabled silent, throw swallowed still Success no deposit, single withdrawal preserved
- [x] 4.4 E2E PricePerModeE2ETest (MockBukkit) — scalar file startup activationEnabled false, reload retains prior, hand/all distinct withdrawals, empty no charge, valid reload swaps prices
- [x] 4.5 spotlessApply then spotlessCheck PASS (Google Java Format 1.17 + license headers)
- [x] 4.6 clean test build — shadowJar 524K relocated anvillink.libs.kyori, major 61, plugin.yml api-version 1.13, PDC namespace unchanged
- [x] 4.7 Docs — config header already accurate; README/CHANGELOG updated noting BREAKING price.hand/price.all >=10_000 + feedback + repair-success {count}/{price} toPlainString MAJOR rationale

## Test Evidence (Slice 1)
- Focused: GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew cleanTest test
  Result: BUILD SUCCESSFUL, 141 tests PASSED, 0 FAILED (includes 6 new FileConfigurationPortTest scenarios, 4 MoneyAmount floor, 3 ValidatedPrice floor, 4 TransactionResult count, 7 RepairActivation per-mode)
- Full: GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build
  Result: BUILD SUCCESSFUL in 4s, spotlessCheck PASS, shadowJar 521K, plugin.yml api-version 1.13 preserved
- Domain purity: grep -R "org.bukkit\|net.milkbowl\|net.kyori" src/main/java/.../domain => 0 hits
- Bytecode: javap major version 61 (Java 17) confirmed

## Test Evidence (Slice 2)
- Focused Slice 2: GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew cleanTest test --rerun-tasks
  Result: BUILD SUCCESSFUL — BukkitFeedbackAdapterTest 4 PASSED, FileConfigurationPortTest 17 PASSED (5 new feedback scenarios), FeedbackE2ETest 6 PASSED, PricePerModeE2ETest 5 PASSED, RepairActivationTest 15 PASSED, TransactionResultTest 9 PASSED, BytecodeFloor 3 PASSED, PluginDescriptor 3 PASSED, SignIntegration 13 PASSED, platform 8 PASSED
- Full: GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build
  Result: BUILD SUCCESSFUL in 4s, spotlessCheck PASS, shadowJar 524K, plugin.yml api-version 1.13 preserved
- Domain purity: grep -R "org.bukkit\|net.milkbowl\|net.kyori" src/main/java/.../domain => 0 hits (Bukkit only in adapter/entrypoint)
- Bytecode: javap major 61 (Java 17) on anvilink JAR; kyori relocated to io.github.danielxxomg.anvillink.libs.kyori
- Sound/Particle compat: BukkitFeedbackAdapter uses string-based playSound + particleOf loop to avoid Paper 1.18.2 enum vs 1.21.x interface break (verified green on MockBukkit 4.110 runtime)

## Work Unit Evidence (Slice 1)
| Evidence | Value |
|---|---|
| Focused test command | GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew cleanTest test => 141 PASSED |
| Runtime harness | N/A — pure domain + parser; no server boundary (MockBukkit integration still green: 13 SignIntegration tests PASSED) |
| Rollback boundary | Revert src/main/resources/config.yml, src/main/java/.../domain/ports/ConfigurationPort.java, src/main/java/.../domain/MoneyAmount.java, src/main/java/.../domain/ValidatedPrice.java, src/main/java/.../domain/TransactionResult.java, src/main/java/.../domain/RepairActivation.java, src/main/java/.../adapter/FileConfigurationPort.java — no feedback files touched |

## Work Unit Evidence (Slice 2)
| Evidence | Value |
|---|---|
| Focused test command | GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew test --tests "io.github.danielxxomg.anvillink.adapter.BukkitFeedbackAdapterTest" --tests "io.github.danielxxomg.anvillink.adapter.FileConfigurationPortTest" --tests "io.github.danielxxomg.anvillink.integration.FeedbackE2ETest" --tests "io.github.danielxxomg.anvillink.integration.PricePerModeE2ETest" => BUILD SUCCESSFUL (4+17+6+5 PASSED in those suites; full suite green) |
| Runtime harness | MockBukkit 4.110 (Paper 1.21.11 runtime, prod --release 17 stays 61): FeedbackE2ETest — paid HAND → one repair-success {count}==1 {price}==hand.toPlainString, paid ALL 3 slots → {count}==3, zero → no render, disabled → silent, throw → swallowed still Success no deposit; PricePerModeE2ETest — scalar startup activationEnabled false, reload retains prior, hand/all distinct withdrawals (10000/20000), empty no charge |
| Rollback boundary | Revert src/main/java/.../domain/ports/FeedbackPort.java, src/main/java/.../adapter/BukkitFeedbackAdapter.java, src/main/java/.../entrypoint/AnvilLinkPlugin.java (wiring only), src/test/java/.../adapter/BukkitFeedbackAdapterTest.java, src/test/java/.../integration/FeedbackE2ETest.java, src/test/java/.../integration/PricePerModeE2ETest.java + README/CHANGELOG feedback note — Slice 1 pricing intact |

## Risks / Next
- Slice 1 + Slice 2 rollback-safe and slice-isolated per feature-branch-chain: tracker branch feat/anvillink/slice-1-scaffold accumulates both; Slice 2 revert leaves pricing intact.
- Next: sdd-verify then sdd-archive; tag MAJOR bump rationale documented in CHANGELOG.
- Aggregate diff prod ~355 lines (Slice1 ~210 + Slice2 ~145) within 800 hard gate; each slice ≤400 ideal budget.
