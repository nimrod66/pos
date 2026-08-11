# Pharmacy POS Frontend

Next.js frontend for an affordable pharmacy POS. It supports the Spring Boot
local pharmacy node as its authoritative data source and retains an optional
browser-only preview for demonstrations.

## Implemented frontend

- Role-specific login, protected routes, and command-level permission checks.
- Dashboard, medicine catalogue, batches, stock movements, suppliers, and GRNs.
- Cashier shifts, barcode/SKU entry, cart, cash/M-Pesa checkout, and receipts.
- Server-authoritative FEFO allocation, sale-linked returns, refund totals, and 58/80 mm print
  layouts.
- Sales and inventory reports with permission-filtered content.
- Staff creation, role editing, enable/disable controls, self-edit protection,
  and final-owner protection.
- Typed live and preview workspace gateways. Components do not import transport
  or browser storage details directly.

## Local development

1. Copy `.env.local.example` to `.env.local`.
2. Run `npm install` if dependencies have not been installed.
3. Run `npm run dev`.
4. Open `http://localhost:3000`.

With the example environment, the backend provides these development accounts:

| Account | Email | Password | Roles |
| --- | --- | --- | --- |
| Owner | `admin@demo.com` | `admin123` | `OWNER` |
| Branch manager | `manager@demo.com` | `manager123` | `BRANCH_MANAGER` |
| Pharmacist | `pharmacist@demo.com` | `pharmacist123` | `PHARMACIST` |
| Cashier | `cashier@demo.com` | `cashier123` | `CASHIER` |
| Store keeper | `storekeeper@demo.com` | `stock1234` | `STORE_KEEPER` |
| Pharmacy technician | `technician@demo.com` | `tech12345` | `CASHIER`, `STORE_KEEPER` |

`NEXT_PUBLIC_SHOW_DEMO_ACCOUNTS=true` displays these options on the local login
screen. Set it to `false` together with `POS_SEED_DEMO_ENABLED=false` for every
real pharmacy deployment.

Set `NEXT_PUBLIC_DEMO_AUTH=true` only when a frontend-only demonstration is
needed. The available preview accounts are:

Frontend-only preview mode uses the same account table and permission bundles as
the live local backend.

Live authentication uses an opaque `HttpOnly` Spring Session cookie and an
in-memory CSRF token. No JWT or session identifier is stored by frontend
JavaScript.

Preview data is intentionally browser-local and non-authoritative. In live mode,
sales, stock changes, returns, staff, shifts, suppliers, medicines, GRNs, and
settings are loaded from and written to the backend API.

## Checks

```powershell
npm run lint
npm test
npm run build
npm audit
```
