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
