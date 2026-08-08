# Design: AnvilLink Paid Repair Signs

## Technical Approach

Java 17 JAR: `group=io.github.danielxxomg`, `artifact=anvillink`, `plugin.yml` `name: AnvilLink`, Paper API 1.18.2/VaultAPI 1.7; host APIs `compileOnly`, unshaded; public Bukkit APIs only. No NMS/reflection/DB/remote API; no GUI/anvil/full-inventory/XP/material/per-item/durability modes, command product, legacy migration, marketplace, or Folia certification.

## Architecture Decisions and Contracts

| Choice | Rejected | Rationale |
|---|---|---|
| `domain <- application <- adapters`; one transaction use case. | Bukkit/Vault types in domain; service locator. | Pure deterministic tests and explicit failure policy. |
| Permanent repository `danielxxomg/anvillink`, Java root `io.github.danielxxomg.anvillink`, and PDC `danielxxomg:anvillink_repair_sign`. | Brand/generated identity. | Package and PDC key NEVER change on repository transfer or display rename. |
| `plugin.yml` contains `softdepend: [Vault]`. | Hard dependency or free fallback. | Plugin may load diagnostically without Vault, but paid activation fails closed until Vault and a registered `Economy` provider exist. |

`domain`: `RepairMode`, ordered `EquipmentSlotId`, `RepairPlan`, `PlannedSlot`, `MoneyAmount`, `SignRecord`. `application` ports: `SignPort`, `EquipmentPort`, `EconomyPort`, `SchedulerPort`, `ConfigurationPort`, `MessagePort`, `OperationalReporter`; neutral IDs, `BigDecimal`, opaque snapshots. Bukkit owns events/PDC/signs/inventory/threading; Vault owns services/responses; config/presentation/entrypoint own reload/rendering/commands.

## Identity, Sign Lifecycle, and Data Flow

The PDC `BYTE_ARRAY` is `magic | schema=1 | mode | creator UUID (16 bytes) | authorized-create=1`. Malformed/unknown versions fail closed; supported migrations rewrite this key after success. `SignChangeEvent#setLine(...)` sets future text; after commit, `Sign`/`TileState` persistence sets PDC/color and requires `BlockState#update(true,false)`. Failure cancels pre-commit or removes/invalidates post-commit; no valid PDC means no charge.

New sign: require `create`, parse exact line 1 `equalsIgnoreCase("[repair]")` and line 2 `HAND|ALL` case-insensitively; write canonical text, `DyeColor.BLUE`, and PDC. Existing PDC means registered: edit/break requires `manage`; manager edits remain tampered until rerender. Unauthorized actions cancel. Floor lines are front-side; back text is ignored. Use requires valid PDC/text, `use`, right-click, and `EquipmentSlot.HAND`; off-hand is ignored. Admin `/anvillink inspect|rerender` requires player/manage and a line-of-sight sign within `admin.target-distance` 1–32 (default 8): no-target=`no-target`, absent=`not-registered`, malformed=`invalid-identity`, mismatch=`tampered`; rerender only valid PDC, never text.

```text
create/update: SignChangeEvent -> parse/auth -> TileState PDC+render -> update()
break: BlockBreakEvent -> PDC -> manage? cancel : allow
use: main-hand event -> validate -> plan -> payment -> apply/recover
```

## Repair and Economy Transaction

Plan exactly `HAND=[main]` or `ALL=[main, off, helmet, chest, leggings, boots]`, ordered and never storage. Snapshot full item state; include only non-empty, `Damageable`, positive-damage, breakable items. Empty means no call. `MoneyAmount`=`BigDecimal`; reject non-finite/negative or scale above provider `fractionalDigits` (`-1` unlimited). Convert once to finite `d`; require `BigDecimal.valueOf(d).compareTo(amount) == 0` (`25.00`=`25.0`), reusing `d` for withdrawal/normal compensation. If `!transactionSuccess()`: no mutation/deposit; report/terminate. If success but response amount is invalid/mismatched: no mutation; deposit once only for a finite/non-negative reported withdrawn amount; otherwise severe unresolved-charge evidence and no deposit; terminate/no retry. Apply requires success, finite/non-negative amount, and `BigDecimal.valueOf(response.amount).compareTo(amount) == 0`; later failure uses existing recovery. Vault is non-atomic.

Mutation runs on owning server thread (`BukkitScheduler`; MVP rejects Folia). Failure restores ONLY previously mutated slots from snapshots; untouched planned slots remain untouched. Attempt each restoration, then one deposit; severe evidence records activation/player/sign, amounts, mutated/restored/unresolved slots, and refund. Notify failure and terminate.

## Configuration and Presentation

Load an immutable price/defaults/messages snapshot; atomically swap an `AtomicReference` only after validation. Invalid reload retains it; invalid startup disables activation. Pin Adventure `4.11.0` (`adventure-api`, MiniMessage, legacy serializer); `--release 17` build and smoke-test classloading. Shade/relocate it and expose `String` through `MessagePort`/Bukkit; no relocated type crosses ports and Spigot/Purpur Adventure is assumed nowhere. Signs use Bukkit strings/color, not MiniMessage.

## Compatibility Evidence and Rollout

Pinned smoke matrix:

| Distribution | Server pin | JDK | Claim |
|---|---|---:|---|
| Paper | 1.18.2 build 388 | 17 | Certified |
| Paper | 1.20.6 build 151 | 21 | Certified |
| Paper | 1.21.11 build 132 | 21 | Certified |
| Spigot | 1.20.6 via BuildTools #200 `--rev 1.20.6` | 21 | Verified |
| Purpur | 1.20.6 build 2233 | 21 | Verified |
| Paper | 26.2 build 102 | 25 | Probe only; uncertified |

Commit `compatibility/evidence.json` with `releaseCommit`; jobs carry `distribution`, `version`, `build`, `serverSha256`, `jdkMajor`, `testSuite`, `result` (`pass|fail|missing`). Release automation updates README/support/GitHub release metadata only from passing rows matching commit/checksum; failed/missing rows prohibit claims. Paper 26.x stays uncertified until its Java-25 job passes; Folia stays experimental. SemVer is separate.

## File Changes and Testing

Create Gradle/catalog/locks, `plugin.yml`, `config.yml`, `src/main/java/io/github/danielxxomg/anvillink/{domain,application,adapter,config,presentation,entrypoint}`, tests/workflows, `compatibility/evidence.json`, README, GPL LICENSE, CHANGELOG. RED JUnit/MockBukkit/real-server/economy tests trace **22 requirements / 36 scenarios**: signs 6/12, equipment 4/7, economy 5/8, platform 7/9; JAR tests verify Java17, host exclusion, descriptor, PDC, evidence.

## Threat Matrix, Migration, and Sources

Threat matrix applicable: documentation, repository, commit, push/release, PR. Safe: allowlisted README/evidence, fixed root, matching commit, explicit user tag/approval; no design-triggered side effects. Failure rejects path/root/index/remote/tag/claim mismatch. RED tests cover executable docs, relative/absolute/`git -C`, staged/empty index, tracking/first push/refspec, implicit/composed PR. GitHub remains user-owned/evidence-gated. Greenfield. Sources: [Paper Java/version table](https://docs.papermc.io/paper/getting-started/), [Paper downloads](https://fill.papermc.io/v3/projects/paper), [Spigot BuildTools](https://www.spigotmc.org/wiki/buildtools/), [Purpur API](https://api.purpurmc.org/v2/purpur/1.20.6), [Paper Sign/TileState/PDC](https://jd.papermc.io/paper/1.18.2/org/bukkit/block/Sign.html), [Vault fractionalDigits/response](https://github.com/MilkBowl/VaultAPI), [Adventure 4.11.0 POM](https://repo1.maven.org/maven2/net/kyori/adventure-text-minimessage/4.11.0/adventure-text-minimessage-4.11.0.pom).

## Open Questions

None.
