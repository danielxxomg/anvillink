# Real Vault Provider Pin — Phase 7 (Evidence Deferred to Phase 8)

This document pins the intended real Vault provider for 7.12–7.15. The live Paper
runtime verification is deferred to Phase 8 / manual release; until
`compatibility/evidence.json` exists with a passing EssentialsX row, release claims
are correctly blocked (see `ReleaseClaimGate` and `MissingRealProviderBlocksClaimTest`).

- **Provider**: EssentialsX Economy (official Vault-compatible provider)
- **Source URL**: https://github.com/EssentialsX/Essentials/releases
- **License**: GPL-3.0 (EssentialsX is GPL-3.0; VaultAPI is LGPL — see VaultAPI LICENSE)
- **Note on GPL-2.0 wording in tasks**: EssentialsX is GPL-3.0, not GPL-2.0. The task's
  "GPL-2.0" label is retained as written; this file records the actual upstream license.
- **Pinned version**: _to be filled when Phase 8 downloads the artifact_ (e.g. 2.20.1)
- **SHA-256**: _to be filled from the downloaded JAR at Phase 8_ (`sha256sum EssentialsX-*.jar`)
- **Paper runtime**: Paper 1.21.11 (test) / 1.18.2 floor (prod); JDK 21 for tests, Java 17 bytecode for prod
- **Vault**: 1.7 (compileOnly; runtime via Paper plugins folder)
- **Status**: RED-documented — `compatibility/evidence.json` absent, so
  `ReleaseClaimGate.claimBlockedWhenEvidenceMissing` returns true. The real
  Vault+EssentialsX integration test will run on a live Paper server in Phase 8
  (see `RealVaultProviderEvidenceTest`). This satisfies the negative-evidence contract:
  missing real-provider evidence prohibits release claims.
