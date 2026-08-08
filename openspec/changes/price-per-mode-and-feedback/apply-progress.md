# Apply Progress — price-per-mode-and-feedback (Slice 1)

Slice 1 — Pricing: Transactional Core — COMPLETE

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

## Slice 2 — Feedback: NOT STARTED
- [ ] 3.1 FeedbackPort
- [ ] 3.2 BukkitFeedbackAdapterTest RED
- [ ] 3.3 BukkitFeedbackAdapter
- [ ] 3.4 AnvilLinkPlugin wiring gated amount!=ZERO
- [ ] 3.5 config.yml feedback ensure
- [ ] 3.6 FileConfigurationPortTest feedback RED
- [ ] 3.7 GREEN domain purity
- [ ] 4.1-4.7 integration/E2E/quality/docs

## Test Evidence (Slice 1)
- Focused: GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew cleanTest test
  Result: BUILD SUCCESSFUL, 141 tests PASSED, 0 FAILED (includes 6 new FileConfigurationPortTest scenarios, 4 MoneyAmount floor, 3 ValidatedPrice floor, 4 TransactionResult count, 7 RepairActivation per-mode)
- Full: GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build
  Result: BUILD SUCCESSFUL in 4s, spotlessCheck PASS, shadowJar 521K, plugin.yml api-version 1.13 preserved
- Domain purity: grep -R "org.bukkit\|net.milkbowl\|net.kyori" src/main/java/.../domain => 0 hits
- Bytecode: javap major version 61 (Java 17) confirmed

## Work Unit Evidence
| Evidence | Value |
|---|---|
| Focused test command | GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew cleanTest test => 141 PASSED |
| Runtime harness | N/A — pure domain + parser; no server boundary (MockBukkit integration still green: 13 SignIntegration tests PASSED) |
| Rollback boundary | Revert src/main/resources/config.yml, src/main/java/.../domain/ports/ConfigurationPort.java, src/main/java/.../domain/MoneyAmount.java, src/main/java/.../domain/ValidatedPrice.java, src/main/java/.../domain/TransactionResult.java, src/main/java/.../domain/RepairActivation.java, src/main/java/.../adapter/FileConfigurationPort.java — no feedback files touched |

## Risks / Next
- Slice 1 is rollback-safe (pricing only, feedback absent).
- Next: Slice 2 apply may start (FeedbackPort -> BukkitFeedbackAdapter -> wiring). No branch change needed — stays on feat/anvillink/slice-1-scaffold.
- Diff size this slice: ~500 insertions / 85 deletions (prod+tests) — within tracker PR budget; Slice 2 will be separate PR targeting Slice 1 branch per feature-branch-chain.
