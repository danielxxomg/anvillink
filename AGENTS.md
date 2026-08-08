# AnvilLink Engineering Rules

## Platform — Validates: `build.gradle.kts`, `gradle/libs.versions.toml`, `src/main/resources/plugin.yml`

- Java 17 bytecode: `java { toolchain { languageVersion = 17, vendor = ADOPTIUM } }` and `tasks.withType<JavaCompile> { options.release = 17 }` in `build.gradle.kts`; bytecode major 61 enforced by `src/test/java/io/github/danielxxomg/anvillink/descriptor/BytecodeFloorTest.java`.
- Host floor: `libs.paper.api = "1.18.2-R0.1-SNAPSHOT"` and `libs.vault.api = "1.7"` pinned in `gradle/libs.versions.toml` and consumed as `compileOnly(libs.paper.api)` / `compileOnly(libs.vault.api)` in `build.gradle.kts`; never shaded or packaged.
- Shading only Adventure: `libs.adventure.minimessage = "4.11.0"` as `implementation(libs.adventure.minimessage)` relocated to `io.github.danielxxomg.anvillink.libs.kyori` in `tasks.shadowJar`; `tasks.shadowJar.dependencies` must exclude `io.papermc.paper:paper-api` and `com.github.MilkBowl:VaultAPI`.
- Descriptor: `src/main/resources/plugin.yml` keeps `api-version: 1.13`, `main: io.github.danielxxomg.anvillink.entrypoint.AnvilLinkPlugin`, and `${version}` expansion via `tasks.processResources` / `tasks.shadowJar`.
- Public API only: `net.minecraft.server`, `org.bukkit.craftbukkit`, `sun.*`, and reflection-based version compat (`Class.forName("org.bukkit.craftbukkit.*")`, `getDeclaredMethod` on NMS) are prohibited anywhere.

## Architecture — Validates: `src/main/java/io/github/danielxxomg/anvillink/domain/**/*.java` vs `adapter/**`

- Domain is pure Java: `src/main/java/io/github/danielxxomg/anvillink/domain/` owns `RepairMode`, `EquipmentSlotId`, `MoneyAmount`, `SignRecord`, `SignParser`, `RepairPlanner`, `RepairPlan`, `PlannedSlot`, `ItemSnapshot`, `ItemView`, `EquipmentView` (and future pure types); no Bukkit/Paper/Vault/config/presentation types.
- Ports are explicit: `SignPort`, `EquipmentPort`, `EconomyPort`, `SchedulerPort`, `ConfigurationPort`, `MessagePort` (under `domain`/`domain.ports`); adapters in `adapter.*` implement ports, domain never imports adapters.
- Vetoed imports in `domain/**`: `org.bukkit.*`, `net.milkbowl.*`, `io.papermc.*`, `net.minecraft.*`, `org.bukkit.craftbukkit.*`, `net.kyori.*` (Adventure), Vault `Economy`, and Bukkit config types (`ConfigurationSection`, `YamlConfiguration`) are prohibited.
- Inward dependency: domain has zero dependency on Bukkit, Paper, Vault, configuration, or presentation; all dependencies point inward through ports.
- Permanent identities: base package `io.github.danielxxomg.anvillink` and PDC namespace/key defined by the OpenSpec design are immutable — do not rename or move them.

## Quality — Validates: `src/*/java/**/*.java`, `*.gradle.kts`, `src/test/java/**/*Test.java` via `spotlessCheck`/`test`/`build`

- Behavior-first TDD: changes to `src/main/java/**/*.java` require a `src/test/java/**/*Test.java` describing behavior before implementation.
- Canonical verification (run before delivery): `GRADLE_USER_HOME="$PWD/.gradle" mise x java@21.0.2 -- ./gradlew clean test spotlessCheck build` — covers tests, formatting, and `shadowJar` assembly.
- Formatting: `spotless { java { googleJavaFormat("1.17.0") + licenseHeader + trimTrailingWhitespace } kotlinGradle { ktlint("1.0.1") } }` in `build.gradle.kts`; `*.java` and `*.gradle.kts` must pass `spotlessCheck`.
- Bytecode and descriptor floor: `src/test/java/io/github/danielxxomg/anvillink/descriptor/BytecodeFloorTest.java` asserts major 61 (Java 17) on the assembled JAR; `PluginDescriptorTest.java` parses `src/main/resources/plugin.yml`.
- Generated outputs excluded: `build/`, `.gradle/`, `.codegraph/codegraph.db`, `*.class`, `*.jar`, `gradle/wrapper/*` are ignored via `.gitignore` + `.gga EXCLUDE_PATTERNS` and must never be staged.
- Review budget: keep each review candidate ≤ 400 lines (4R) and each slice ≤ 1200 total; each work unit must stay independently testable.

## Delivery — Validates: staged `git` changes, commit messages, and remote actions

- Local commit allowed: use Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`, `build:`) without AI attribution or `Co-Authored-By`.
- Remote blocked: `git push`, `gh pr create`, `git tag`/`git push --tags`, and releases require explicit maintainer authorization AND `gentle-ai review validate` passing; pre-commit `gga run || exit 1` (`.git/hooks/pre-commit`) is not a substitute.
- Branch boundary: tracker is `feat/anvillink/slice-1-scaffold` (SDD auto-chain); do not create, switch, or push other branches without maintainer approval.
- Secrets never: do not commit credentials, `.env`, tokens, keystores, `local.properties`, environment values, or private machine paths; applies to staged `*.properties`, `*.yml`, `*.yaml`, `*.toml`, `*.md`.
