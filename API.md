# Pharmacy POS API Contract

**MVP contract:** 0.1

**Base URL:** `http://localhost:9090/api/v1`

**Currency:** `KES`

**Display timezone:** `Africa/Nairobi`

This document covers the supported pharmacy MVP. The running OpenAPI document at `/v3/api-docs` remains useful for field discovery, but it may expose legacy controllers that are intentionally denied or disabled. The workflow rules in this document take precedence.

## 1. Conventions

All browser calls use JSON and include the server-side session cookie:

```http
Accept: application/json
Content-Type: application/json
```

All state-changing calls also include the current CSRF header. Checkout, returns, and GRNs require an `Idempotency-Key` header.

Money values accept at most two decimal places. Quantities sold and returned are positive whole numbers in the MVP. UUIDs are lowercase or uppercase standard UUID strings.

Successful responses use this envelope:

```json
{
  "success": true,
  "message": "Optional message",
  "data": {},
  "timestamp": "2026-08-10T12:00:00"
}
```

Paged `data` uses:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Common query parameters are `page`, `size`, and `sort`, for example `?page=0&size=20&sort=createdAt,desc`.

## 2. Authentication and CSRF

Authentication is a server-side Spring Session. The browser receives an HttpOnly cookie and must not create, store, or attach a JWT.

### Login sequence

1. `GET /auth/csrf` with credentials included.
2. Read `data.token` and `data.headerName`.
3. `POST /auth/login` with that header and credentials included.
4. Fetch `GET /auth/csrf` again because login rotates the session.
5. Use the new token for every `POST`, `PUT`, `PATCH`, and `DELETE` request.

```json
{
  "email": "admin@demo.com",
  "password": "admin123"
}
```

The following local-development accounts exist only when
`POS_SEED_DEMO_ENABLED=true`:

| Email | Password | Roles |
| --- | --- | --- |
| `admin@demo.com` | `admin123` | `OWNER` |
| `manager@demo.com` | `manager123` | `BRANCH_MANAGER` |
| `pharmacist@demo.com` | `pharmacist123` | `PHARMACIST` |
| `cashier@demo.com` | `cashier123` | `CASHIER` |
| `storekeeper@demo.com` | `stock1234` | `STORE_KEEPER` |
| `technician@demo.com` | `tech12345` | `CASHIER`, `STORE_KEEPER` |

### Session endpoints

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/auth/csrf` | Public | Create/read CSRF token |
| POST | `/auth/login` | Public plus CSRF | Authenticate and rotate session |
| GET | `/auth/me` | Authenticated | Current user, branch, roles, permissions, flags, expiry |
| POST | `/auth/logout` | Authenticated plus CSRF | Revoke session |

Only one active session per user is retained. Default idle expiry is 30 minutes and absolute expiry is 12 hours.

## 3. Authorization

Use `data.user.permissions` from `/auth/me` for navigation, buttons, and route guards. The backend remains the final authority.

### Roles and permission bundles

`OWNER`

All listed operational permissions except `prescription.approve`. Clinical prescription completion remains a pharmacist action.

`BRANCH_MANAGER`

```text
dashboard.read
pos.discount.approve
sale.read
sale.receipt.reprint
sale.void
sale.return
medicine.read
inventory.read
inventory.adjust.approve
supplier.read
shift.variance.approve
report.sales.read
report.inventory.read
```

`PHARMACIST`

```text
dashboard.read
pos.sell
sale.read
medicine.read
inventory.read
shift.open
shift.close
prescription.approve
```

`CASHIER`

```text
pos.sell
pos.discount.request
sale.read
sale.receipt.reprint
sale.return
medicine.read
shift.open
shift.close
```

`STORE_KEEPER`

```text
medicine.read
medicine.write
inventory.read
inventory.receive
inventory.adjust.request
supplier.read
supplier.write
report.inventory.read
```

`PLATFORM_ADMIN` appears in a few legacy route guards but is not a pharmacy staff role or part of the current tenant-facing UI.

## 4. Error Contract

```json
{
  "success": false,
  "status": 409,
  "error": "Conflict",
  "message": "The selling price changed to 125.00",
  "errorCode": "PRICE_CHANGED",
  "path": "/api/v1/sales",
  "timestamp": "2026-08-10T12:00:00"
}
```

Important status handling:

- `400`: invalid input or invalid workflow command
- `401 UNAUTHENTICATED`: session missing or expired; return to login
- `403 CSRF_VALIDATION_FAILED`: refresh CSRF once, then retry the safe user action
- `403 ACCESS_DENIED`: hide/disable the action and show a permission message
- `404`: resource not visible in the active pharmacy or branch
- `409`: stale price, stock race, duplicate reference, reused idempotency key, or invalid state transition

Validation failures add `validationErrors`, each with `field` and `message`.

## 5. Supported Endpoint Map

### Environment and POS

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET | `/system/status` | Authenticated | API and database readiness |
| GET | `/pos/lookup?barcode=...` | `pos.sell` | Pharmacy catalog plus sellable branch batches |
| GET | `/pos/lookup?name=...` | `pos.sell` | Search up to 30 matches |
| GET | `/pos/quick-items` | `pos.sell` | Sellable non-expired branch stock |

`/actuator/health`, `/swagger-ui/**`, and `/v3/api-docs/**` are outside the `/api/v1` base and are publicly readable.

### Shifts and checkout

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| POST | `/shifts` | `shift.open` | Opens current user's shift and drawer |
| GET | `/shifts/active` | shift permission | Active shifts in current branch |
| GET | `/shifts/{id}` | shift permission | Branch-scoped shift |
| PATCH | `/shifts/{id}/close` | `shift.close` | Reconciles drawer and closes shift |
| PATCH | `/shifts/{id}/cancel` | `shift.variance.approve` | Manager cancellation |
| POST | `/sales` | `pos.sell` | Atomic authoritative checkout |
| GET | `/sales` | `sale.read` | Current-branch sale history |
| GET | `/sales/{id}` | `sale.read` | Sale, lines, allocations, payments, receipt |
| GET | `/receipts/{saleId}` | sale read/reprint | Receipt data |
| GET | `/receipts/{saleId}/print` | `sale.receipt.reprint` | Printable receipt view/data |
| POST | `/sale-returns` | `sale.return` | Atomic refund and quarantine |
| GET | `/sale-returns` | sale read/return | Branch return history |
| GET | `/sale-returns/{id}` | sale read/return | Return detail |

### Catalog, procurement, and stock

| Method | Path | Permission | Notes |
| --- | --- | --- | --- |
| GET/POST | `/medicines` | `medicine.read` / `medicine.write` plus price write | Pharmacy catalog |
| GET | `/medicines/search?q=...` | `medicine.read` | Name/code search |
| GET | `/medicines/barcode/{barcode}` | `medicine.read` | Exact barcode lookup |
| PUT/DELETE | `/medicines/{id}` | medicine write | Update or domain-safe removal |
| GET/POST | `/suppliers` | supplier read/write | Pharmacy suppliers |
| PUT/DELETE | `/suppliers/{id}` | `supplier.write` | Update or domain-safe removal |
| GET/POST | `/purchase-orders` | supplier read/write | Branch purchase orders |
| PATCH | `/purchase-orders/{id}/approve?userId=...` | `inventory.adjust.approve` | Approver must equal session user |
| POST | `/goods-received` | `inventory.receive` | Atomic GRN and stock receipt |
| GET | `/goods-received` | `inventory.read` | Branch GRNs; optionally filter with `poId` |
| GET | `/stock` | `inventory.read` | Current branch stock |
| GET | `/stock/low?branchId=...` | `inventory.read` | Low-stock list |
| GET | `/batches` | `inventory.read` | Batch/expiry list |
| GET | `/stock-movements` | `inventory.read` | Audited movement history |

Reference dictionaries at `/categories`, `/dosage-forms`, `/units`, `/tax-categories`, and `/manufacturers` are read-only to pharmacy users in the MVP.

## 6. Shift Commands

Open a shift. The backend ignores identity claims from the browser and uses the authenticated user and active branch.

```http
POST /api/v1/shifts
X-CSRF-TOKEN: <token>
```

```json
{
  "shiftName": "Morning",
  "openingFloat": 500.00,
  "remarks": "Drawer counted"
}
```

Only one active shift is allowed per user/branch, and only one open drawer is allowed per shift.

Close it with the counted physical cash:

```json
{
  "actualCash": 980.00,
  "remarks": "End of shift count"
}
```

The backend calculates expected cash from opening float, cash sales, cash returns, and drawer cash transactions. The frontend must not calculate the authoritative variance.

## 7. Authoritative Checkout

Generate one UUID for both `clientSaleId` and the `Idempotency-Key` header. Generate a stable UUID for each cart `lineId`.

```http
POST /api/v1/sales
Idempotency-Key: 5df9752c-0c6e-48f3-9d7b-91a513620417
X-CSRF-TOKEN: <token>
```

```json
{
  "clientSaleId": "5df9752c-0c6e-48f3-9d7b-91a513620417",
  "shiftId": "0a6fc9b2-11eb-40da-a4f1-ac518e742a20",
  "customerId": null,
  "prescriptionReferenceId": null,
  "items": [
    {
      "lineId": "b06d1bc5-6c40-4383-a9d8-47d80709bc20",
      "medicineId": "8b426c06-7c7b-4eae-9451-ed0cc96f213b",
      "quantity": 2,
      "expectedUnitPrice": 120.00,
      "requestedBatchId": null
    }
  ],
  "payments": [
    {
      "method": "CASH",
      "amount": 240.00,
      "reference": null
    }
  ],
  "cashTendered": 500.00,
  "note": null
}
```

Supported payment methods:

- `CASH`: requires `cashTendered`; cash payment reference must be null
- `MPESA_MANUAL` (aliases `MPESA` and `M_PESA`): requires a unique manually verified reference

Payment amounts must exactly equal the server-computed total. Split cash/M-Pesa payment is allowed. `cashTendered` must cover the cash portion and the server returns `changeDue`.

Checkout is one database transaction. The server:

- Verifies the active shift belongs to the session user and branch
- Reloads current medicine price and rejects stale `expectedUnitPrice`
- Locks stock and allocates non-expired units in FEFO order
- Calculates tax-inclusive line amounts and totals
- Validates controlled/prescription medicine rules
- Creates sale items, payment records, receipt, stock movements, and sync outbox events
- Rolls everything back if any line or payment fails

The response includes `items[].allocations[]` with `saleItemId`, `batchId`, `batchNumber`, and quantity. Retain `saleItemId` for return selection.

Do not call `/payments` after checkout. Direct payment creation is disabled with `DIRECT_PAYMENT_DISABLED`.

## 8. Authoritative Returns

Generate one UUID for both `clientReturnId` and the `Idempotency-Key` header.

```http
POST /api/v1/sale-returns
Idempotency-Key: 7288266d-2e29-4de3-bfe5-43298e3d755d
X-CSRF-TOKEN: <token>
```

```json
{
  "clientReturnId": "7288266d-2e29-4de3-bfe5-43298e3d755d",
  "saleId": "a9399c37-e5e8-4ece-af93-c84c74c2e837",
  "reason": "Customer reported damaged packaging",
  "refundMethod": "CASH",
  "refundReference": null,
  "items": [
    {
      "saleItemId": "19266dc5-c561-4f50-9abc-548895242a8f",
      "medicineBatchesId": "63f888f1-1589-4272-a5e0-133a4848a3eb",
      "quantity": 1
    }
  ]
}
```

Return rules:

- The sale must be paid, completed, in the active branch, and within the configured return window (default seven days).
- Return quantities cannot exceed the remaining returnable quantity for each historical sale item.
- Refund value comes from the historical sale line, never from today's price.
- `CASH` requires an active shift, open drawer, and sufficient expected drawer cash.
- `MPESA_MANUAL` requires a unique manual refund reference.
- Returned units increase `quantityQuarantined`; they do not become sellable stock.
- Cash refunds create an audited `CASH_OUT` transaction and reduce expected drawer cash.

The response reports `refundAmount` and each line's `disposition: "QUARANTINE"`.

Do not call `/payments/{id}/refund`. Direct payment refunds are disabled with `DIRECT_REFUND_DISABLED`.

## 9. Receiving Stock Through a GRN

Normal purchasing flow:

1. Create/select medicine and supplier.
2. Create `POST /purchase-orders` using branch/user IDs from `/auth/me`.
3. A manager approves it with `PATCH /purchase-orders/{id}/approve?userId=<me>`.
4. Receive one or more deliveries through a GRN.

Purchase-order example:

```json
{
  "supplierId": "a30bd639-7284-41a5-9dff-94b21bb4623c",
  "branchId": "47c19511-4bf0-4ce4-9c3f-b6f9883c6552",
  "orderedById": "1b690c9f-92b6-4517-8314-653be40bd61c",
  "expectedDeliveryDate": "2026-08-12T10:00:00",
  "items": [
    {
      "medicineId": "8b426c06-7c7b-4eae-9451-ed0cc96f213b",
      "quantity": 50,
      "buyingPrice": 75.00,
      "discount": 0.00,
      "tax": 0.00
    }
  ]
}
```

GRN example:

```http
POST /api/v1/goods-received
Idempotency-Key: 232b14f0-4487-4d7e-b0bc-8ec20e810d3c
X-CSRF-TOKEN: <token>
```

```json
{
  "supplierId": "a30bd639-7284-41a5-9dff-94b21bb4623c",
  "supplierInvoiceNumber": "INV-1045",
  "purchaseOrdersId": "ab66b581-8401-4f55-961c-140680832c13",
  "receivedAt": "2026-08-10T11:30:00",
  "remarks": "Checked against delivery note",
  "lines": [
    {
      "medicineId": "8b426c06-7c7b-4eae-9451-ed0cc96f213b",
      "purchaseOrderLineId": "4ca37002-06de-4766-adff-2e7289ce4197",
      "batchNumber": "BATCH-2026-08",
      "expiryDate": "2028-08-31",
      "quantity": 50,
      "unitCost": 75.00
    }
  ]
}
```

For an authorized direct/opening receipt, omit `purchaseOrdersId` and each `purchaseOrderLineId`. A GRN UUID idempotency key does not need to match a body field because the GRN request has no client ID field.

The server rejects over-receipt, expired stock, mismatched suppliers/PO lines, duplicate conflicting batches, and cross-branch references. It atomically updates batch weighted cost, available stock, purchase-order status, and movement history.

Do not use `/stock/receive`, `/stock/deduct`, or `POST /stock-movements` for normal workflows. Those direct mutation paths are disabled or restricted to zero-quantity metadata setup.

## 10. Idempotency

The frontend must keep the same key while retrying the same logical command after a timeout or lost response.

- Same key plus identical payload returns the already-completed resource.
- Same key plus different payload returns `409 IDEMPOTENCY_KEY_REUSED`.
- Concurrent duplicate work is serialized with PostgreSQL transaction locks.
- Never generate a new key merely because the first response was not received.

## 11. Deferred or Disabled Routes

The following are not part of the MVP contract:

```text
/insurance/**
/etims/**
/compliance/**
/invoices/**
/credit-notes/**
/debit-notes/**
/expenses/**
/expense-categories/**
/supplier-invoices/**
/supplier-payments/**
```

Payment gateway callbacks currently return a disabled response. The supported M-Pesa MVP path is a manually verified reference inside authoritative checkout or return.
