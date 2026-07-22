# Notes to Frontend Dev — eTIMS Compliance Module

## 1. New Data Model — Tax Categories on Medicines

Every **Medicine** now has a required `taxCategoryId` pointing to `TaxCategory`.

**TaxCategory** (managed via Settings UI):
```json
{
  "id": 1,
  "name": "VAT 16%",
  "code": "VAT16",
  "taxType": "VAT_STANDARD",
  "rate": 16.00,
  "active": true
}
```

Tax types: `VAT_STANDARD` | `VAT_REDUCED` | `VAT_ZERO` | `VAT_EXEMPT` | `OUT_OF_SCOPE`

**Frontend changes:**
- Add a "Tax Category" dropdown to the Medicine create/edit form
- Show tax rate and type in the product listing (columns: Name, Barcode, Price, Tax)
- Tax category can be toggled active/inactive — don't show inactive ones in new medicine forms
- Endpoints: `GET /api/tax-categories/` (accepts `?activeOnly=true`), `PATCH /api/tax-categories/{id}/toggle`

## 2. Sale Flow — What Changed

When a cashier completes a sale (`POST /api/sales`), `SaleService` now snapshots `taxRate` and `taxableAmount` on each `SaleItem`. The response is unchanged — these are internal fields.

**No frontend changes needed at sale time.** Tax is calculated per-item automatically.

## 3. New Step: Issue Invoice (after sale)

After sale completion, the cashier **must** issue a tax invoice. This is the trigger for all compliance flows.

```javascript
// After successful POST /api/sales
const invoice = await api.post(`/api/invoices/issue/${saleId}`);
// Returns:
{
  "id": 1,
  "saleId": 42,
  "invoiceNumber": "INV-001-20260721-000001",
  "status": "ISSUED",
  "subtotal": 500.00,
  "taxAmount": 80.00,
  "discount": 0,
  "grandTotal": 580.00,
  "currency": "KES",
  "issueDate": "2026-07-21T14:30:00",
  "items": [
    {
      "medicineName": "Amoxicillin 500mg",
      "barcode": "8901234567890",
      "quantity": 2,
      "unitPrice": 250.00,
      "taxableAmount": 250.00,
      "taxRate": 16.00,
      "taxType": "VAT_STANDARD",
      "taxAmount": 40.00,
      "discount": 0,
      "subtotal": 500.00,
      "total": 580.00
    }
  ]
}
```

**Frontend requirements:**
- After sale, show an **"Issue Tax Invoice"** button
- Display invoice number prominently on the receipt screen
- Show status badge (ISSUED = green, TRANSMITTED = blue, FAILED = red)
- The invoice **cannot** be edited after issuance — only voided or credited

## 4. Invoice Status & Transmission Flow (Async)

The invoice and transmission have **separate statuses**:

### Invoice Status
| Status | Meaning |
|---|---|
| `DRAFT` | Not yet issued (should not appear in normal flow) |
| `ISSUED` | Created and saved, awaiting transmission |
| `VOID` | Canceled before any financial effect |
| `CREDITED` | A credit note was issued against this invoice |
| `CLOSED` | Final — invoice + transmission complete |

### Business (Transmission) Status
| Status | Meaning |
|---|---|
| `PENDING` | Issued but not yet sent to KRA |
| `TRANSMITTED` | Successfully sent to KRA |
| `FAILED` | Transmission failed (auto-retrying) |
| `ACKNOWLEDGED` | KRA confirmed receipt |

**Frontend polling pattern:**
```javascript
const pollInvoice = async (invoiceId) => {
  const invoice = await api.get(`/api/invoices/${invoiceId}`);
  if (invoice.businessStatus === 'TRANSMITTED' || invoice.businessStatus === 'ACKNOWLEDGED') {
    showSuccess('Invoice transmitted to KRA');
    stopPolling();
  } else if (invoice.businessStatus === 'FAILED') {
    showWarning('KRA transmission failed. Auto-retrying...');
  }
};
```

## 5. Receipts (Compliance-Grade)

After invoice issuance, compliance-grade receipts are available:

```javascript
const receipt = await api.post(`/api/compliance/receipts/generate/${saleId}`);
// Returns:
{
  "receiptNumber": "RCT-001-20260721-000001",
  "invoiceNumber": "INV-001-20260721-000001",
  "receiptData": "{...}",
  "qrCodeContent": "KRA-eTIMS|INV-001-...|580.00|...",
  "verificationUrl": "https://verify.kra.go.ke/eTIMS?code=...",
  "reprintCount": 0
}
```

- Receipt data is a snapshot — never changes after generation
- QR code contains verification data for KRA
- Reprints: `POST /api/compliance/receipts/reprint/{receiptId}` (increments counter)

## 6. What the Cashier UI Needs

```
+-------------------------------------------+
|  Sale Complete — KES 580.00               |
+-------------------------------------------+
|  Invoice: INV-001-20260721-000001         |
|  Status: TRANSMITTED (to KRA)             |
|                                           |
|  [Print Receipt]  [Issue Credit Note]     |
|  [View History]                           |
+-------------------------------------------+
```

**Status indicators:**
- **PENDING** (gray) — not yet sent
- **TRANSMITTING** (yellow/pulsing) — being sent now
- **TRANSMITTED** (blue) — sent to KRA
- **ACKNOWLEDGED** (green) — confirmed by KRA
- **FAILED** (red with retry count) — e.g. "Failed (3/10 retries)"

## 7. Admin / Manager Dashboard

New tab: **"eTIMS Compliance"** in the admin panel.

**Top section — Status Cards:**
```
  SANDBOX Mode    OSCU Active    Certificate ACTIVE    Device POS-001
  +--------------------------------------------------------------+
  | Pending: 12  |  Failed: 2  |  Sent: 147  |  Retry Q: 5     |
  +--------------------------------------------------------------+
```

**Endpoints:**
- Dashboard summary: `GET /api/compliance/dashboard`
- Transmission log: `GET /api/transmissions?page=&size=&status=&dateFrom=&dateTo=`
- Dead letter queue: `GET /api/transmissions/dead-letter`
- Patch dead letter: `PATCH /api/transmissions/dead-letter/{id}?action=retry|review|discard`

## 8. Sync Management

**Button: "Sync to KRA"** — triggers `POST /api/compliance/sync/run?scope=all`

After sync, shows results per sync type:
| Sync Type | Last Run | Records | Status |
|-----------|----------|---------|--------|
| Tax Codes | 14:30 | 5 | PASS |
| Items | 14:31 | 143 | PASS |
| Branches | 14:31 | 3 | PASS |
| Purchases | 14:32 | 0 | PASS |
| Stock | 14:33 | 22 | PASS |
| Invoices | 14:34 | 147 | PASS |

- Sync state: `GET /api/compliance/sync/state`
- Individual type: `POST /api/compliance/sync/run?scope=ITEM`

## 9. Certification Testing UI

For pre-certification testing with KRA:

```javascript
// Generate demo data (creates VAT16, VAT8, VAT0, EXEMPT tax categories)
await api.post('/api/compliance/certification/generate-demo-data');

// Run all test scenarios
const results = await api.post('/api/compliance/certification/run');
// Returns array:
[
  { scenario: "Invoice Generation", passed: true, durationMs: 450 },
  { scenario: "Tax Calculation", passed: true, durationMs: 120 },
  { scenario: "Credit Note", passed: true, durationMs: 300 },
  { scenario: "Synchronization", passed: true, durationMs: 890 }
]

// Export certification artifacts (counts summary)
const exportData = await api.post('/api/compliance/certification/export');
// { invoicesIssued: 150, transmissionsSent: 142, events: 450 }
```

## 10. Voiding and Credit Notes

```javascript
// Void (before transmission or after)
await api.patch(`/api/invoices/${invoiceId}/void`, { reason: "Wrong amount" });

// Credit Note (proper KRA-compliant reversal)
await api.post('/api/invoices/credit-notes', {
  originalInvoiceId: 42,
  reason: "Customer returned item",
  amount: 580.00
});

// Debit Note (additional amount due)
await api.post('/api/invoices/debit-notes', {
  originalInvoiceId: 42,
  reason: "Undercharged by 50.00",
  amount: 50.00
});
```

## 11. New Error States to Handle

| Scenario | HTTP | Display |
|----------|------|---------|
| Non-existent sale | 404 | "Sale not found" |
| Already invoiced | 409 | "Invoice already exists for this sale" |
| Invalid customer PIN | 400 | "KRA PIN must be 11 characters (P0XXXXXXXXX)" |
| Invoice already voided | 409 | "Invoice INV-001 is already voided" |
| Credit note exceeds original | 400 | "Credit note amount exceeds invoice total" |
| Sync in progress | 409 | "Synchronization already running" |

## 12. Frontend Navigation Summary

| Path | Component | API Calls |
|------|-----------|-----------|
| `/sales/:id` | Sale Detail | Issue invoice, show status, print receipt |
| `/invoices` | Invoice List | `GET /api/invoices?page=&size=&status=&dateFrom=&dateTo=` |
| `/invoices/:id` | Invoice Detail | Status, items, history, credit note button |
| `/admin/compliance` | Compliance Dashboard | `GET /api/compliance/dashboard` |
| `/admin/compliance/transmissions` | Transmission Log | `GET /api/transmissions?page=&size=&status=` |
| `/admin/compliance/dead-letter` | Dead Letter Queue | `GET /api/transmissions/dead-letter`, patch actions |
| `/admin/compliance/sync` | Sync Management | `POST /api/compliance/sync/run`, `GET /api/compliance/sync/state` |
| `/admin/compliance/certification` | Certification | Run scenarios, export, generate demo data |
| `/admin/tax-categories` | Tax Settings | CRUD for tax categories |

## 13. Key Implementation Notes

1. **Invoice is separate from Sale** — `GET /api/sales/:id` does NOT include the invoice. Use `GET /api/invoices?saleId=:id` or the sale's `invoiceId` field (after issue).
2. **Transmission is async** — the invoice response returns immediately. Poll or subscribe to WebSocket for transmission status updates.
3. **Receipt data is a snapshot** — once generated, it never changes. Price recalculations won't affect printed receipts.
4. **Tax amounts are computed server-side** — no need to calculate on the frontend. The `taxAmount` on each invoice line is the final value.
5. **Schema version** — every invoice has a `schemaVersion` field for future KRA schema changes. Old invoices retain their version.

## 14. Environment Awareness

The compliance mode affects behavior:

| Mode | When | Behavior |
|------|------|---------|
| `MOCK` | Local dev | Invoice creates, receipt generates, transmission simulates (no real KRA call) |
| `SANDBOX` | Staging | Talks to KRA sandbox API. Real payloads, real responses, no legal effect |
| `CERTIFICATION` | Pre-cert | Same as sandbox but with additional logging for KRA auditors |
| `PRODUCTION` | Live | Real KRA OSCU/VSCU calls. Transmissions are legally binding |

Check current mode: `GET /api/compliance/health`

## 15. Full API Reference

See **`API.md`** for the complete endpoint catalog (all 47 sections, request/response schemas, ERD diagrams, sequence diagrams, and architecture documentation).
