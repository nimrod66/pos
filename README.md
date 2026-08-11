# Pharmacy POS Backend

Spring Boot backend for a small pharmacy point-of-sale and inventory system. The current MVP is designed to run as a local pharmacy node with PostgreSQL. Checkout does not depend on internet access.

This repository is now the complete pilot monorepo:

- `src/`: Spring Boot API
- `pharmacy-frontend/`: Next.js staff application
- `docker-compose.pilot.yml`: PostgreSQL, API, and frontend pilot stack
- `installer/`: repeatable Windows installer source and lifecycle scripts

For partner testing, download or build `PharmacyPOS-Pilot-Setup.exe`. Docker
Desktop is the only client prerequisite for this pilot package. See
`installer/README.md` for the build and installation details.

## Current MVP

Implemented and hardened:

- Server-side login sessions with an HttpOnly cookie and CSRF protection
- Pharmacy and branch-scoped access control
- Five pharmacy roles with explicit permissions
- Medicine and supplier management
- Purchase orders and idempotent goods-received notes (GRNs)
- Batch and expiry tracking with audited stock movements
- Staff shifts and cash-drawer reconciliation
- Authoritative checkout with server pricing, tax, FEFO allocation, and receipts
- Cash and manually confirmed M-Pesa payments
- Idempotent sale returns with returned stock placed in quarantine
- Sales, stock, audit, customer, prescription, and reporting reads scoped to the active branch
- Flyway migrations and PostgreSQL-backed integration tests

Deferred from the MVP:

- eTIMS/KRA fiscal submission
- Automated M-Pesa STK callbacks and other online payment gateways
- Insurance, supplier accounting, expenses, credit notes, and debit notes
- A production central sync deployment

Deferred routes are denied by the security layer even where legacy controller code remains in the monolith.

## Runtime Model

| Mode | Pharmacy node | Internet needed for checkout | Central service |
| --- | --- | --- | --- |
| Local | API and PostgreSQL run at the pharmacy | No | None |
| Hybrid | API and PostgreSQL still run at the pharmacy | No | Outbox syncs when connectivity returns |
| Hosted | API and PostgreSQL run on a managed server | Yes | The hosted API is the primary node |

`POS_SYNC_MODE=local` is the supported MVP setting. Hybrid transport is opt-in and must not be enabled for a client until a compatible central API, conflict policy, monitoring, backups, and restore drills have been deployed and tested.

Local and hybrid modes use the same PostgreSQL schema and frontend API. There is no H2 database and no separate frontend build for offline use.

## Requirements

Recommended:

- Docker Desktop with Docker Compose

For direct Java development:

- Java 21
- PostgreSQL 17 (or a compatible supported PostgreSQL version)
- Docker for the Testcontainers integration test

The Maven wrapper is included, so a global Maven installation is not required.

## Quick Start

From this repository in PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

Open:

- API: `http://localhost:9090`
- Swagger UI: `http://localhost:9090/swagger-ui/index.html`
- Health: `http://localhost:9090/actuator/health`
- OpenAPI JSON: `http://localhost:9090/v3/api-docs`

The example environment enables these development accounts:

| Account | Email | Password | Roles |
| --- | --- | --- | --- |
| Owner | `admin@demo.com` | `admin123` | `OWNER` |
| Branch manager | `manager@demo.com` | `manager123` | `BRANCH_MANAGER` |
| Pharmacist | `pharmacist@demo.com` | `pharmacist123` | `PHARMACIST` |
| Cashier | `cashier@demo.com` | `cashier123` | `CASHIER` |
| Store keeper | `storekeeper@demo.com` | `stock1234` | `STORE_KEEPER` |
| Pharmacy technician | `technician@demo.com` | `tech12345` | `CASHIER`, `STORE_KEEPER` |

These accounts are only for local development. Set `POS_SEED_DEMO_ENABLED=false` for every real pharmacy, create a proper owner account, and do not reuse any demonstration password.

To view logs:

```powershell
docker compose logs -f app
```

To stop the services while retaining pharmacy data:

```powershell
docker compose down
```

## Direct Java Start

Start PostgreSQL first, then provide its connection values if they differ from the defaults:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/pos"
$env:SPRING_DATASOURCE_USERNAME = "pos"
$env:SPRING_DATASOURCE_PASSWORD = "pos_dev_only"
.\mvnw.cmd spring-boot:run
```

`start-offline.bat` and `start-offline.sh` remain as compatibility names. They start the PostgreSQL local-node profile; they do not start H2 or PostgreSQL itself.

## Authentication

The browser does not store a JWT. Authentication uses a server-side Spring Session record and an HttpOnly `pos_session` cookie. Every frontend request must include credentials.

For state-changing requests:

1. Fetch `GET /api/v1/auth/csrf`.
2. Send the returned token in its returned header name, normally `X-CSRF-TOKEN`.
3. Log in with `POST /api/v1/auth/login`.
4. Fetch a new CSRF token because login rotates the session.
5. Use the new token for `POST`, `PUT`, `PATCH`, and `DELETE` requests.

Production uses a secure `__Host-pos_session` cookie. Serve the frontend and API under the same site or behind one reverse proxy. Do not weaken cookie policy merely to make unrelated hosting domains communicate.

## Roles

The pharmacy-facing roles are:

- `OWNER`: administration and all non-clinical operational permissions
- `BRANCH_MANAGER`: branch supervision, approvals, reports, returns, and variances
- `PHARMACIST`: dispensing, prescription approval, POS, inventory reads, and shifts
- `CASHIER`: POS, receipts, returns, and shifts
- `STORE_KEEPER`: medicines, suppliers, procurement, GRNs, and inventory reports

Frontend visibility is based on permission codes returned by `GET /api/v1/auth/me`, not role-name guesses. See `NOTES-TO-FRONTEND.md` for the permission matrix.

## Stock Workflow

Do not increase stock by editing a stock row or posting a stock movement.

1. Create or select a medicine.
2. Create or select a supplier.
3. Create and approve a purchase order where applicable.
4. Receive physical stock through `POST /api/v1/goods-received` with an `Idempotency-Key` UUID.

The GRN transaction creates or updates the batch, updates branch stock, records weighted cost, records the stock movement, and updates purchase-order delivery status together.

## Tests

Docker must be running. The application-context test starts a fresh PostgreSQL 17.7 container, applies Flyway V1 through V8, and asks Hibernate to validate the resulting schema.

```powershell
.\mvnw.cmd test
```

The current suite contains 17 tests. API-level checkout and return behavior has also been verified against a running PostgreSQL node.

## Reference

- `API.md`: concise backend contract and request examples
- `NOTES-TO-FRONTEND.md`: frontend integration and security flow
- `FISCAL-EXTRACTION.md`: future fiscal-module boundary, not an MVP feature
- Swagger UI: generated controller-level reference for the running build
