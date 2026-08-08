# AnvilLink Engineering Rules

## Platform

- Target Java 17 bytecode and compile against the Paper 1.18.2 API floor.
- Use public Bukkit-compatible APIs only; do not use NMS or reflection for version compatibility.
- Keep Paper and Vault APIs as `compileOnly` dependencies and never package host API classes.

## Architecture

- Keep domain code independent from Bukkit, Paper, Vault, configuration, and presentation types.
- Depend inward through explicit ports; adapters implement those ports.
- Preserve the permanent package and PDC identities defined by the OpenSpec design.

## Quality

- Write behavior-first tests before implementation changes.
- Run `./gradlew test`, `./gradlew spotlessCheck`, and `./gradlew build` before delivery.
- Keep generated outputs (`build/`, `.gradle/`) out of version control.
- Keep each work unit independently testable and within its approved review budget.

## Delivery

- Use conventional commit messages without AI attribution.
- Do not commit, push, create pull requests, tag, or release without explicit maintainer authorization.
- Never commit credentials, environment values, local caches, or private machine paths.
