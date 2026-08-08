## Exploration: paid repair signs

### Current State

**Decision in brief:** build a new, narrowly scoped Paper plugin rather than fork an old repair-shop implementation. The strongest default for the proposal is a public-API-only, conventional Bukkit plugin compiled for Paper 1.18.2 with Java 17 bytecode, then verified on representative 1.20/1.21 and current Paper lines. This preserves a practical older-server floor without NMS, while keeping the architecture modern. If 1.18.2 is not a required compatibility point, the simpler Paper 1.20.6/Java 21 baseline remains the better alternative.

#### Verified repository state

- This is a greenfield repository. It currently contains SDD configuration and tooling metadata, but no Java sources, Gradle/Maven build, plugin descriptor, tests, or runtime behavior to preserve.
- `openspec/config.yaml` explicitly records an undetected stack, no test runner, no linter, no formatter, no type checker, and `strict_tdd: false`. The proposal must turn those placeholders into an intentional Java/Paper build and verification plan.
- No main specs or prior change artifacts exist. The only artifact permitted by this phase is this file; no implementation or proposal is being created.

#### Verified ecosystem and dependency map

The niche is fragmented rather than empty. Existing products provide useful workflow precedents, but none is an appropriate implementation base for a new maintained identity:

| Evidence | Verified observation | Reuse/build implication |
|---|---|---|
| [Repair Sign Standalone on SpigotMC](https://www.spigotmc.org/resources/repair-sign-standalone.72841/) | A small exact-match sign plugin: `[repair]`, tested on 1.13, separate create/use permissions, 161 total downloads, last update December 1, 2019. | Confirms the interaction vocabulary and permission split; its age and narrow test claim make it a reference, not a base. |
| [RepairSign on CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/repairsign) | Legacy sign-based repair/estimate/salvage workflow with Vault, 11,548 downloads, last update October 4, 2013, and an All Rights Reserved license. | Do not copy code or assume rights; reuse only the product insight that price previews and repair stations were valuable. |
| [RepairShop on CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/repairshop) | Marked abandoned; 52,978 downloads, last update January 20, 2013, sign/chest resource model, economy integrations, and All Rights Reserved licensing. | Demonstrates historical demand but is technologically and legally unsuitable for direct reuse. |
| [SimpleRepair on Modrinth](https://modrinth.com/plugin/simplerepair) | Maintained adjacent project with MIT licensing and broad Paper/Folia/Fabric coverage; its public workflow is command-based repair, not paid signs. | A useful example of a small cross-platform repair scope, not a competitor with the same contract. |
| [DP-ToolRepair on Modrinth](https://modrinth.com/plugin/dp-toolrepair) | Current adjacent paid repair: GUI-based tool repair with money/experience, MIT license, and dependencies on DPP-Core plus Essentials economy. It is not sign-based. | Confirms current demand for paid repair while showing a different UX and dependency tradeoff. |
| [RepairIT on Modrinth](https://modrinth.com/plugin/repairit) | Current Paper/Spigot/Purpur plugin using an anvil, material, and custom hammer; GPL-3.0-only. It is not a paid sign system. | Useful product differentiation evidence; GPL obligations would matter if code were reused. |
| [Movecraft-Repair on GitHub](https://github.com/APDevTeam/Movecraft-Repair) | Active open-source addon with craft repair signs, GPL-3.0, and explicit Movecraft coupling; its main branch targets 1.14.4+. | Event/sign patterns may be studied, but the domain dependency and copyleft license argue for building independently. |

**Reuse-vs-build assessment:** build the sign identity, parsing, repair planning, and economy transaction from scratch. Borrow only publicly documented behavior and testing ideas. Do not reuse code from the All Rights Reserved/no-license legacy projects. Reuse of GPL code would require an explicit project-license decision and still would not remove the compatibility/domain mismatch.

#### Verified Paper and dependency facts

- Paper documents `plugin.yml` as the conventional plugin descriptor and labels `paper-plugin.yml`/Paper plugins experimental. A stable `plugin.yml` is the safer public distribution contract for this plugin. Sources: [project setup](https://docs.papermc.io/paper/dev/project-setup/), [plugin.yml](https://docs.papermc.io/paper/dev/plugin-yml/), [experimental Paper plugins](https://docs.papermc.io/paper/dev/getting-started/paper-plugins/).
- Paper's current runtime table says Java 17 for 1.17–1.19, Java 21 for 1.20–1.21.11, and Java 25 for 26.1+. The project setup page currently demonstrates Paper 26.2 with a Java 25 toolchain. Source: [Paper getting started](https://docs.papermc.io/paper/getting-started/).
- Paper's old Javadocs verify that the proposed compatibility-first floor already exposes the needed public primitives: `Sign` is a `TileState`, `TileState` exposes a `PersistentDataContainer`, `PlayerInventory` exposes equipment slots, and `Damageable` exposes `hasDamage`, `getDamage`, and `setDamage`. Sources: [1.18.2 Sign](https://jd.papermc.io/paper/1.18.2/org/bukkit/block/Sign.html), [1.18.2 TileState](https://jd.papermc.io/paper/1.18.2/org/bukkit/block/TileState.html), [1.18.2 PlayerInventory](https://jd.papermc.io/paper/1.18.2/org/bukkit/inventory/PlayerInventory.html), [1.18.2 Damageable](https://jd.papermc.io/paper/1.18.2/org/bukkit/inventory/meta/Damageable.html).
- Paper 1.20.6 additionally exposes side-aware sign APIs and documents that `PlayerInteractEvent` may fire once per hand, with `getHand()` available. `Damageable` still uses `setDamage(0)`; a current-baseline `resetDamage()` must not be assumed. Sources: [1.20.6 SignChangeEvent](https://jd.papermc.io/paper/1.20.6/org/bukkit/event/block/SignChangeEvent.html), [1.20.6 Sign](https://jd.papermc.io/paper/1.20.6/org/bukkit/block/Sign.html), [1.20.6 PlayerInteractEvent](https://jd.papermc.io/paper/1.20.6/org/bukkit/event/player/PlayerInteractEvent.html), [1.20.6 Damageable](https://jd.papermc.io/paper/1.20.6/org/bukkit/inventory/meta/Damageable.html).
- Paper's PDC guide lists `TileState` as a supported holder and explains that a modified tile state must be updated. The guide is written for 1.21.8, so the older Javadocs above are the evidence for the lower floor. Source: [PDC guide](https://docs.papermc.io/paper/dev/pdc/).
- Paper natively implements Adventure components and documents MiniMessage as included by Paper. The selected lowest API must still be pinned and tested for the exact serializer surface used by messages. Source: [component API introduction](https://docs.papermc.io/paper/dev/component-api/introduction/).
- VaultAPI 1.7 is a stable economy abstraction with a `compileOnly` Gradle dependency, provider lookup through Bukkit's `ServicesManager`, and `EconomyResponse` success/failure results. The API is based on `double` amounts and explicitly says not to use negative withdrawals. Sources: [VaultAPI README](https://github.com/MilkBowl/VaultAPI), [Economy API](https://github.com/MilkBowl/VaultAPI/blob/master/src/main/java/net/milkbowl/vault/economy/Economy.java), [EconomyResponse](https://github.com/MilkBowl/VaultAPI/blob/master/src/main/java/net/milkbowl/vault/economy/EconomyResponse.java).
- Gradle recommends Java toolchains for reproducible compile/test JDK selection and `--release` when strict API/bytecode compatibility is required. Source: [Gradle JVM toolchains](https://docs.gradle.org/current/userguide/toolchains.html).
- MockBukkit provides plugin lifecycle, mock players, event simulation, and mock plugins, but its README states that older version branches are not actively patched. The current active line is v26. Sources: [MockBukkit README](https://github.com/MockBukkit/MockBukkit), [MockBukkit branches](https://github.com/MockBukkit/MockBukkit/branches).

#### Product map: intended workflow

The following is the recommended contract to take into the proposal; unresolved items are called out rather than silently invented.

1. **Create:** an operator or player with the create permission places/edits a sign with a case-insensitive repair token on line one. The plugin validates the mode and price contract, stores a versioned sign record in PDC, and renders the canonical first line as blue `[repair]`.
2. **Canonical display:** line two is normalized to uppercase `HAND` or `ALL`. The visible lines are presentation; the versioned PDC record is the identity and source of truth. A stable explicit PDC namespace must be chosen before the first release and must not be coupled to a future display-brand rename.
3. **Interact:** a right-click on a recognized sign is processed once. The listener filters the interaction hand to prevent duplicate charges, checks use permission, resolves a bounded target set, and creates a deterministic repair plan before touching money or items.
4. **Repair:** the MVP considers only damaged, repairable `Damageable` item metas. Undamaged items, empty slots, and unbreakable items are skipped. `HAND` should mean the main-hand equipment slot by default; `ALL` should mean main hand, off hand, helmet, chestplate, leggings, and boots—not the entire storage inventory—unless the proposal chooses a different product definition.
5. **Economy:** if the plan has no eligible item, no charge occurs. Otherwise the plugin calls Vault once, checks `EconomyResponse.transactionSuccess()`, applies `setDamage(0)` to the planned items, and reports the result. Missing Vault/provider, invalid signs, insufficient funds, and provider failure fail closed.
6. **Protection:** unauthorized players cannot create, edit, or break repair signs. The plugin should use PDC identity rather than trusting copied text, and should either reject visibly tampered records or provide an admin-only inspect/re-render path. Automatic waxing can be considered, but it should not be assumed without deciding the staff-edit workflow.

The pricing decision is explicit: every repair-sign activation charges the same configured flat price. The MVP does not price per repaired item or by missing durability. The proposal should carry this fixed-per-activation contract forward without treating per-sign pricing as an assumption.

#### Source limitations and unknowns

- BuiltByBit and Polymart could not be verified because their accessible pages returned access challenges/403 responses. No absence claim is made about either marketplace.
- Modrinth's public search API confirms current adjacent projects, but marketplace download counts are not comparable across platforms and search results are not an exhaustive market census. Hangar's public search page did not expose a reliable filtered result in this pass.
- GitHub/Modrinth exact-name checks found no exact Modrinth projects for the working candidates, but GitHub did expose unrelated collisions: `MendMark` ([mendmark](https://github.com/danielgaskins/mendmark), [MendMarkRepairAtelier](https://github.com/wangrenzhu-ola/MendMarkRepairAtelier)), `RepairRelay` ([ai-repairrelay-ai](https://github.com/wuzz-dev/ai-repairrelay-ai)), `MendPoint` ([khanhdoth/mendpoint](https://github.com/khanhdoth/mendpoint), [gondalaimafia/mendpoint](https://github.com/gondalaimafia/mendpoint)), and `ForgePost` ([zanker/forgepost](https://github.com/zanker/forgepost), among others). `AnvilLink` had no exact result in these checks. These are collision signals, not trademark, domain, or marketplace clearance.
- Paper's version and Java requirements evolve. Current/future 26.x support must be CI-tested and advertised only as a tested range; no future-version guarantee belongs in the proposal.

### Affected Areas

The following are proposed impact areas; they do not exist yet and must not be created during exploration:

- `openspec/config.yaml` — replace the current undetected-stack/testing placeholders only after the proposal selects the compatibility floor and test commands.
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/` — pinned Java toolchain, Paper API, VaultAPI, JUnit, MockBukkit, formatting, and packaging.
- `src/main/resources/plugin.yml` and `src/main/resources/config.yml` — stable plugin metadata, permissions, dependency softness, messages, defaults, and reloadable configuration.
- `src/main/java/<stable-namespace>/` — pure domain parsing/planning, PDC sign repository, economy gateway, equipment resolver, transaction coordinator, listeners, and presentation/messages.
- `src/test/java/` and `src/test/resources/` — parser/plan unit tests, MockBukkit lifecycle/event tests, fake economy-provider tests, and compatibility fixtures.
- `.github/workflows/` — build checks plus real Paper smoke-test jobs for the advertised version matrix.
- `README.md`, `LICENSE`, `CHANGELOG.md`, and release metadata — product identity, permissions, setup, supported versions, SemVer releases, and explicit non-goals.

### Approaches

1. **Compatibility-first single artifact (recommended default)** — compile against Paper 1.18.2 with Java 17 bytecode, use only public Bukkit/Paper APIs common to the floor, and test representative old, middle, newest Java-21, and current Java-25 server lines.
   - Pros: materially better coverage of older and current Paper servers without NMS; Java 17 is a modern LTS baseline; PDC, Adventure components, equipment slots, and `Damageable` are already available at 1.18.2; one plugin identity and one release stream.
   - Cons: the implementation must avoid newer conveniences such as side-aware `SignSide` APIs; older MockBukkit branches are weaker; real-server CI and compatibility discipline are mandatory; current Paper 26.x still requires a Java-25 test runner.
   - Effort: Medium.
   - Honest advertisement if the matrix passes: `Paper 1.18.2–1.21.11 tested`; current 26.x is experimental/untested until a Java-25 smoke job passes. Spigot/Purpur remain compatible-fork candidates, not promises, until separately tested.

2. **Modern narrow baseline** — compile against Paper 1.20.6 with Java 21, use the modern sign-side/component surface, and verify 1.20.6 through 1.21.11 first.
   - Pros: smallest API compatibility surface; aligns with the originally researched 1.20.6/Java 21 candidate; simpler use of current Paper APIs and current MockBukkit; lower implementation and CI cost.
   - Cons: does not honor a meaningful 1.18/1.19 older-server promise; Java 21 bytecode excludes Java-17 server runtimes; 26.x requires a separate Java-25 verification job; it can overstate “multi-version” support if only modern versions are tested.
   - Effort: Low to Medium.
   - Honest advertisement: `Paper 1.20.6–1.21.11 tested`; no older/current-future claims without evidence.

3. **Adapter or multi-artifact compatibility line** — keep a version-neutral core but add platform adapters or separate builds for an older API floor and a current API floor.
   - Pros: widest possible compatibility and freedom to use newer APIs in the current artifact; explicit per-artifact support claims.
   - Cons: substantially more build/release/fixture complexity, duplicated event/PDC/message code, more migration paths for the stable sign namespace, and a larger regression matrix; reflection or NMS would undermine the stated public-API identity.
   - Effort: High.
   - Honest advertisement: only the exact tested matrix for each artifact; not appropriate for the first MVP unless the compatibility question is a hard business requirement.

### Recommendation

Proceed to proposal with approach 1 as the default, subject to the compatibility question below. The plugin is small enough to keep its business logic pure and synchronous, while the public API surface is old enough at Paper 1.18.2 to support a useful single-jar range. This is a better reconciliation of “modern architecture” and “popular older/current versions” than declaring 1.20.6+ support and calling it broad compatibility.

#### Recommended architecture and transaction boundary

- **Domain core:** immutable sign specification, canonical-token parser, mode/price validation, equipment-target policy, repair-plan calculation, and transaction result types with no Bukkit classes. This makes the high-risk rules unit-testable without a server.
- **Paper adapter:** event listeners for sign creation/edit, interaction, and break; PDC codec/repository; item-meta mutation; permission checks; and Adventure/MiniMessage presentation. Use front-side sign APIs common to the selected floor; do not make side-aware APIs part of the MVP contract.
- **Economy adapter:** `EconomyGateway` around Vault's `Economy`, resolving the provider through the service manager and returning a domain result. Declare Vault as a soft dependency so the plugin can load in a failed-closed diagnostic state rather than silently offering free repairs.
- **Repair transaction:** validate sign and targets, snapshot/plan all mutations, withdraw once, apply all planned item changes on the server thread, and send feedback. If an unexpected apply failure occurs, restore snapshots and attempt a compensating deposit; log an unreconciled refund as a high-severity operational event. Vault does not provide a cross-system atomic transaction with the player's inventory.
- **Persistence:** store a schema version, mode, and canonical identity in a PDC record under a permanent explicit namespace; resolve the configured flat price from plugin configuration rather than from item count or durability. Treat visible sign text as a canonical projection, not as the security boundary. Never silently derive identity from `[repair]` text alone.
- **Build/test:** Gradle Kotlin DSL with a pinned wrapper, Java 17 toolchain and `--release 17`, `compileOnly` Paper API and VaultAPI, JUnit 5, MockBukkit for lifecycle/event coverage, and real Paper smoke tests for each advertised server/JVM combination. Paper and Vault must not be shaded into the plugin.
- **Release:** conventional `plugin.yml`, SemVer plugin versions independent of Minecraft versions, stable package/PDC namespace, GitHub Actions checks, a compatibility table in the README, and release artifacts only after license/brand/distribution decisions are explicit. A later auto-chain should split implementation into reviewable slices rather than one greenfield diff.

#### MVP boundary

**In scope:** sign creation authorization; `[repair]` normalization and blue rendering; `HAND`/`ALL` parsing; PDC identity; bounded equipment repair; Vault economy; clear permissions and messages; safe failure/refund policy; protected sign edits/breaks; reloadable messages/defaults; unit and server-level verification.

**Out of scope:** GUI repair stations; XP/material repair modes; command-only repair as a second product; full inventory repair; per-durability pricing; databases or remote APIs; NMS; automatic support for Folia; automatic legacy sign migration; marketplace-specific integrations; and a final brand selection.

#### Strictly adjacent enhancements

Prioritize only enhancements that strengthen the same sign product without duplicating other repair plugins:

1. Admin inspect/re-render and a structured audit log for sign, economy, and refund failures.
2. Configurable MiniMessage messages, currency formatting, and a safe `/... reload` command whose scope is explicit.
3. Optional per-world enable/permission/price policy after the single-server semantics are proven.
4. Folia support only as a separate compatibility slice once region/entity schedulers and an economy interaction policy are designed and tested; Paper explicitly requires more than setting `folia-supported: true`. Source: [Paper/Folia support](https://docs.papermc.io/paper/dev/folia-support/).

### Risks

- **Compatibility drift:** Paper 26.x moved to Java 25 and Paper's APIs continue to evolve. The advertised range must be generated from CI evidence, not from compile success or a future-version assumption.
- **Transaction non-atomicity:** Vault withdrawal and item mutation are separate systems. A refund policy, snapshot strategy, failure message, and operator-visible audit path are required before charging real balances.
- **Duplicate or ambiguous interactions:** `PlayerInteractEvent` can fire per hand; ignoring the hand or treating off-hand activation as a second transaction can double-charge players. `HAND` and `ALL` need an explicit slot contract.
- **Sign tampering and identity loss:** visible lines can be edited or copied, while block destruction removes PDC. Stable namespace, permissions, fail-closed validation, and an admin recovery workflow are more important than visual formatting alone.
- **Economy precision/provider behavior:** Vault exposes `double` amounts and provider-specific rounding/errors. Price parsing, finite/non-negative validation, currency display, and insufficient-funds behavior need deterministic tests against a fake provider and at least one real economy integration.
- **Test realism:** MockBukkit is valuable but its older branches are not actively patched and mocks may not reproduce every sign, item-meta, or economy behavior. Real Paper smoke tests are required for release claims.
- **Legal/brand uncertainty:** legacy sources include All Rights Reserved/no-license material, and working brand candidates have unrelated public collisions. Trademark, domain, repository, and marketplace clearance remains open.
- **Greenfield delivery load:** the build, plugin standards, test harness, CI, docs, and implementation are all new. With `auto-chain`, proposal/tasks should keep each slice independently testable and reviewable rather than treating the 1,200-line allowance as a single diff target.

### Ready for Proposal

**Yes — conditionally.** The exploration is decision-ready and no proposal has been created. Start the interactive proposal round using the compatibility-first architecture above, but resolve these four remaining product/business questions before locking requirements:

1. **Target semantics:** Does `HAND` mean the main-hand slot only or whichever hand initiated the click? Does `ALL` mean the six equipment slots recommended here, or every inventory item including storage?
2. **Sign lifecycle:** Is only the front side supported in MVP? Who may edit/break a registered sign, should creation auto-wax it, and is an admin inspect/re-render command required for tampered text?
3. **Compatibility promise:** Is Paper 1.18.2/Java 17 an explicit requirement, or is Paper 1.20.6/Java 21 an acceptable floor? Must Spigot, Purpur, Folia, or Paper 26.x be release-supported rather than merely smoke-tested?
4. **Identity and distribution:** Which final display brand, stable Java/PDC namespace, open-source license, and initial release channels are authorized after trademark/domain/marketplace clearance? The working names are candidates only, not a decision.
