# Pharmacy POS — Backend

Spring Boot 4.1 / PostgreSQL / Spring Session JDBC — pharmacy point of sale backend for the Kenyan market.

## Quick Start

### Prerequisites
- **Java 21** (JDK)
- **PostgreSQL 16+**
- **Maven Wrapper** (included — `./mvnw`)
- RabbitMQ (optional — only for hybrid sync)

### Setup

```bash
# 1. Create database
psql -U postgres -c "CREATE DATABASE pos;"
psql -U postgres -c "CREATE USER pos WITH PASSWORD 'pos';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE pos TO pos;"

# 2. Build and run
./mvnw spring-boot:run

# Or build JAR and run
./mvnw package -DskipTests
java -jar target/POS-0.0.1-SNAPSHOT.jar
```

App starts on **http://localhost:9090**.

## URLs

| Service | URL |
|---------|-----|
| **API base** | `http://localhost:9090/api/v1` |
| **Swagger UI** | `http://localhost:9090/swagger-ui/index.html` |
| **OpenAPI spec** | `http://localhost:9090/v3/api-docs` |
| **Actuator health** | `http://localhost:9090/actuator/health` |
| **H2 Console** (offline) | `http://localhost:9090/h2-console` |

## Test Accounts

| Email | Password | Role | Branch |
|-------|----------|------|--------|
| `admin@demo.com` | `admin123` | OWNER | Main |

Additional roles are seeded on startup.

## Run Tests

```bash
./mvnw test
```

Tests require PostgreSQL running. For CI, use the `ci` profile:

```bash
./mvnw test -Dspring.profiles.active=ci -Dspring.datasource.url=jdbc:h2:mem:pos
```

## Profiles

| Profile | Command | Database |
|---------|---------|---------|
| **default** (dev) | `./mvnw spring-boot:run` | PostgreSQL |
| **offline** | `./mvnw spring-boot:run -Dspring-boot.run.profiles=offline` | H2 (file-based) |
| **prod** | `--spring.profiles.active=prod` | PostgreSQL (secure config) |

## Docker

```bash
# Full stack: PostgreSQL + RabbitMQ + App
docker compose up -d
```

## API Documentation

Full endpoint reference: [API.md](./API.md)

### Key endpoints

```text
GET    /api/v1/auth/csrf              CSRF token
POST   /api/v1/auth/login             Session login
GET    /api/v1/auth/me                Current user context
POST   /api/v1/auth/logout            End session

POST   /api/v1/sales                  Atomic checkout (idempotent)
POST   /api/v1/goods-received         GRN with line items
POST   /api/v1/payments               Record payment
GET    /api/v1/system/status          Health + readiness
```

### Conventions
- All endpoints use `/api/v1` prefix
- UUIDs for all resource IDs
- ISO 8601 UTC timestamps
- `BigDecimal` as decimal string for money
- `camelCase` JSON fields

## Architecture

```
Local Spring Boot node
├── PostgreSQL (operational DB)
├── Spring Session JDBC (server-side sessions)
├── Flyway (versioned migrations)
├── Rate limiting (token bucket)
├── Idempotency keys (checkout + GRN)
└── FEFO stock allocation
```

## Backup & Restore

```bash
# Backup (encrypted pg_dump)
./backup.sh ./backups

# Restore
./restore.sh backups/pos_backup_20260805_120000.zip
```

## Configuration

Copy `.env.example` for environment variables. Default test keys are hardcoded in `application.properties` for immediate development.

Production: use `--spring.profiles.active=prod` for CSRF enabled, SQL logging off, restricted actuator, stricter rate limits.

## Package Structure

```
com.example.pos/
├── catalog/           Supplier catalogue imports
├── common/            BaseEntity, ApiResponse, PagedResponse, filters, exception handling
├── compliance/        KRA eTIMS fiscal compliance
├── core/              Pharmacy, Branch, SystemSettings
├── customer/          Customer CRUD
├── finance/           Expenses, CashDrawers, CashTransactions
├── insurance/         NHIF/private insurer claims
├── integration/       Fiscal, Payment, Email, SMS adapters
├── inventory/         Stock, Batches, StockMovements
├── masterdata/        Medicine, Tax, Manufacturer, DosageForm, Categories, Units
├── messaging/         RabbitMQ outbox pattern
├── notification/      In-app notifications
├── payment/           Payment gateway abstraction
├── pharmacy/          Controlled drugs, expiry logs
├── pos/               Quick operations, hardware bridge
├── prescriptions/     Prescriptions, dispensary
├── procurement/       Suppliers, POs, GRNs, invoices, payments, price history
├── reporting/         Dashboard, GraphQL
├── sale/              Sales, SaleReturns, Payment, Receipts, Idempotency
├── security/          Auth, session JDBC, CSRF
├── sync/              Offline sync, terminal management
├── terminal/          Terminal registry, barcode, scanner, printer
└── user/              Users, Roles, Permissions, Shifts, LoginHistory
```
