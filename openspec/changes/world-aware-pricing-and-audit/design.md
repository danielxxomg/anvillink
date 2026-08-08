# Design: world-aware-pricing-and-audit

## Technical Approach

Extend the hexagonal flow without dependencies. The pure domain owns price invariants,
immutable configuration, world resolution, and audit contracts; Bukkit, file I/O, and scheduling
remain adapters. Ship pricing and audit as two independently testable slices.

## Architecture Decisions

| Decision | Choice | Alternatives and rationale |
|---|---|---|
| Price model | Add pure `WorldPrice(hand, all)` and unmodifiable `ConfigSnapshot.worldPrices`. | A catalog is premature; raw values keep provider precision at activation. |
| Parsing/reload | Extend the hand-rolled nested scan; build a complete snapshot, then swap `AtomicReference`. | Avoids SnakeYAML version skew and preserves retain-prior reloads. |
| World seam | `activate(id, uuid, worldName)` receives a `String` captured on the server thread. | Capturing a `String` fixes the thread boundary; Bukkit `World` breaks purity. |
| Audit | Add pure `AuditPort`; `FileAuditAdapter` synchronously appends fixed `audit.log`, swallowing I/O. Entrypoint calls it after feedback and catches again; format is `ISO_INSTANT|uuid|name|mode|world|toPlainString|count|SUCCESS`. | Async/rotation are follow-ups. |

`RepairActivation` resolves the effective mode price (`worldPrices.get(worldName)` first, then the
global field), validates it with `ValidatedPrice`, and owns the transaction. Its returned
paid `Success` is the audit hook consumed by the entrypoint; failures and `Success(ZERO)` never audit.
`BukkitFeedbackAdapter` is unchanged.

## Data Flow

```
config.yml -> FileConfigurationPort.parseFile(global + worlds) -> immutable ConfigSnapshot
           -> AtomicReference swap
interact -> capture worldName on server thread -> RepairActivation resolver
         -> ValidatedPrice per activation -> one withdraw -> apply -> Success(amount,count)
         -> existing feedback (SchedulerPort, swallowed) -> AuditPort
         -> FileAuditAdapter: mkdirs + CREATE|APPEND to audit.log (double-swallowed)
```

Missing/empty names, unknown worlds, and missing per-world keys use the corresponding global value;
matching is exact and case-sensitive.

## Sequence Diagram

```mermaid
sequenceDiagram
  participant E as AnvilLinkPlugin
  participant R as RepairActivation
  participant C as ConfigSnapshot
  participant V as Vault
  participant F as Feedback
  participant A as FileAuditAdapter
  E->>R: activate(id, uuid, captured worldName)
  R->>C: worldPrices.get(worldName), then global fallback
  alt world has hand only
    C-->>R: HAND=world.hand; ALL=global.all
  else world has all only
    C-->>R: ALL=world.all; HAND=global.hand
  else unknown/null/empty world
    C-->>R: requested mode=global value
  end
  R->>R: ValidatedPrice.of(effective, fractionalDigits)
  R->>V: withdraw once; apply
  V-->>E: Success(amount, count)
  E->>F: feedback.play (swallowed)
  E->>A: record paid success (after feedback)
  A->>A: mkdirs; CREATE+APPEND; swallow
```

## Configuration Schema

```yaml
price:
  hand: 12000.00 # mandatory, finite >= 0
  all: 25000.00 # mandatory, finite >= 0
worlds: # optional; each entry may be partial
  world:
    hand: 5000
  world_nether:
    all: 1000
feedback: # unchanged: enabled, sound, particles
  enabled: true
messages: # unchanged
  repair-success: "<green>Repaired {count} items for {price}.</green>"
# audit path is fixed; there is no audit configuration key
```

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/resources/config.yml` | Modify | Partial `worlds:`, `>=0`; feedback/messages retained. |
| `src/main/java/io/github/danielxxomg/anvillink/domain/ports/ConfigurationPort.java` | Modify | Add `worldPrices`. |
| `src/main/java/io/github/danielxxomg/anvillink/domain/WorldPrice.java` | Create | Pure hand/all pair. |
| `src/main/java/io/github/danielxxomg/anvillink/adapter/FileConfigurationPort.java` | Modify | Nested scan and atomic validation. |
| `src/main/java/io/github/danielxxomg/anvillink/domain/MoneyAmount.java` | Modify | Remove `MIN_PRICE`; keep finite/non-negative. |
| `src/main/java/io/github/danielxxomg/anvillink/domain/ValidatedPrice.java` | Modify | Enforce `>=0` and activation scale. |
| `src/main/java/io/github/danielxxomg/anvillink/domain/RepairActivation.java` | Modify | World resolver and `worldName` seam. |
| `src/main/java/io/github/danielxxomg/anvillink/domain/ports/AuditPort.java` | Create | Pure audit contract. |
| `src/main/java/io/github/danielxxomg/anvillink/adapter/FileAuditAdapter.java` | Create | Fixed append-only `audit.log`. |
| `src/main/java/io/github/danielxxomg/anvillink/entrypoint/AnvilLinkPlugin.java` | Modify | Capture world/name; audit after feedback. |
| `openspec/changes/world-aware-pricing-and-audit/specs/` | Modify/Create | Pricing and audit deltas. |

## Interfaces / Contracts

```java
public record WorldPrice(BigDecimal hand, BigDecimal all) {}
record ConfigSnapshot(BigDecimal priceHand, BigDecimal priceAll,
    Map<String, WorldPrice> worldPrices, int targetDistance, Map<String,String> messages,
    boolean activationEnabled, boolean feedbackEnabled, String feedbackSound,
    String feedbackParticles) {}
public interface AuditPort {
  record AuditEntry(Instant timestamp, UUID playerUuid, String playerName, RepairMode mode,
      String worldName, BigDecimal price, int repairedCount, String result) {}
  void record(AuditEntry entry);
}
TransactionResult activate(SignPort.SignId id, UUID player, String worldName);
public FileAuditAdapter(File auditFile) { /* always dataFolder/audit.log */ }
```

## Testing Strategy and Failure Modes

Use RED-GREEN-REFACTOR despite `strict_tdd: false`: unit-test `>=0`, fallback, partial entries,
case, and scale; adapter-test malformed values, unknown keys, atomic retention, `mkdirs`, format,
append, and swallowed I/O; integration-test one withdrawal, zero/no audit, paid-success order, and
feedback. A present malformed world `hand`/`all` (unparseable, negative, non-finite)
invalidates the whole file: startup disables activation and reload retains prior. Valid partial
entries are lenient; activation precision failure makes no withdrawal. Adapter and caller both
swallow exceptions, isolating transaction, compensation, and feedback.

Domain remains free of Bukkit/Vault/Adventure/config types. `SchedulerPort` keeps feedback and the
audit hook on the server thread; the audit write is synchronous in v1 and may stall a tick.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or external
process-integration boundary; audit is in-process file I/O.

## Migration / Rollout

BREAKING rollout: `>=0` replaces `MIN_PRICE=10000`; defaults remain valid; `worlds:` is optional.
Rollback restores `MIN_PRICE`/validation, removes `worlds:` resolution, and deletes/archives
`audit.log`; no PDC migration. Slice 1 is transactional pricing (<=400 production lines); slice 2
is audit observability (<=400), within the 800-line gate.

## Open Questions

None.
