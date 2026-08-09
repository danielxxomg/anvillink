# platform-compatibility Specification

## Purpose

Define plugin build, release, evidence, support boundaries.

## Requirements

### Requirement: Public API compatibility floor
The plugin MUST compile against Paper API 1.18.2 and emit Java 17 bytecode using a pinned Java 17 toolchain and `--release 17`. It MUST use only public Bukkit/Paper APIs available at that floor and MUST NOT use NMS or require side-aware sign APIs for the MVP.

#### Scenario: Newer build JDK still targets the floor
- GIVEN the build runs on a newer installed JDK
- WHEN production classes are compiled
- THEN classes target Java 17 bytecode and the compile API is Paper 1.18.2

### Requirement: Single-JAR dependency boundary
The release MUST be one plugin JAR with `plugin.yml`. Paper API and VaultAPI MUST be `compileOnly` and unshaded; Vault MUST be a soft dependency.

#### Scenario: Host APIs are not packaged
- GIVEN the release JAR is assembled
- WHEN its contents and descriptor are inspected
- THEN no Paper or Vault API classes are present and Vault is a soft dependency

### Requirement: Stable identity namespace
The repair-sign PDC namespace, key, and supported schema MUST remain independent of the provisional display brand. A brand rename MUST NOT change them.

#### Scenario: Display-brand change preserves identity
- GIVEN a registered sign uses the permanent PDC namespace and schema
- WHEN the display brand changes
- THEN the namespace and schema remain unchanged

### Requirement: SemVer is separate from Minecraft support
The plugin MUST use `MAJOR.MINOR.PATCH` SemVer and publish Minecraft/server/JVM compatibility in a separate evidence-derived matrix. A matrix update MUST NOT be a SemVer guarantee.

#### Scenario: Version and tested range stay distinct
- GIVEN a release has a SemVer value and passing runtime jobs
- WHEN compatibility documentation is generated
- THEN SemVer and the exact tested matrix are separate claims

### Requirement: Evidence-gated support tiers
The project MUST advertise Paper as certified only after the named real Paper smoke matrix passes for every advertised server/JVM combination; otherwise it MUST NOT advertise certification. Spigot and Purpur MUST be labeled verified only after separate smoke evidence and never certified. Folia MUST remain experimental. Paper 26.x MUST NOT be advertised or certified until a Java 25 smoke job passes.

#### Scenario: Missing Paper runtime evidence blocks certification
- GIVEN a Paper version compiles and unit or MockBukkit tests pass but its real Paper smoke matrix has not passed
- WHEN the support matrix is published
- THEN that version is not advertised as tested or certified

#### Scenario: Support labels follow their evidence
- GIVEN the Paper matrix passes, Spigot/Purpur tests have not run, and no Folia matrix exists
- WHEN support status is published
- THEN Paper is certified for the exact matrix, Spigot/Purpur are not certified, and Folia remains experimental

#### Scenario: Paper 26.x requires Java 25 evidence
- GIVEN a Paper 26.x environment has no passing Java 25 smoke job
- WHEN release claims are evaluated
- THEN Paper 26.x is not advertised or certified

### Requirement: Verification layers and economy evidence
The project MUST have JUnit 5 domain tests, MockBukkit event tests, real Paper smoke jobs for each advertised server/JVM, and one real Vault-compatible provider integration path besides fake/mock providers before release claims. Mock-only or fake-only success MUST NOT establish a claim.

#### Scenario: Incomplete evidence blocks release claims
- GIVEN unit, MockBukkit, and fake-provider checks pass but real Paper or Vault-provider evidence is missing or fails
- WHEN a release candidate is evaluated
- THEN the affected claim is not advertised

### Requirement: GPL licensing and GitHub release presence
The release MUST carry GPL-3.0-or-later licensing and have a GitHub release containing the plugin artifact before it is complete.

#### Scenario: Distribution criterion is satisfied
- GIVEN the GPL-3.0-or-later license is present and the plugin artifact is published in a GitHub release
- WHEN release readiness is evaluated
- THEN the licensing and GitHub distribution criterion passes

### Requirement: Released-JAR relocation completeness

The `shadowJar` release JAR MUST relocate ALL `net.kyori.*` (including `adventure-text-serializer-legacy`/`gson`) to `io.github.danielxxomg.anvillink.libs.kyori`. It MUST contain `libs/kyori/text/serializer/legacy/LegacyComponentSerializer.class` and MUST contain zero entries under `net/kyori/adventure/text/serializer`.

#### Scenario: Legacy serializer survives minimize and relocation

- GIVEN the release JAR from `shadowJar { minimize(); relocate("net.kyori","io.github.danielxxomg.anvillink.libs.kyori") }`
- WHEN inspected via `jar tf build/libs/anvillink-*.jar | grep libs/kyori/legacy`
- THEN `LegacyComponentSerializer.class` is present (≈9 classes, ~557K JAR after 9f190b6)

#### Scenario: No unrelocated Adventure remains

- GIVEN the same release JAR
- WHEN inspected via `jar tf build/libs/anvillink-*.jar | grep "^net/kyori"`
- THEN zero matches; any match fails `clean test spotlessCheck build`

#### Scenario: Host APIs not packaged and bytecode floor holds

- GIVEN the same release JAR
- WHEN checked via `jar tf | grep -E "org/bukkit|net/minecraft/server"` and `javap -verbose` on `anvillink/**` excluding `/libs/`
- THEN no `org/bukkit`/`net/minecraft` entries and production bytecode is major 61 (Java 17)

### Requirement: Shipped-config File I/O parse

The harness MUST prove `FileConfigurationPort(File)` parses shipped `src/main/resources/config.yml` via real File I/O. Parser MUST accept `price.hand: 12000.00 # comment` via quote-aware `stripInlineComment`; the test MUST use a `File` on disk, not a synthetic string.

#### Scenario: Shipped file with inline comments parses via File

- GIVEN `src/main/resources/config.yml` copied to a `@TempDir` `File` via `Files.writeString`
- WHEN loaded via `new FileConfigurationPort(File)` and `current()` inspected
- THEN `activationEnabled==true`, `priceHand==12000.00`, `priceAll==25000.00`, `targetDistance==8`, commented `worlds:` absent

#### Scenario: Quoted hash preserved, unquoted hash stripped

- GIVEN a temp `config.yml` with `messages.g: "a # b"` and `price.hand: 12000.00 # comment`
- WHEN loaded via `FileConfigurationPort(File)` (quote-aware `'`/`"`)
- THEN `messages.g` is `a # b` and `priceHand` is `12000.00`

#### Scenario: Synthetic-string-only coverage insufficient

- GIVEN a test asserting `FileConfigurationPort` only against an in-memory string
- WHEN the harness gate is evaluated
- THEN it MUST fail — only a `File`-based test against the shipped resource satisfies this requirement

### Requirement: MockBukkit-load bootstrap end-to-end

The harness MUST prove `MockBukkit.load(AnvilLink.class)` bootstrap through the plugin classloader via `pluginManager.callEvent(SignChangeEvent)` then `PlayerInteract`. The path MUST assert PDC `danielxxomg:anvillink_repair_sign` + `DyeColor.BLUE` survive post-event coalescing, cover fresh-`TileState` and non-`TileState` fallback (stale-vs-fresh), drive Vault mock matrix (`fractionalDigits` 0/2/-1), and verify `audit.log` to a temp data folder. Baselines: Paper 1.18.2/J17, 1.20.6/J21, 1.21.11/J21, and 1.21.5-114/J25 promoted from probe to mandatory; `compatibility/evidence.json` is the gate.

#### Scenario: SignChangeEvent through plugin classloader persists identity and color

- GIVEN `MockBukkit.mock()` + `MockBukkit.load(AnvilLink.class)` with `getDataFolder()` created
- WHEN `pluginManager.callEvent(new SignChangeEvent(...,"[repair]","HAND"))` fires and `Block.getState()` is re-read
- THEN `TileState` has PDC `danielxxomg:anvillink_repair_sign`, `getLine(0)=="[repair]"`, `getLine(1) in {"HAND","ALL"}`, `getColor()==BLUE`, `update(true,false)` called

#### Scenario: Stale-vs-fresh TileState fallback exercised

- GIVEN bootstrap where `fresh = block.getState()` after `setLine` is not `TileState` but pre-event `state instanceof TileState`
- WHEN `SignChangeEvent` completes via `callEvent`
- THEN fallback writes PDC + BLUE to original `state` and calls `update(true,false)` — covering `if (!wrote && state instanceof TileState)`

#### Scenario: PlayerInteract covers Vault matrix and audit.log

- GIVEN loaded plugin with `Proxy` Vault `Economy` mock (matrix `fractionalDigits` 0,2,-1) and temp `audit.log` under `getDataFolder()`
- WHEN `PlayerInteractEvent` on the registered sign fires via `callEvent` and `ValidatedPrice` settles
- THEN `withdraw` uses gateway amount, tampered text fails closed without charge, only `Success(non-zero)` appends `ISO_INSTANT|uuid|name|HAND|world|toPlainString|count|SUCCESS` to `audit.log`

#### Scenario: Evidence gate for Paper 1.21.5-114

- GIVEN `compatibility/evidence.json`
- WHEN mandatory matrix is evaluated (`GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` green and `smoke.yml` `continue-on-error:false`)
- THEN a row for `Paper 1.21.5-114` / `jdkMajor 25` with `result: pass` MUST exist
