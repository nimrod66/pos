# Pharmacy POS — Environment Matrix

## Comparison of Demo, Pilot and Production Configuration

| Setting | Demo | Pilot | Production |
|---------|------|-------|------------|
| **POS_SEED_DEMO_ENABLED** | `true` | `true` (for onboarding) | `false` |
| **Demo accounts** | Active (`admin@demo.com`, `manager@demo.com`, `pharmacist@demo.com`, `cashier@demo.com`, `storekeeper@demo.com`, `technician@demo.com`) | Active (first-week onboarding) | **Disabled** — custom users only |
| **M-Pesa environment** | `sandbox` (`MPESA_ENVIRONMENT=sandbox`) | `sandbox` (pilot may use test config) | **Live** (real Daraja credentials, production environment) |
| **Session cookie secure** | `false` (HTTP only) | `false` (HTTP pilot LAN) | **`true`** (HTTPS production) |
| **Rate limiting** | `false` | `may be true` (pilot may enable for abuse protection) | **enabled** (protect against excessive requests) |
| **Fiscal features** | `false` | `false` | **enabled** (tax reporting, fiscal regulations) |
| **Backup retention** | manual (ad hoc) | **30 days** (PowerShell script `backup-db.ps1` `-KeepDays 30`) | **enterprise backup with off-site rotation** (daily, weekly, monthly) |
| **LAN exposure** | `192.168.0.101` (for demos and handhelds) | `192.168.0.101` (for pilot LAN) | **restricted LAN** + **external firewall** + possible DMZ |
| **Backup location** | `backups/` directory (local) | `backups/` directory (local) | **enterprise backup server** or **cloud storage** (S3, Azure Blob) with encryption |
| **Upgrade path** | reset to demo state | manual restore from backup | **in-place upgrade** with Flyway migrations |
| **Backup schedule** | ad hoc / as needed | **Daily at 02:00** (`0 0 2 * * *` cron) + `KeepDays 30` | **Daily** + **weekly verification** + **monthly off-site copy** |
| **Backup encryption** | None (plain SQL dump) | None (plain SQL dump) | **AES-256 encrypted** (recommended) |
| **Audit logging** | Basic (Spring Session) | Basic (Spring Session) | **Extended** (full audit trail, immutable logs, access logging) |
| **Session timeout** | 30 min idle / 12h absolute | 30 min idle / 12h absolute | ** configurable**, shorter for security |
| **Password policy** | Minimum 8 chars (auto-generated) | Minimum 8 chars (admin-set) | **Complex policy** (12+ chars, special chars, rotation every 90 days) |
| **Cross-branch access** | All branches (demo data) | Controlled by branch IDs | **Branch-permissions matrix** (fine-grained) |
| **Cross-pharmacy access** | N/A (single pharmacy) | N/A (single pharmacy) | **Multi-tenant isolation** required |
| **Fiscal export** | None | None | **Daily/weekly export** (XML/EET format per local law) |
| **Audit trail retention** | 30 days | 30 days | **3–7 years** (per local regulations) |
| **Disaster recovery** | Re-seed from demo data | Restore from `.dump` backup | **Full DR plan** with RTO/RPO targets |

### Key Differences Summary

- **Demo** is for evaluation and training — has demo data, sandbox payments, no security requirements.
- **Pilot** is for a single pharmacy's controlled trial — has isolated demo data, sandbox payments, 30-day backup retention, LAN access for on-site use.
- **Production** is the live pharmacy system — real user data, live payments, enterprise backup, HTTPS, complex audit and retention requirements.

### Transition Paths

- **Demo → Pilot**: Enable `POS_BOOTSTRAP_ADMIN_EMAIL`/`POS_BOOTSTRAP_ADMIN_PASSWORD`, disable demo accounts for new users, configure sandbox M-Pesa settings for pilot use.
- **Pilot → Production**: Replace sandbox M-Pesa credentials with live Daraja config, enable `POS_SESSION_COOKIE_SECURE=true`, enable fiscal features, configure enterprise backup, adjust audit retention per local law.
- **Demo → Production**: Same as Pilot → Production, but also seed with real pharmacy data instead of demo data.

---
*This matrix is the single source of truth for which configuration values apply to which deployment mode. Do not mix settings across modes.*