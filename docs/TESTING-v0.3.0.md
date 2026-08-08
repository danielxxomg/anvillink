# AnvilLink — Guía de pruebas v0.3.0 (BREAKING)

> Unifica `v0.2.0 BREAKING` (per-mode) + `v0.3.0 BREAKING` (`worlds:` + floor `>=0` + `audit.log`).
> Probá sobre Paper 1.18.2+ / 1.20.6 / 1.21.11 con Vault + EssentialsX Economy (o cualquier `Economy`).

---

## 1. Instalación y versión

```bash
# Release JAR (Paper/Vault nunca shadeados, Adventure relocado)
# v0.3.0 — 533K
wget https://github.com/danielxxomg/anvillink/releases/download/v0.3.0/anvillink-0.3.0.jar
# en el server
cp anvillink-0.3.0.jar plugins/
# requiere Vault + economía; reiniciá
```

Verificá en `logs/latest.log`: `AnvilLink enabled.` + sin `Invalid initial configuration`. En `plugins/AnvilLink/config.yml` debe figurar:

```yaml
price:
  hand: 12000.00 # mandatory >= 0
  all: 25000.00 # mandatory >= 0
worlds: # opcional, comentado por defecto
#  world:
#    hand: 5000
#  world_nether:
#    all: 1000
feedback:
  enabled: true
  sound: BLOCK_ANVIL_USE
  particles: CRIT
```

Si venís de `v0.2.0` con `price.hand/price.all >= 10000` tu `config.yml` ya funciona. Si venís de `v0.1.0` con `price: 25.00` escalar, el plugin fail-closed (`activationEnabled=false`, warning al inicio) hasta que edites a `price.hand/all`.

---

## 2. Permisos (de `plugin.yml`)

| Node | default | Qué hace |
|---|---|---|
| `anvillink.create` | `op` | Crear carteles `[repair]` autorizados |
| `anvillink.use` | `true` | Usar cartel (reparar) — todos pueden por defecto |
| `anvillink.manage` | `op` | Romper/editar cartel registrado + `/anvillink inspect\|rerender\|reload` |

Solo `true` es para jugador normal; el resto `op`.

Para negar uso a un grupo: `lp group default permission set anvillink.use false` (LuckPerms) o similar. Para testear denegación: quitá `anvillink.use` al tester y validá que el click no cobra ni repara (sección 4.8).

---

## 3. Comandos

| Comando | Permiso | Qué |
|---|---|---|
| `/anvillink inspect` | `anvillink.manage`, solo jugador | Reporta si el cartel mirado (1–32 bloques, `target-distance`) es registrado y si está tamperado |
| `/anvillink rerender` | `anvillink.manage`, solo jugador | Restaura texto canónico azul `[repair]`/`HAND|ALL` si el PDC es válido |
| `/anvillink reload` | `anvillink.manage` (consola/jugador) | Recarga `config.yml` atómico: inválido retiene previo y reporta `reload-failure`; válido hace swap |

Todos mandan `no-permission` si falta `anvillink.manage`, y `no-target`/`not-registered`/`invalid-identity`/`tampered` según el caso.

---

## 4. Qué probar — por feature

### 4.1 Pricing global per-mode (base `v0.2.0`)

1. Con defaults `hand 12000 / all 25000`, equipá un item dañado en mano y otro set en `world` → cartel `HAND` debe cobrar `12000` (`repairedCount=1`), cartel `ALL` debe cobrar `25000`.
2. Con manos vacías / solo items sin daño → `no-eligible-items` (`<yellow>No damaged repairable items to repair.</yellow>`), **cero** cobro, **sin** `audit.log`.

### 4.2 `worlds:` — el nuevo en `v0.3.0`

**Setup:** en `plugins/AnvilLink/config.yml` descomentá y editá:

```yaml
worlds:
  world:
    hand: 5000
  world_nether:
    all: 1000
```

`/anvillink reload` → `reload-success` (`<green>Configuration reloaded.</green>`). Si el reload fuese inválido verías `reload-failure` y se retiene el previo (nada cambia).

| Caso | Mundo | Cartel | Esperado |
|---|---|---|---|
| hand-only | `world` | `HAND` | cobra `5000` (override), `repairedCount` correcto |
| hand-only fallback | `world` | `ALL` | cobra global `25000` (fallback) |
| all-only | `world_nether` | `ALL` | cobra `1000` |
| all-only fallback | `world_nether` | `HAND` | cobra global `12000` |
| ambos | `world_the_end` con `hand: 100, all: 200` | `HAND`/`ALL` | `100` / `200` |
| desconocido | `world_unknown` | cualquiera | cobra global (`12000`/`25000`) |
| case-sensitive | `World` (mayúscula) con `world: hand 5000` | `HAND` | cobra global (exact `Map.get`) |
| piso relajado | `price.hand: 0` o `world: hand: 100` | `HAND` | ahora válido `>=0` — debe cobrar `0` o `100` (antes `v0.2.0` lo rechazaba) |
| escala | `world: hand: 100.001` con `fractionalDigits=2` (Vault típico) | `HAND` | fail-closed `activation-failure` con `invalid-price:…` / `InvalidResponse`, **cero** cobro, el otro modo sigue válido |

**Cómo verificar cobro:** `/eco balance <player>` antes/después (EssentialsX). Cada activación exitosa hace **un solo** `withdrawPlayer`.

### 4.3 Fail-closed de `worlds:` (whole-file)

Si un valor presente en `worlds.<world>.hand|all` está mal, **todo el archivo** falla y se retiene el previo:

```yaml
worlds:
  world:
    hand: -1       # negativo → invalid file
  # o: hand: abc   # unparseable
  # o: hand: Infinity
```

`/anvillink reload` → `reload-failure` con `worlds.world.hand: …`. Las activaciones siguen usando el `worlds:` previo válido. Claves desconocidas (`worlds.world.handd: 5000`) se ignoran con warning y el faltante cae a global (no inválida el archivo).

### 4.4 Feedback `repair-success`

Solo en `Success` pagado (`amount != ZERO`), post-transacción. En cada éxito pagado esperá:

* Mensaje: `repair-success` → `<green>Repaired {count} items for {price}.</green>` donde `{count}=repairedCount`, `{price}=amount.toPlainString()` (ej `1`/`25000`, no `1E4`).
* Sonido Bukkit `feedback.sound` (default `BLOCK_ANVIL_USE`) + partículas `feedback.particles` (default `CRIT`), ambos en el hilo del server vía `SchedulerPort`.
* Con `feedback.enabled: false` → silencio (sin mensaje/sonido/partículas), la reparación igual sucede.
* Con items vacíos / sin daño + con `enabled: true` igual no hay feedback (es `Success(ZERO)`, no pago).

### 4.5 `audit.log` fijo (nuevo `v0.3.0`)

Solo `Success` pagado genera una línea en `plugins/AnvilLink/audit.log` (fijo, `mkdirs`+`CREATE|APPEND`, nunca configurable). Formato:

```
ISO_INSTANT|uuid|name|HAND|world|toPlainString|count|SUCCESS
# ej: 2026-08-08T20:00:00Z|123e4567-...|danielxxomg|HAND|world|5000|1|SUCCESS
```

| Caso | Audita? |
|---|---|
| `Success` pagado `HAND`/`ALL` por mundo | sí, una línea con modo+`worldName` exacto+`toPlainString`+`count` |
| `Success(ZERO)` (vacío/sin daño) | no, no se crea `audit.log` si no existía |
| `InsufficientFunds` (`<red>You do not have enough funds.</red>`) | no |
| `NoProvider` / `InvalidResponse` (Vault ausente, escala inválida, tampered) | no |
| `IOException` al escribir | no propaga — doble-swallow, la transacción queda `Success`, sin retry/compensation, la línea se pierde (fail silent como `feedback`) |

**Operar el log:** crece sin límite en `v1` — rotalo renombrando/borrando `audit.log`; la próxima reparación pagada lo recrea (`CREATE`). Es claro (`UUID`+nombre), el operador es dueño de GDPR (no compartir sin scrubbing).

### 4.6 Carteles y tamper

* Crear: línea 1 `[repair]` (case-insensitive, corchetes obligatorios) + línea 2 `HAND`|`ALL` (case-insensitive, solo esos dos) con `anvillink.create` sobre un cartel en pared/pie. Al crear se escribe PDC `danielxxomg:anvillink_repair_sign` (`ALR|schema=1|mode|creator UUID|authorized=1`) y el texto queda azul canónico.
* Tamper: editar línea 1/2 a mano → al click válida PDC pero falla `frontText` vs PDC → mensaje `tampered` (`<red>That sign's text has been tampered with and is not usable.</red>`), cero cobro. `/anvillink rerender` (mirando, `anvillink.manage`) restaura canónico si el PDC sigue válido.
* Solo `ALL` toca hasta 6 slots de equipo (`main`, `off`, `helmet`, `chest`, `leggings`, `boots`); nunca baúl. Solo `Damageable` con `damage > 0` se cuenta; si no hay elegibles es `no-eligible-items` sin cobro.
* Break/editar cartel registrado pide `anvillink.manage`.

### 4.7 Recarga y distancias

* `admin.target-distance: 8` (1–32). Inválido (`0`, `33`, no numérico) fail-closed al reload.
* Flat scalar `price: 25.00` sigue INVALID (fail-closed) — escalar global.
* Falta `price.hand` o `price.all` → invalid file closed.
* Negativo / no-finito global → invalid. `0` / `100` con `fractionalDigits=2` ahora válidos.
* Precisión por provider: `price.hand: 100.001` con `fractionalDigits=2` (Vault fd2) es validado en activación, no en parse — produce `activation-failure` sin cobro.

### 4.8 Permisos negativos

* Sin `anvillink.use` → el `onPlayerInteract` hace return antes de `RepairActivation`; ningún `withdraw`, ningún `audit`, ningún `feedback`, sin mensaje (filter temprano). Verificalo quitando el permiso al tester.
* Sin `anvillink.manage` → `/anvillink inspect|rerender|reload` responde `no-permission`, el cartel sigue usable si tiene `anvillink.use`.

---

## 5. Checklist rápido para el tester

```
[ ] HAND global cobra price.hand, ALL cobra price.all
[ ] HAND con 0 elegibles → no-eligible-items, 0 cobro, sin audit
[ ] HAND en world con hand-only 5000 cobra 5000, ALL en world cobra global 25000
[ ] ALL en world_nether con all-only 1000 cobra 1000, HAND cae a global
[ ] mundo desconocido / "World" case-mismatch / null → global
[ ] reload con worlds.world.hand=-1 → reload-failure, se retiene previo
[ ] price.hand: 100 / 0 válido (>=0); 100.001 con fd2 → activation-failure sin cobro
[ ] repair-success con count/price toPlainString solo en Success pagado
[ ] feedback.enabled=false → sin mensaje/sonido/partículas, reparación ok
[ ] audit.log una línea ISO|uuid|name|HAND/ALL|world|toPlainString|count|SUCCESS por éxito pagado; nada en vacíos/fallos
[ ] audit.log rota renombrando y se recrea al próximo pago
[ ] [repair]/HAND|ALL case-insensitive al crear → azul; tamper → tampered; rerender restaura
[ ] sin anvillink.use → no opera; sin anvillink.manage → inspect/rerender/reload no-permission
```

---

## 6. Requisitos del servidor

Paper 1.18.2+ (certificado 1.18.2/388/J17, 1.20.6/J21, 1.21.11/J21), Vault + economía, Java 17 prod (`--release 17`, major 61), tests sobre JDK 21 (MockBukkit 4.110). Paper/Vault `compileOnly` nunca shadeados; solo Adventure MiniMessage 4.11.0 relocado a `anvillink.libs.kyori`.

CI `build` + `smoke` (388/J17, 151/J21, 132/J21, Spigot #200/J21, Purpur 2233/J21) debe quedar `pass`; probe Paper 26.2/102/J25 es `fail` esperado no bloqueante en v0.3.0.

---

## 7. Archivos y release

* Release: https://github.com/danielxxomg/anvillink/releases/tag/v0.3.0 — `anvillink-0.3.0.jar 533K 97b1315f`
* Tracked branch: `feat/anvillink/slice-1-scaffold` (Scaffold sí es worktree CodeGraph: `../plugin-repair-worktrees/scaffold` si necesitás index)
* Specs: `openspec/specs/repair-economy/spec.md` (189 líneas) + `openspec/specs/audit-log/spec.md` (41 líneas) — fuente para escenarios 22/22 (16 economy + 6 audit)

¿Dudas con algún caso? Decime el mundo/config que querés probar y te digo la linea YAML exacta.
