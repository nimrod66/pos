# FISCAL EXTRACTION REPORT

## Compliance → Microservice Extraction Readiness

This document captures the remaining coupling between the POS monolith and the
Compliance module, prepared for future extraction into an independent Spring Boot microservice.

---

## Packages Ready to Move (16 — Zero External Coupling Beyond `common`)

These packages import ONLY from `java.*`, `org.springframework.*`, `jakarta.*`, `lombok.*`,
`org.slf4j.*`, `com.example.pos.common.*`, and other `com.example.pos.compliance.*` subpackages:

| Package | Files | Description |
|---------|-------|-------------|
| `transmission/` | 20 | Queue, retry, dead-letter, batch processor |
| `gateway/` | 11 | OSCU/VSCU adapters, mapper, certificates |
| `tax/` | 3 | TaxEngine, DefaultTaxEngine, TaxSnapshot |
| `numbering/` | 6 | Document number generation, sequences |
| `sync/` | 4 | SyncEngine, EtimsSynchronizer, SyncState |
| `tis/` | 4 | TraderInvoicingSystem, TisFacade, TisWorkflow |
| `event/` | 3 | ComplianceEvent, event repository |
| `rules/` | 4 | RuleEngine, validation rules |
| `initialization/` | 4 | EtimsInitializer, CommunicationKeyManager, DeviceRegistration |
| `config/` | 3 | ComplianceConfiguration, FiscalYear, TaxPeriod |
| `monitoring/` | 7 | Dashboard, health, reconciliation |
| `reference/` | 14 | KRA code lists, classifications, reference sync |
| `fiscal/` | 2 | Country-specific fiscal documents (Kenya, Uganda) |
| `catalog/` | 1 | CatalogSynchronizer |
| `stock/` | 1 | StockSynchronizer |
| `purchases/` | 1 | PurchaseSynchronizer |

---

## Packages Needing Minor Work (3)

### 1. `invoice/` — 1 file needs cleanup
- **Done** — `SaleCompletedEvent.java` was deleted (dead code, never published)
- Remaining concern: none. All 32 remaining files are self-contained.

### 2. `receipt/` — ReceiptAssembler couples to sale entities
- **File:** `ReceiptAssembler.java`
- **Problem:** Injects `SalesRepository` + reads `Sales`, `Payment`, `SaleItems`, `Branch`, `Pharmacy` entities
- **Status:** `FiscalSaleSnapshot` record created at `integration/fiscal/snapshot/`
- **Fix required:** Refactor `ReceiptAssembler.assemble()` to accept `FiscalSaleSnapshot` instead of `Long saleId`. Move the entity→snapshot conversion to `UnifiedReceiptController`.
- **Effort:** ~60 lines changed, 2 files

### 3. `certification/` — DemoDataGenerator couples to masterdata
- **File:** `DemoDataGenerator.java`
- **Problem:** Imports `masterdata.tax.model.Tax`, `TaxRepository`
- **Recommendation:** Move as-is with the compliance service. Certification is dev tooling that only runs in certification mode.

---

## External Files Importing FROM Compliance (1)

| File | Import | Action |
|------|--------|--------|
| `terminal/printer/PrintService.java` | `compliance.receipt.dto.ReceiptDTO` | Can move `ReceiptDTO` to `integration/fiscal/dto/v1/` as shared contract. Low priority — it's a pure DTO, not an entity or service. |

---

## Shared Kernel: `com.example.pos.common.*`

Both POS and Compliance depend on:
- `BaseEntity` — JPA `@MappedSuperclass` with `id`, `createdAt`, `updatedAt`, `version`
- `ApiResponse<T>` — generic response wrapper
- `ResourceNotFoundException`, `BadRequestException`, `ConflictException`

**Microservice strategy:** For extraction, duplicate these into the microservice or publish as a shared library (`pos-common` JAR). Duplication is acceptable — it's ~3 classes, ~60 lines each.

---

## Database Separation

### Entities with Cross-Boundary References

| Compliance Entity | Reference | Type | Risk |
|-------------------|-----------|------|------|
| `TaxInvoice.saleId` | → `Sales.id` | `Long` column, no JPA FK | Safe — clean logical key |
| `Receipt.saleId` | → `Sales.id` | `Long` column, no JPA FK | Safe |
| `EtimsFiscalDocument.saleId` | → `Sales.id` | `Long` column, no JPA FK | Safe |
| `EfrisFiscalDocument.saleId` | → `Sales.id` | `Long` column, no JPA FK | Safe |

All cross-boundary references are plain `Long` IDs — no `@ManyToOne`, no cascade, no JPA relationship. The compliance database can be extracted to a separate schema with zero DDL changes.

---

## REST Contract (API Boundary)

The POS ↔ Compliance API is defined in `integration/fiscal/dto/v1/`:

| DTO | Purpose |
|-----|---------|
| `FiscalSaleRequest` | Sale data sent from POS to fiscal service |
| `FiscalSaleResponse` | Invoice confirmation with KRA receipt number |
| `FiscalHealthResponse` | Fiscal service health status |

These are versioned from day 1 (`v1/` subpackage). When the compliance service is extracted, these DTOs become the REST contract — no entity leaking, no shared JPA.

---

## Configuration Separation

```properties
# POS deployment concern (integration/fiscal/)
pos.fiscal.enabled=true
pos.fiscal.mode=LOCAL     # OFF | LOCAL | REMOTE
pos.fiscal.remote-url=
pos.fiscal.api-key=

# Compliance business concern (compliance/config/)
compliance.mode=MOCK       # MOCK | SANDBOX | CERTIFICATION | PRODUCTION
compliance.kra-pin=
compliance.osuc.api-url=
```

The compliance module never sees `OFF` mode — it only runs when active. The POS decides whether to invoke it.

---

## Fiscal Adapter Resolution Chain

```
TraderInvoicingSystem (interface — exists)
        ▲
        │ FiscalConfiguration @Primary bean resolves by mode
 ┌──────┴──────────────────────────┐
 │                                  │
OFF: NoOpTraderInvoicingSystem      LOCAL: LocalTraderInvoicingSystem → TisFacade
 │  (logs, returns DISABLED)         │  (zero-logic wrapper, constructor-injected)
 │                                  │
REMOTE: RemoteTraderInvoicingSystem │
   → FiscalClient (interface)      │
      → RestFiscalClient           │
         (RestClient + X-API-Key)  │
                                   │
                                   ▼
                              Compliance Module
                              (existing, untouched)
```

---

## Remaining Coupling Matrix

| From | To | Type | Remediation |
|------|----|------|-------------|
| `ReceiptAssembler` | `sale/Sales`, `sale/Payment`, `sale/SaleItems`, `core/Branch`, `core/Pharmacy` | Entity reads via JPA | Refactor to `FiscalSaleSnapshot` (record ready, implementation deferred) |
| `PrintService` | `compliance/receipt/dto/ReceiptDTO` | DTO import | Move DTO to `integration/fiscal/dto/v1/` (deferred) |
| `DemoDataGenerator` | `masterdata/tax/*` | Certification tooling | Move with compliance service (deferred) |
| `FiscalConfiguration` | `compliance/tis/TisFacade` | Constructor injection | Expected — LOCAL mode wraps the existing facade |

---

## Extraction Order

1. **Cut database:** Compliance tables moved to separate schema
2. **Extract self-contained packages:** transmission, gateway, tax, numbering, sync, tis, event, rules, initialization, config, monitoring, reference, fiscal, catalog, stock, purchases
3. **Refactor invoice:** Already clean after SaleCompletedEvent deletion
4. **Create shared DTO library:** `integration/fiscal/dto/v1/` becomes `fiscal-contracts`
5. **Deploy microservice:** `RemoteTraderInvoicingSystem` points to it
6. **Cutover:** Change `pos.fiscal.mode=LOCAL` → `REMOTE`
7. **Remove embedded compliance:** After validation period, delete compliance package

---

## Metrics

| Metric | Count |
|--------|-------|
| Total new files created | 17 |
| Existing files edited | 1 (`application.properties`) |
| Files deleted | 1 (`SaleCompletedEvent.java`) |
| Ready-to-move files | ~85 (16 packages) |
| Files needing refactoring | 2 (`ReceiptAssembler`, `DemoDataGenerator`) |
| External coupling points remaining | 4 (all low-risk) |
