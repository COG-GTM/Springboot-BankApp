# cascade/batch-scheduler-config — EOD / month-end job definitions for the DNA core (Cascade)

Declarative job definitions for the batch scheduler that drives the core's PL/SQL (today) and PL/pgSQL (after the
migration). One YAML per job under `jobs/`; `validate.py` checks shape, dependency order and that every target is a
procedure the migration inventory knows about. This is the Cascade-side caller of the tranche-1 packages, so it is
part of the migration inventory (`bk-migration-manifest.md` → callers).

| job | invokes | when | migration note |
|---|---|---|---|
| `eod-interest-accrual` | `pkg_interest_engine.accrue_daily_interest` | daily 23:30 | tranche 1 — target switches to `SELECT * FROM pkg_interest_engine.accrue_daily_interest(:as_of)` |
| `month-end-fee-assessment` | `pkg_fee_processing.assess_monthly_fees` | last day of month 23:45 | tranche 1 — DR-FEE-001: `fee_parameters` row replaces package state; a per-run override is **[product to decide]** |
| `month-end-interest-capitalization` | `pkg_interest_engine.capitalize_interest` per SAV/MMA account | last day of month 23:50 | fixture assumption **[core operations to confirm]** |
| `eod-gl-trial-balance` | `pkg_gl_posting.gl_balance` per GL code | daily 23:55 | DEV-GL-001: also asserts `gl_balance_summary == gl_balance` |
| `disclosure-refresh` | `cascade/disclosure-service` reload | daily 00:10 | consumer of the migrated numbers |

Run `python3 validate.py` (no dependencies). No credentials: the `connection` field is a named profile, resolved by the scheduler.
