# Delta for audit-log

## ADDED Requirements

### Requirement: Paid activation audit to fixed append-only log

The plugin MUST append one line to fixed `plugins/AnvilLink/audit.log` (relative to data folder; `mkdirs()` + `CREATE+APPEND`) ONLY on paid `TransactionResult.Success` where `amount != ZERO`. Each line MUST contain ISO-8601 instant (`Instant.now`/`OffsetDateTime`), `playerUuid`, `playerName`, `mode` (`HAND`/`ALL`), `worldName`, `price.toPlainString()`, `repairedCount`, discriminator `SUCCESS` (format `ISO_INSTANT|uuid|name|HAND/ALL|world|toPlainString|count|SUCCESS`). Non-paid `Success(ZERO)` (empty plan) and all failures (`InsufficientFunds`, `NoProvider`, `InvalidResponse`, `CompensationFailed`, `RestorationFailed`) MUST NOT be audited. Audit I/O MUST be best-effort swallowed — failure MUST NOT affect transaction, compensation, or feedback (double-swallow: `FileAuditAdapter` swallows `IOException` and caller swallows). File grows unbounded, no DB/auto-rotation in v1; operator rotates by renaming/deleting, plugin recreates on next paid success. Privacy: cleartext `UUID`+`name` retained, operator owns GDPR retention/deletion.

#### Scenario: Paid HAND success audits one line with correct fields
- GIVEN `HAND` activation with `world=world`, `price=5000`, `repairedCount=1`, `Success(amount=5000)`
- WHEN audit dispatched after feedback
- THEN one line MUST be appended with `mode=HAND`, `world=world`, `price=5000` via `toPlainString()`, `count=1`, `result=SUCCESS`, and ISO instant

#### Scenario: Paid ALL success audits
- GIVEN `ALL` activation with `Success(amount=25000, repairedCount=4)` in `world_nether`
- WHEN audit dispatched
- THEN one line MUST be appended with `mode=ALL`, `world=world_nether`, correct fields

#### Scenario: Zero and empty plan not audited
- GIVEN `Success(ZERO)` from empty/ineligible plan
- WHEN activation completes
- THEN no audit line MUST be written and no file created if absent

#### Scenario: Failures not audited
- GIVEN `InsufficientFunds` or `NoProvider` or `InvalidResponse` (including per-activation scale failure)
- WHEN activation completes
- THEN no audit line MUST be written

#### Scenario: Audit IOException swallowed transaction still Success
- GIVEN paid `Success` where `Files.writeString` throws `IOException`
- WHEN audit attempted
- THEN exception MUST be swallowed, transaction MUST remain `Success`, no compensation or feedback retry

#### Scenario: Audit line uses toPlainString not scientific notation
- GIVEN `price=1000` or large `price=1000000`
- WHEN audited
- THEN line MUST contain `toPlainString()` (e.g. `1000`, not `1E3`)
