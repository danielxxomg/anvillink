# Changelog

All notable changes to AnvilLink will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Gradle scaffold: pinned wrapper (8.14.3), version catalog, Java 17 toolchain
  with `--release 17` bytecode floor (class major version 61), Spotless
  formatting, Shadow JAR with Adventure relocation.
- Plugin metadata: `plugin.yml` with stable identity, `api-version: 1.13`,
  `softdepend: [Vault]`, and `anvillink.create` / `anvillink.use` /
  `anvillink.manage` permissions.
- Default configuration: fixed price `25.00`, MiniMessage message templates,
  `admin.target-distance: 8`.
- Domain value types: `RepairMode`, `EquipmentSlotId`, `MoneyAmount`,
  `SignRecord` (versioned PDC byte layout) — no Bukkit/Vault dependencies.
- License: GPL-3.0-or-later.
