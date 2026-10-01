# ci-shared-workflows

Reusable GitHub Actions workflows for the estate. Callers pin a major tag (`@v3`, `@v4`).
`versions/v3/` keeps the previous major for reference; the live `v3` tag still points at the old workflow so
callers keep building until they migrate.

- **Owner:** `@ardencm/platform-eng`
