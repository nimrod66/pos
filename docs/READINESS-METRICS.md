# Readiness Metrics Tracker

This tracker maps commercial-readiness KPIs to the current codebase and highlights likely issues/gaps.

## Current automated checks

Last checked locally from repository root:

- Backend: `./mvnw test` — PASS, 29 tests
- Frontend: `cd pharmacy-frontend && npm test` — PASS, 50 tests

## Metric coverage

| Metric | Current source / endpoint | Status | Likely issue or gap |
| --- | --- | --- | --- |
| Completed sales count | `GET /api/v1/reports/dashboard`, `/sales-summary` | Tracked | Needs daily branch trend/target view for owners. |
| Gross/net sales | `DashboardReportDto`, `SalesReportDto`, `FinancialSummaryDto` | Tracked | Good business coverage. |
| Refunds/returns | Sales and financial reports | Tracked | Needs exception alerts for high return rates. |
| Payment split: cash/M-Pesa/other/credit | `SalesReportDto`, `FinancialSummaryDto` | Tracked | Needs payment failure and reconciliation metrics. |
| Top products / PLU | `/reports/sales-summary`, `/reports/plu` | Tracked | Good for product movement. |
| Profit/margin/COGS | `/reports/profit`, `/reports/financial-summary` | Tracked | Accuracy depends on batch buying price and stock movement integrity. |
| Expenses by category | `/reports/financial-summary` | Tracked | Needs cash-flow dashboard integration. |
| Low stock | `/reports/dashboard`, `/inventory-summary` | Tracked | Needs SLA metric: time until reordered/resolved. |
| Stock value | `/reports/inventory-summary` | Tracked | Good. Needs valuation audit after stock adjustments. |
| Near-expiry/expired stock | Dashboard and inventory report | Tracked | Needs alert workflow/owner notification. |
| Slow/dead stock | `/reports/slow-stock` | Tracked | Good. Needs UI action path for markdown/transfer/return to supplier. |
| Reorder suggestions | `/reports/reorder-suggestions` | Tracked | Suggestion logic is basic; needs supplier lead time and min/max rules. |
| Supplier price comparison | `/reports/supplier-prices` | Tracked | Good procurement metric. |
| Customer sale history/value | `/reports/customer-history/{id}` | Tracked | Needs privacy/access review for real deployment. |
| API/database/disk/memory/pool health | `/api/v1/system/health`, Actuator health/metrics | Partially tracked | No historical telemetry; only snapshot. |
| Backup count/last backup/age | `/api/v1/system/health`, backup service | Partially tracked | Needs backup success/failure log and restore-test status. |
| Sync online/latency/mode | `/api/v1/system/health` via `ConnectivityService` | Partially tracked | Does not measure offline queue age, retry count, stuck events. |
| Terminal online/active count | `/api/v1/system/health`, terminal heartbeat | Partially tracked | Needs per-terminal stale duration and peripheral failure rates. |
| Printer/scanner/drawer/display health | Terminal peripheral health | Partially tracked | Local connector failures are mostly client-side and not persisted as metrics. |
| Login failures | Migration `V18__auth_failed_login.sql`, auth/login history modules | Partially tracked | Needs dashboard counters/lockout alerts. |
| Permission denied events | Security/audit modules | Weak/unknown | Confirm 403s are audited consistently. |
| Checkout success/failure rate | Sales persisted, errors not consistently counted | Weak | Need counters for attempted checkout, validation failure, stock failure, payment failure. |
| Offline queued sales | Frontend `localStorage` key `pharmacy-pos:offline-queue` | Weak | Client-only queue; no server visibility, no CSRF/idempotency-safe replay through API client. |
| Installer success rate | Installer scripts/logs | Not tracked | Need install telemetry/log parsing or local diagnostic bundle. |
| Restore drill success | Backup/restore scripts | Not tracked | Need periodic restore verification metric. |
| Hardware connector uptime | Python connector + frontend health polling | Not tracked historically | Need persisted heartbeat/failure events. |
| Real-world pilot validation | Docs/scripts only | Not tracked | Need site checklist and sign-off records. |
| Support tickets/incidents | Not in app | Not tracked | Need external tracker or simple incidents table. |

## Highest-risk issues found

1. **Offline sale queue is fragile.** `pharmacy-frontend/src/features/pos/components/pos-page.tsx` stores offline sales in browser `localStorage` and replays with raw `fetch('/api/v1/sales')`. This bypasses the shared `apiRequest` client, so CSRF header, configured API base URL, credentials behavior, envelope handling, and idempotency header handling are likely wrong.
2. **Operational metrics are snapshots, not time series.** `/api/v1/system/health` exposes useful current health, but no historical trend, alerting, SLA, or incident tracking.
3. **Checkout failures are not first-class metrics.** Successful sales are counted, but failed attempts by reason are not tracked.
4. **Payment readiness is incomplete.** M-Pesa callbacks have handling and optional HMAC, but there is no visible reconciliation dashboard for pending/failed/stale payments.
5. **Backup readiness is partial.** Last backup age is checked, but restore validation and backup failure history are missing.
6. **Hardware reliability is not measured over time.** Terminals have heartbeat/peripheral status, but repeated scanner/printer/cash-drawer failures are not persisted into operational KPIs.
7. **Installer/commercial pilot feedback is outside the app.** No in-app/site-level install success, support incident, or pilot validation metric exists.

## Issue remediation targets

This section tracks the seven readiness issues separately from implementation sequencing.

| # | Issue | Target outcome | Readiness impact |
| --- | --- | --- | --- |
| 1 | Offline sale queue is fragile | Offline sales replay through the same authenticated/idempotent API path as normal checkout, with visible pending/failed/stuck states. | Offline operation can move from ~45% to ~70-75%. |
| 2 | Operational metrics are snapshots, not time series | Health, failures, backups, sync, payments, and terminals have historical events and summary windows. | Recovery/operations can move from ~60% to ~75%. |
| 3 | Checkout failures are not first-class metrics | Every checkout attempt has an outcome and failure reason: validation, stock, payment, auth, network, duplicate, unknown. | POS integrity can move from ~85% to ~90%; real-world validation improves. |
| 4 | Payment readiness is incomplete | Pending/failed/stale/unmatched M-Pesa and manual payments are visible and reconcilable. | Payments can move from ~65% to ~80%. |
| 5 | Backup readiness is partial | Backup success/failure and restore drill success/failure are tracked with last-good timestamps. | Recovery/operations can move from ~60% to ~80%. |
| 6 | Hardware reliability is not measured over time | Scanner, printer, drawer, display, and connector failures are persisted and shown by terminal. | Integrations/automation can move from ~35% to ~55-60%. |
| 7 | Installer/commercial pilot feedback is outside the app | Install status, diagnostic bundle, support incidents, pilot checklist, and site sign-off are tracked. | Deployment can move from ~60% to ~75%; commercial readiness can move from ~40% to ~60%. |

## Projected readiness if issues 1-7 are addressed

| Dimension | Current | After issues 1-7 |
| --- | ---: | ---: |
| Core domain functionality | 🟢 ~80% | 🟢 ~82-85% |
| POS transaction integrity | 🟢 ~85% | 🟢 ~90% |
| Inventory/procurement | 🟢 ~80% | 🟢 ~82-85% |
| UI/product experience | 🟡 ~65% | 🟡 ~70-75% |
| Payments | 🟡 ~65% | 🟢 ~80% |
| Offline operation | 🟠 ~45% | 🟡 ~70-75% |
| Deployment/installer | 🟡 ~60% | 🟡 ~75% |
| Security hardening | 🟡 ~65% | 🟡 ~72-78% |
| Recovery/operations | 🟡 ~60% | 🟢 ~80% |
| Integrations/automation | 🟠 ~35% | 🟡 ~55-60% |
| Real-world validation | 🔴 ~20% | 🟠 ~45-55% |
| Commercial readiness | 🟠 ~40% | 🟡 ~60-65% |

Expected overall result: the system would move from a strong technical MVP to a much more pilot-manageable product. The biggest remaining blockers after this would still be real pharmacy validation, installer proof across machines, support operations, and production-grade fiscal/payment integrations.

## Recommended next implementation order

1. Fix issue 1: offline queue safety.
2. Fix issues 2 and 3 together: operational metric events and checkout outcome tracking.
3. Fix issue 4: payment reconciliation dashboard/data.
4. Fix issue 5: backup/restore drill tracking.
5. Fix issue 6: persisted hardware reliability metrics.
6. Fix issue 7: installer/pilot feedback tracking.
