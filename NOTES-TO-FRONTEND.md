# Frontend Integration Note

This is the handoff contract for the Pharmacy POS frontend. Read `API.md` for full command examples.

## Non-Negotiable Boundaries

- The frontend talks to `/api/v1` on the pharmacy node.
- Authentication is an HttpOnly server-side session cookie. Do not add browser JWT storage.
- Every fetch uses `credentials: "include"`.
- Every `POST`, `PUT`, `PATCH`, and `DELETE` uses the current CSRF token.
- `/auth/me` is the source of user, active branch, role, permission, and session-expiry state.
- The backend calculates checkout price, tax, totals, change, stock allocation, refund value, and drawer variance.
- The frontend never writes stock quantities directly.
- Local and hybrid deployments use the same frontend contract. The browser still talks to its pharmacy node.

## Local URLs

```text
Frontend: http://localhost:3000
API:      http://localhost:9090/api/v1
Health:   http://localhost:9090/actuator/health
Swagger:  http://localhost:9090/swagger-ui/index.html
```

Use one configurable public API origin, for example:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:9090/api/v1
```

Do not append `/api/v1` a second time in endpoint helpers.

## Fetch Client

Keep the CSRF token in memory. The session cookie is managed by the browser and is intentionally unreadable from JavaScript.

```ts
const API_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:9090/api/v1";

type CsrfState = { token: string; headerName: string };
let csrf: CsrfState | null = null;

export async function refreshCsrf(): Promise<CsrfState> {
  const response = await fetch(`${API_URL}/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!response.ok) throw await toApiError(response);
  const body = await response.json();
  csrf = body.data;
  return csrf;
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const mutates = ["POST", "PUT", "PATCH", "DELETE"].includes(method);
  if (mutates && !csrf) await refreshCsrf();

  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body) headers.set("Content-Type", "application/json");
  if (mutates && csrf) headers.set(csrf.headerName, csrf.token);

  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers,
    credentials: "include",
    cache: "no-store",
  });

  if (!response.ok) throw await toApiError(response);
  return response.json();
}
```

`toApiError` should parse the backend error body and retain `status`, `errorCode`, `message`, and `validationErrors`.

## Login Flow

1. Call `refreshCsrf()` when the login page opens.
2. `POST /auth/login` with email/password and the token.
3. Immediately call `refreshCsrf()` again. Login rotates the session and invalidates the old token.
4. Call `GET /auth/me` and put its data in the auth store.
5. Redirect to the first route allowed by returned permissions.

```ts
await refreshCsrf();
await apiFetch("/auth/login", {
  method: "POST",
  body: JSON.stringify({ email, password }),
});
await refreshCsrf();
const session = await apiFetch<MeEnvelope>("/auth/me");
```

On app reload, call `/auth/me`. A `401` means the user must log in again. Do not infer login from local storage.

Logout with `POST /auth/logout`, clear in-memory auth/CSRF state, then return to login.

## Current User Shape

```ts
type Me = {
  expiresAt: string;
  user: {
    id: string;
    email: string;
    displayName: string;
    pharmacyId: string;
    pharmacyName: string;
    activeBranch: { id: string; code: string; name: string };
    roles: string[];
    permissions: string[];
    featureFlags: Record<string, boolean>;
  };
};
```

Treat `expiresAt` as display/idle-warning information. A server `401` is authoritative.

## Permission-Driven UI

Do not hardcode role checks throughout components. Define a small permission helper:

```ts
const can = (permission: string) => session.user.permissions.includes(permission);
```

Suggested navigation guards:

| Frontend area | Required permission |
| --- | --- |
| Dashboard | `dashboard.read` |
| Point of sale | `pos.sell` |
| Current shift open | `shift.open` |
| Current shift close | `shift.close` |
| Medicines read | `medicine.read` |
| Medicine add/edit | `medicine.write` and `medicine.price.write` |
| Inventory | `inventory.read` |
| Receive GRN | `inventory.receive` |
| Suppliers | `supplier.read` |
| Supplier add/edit | `supplier.write` |
| Sales and receipts | `sale.read` |
| Reprint receipt | `sale.receipt.reprint` |
| Return sale | `sale.return` |
| Sales reports | `report.sales.read` |
| Inventory reports | `report.inventory.read` |
| Staff management | `user.manage` |
| Settings | `settings.manage` |
| Audit | `audit.read` |

The five UI roles are `OWNER`, `BRANCH_MANAGER`, `PHARMACIST`, `CASHIER`, and `STORE_KEEPER`. Render labels for roles, but authorize actions with permissions.

## Error Handling

Handle these cases centrally:

- `401 UNAUTHENTICATED`: clear session state and route to login.
- `403 CSRF_VALIDATION_FAILED`: refresh CSRF once and retry only the same safe command. Never loop.
- `403 ACCESS_DENIED`: show a permission message; do not retry.
- `409 PRICE_CHANGED`: refresh product/cart price and ask the cashier to confirm.
- `409 INSUFFICIENT_STOCK`: refresh POS lookup and keep the cart for correction.
- `409 IDEMPOTENCY_KEY_REUSED`: this is a client bug; do not silently generate another key.
- `400 VALIDATION_ERROR`: map `validationErrors` to fields.

Generate and optionally send `X-Request-ID` for support tracing. The API exposes the request ID header in CORS responses.

## Shift Workflow

The cashier/pharmacist must have an active shift before checkout.

1. Check `GET /shifts/active` after login.
2. Open with `POST /shifts` and `{ "openingFloat": 500.00, "shiftName": "Morning" }`.
3. Store the returned shift ID in current session state, not permanent browser storage.
4. Send that shift ID in checkout.
5. Close with `PATCH /shifts/{id}/close` and the physically counted `actualCash`.

Do not send `userId` or `branchId` when opening a shift. The server derives both from login context.

Opening a shift also opens its cash drawer. Closing the shift reconciles and closes it. Do not call direct cash-drawer open/close endpoints.

## POS Lookup and Cart

Search with:

```text
GET /pos/lookup?barcode=<scan>
GET /pos/lookup?name=<query>
GET /pos/quick-items
```

The lookup response includes current product price, total sellable stock, and non-expired branch batches. Use it to build the cart, but expect checkout to revalidate everything.

For each cart line retain:

```ts
type CartLine = {
  lineId: string;          // generated once and stable while retrying
  medicineId: string;
  quantity: number;        // positive integer
  expectedUnitPrice: number;
  requestedBatchId?: string;
};
```

A requested batch may only be the current FEFO batch. Usually omit it and let the server allocate automatically.

## Checkout

Checkout is only:

```text
POST /sales
```

Use one UUID as both `clientSaleId` and `Idempotency-Key`. Keep that UUID and the exact serialized command until a definitive response arrives.

Do not perform these as separate frontend steps:

- Create sale items
- Deduct stock
- Create payment
- Create receipt
- Record movement

The backend performs all of them atomically. A timeout is not proof of failure; retry the exact same request with the exact same idempotency key.

Supported payment choices in the current UI:

- Cash
- Manual M-Pesa reference
- Split cash plus manual M-Pesa

Do not show STK push, card, insurance, credit, or callback-confirmed payment controls yet.

After success, use the response total/change/receipt as final. Preserve `items[].allocations[].saleItemId` so the sale detail screen can construct precise returns.

## Returns

A return is only:

```text
POST /sale-returns
```

Use one UUID as both `clientReturnId` and `Idempotency-Key`. Build selectable lines from the original sale allocation `saleItemId` values. Let the user enter a reason and choose `CASH` or `MPESA_MANUAL`.

Do not add returned quantities to the sellable figure in the UI. The backend places them in quarantine pending a future inspection/disposition workflow.

## Adding Stock

The frontend workflow is:

1. Add/select medicine.
2. Add/select supplier.
3. Create purchase order.
4. Manager approves purchase order where required.
5. Receive stock with `POST /goods-received` and a UUID idempotency key.

The GRN form needs supplier, optional supplier invoice number, optional PO, batch number, expiry date, received quantity, and unit cost. If a PO is selected, send each matching `purchaseOrderLineId`.

Do not expose direct quantity fields backed by `/stock`, `/batches`, or `/stock-movements`. Those screens are reads/audit views; stock enters through a GRN and leaves through checkout/returns or a future approved-adjustment workflow.

## Delete Behavior

Medicine and supplier create/update/delete commands exist for authorized users. A delete may be rejected when history or stock makes physical deletion unsafe. In that case show the backend message and prefer an inactive/disabled status where supported.

Never remove a medicine, supplier, batch, sale, receipt, payment, or movement from local frontend state until the API confirms the command.

## Integration Order

Replace frontend mocks in this order:

1. API client, CSRF, login, `/auth/me`, logout, and route guards
2. System health and active branch header
3. Medicine/supplier/reference-data reads and writes
4. Active shift open/close
5. POS lookup and authoritative checkout
6. Sale history, receipt display, and returns
7. Purchase orders, GRN receiving, inventory, and movement history
8. Reports and staff/settings screens allowed by permissions

Keep the existing UI components where possible. Replace their data adapters and command handlers rather than rebuilding the visual frontend around backend entity shapes.

## Not Yet Available

Do not integrate UI controls for:

- eTIMS/KRA fiscalization
- Online gateway callbacks or automated M-Pesa STK
- Insurance billing
- Expenses and supplier accounting
- Credit/debit notes
- Hybrid sync administration

These need separate acceptance criteria and tests before being exposed to pharmacy users.
