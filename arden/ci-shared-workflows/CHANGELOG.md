# Changelog

## v4 — 2026-09-05
- `java-build`, `python-test`, `node-test`: Artifactory auth via GitHub OIDC token exchange (short-lived tokens).
- `ARTIFACTORY_TOKEN` secret is now optional and ignored when OIDC succeeds. Remove it from callers.
- Motivation: CVE-2026-82329 response — all identity tokens minted before 28 Aug 2026 are being revoked.

## v3 — 2025-03-12
- Java 17 default, Maven cache.
