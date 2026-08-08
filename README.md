# AnvilLink

Paid repair signs for Paper servers — players create `[repair]` signs (HAND or ALL), right-click to repair equipment for a single flat Vault charge, with deterministic compensation when things go wrong.

## Quick start

1. Require Paper 1.18.2+ and Vault + a Vault economy (EssentialsX Economy recommended).
2. Drop `anvillink-*.jar` into `plugins/`, restart.
3. Give `anvillink.create` to builders, `anvillink.use` to players (default true), `anvillink.manage` to admins.
4. Place a sign: line 1 `[repair]`, line 2 `HAND` or `ALL` (case-insensitive) on the front side — it turns blue and registers.
5. Right-click the sign with the main hand to repair. Economy is charged once (per-mode price: `price.hand 12000` / `price.all 25000`, each `>= 0`, optional `worlds:` overrides per world with global fallback) on success; otherwise the repair is cancelled and equipment is preserved.

## Support tiers (evidence-gated)

Support labels follow `compatibility/evidence.json`. A tier is advertised only when its smoke row is `pass` for the exact server build and JDK below; Paper 26.x stays uncertified until its Java 25 job passes.

| Distribution | Version | Build | JDK | Tier |
|---|---|---:|---:|---|
| Paper | 1.18.2 | 388 | 17 | Certified |
| Paper | 1.20.6 | 151 | 21 | Certified |
| Paper | 1.21.11 | 132 | 21 | Certified |
| Spigot | 1.20.6 | BuildTools #200 `--rev 1.20.6` | 21 | Verified |
| Purpur | 1.20.6 | 2233 | 21 | Verified |
| Paper | 26.2 | 102 | 25 | Probe only — uncertified |
| Folia | any | — | — | Experimental (unsupported) |

Mandatory rows (Paper 1.18.2/J17, 1.20.6/J21, 1.21.11/J21, Spigot 1.20.6/J21, Purpur 1.20.6/J21) must all be `pass` for certification; probe failure (Paper 26.x/J25) does not block certified ranges. See `compatibility/evidence.json` (`distribution`, `version`, `build`, `serverSha256`, `jdkMajor`, `testSuite`, `result`).

## Permissions

| Node | Default | Grants |
|---|---|---|
| `anvillink.create` | op | Create authorized `[repair]` signs |
| `anvillink.use` | true | Use registered signs (right-click repair) |
| `anvillink.manage` | op | Break/edit registered signs, `/anvillink inspect`/`rerender`/`reload` |

Signing identity is the PDC key `danielxxomg:anvillink_repair_sign` (versioned byte array); display text alone has no authority. Tampered text is rejected; `rerender` restores canonical `[repair]`/`HAND|ALL` with blue dye when PDC is valid.

## Configuration (`plugins/AnvilLink/config.yml`)

> BREAKING (MAJOR v0.3.0): `price` is per-mode `price.hand` + `price.all` (each `>= 0`, flat scalar `price: 25.00` is INVALID) with optional `worlds:` overrides (`worlds.<world>.hand/all` partial, missing key → global, case-sensitive exact, malformed `worlds:` entry fails whole file closed). Migration: `price` floor relaxed `>=10_000`→`>=0`; `worlds:` is optional. Invalid startup → `activationEnabled=false` until fixed; invalid reload retains prior valid config atomically.

```yaml
price:
  hand: 12000.00 # mandatory >= 0, per HAND (1 slot)
  all:  25000.00 # mandatory >= 0, per ALL (6 slots)
worlds: # optional per-world overrides (partial); example commented
#  world:
#    hand: 5000
#  world_nether:
#    all: 1000
feedback:
  enabled: true
  sound: BLOCK_ANVIL_USE
  particles: CRIT
admin:
  target-distance: 8   # 1–32, line-of-sight for /anvillink inspect|rerender
messages:
  repair-success: "<green>Repaired {count} items for {price}.</green>" # {count}=repaired slots, {price}=amount.toPlainString()
  no-target: "<red>No sign in sight."
  not-registered: "<red>Not a registered repair sign."
  invalid-identity: "<red>Invalid sign identity."
  tampered: "<red>Sign text tampered — use /anvillink rerender."
  # ... MiniMessage templates (reload with /anvillink reload)
```

Reload: `/anvillink reload` (manage). Valid reload swaps atomically; invalid reload keeps the prior config and reports failure. Invalid startup disables activation until fixed. `feedback.enabled=false` silences sound/particles/message for paid `repair-success` (still gated on `amount != ZERO`); feedback failures are swallowed and never trigger compensation. Paid `Success` also appends `plugins/AnvilLink/audit.log` (`ISO_INSTANT|uuid|name|HAND/ALL|world|toPlainString|count|SUCCESS`, fixed path `CREATE|APPEND`+`mkdirs`, unbounded — rotate by renaming/deleting, cleartext IDs, operator owns GDPR).

## Testing

Full manual guide for `v0.3.0`: [`docs/TESTING-v0.3.0.md`](docs/TESTING-v0.3.0.md) — permisos, `config.yml` (`worlds:` + floor `>=0`), `audit.log`, feedback, tamper, y checklist.

## FAQ

**Does it use NMS or modify the anvil?** No. Only public Paper/Bukkit APIs, Vault's `Economy` port, and a single relocated Adventure (MiniMessage 4.11.0) for config messages. Signs use Bukkit color strings, not MiniMessage.

**How is charging handled?** One `withdrawPlayer` per activation; price scale is validated against `Economy#fractionalDigits`. Insufficient funds, missing provider, or invalid provider response fails closed with no mutation. On apply failure, already-mutated slots are restored from snapshots and a compensating deposit is attempted; failures are reported as high-severity operational events.

**Why is Paper 26.x uncertified?** It requires a passing Java 25 smoke job in `compatibility/evidence.json`. Until that probe row is `pass`, 26.x is listed but not certified; certified tiers remain valid.

**Build floor?** Production compiles to Java 17 (`--release 17`, class major 61) against Paper 1.18.2. Paper/Vault are `compileOnly` and never shaded; only Adventure is shaded/relocated to `io.github.danielxxomg.anvillink.libs.kyori`.

## License

GPL-3.0-or-later. See `LICENSE`.

<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "AnvilLink",
  "applicationCategory": "GameApplication",
  "operatingSystem": "Paper 1.18.2+",
  "license": "https://www.gnu.org/licenses/gpl-3.0.html",
  "softwareVersion": "0.3.0",
  "url": "https://github.com/danielxxomg/anvillink",
  "description": "Paid repair signs for Minecraft Paper servers — fixed-price HAND/ALL equipment repair via Vault.",
  "author": { "@type": "Person", "name": "danielxxomg" }
}
</script>
