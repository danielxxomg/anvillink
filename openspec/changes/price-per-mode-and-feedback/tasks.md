# Tasks: price-per-mode-and-feedback

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated prod changed lines Slice 1 (Pricing) | ~210 prod (config + domain floor + selector + Success count) |
| Estimated prod changed lines Slice 2 (Feedback) | ~145 prod (FeedbackPort + adapter + wiring + message) |
| Estimated aggregate prod (both slices) | ~355 prod |
| Estimated authored incl. tests+docs (both slices) | ~680–750 |
| 800-line hard gate (preflight) | NOT exceeded per slice; aggregate 355 < 800 |
| 400-line ideal budget risk | Low |
| Chained PRs recommended | Yes (transactional vs presentation must stay sliceable) |
| Suggested split | Slice 1 → Slice 2 (feature-branch-chain) |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Low

Auto-chain + feature-branch-chain is preflight-approved: tracker branch `feat/anvillink/slice-1-scaffold` accumulates slices; Slice 1 PR targets tracker; Slice 2 PR targets Slice 1 branch so child diff stays focused. Each slice independently `clean test spotlessCheck build` green and rollback-safe.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Pricing transactional core: mandatory `price.hand`/`price.all` (floor 10_000), `ConfigSnapshot(priceHand,priceAll,…)`, `MoneyAmount.MIN_PRICE` + `ValidatedPrice` floor, per-mode precision at activation, `TransactionResult.Success(amount,repairedCount)`, `RepairActivation` mode selector | PR 1 → `feat/anvillink/slice-1-scaffold` | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew test --tests "*MoneyAmountTest*ValidatedPriceTest*FileConfigurationPortTest*RepairActivationTest*TransactionResultTest"` | N/A — pure domain + parser; no server (MockBukkit not required for this unit) | Revert `src/main/resources/config.yml`, `src/main/java/io/github/danielxxomg/anvillink/domain/ports/ConfigurationPort.java`, `src/main/java/io/github/danielxxomg/anvillink/domain/MoneyAmount.java`, `src/main/java/io/github/danielxxomg/anvillink/domain/ValidatedPrice.java`, `src/main/java/io/github/danielxxomg/anvillink/domain/TransactionResult.java`, `src/main/java/io/github/danielxxomg/anvillink/domain/RepairActivation.java`, `src/main/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPort.java` — no feedback files touched |
| 2 | Feedback presentation: `FeedbackPort` pure port, `BukkitFeedbackAdapter` (server-thread, swallowed), `AnvilLinkPlugin` wiring gated on `amount != ZERO`, `messages.repair-success` with `{count}`/`{price}` via `toPlainString`, E2E isolation | PR 2 → Slice 1 branch | `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew test --tests "*Feedback*AnvilLinkPlugin*FileConfigurationPortTest*RepairActivationTest"` + `spotlessCheck` | MockBukkit 4.110 E2E: Paper 1.18.2 — place HAND+ALL signs, right-click each, verify sound/particles/message once on paid success, silence on zero/disabled/throw | Revert `src/main/java/io/github/danielxxomg/anvillink/domain/ports/FeedbackPort.java`, `src/main/java/io/github/danielxxomg/anvillink/adapter/BukkitFeedbackAdapter.java`, `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` + `messages.repair-success` + `feedback:` block in config — Slice 1 pricing logic unchanged |

> Slice isolation: Slice 1 delivers all pricing semantics with no feedback code; Slice 2 adds presentation only and never touches withdrawal/compensation. Rollback of Slice 2 leaves Slice 1 pricing intact.

---

## Slice 1 — Pricing: Transactional Core

### Phase 1: Config & Domain Floor — Snapshot, Validation, Parser

> Dependency: `ConfigurationPort.ConfigSnapshot` shape (1.1) before parser (1.5); `MoneyAmount.MIN_PRICE` (1.3) before `ValidatedPrice` floor (1.4) before parser floor check.

- [x] 1.1 Modify `src/main/java/io/github/danielxxomg/anvillink/domain/ports/ConfigurationPort.java` — replace `ConfigSnapshot(BigDecimal price, int targetDistance, Map messages, boolean activationEnabled)` with `ConfigSnapshot(BigDecimal priceHand, BigDecimal priceAll, int targetDistance, Map<String,String> messages, boolean activationEnabled, boolean feedbackEnabled, String feedbackSound, String feedbackParticles)`; document `priceHand`/`priceAll` mandatory, `feedback*` global defaults
- [x] 1.2 RED (TDD) `src/test/java/io/github/danielxxomg/anvillink/domain/MoneyAmountTest.java` — add failing tests: `of("10000")` passes, `of("9999.99")`/`of("5000")`/`of("-1")` rejected, `of(null)` rejected, `representableAt` unchanged; proves floor 10_000 not yet enforced
- [x] 1.3 GREEN `src/main/java/io/github/danielxxomg/anvillink/domain/MoneyAmount.java` — add `public static final BigDecimal MIN_PRICE = new BigDecimal("10000")`; in compact constructor after finite/non-negative checks, reject `value.compareTo(MIN_PRICE) < 0`; keep `representableAt(int)` unchanged
- [x] 1.4 GREEN `src/main/java/io/github/danielxxomg/anvillink/domain/ValidatedPrice.java` — mirror floor: after `MoneyAmount.of(value)` construction, assert `amount.value().compareTo(MoneyAmount.MIN_PRICE) >= 0` else throw `IllegalArgumentException("price below floor: …")`; preserve `representableAt` scale check
- [x] 1.5 Modify `src/main/resources/config.yml` — BREAKING replace `price: 25.00` with mandatory nested block `price: hand: 12000.00 all: 25000.00` (+ `feedback: enabled/sound/particles` placeholder and `messages.repair-success: "<green>Repaired {count} items for {price}.</green>"` — feedback fields consumed in Slice 2 but schema landed here so Slice 1 snapshot compiles); add comments: both required, each >= 10_000, flat scalar invalid
- [x] 1.6 RED (TDD) `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortTest.java` — add failing cases: bare `price: 25.00` → `Failure("missing price.hand")` or scalar-rejected, missing `price.hand` absent → fail-closed, missing `price.all` absent → fail-closed, `price.hand: 9999.99` → fail-closed, `price.all: 5000` → fail-closed, empty indented `price:` → fail-closed; verify `current()` unchanged after failure
- [x] 1.7 GREEN `src/main/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPort.java` — extend hand-rolled scan: require `price:` header line with empty value (reject `price: <scalar>` immediately → `err("missing price.hand")`), then under `price:` indented block parse `hand:` and `all:` into `priceHandRaw`/`priceAllRaw` (strip quotes); if header never seen but scalar present → scalar-rejected; require both non-null else `err("missing price.hand")`/`err("missing price.all")`; parse each to `BigDecimal`, reject non-finite/negative/below `MoneyAmount.MIN_PRICE`; also parse `feedback:` block (`enabled`, `sound`, `particles`) into snapshot fields (defaults: enabled=true, sound=BLOCK_ANVIL_USE, particles=CRIT if absent); keep `AtomicReference.swap` only on `SnapshotOrError.snapshot != null`; preserve `admin.target-distance` and `messages` parsing; startup `tryLoad()==null` → `ConfigSnapshot(ZERO,ZERO,8,empty,false,…)` disables activation

### Phase 2: Transaction Boundary — Success Count + Mode Selector

> Dependency: Phase 1 snapshot + floor done before selector; `TransactionResult.Success` shape (2.1) before `RepairActivation` switch (2.3).

- [x] 2.1 Modify `src/main/java/io/github/danielxxomg/anvillink/domain/TransactionResult.java` — change `record Success(BigDecimal amount)` to `record Success(BigDecimal amount, int repairedCount)`; compact constructor validates `amount != null`, `repairedCount >= 0`; update Javadoc for `{count}` consumers
- [x] 2.2 RED (TDD) `src/test/java/io/github/danielxxomg/anvillink/domain/TransactionResultTest.java` — failing: `new Success(new BigDecimal("20000"), 3)` carries both fields, `Success(ZERO, 0)` for empty plan, JSON-agnostic equality; existing `NoProvider`/`InsufficientFunds`/compensation tests still pass
- [x] 2.3 Modify `src/main/java/io/github/danielxxomg/anvillink/domain/RepairActivation.java` — after `config.current()` and tamper check, select price: `BigDecimal selected = rec.get().mode() == RepairMode.HAND ? cfg.priceHand() : cfg.priceAll()`; then `ValidatedPrice.of(selected, economy.fractionalDigits())` (per-mode precision, fail-closed `InvalidResponse("invalid-price:…")`); keep `plan` → empty → `Success(ZERO, 0)` no withdrawal; collect `planned` list; single `economy.withdraw(player, price.value())` once; on `Success` path inside `scheduler.runOnServerThread(apply…)` compute `repairedCount = planned.size()` (or mutated count post-apply) and return `Success(withdrawn, repairedCount)`; update `apply()` to construct `Success(withdrawn, planned.size())` on apply success; compensation paths propagate `CompensationSuccess/Failed/RestorationFailed` unchanged
- [x] 2.4 RED (TDD) `src/test/java/io/github/danielxxomg/anvillink/domain/RepairActivationTest.java` — add failing per-mode scenarios: HAND mode withdraws `price.hand` not `price.all`, ALL withdraws `price.all`, `price.hand=10000.001` with `fractionalDigits=2` → `InvalidResponse` no withdrawal, `price.all=10000.001` HAND still succeeds (other mode not validated), empty plan → `Success(ZERO,0)` no Vault call, non-empty ALL 3 slots → `Success(20000,3)` (or `priceAll` value), single-withdrawal still enforced
- [x] 2.5 Fix compilation `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` (Slice 1 minimal) — update any `config.current().price()` call sites to not break build (feedback wiring deferred to Slice 2); keep existing `onPlayerInteract` success branch handling `Success.amount()` only; verify `Success` two-arg construction in test fakes
- [x] 2.6 GREEN: make Phase 2 REDs pass; confirm `domain/**` imports contain no `org.bukkit.*`/`net.milkbowl.*`/`net.kyori.*` (ports + `MoneyAmount`/`ValidatedPrice`/`TransactionResult` remain pure)
- [x] 2.7 Verify Slice 1: `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` — unit + adapter parser tests green; `build/libs/*.jar` bytecode major 61; no feedback files present yet

---

## Slice 2 — Feedback: Presentation

### Phase 3: Feedback Port / Adapter + Wiring

> Dependency: Slice 1 `ConfigSnapshot` feedback fields already present; Slice 2 creates pure port before adapter before wiring.

- [x] 3.1 Create `src/main/java/io/github/danielxxomg/anvillink/domain/ports/FeedbackPort.java` — pure interface `void play(SignPort.PlayerId playerId, BigDecimal amount, int repairedCount)`; Javadoc: called only after committed `Success(non-zero)`, never affects economy, `repairedCount` for `{count}`, `amount.toPlainString()` for `{price}`; no Bukkit/Adventure/Vault imports
- [x] 3.2 RED (TDD) `src/test/java/io/github/danielxxomg/anvillink/adapter/BukkitFeedbackAdapterTest.java` — failing: mock `MessagePort` + `SchedulerPort` + `ConfigurationPort`; given `feedbackEnabled=false` → `play` no-ops (no MessagePort.render, no sound/particles); given `feedbackEnabled=true` → renders `repair-success` with `{count}=4 {price}=20000` via `amount.toPlainString()`; given `MessagePort` throws → swallowed, test still passes; verify `SchedulerPort.runOnServerThread` used for dispatch
- [x] 3.3 Create `src/main/java/io/github/danielxxomg/anvillink/adapter/BukkitFeedbackAdapter.java` — implements `FeedbackPort`; ctor `BukkitFeedbackAdapter(ConfigurationPort, MessagePort, SchedulerPort, Function<UUID,Player>)`; `play` checks `config.current().feedbackEnabled()` early return, otherwise `scheduler.runOnServerThread(() -> { try { messagePort.render("repair-success", Map.of("count", String.valueOf(count), "price", amount.toPlainString())); player.playSound(...feedbackSound...); player.spawnParticle(...feedbackParticles...); } catch (Exception ignored) {} })`; resolve sound/particle strings with safe defaults, swallow all throwables, never call `EconomyPort`
- [x] 3.4 Modify `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` — wire `FeedbackPort feedback = new BukkitFeedbackAdapter(configPort, messagePort, scheduler, Bukkit::getPlayer)`; in `onPlayerInteract` after `activation.activate(...)` returns `TransactionResult.Success s`, gate: `if (s.amount().compareTo(BigDecimal.ZERO) != 0) { try { feedback.play(new SignPort.PlayerId(player.getUniqueId()), s.amount(), s.repairedCount()); } catch (Exception ignored) {} }`; ensure empty `Success(ZERO,0)` and any failure never triggers feedback; keep prior `insufficient-funds`/`tampered`/`no-eligible-items` messages unchanged
- [x] 3.5 Modify `src/main/resources/config.yml` (if not fully in 1.5) — ensure `feedback: enabled: true sound: BLOCK_ANVIL_USE particles: CRIT` and `messages.repair-success` present with MiniMessage default; verify `FileConfigurationPort` feedback parsing defaults tested in 3.2
- [x] 3.6 RED `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortTest.java` (feedback) — failing: `feedback.enabled=false` snapshot reflects disabled, missing `feedback:` block defaults to enabled+defaults, `repair-success` absent → empty string message map still succeeds; invalid reload retains prior feedback fields together with prior prices (no partial apply)
- [x] 3.7 GREEN: make 3.2/3.6 pass; domain purity check `grep -R "org.bukkit\|net.milkbowl\|net.kyori" src/main/java/io/github/danielxxomg/anvillink/domain` must be empty; adapter allowed to import Bukkit/Adventure

### Phase 4: Integration / E2E + Docs / Quality

> Dependency: Phases 1–3 complete; E2E proves failure isolation and `toPlainString` contract.

- [x] 4.1 Integration `src/test/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPortTest.java` — extend reload atomics: write temp file with valid `price.hand/all` then reload with scalar/missing/below-floor/precision-bad file → assert `ReloadOutcome.Failure(reason, retained)` and `current()` still equals prior valid snapshot (both prices + feedback unchanged); valid reload swaps atomically
- [x] 4.2 Integration `src/test/java/io/github/danielxxomg/anvillink/domain/RepairActivationTest.java` (per-mode precision) — parametric: `fractionalDigits=2`, `price.hand=10000.001` → HAND fails closed no withdrawal, `price.hand=10000.00` → succeeds; same for `price.all` in ALL mode; cross-mode pollution check: HAND invalid does not cache failure for subsequent ALL
- [x] 4.3 E2E `src/test/java/io/github/danielxxomg/anvillink/integration/FeedbackE2ETest.java` (JUnit 5 + MockBukkit 4.110) — scenarios: paid HAND success → one `repair-success` render with `{count}`==1 `{price}==price.hand.toPlainString()`, no second render; paid ALL 3 slots → `{count}==3`; `Success(ZERO,0)` empty plan → no render/no sound; `feedback.enabled=false` with paid success → silent; adapter `MessagePort` throws → swallowed, transaction still `Success`, no deposit/restoration; verify `VaultEconomyGateway` single withdrawal per activation preserved
- [x] 4.4 E2E `src/test/java/io/github/danielxxomg/anvillink/integration/PricePerModeE2ETest.java` (MockBukkit 4.110) — flat `price: 25.00` scalar file → startup `activationEnabled=false`, no repair; reload from valid to scalar → `Failure` retains prior prices; `price.hand=10000` `price.all=20000` → HAND withdraws 10000, ALL withdraws 20000, plan empty → no charge
- [x] 4.5 Quality: run `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew spotlessApply` then `spotlessCheck` on all new/modified files; ensure Google Java Format 1.17 + license headers pass
- [x] 4.6 Quality: `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test build` — all unit/integration/E2E green, shadowJar with relocated `anvillink.libs.kyori`, `BytecodeFloorTest` major 61, `PluginDescriptorTest` `plugin.yml` api-version 1.13 still passes
- [x] 4.7 Docs: update `src/main/resources/config.yml` header comments and `README`/`CHANGELOG` config section noting BREAKING `price.hand`/`price.all` >=10_000, scalar rejection, `feedback:` + `repair-success` with `{count}`/`{price}` exact `toPlainString`, MAJOR bump rationale (if docs part of change; otherwise note in PR description)

---

## Dependency Notes

- 1.1 → 1.5 → 1.6 → 1.7 sequential: snapshot shape lands before file writes and parser.
- 1.2 → 1.3 → 1.4 sequential: domain floor proven before parser mirrors it.
- 1.x (all Phase 1) → 2.1 → 2.3: `Success(amount,count)` and validated snapshot required before selector.
- Slice 1 (Phase 1+2) → Slice 2 (Phase 3+4): feedback port needs snapshot feedback fields and `TransactionResult.Success(amount,count)` from Slice 1; do NOT start Slice 2 until Slice 1 CI is green on tracker branch.
- 3.1 → 3.3 → 3.4: pure port before adapter before wiring; 3.4 gated on `amount != ZERO`.
- 4.1/4.2 require 1.7+2.3 done; 4.3/4.4 require 3.4 done.

## Scenario Traceability Matrix

| # | Spec Scenario | Requirement | Task(s) |
|---|---------------|-------------|---------|
| 1 | HAND charges price.hand | Valid per-mode configured price | 1.7, 2.3, 2.4, 4.4 |
| 2 | ALL charges price.all | Valid per-mode configured price | 1.7, 2.3, 2.4, 4.4 |
| 3 | Flat scalar rejected | Valid per-mode configured price | 1.5, 1.6, 1.7, 4.1, 4.4 |
| 4 | Missing per-mode price rejected | Valid per-mode configured price | 1.6, 1.7, 4.1 |
| 5 | Below-floor rejected | Valid per-mode configured price | 1.2, 1.3, 1.4, 1.6, 1.7 |
| 6 | Per-mode invalid precision fails closed | Valid per-mode configured price | 2.3, 2.4, 4.2 |
| 7 | Single withdrawal uses selected price | Single withdrawal and failed-payment handling | 2.3, 2.4, 4.4 |
| 8 | Success carries amount and count | Transaction success carries repaired count | 2.1, 2.2, 2.3, 4.3 |
| 9 | Empty plan yields zero success | Transaction success carries repaired count | 2.3, 2.4, 4.3, 4.4 |
| 10 | Paid success renders repair-success | Success feedback presentation | 3.2, 3.3, 3.4, 4.3 |
| 11 | No feedback on zero or failure | Success feedback presentation | 3.4, 4.3 |
| 12 | Disabled feedback is silent | Success feedback presentation | 3.2, 3.3, 4.3 |
| 13 | Feedback failure never affects transaction | Success feedback presentation | 3.2, 3.3, 3.4, 4.3 |
| 14 | Error messages unchanged | Success feedback presentation | 3.4, 4.3 |

## Verification Checklist (per slice)

- [x] `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test` — all REDs now green
- [x] `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew spotlessCheck` — format+headers pass
- [x] `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew build` — shadowJar, bytecode major 61, PDC namespace unchanged
- [x] Domain purity: `grep -R "org.bukkit\|net.milkbowl\|net.kyori\|ConfigurationSection\|YamlConfiguration" src/main/java/io/github/danielxxomg/anvillink/domain` empty
- [x] Slice 1 rollback boundary verified: revert pricing files only, feedback absent
- [x] Slice 2 rollback boundary verified: revert feedback files only, pricing intact
