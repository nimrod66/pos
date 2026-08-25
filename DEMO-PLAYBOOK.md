# Demo Playbook — Questions You Will Get Asked

Answers grounded in what the system actually does today. Logins below use the
demo accounts; change them before any real deployment.

## Sales & Payments

**"Can I sell a single tablet instead of a whole pack?"**
Yes — stock and pricing are per stock unit you define. Create the medicine
priced per tablet (e.g., KES 5/tablet), receive 1,000 tablets, and sell qty 1.
Or price per strip/pack and sell strips. Quantities are positive whole units.

**"Customer hands you 1,000 bob for a 400 bill?"**
Enter cash tendered; the POS shows change due instantly (600) and records it.
The drawer reconciliation only counts the 400 — tendered never inflates the till.

**"M-Pesa — does the cashier type the confirmation code?"**
Two modes. With Daraja credentials configured (per pharmacy in System → M-Pesa),
STK Push fires to the customer's phone automatically and the POS polls until it
completes. Without credentials, MANUAL mode: customer pays, cashier types the
confirmation code. Card is wired behind the same settings pattern.

**"What if two tills sell the last item at the same moment?"**
Stock allocation runs under database row locks with FEFO ordering. The second
till waits, re-checks, and gets "Insufficient stock" — overselling is impossible.

**"Customer returns something?"**
Returns require an open shift with enough drawer cash; refund pays out of the
drawer with a logged CASH_OUT transaction; returned goods go to QUARANTINE for
inspection, never silently back on the shelf. Each line can only be returned once.

## Control & Theft

**"How do I catch theft / till shortages?"**
Every shift close compares expected vs counted cash and flags variance. The
owner's Shift history page shows all branches' shifts with variances highlighted,
plus a Resolve action that records who reviewed it and why. Sensitive actions
(sales, voids, user changes, PO approvals) are audit-logged.

**"Can staff give themselves discounts?"**
Line discounts are capped by the pharmacy's configured max (default 20%).
Attempts above the cap are rejected at checkout. Every discount is stored per line.

**"Who did what?"**
Audit log page with date/entity/action filters, CSV export. Login history exists;
PO approval and payment processing are audited.

## Multi-branch & Staff

**"I have 3 shops — how do branches work?"**
Owner creates branches, registers staff into each branch, pairs terminals per
branch, and sees one shift-reconciliation view across all of them. Reports can be
per-branch or pharmacy-wide. Stock, sales, and notifications are branch-scoped.

**"Can staff work across branches?"**
Each user belongs to a branch; roles are branch-assigned (user_branch_role).
The owner can move a staff member between branches from the Staff page.

**"What stops a cashier seeing another shop's data?"**
Every query is pharmacy+branch scoped server-side; cross-branch requests get 403.
Pharmacy-wide views are owner-only.

## Inventory

**"Expiring drugs?"**
Dashboard counters, inventory expiry list with days-remaining badges, automatic
bell notifications inside the alert window, and a Write-off action that removes
stock with a regulatory disposal log + EXPIRED stock movement. Expired batches
are never sellable (FEFO skips them).

**"Running low — reorder?"**
Low-stock alerts show available vs reorder level; one click drafts a purchase
order pre-filled with suggested quantities. The full PO lifecycle works:
create → approve → receive via GRN → stock updates with weighted-average cost.

**"Price changes — history kept?"**
Yes. Every selling/buying price change writes price history, viewable per medicine.

## Hardware & Devices

**"What hardware do you support?"**
Receipt printers, barcode scanners, cash drawers, customer displays via a local
connector service on the till (ESC/POS, serial, USB). Configured per terminal
from the admin UI with live health status.

**"How does the till know its terminal?"**
One-time pairing codes: generate on the Terminals page, enter once on that
device's POS screen. Heartbeats every 2 minutes keep the online status honest.

**"Handhelds?"**
The POS is responsive with a mobile bottom-tab bar (Sell / Shift / Receipts /
Customers / Stock) — same backend, same login, works on Android/iPhone browsers.

## Reliability & Compliance

**"Internet goes down mid-day?"**
Checkout retries safely (idempotency keys prevent double charges); if the till
was already signed in, it keeps working offline and warns on the banner; queued
M-Pesa STK reservations roll back cleanly on failure.

**"KRA / eTIMS?"**
Architecture is ready (TIS facade, fiscal document tables, tax categories
A–E style mapping, invoice sequences) but deliberately disabled pending KRA
certification. Receipts carry PIN, address, items count, and payment breakdown;
COPY reprints are watermarked.

**"Backups?"**
Postgres runs in a named Docker volume; backup automation is on the roadmap —
for pilot sites we snapshot the volume on schedule.

## Demo logins

| Role | Email | Password |
|------|-------|----------|
| Owner | admin@demo.com | admin123 |
| Manager | manager@demo.com | manager123 |
| Pharmacist | pharmacist@demo.com | pharmacist123 |
| Cashier | cashier@demo.com | cashier123 |
| Store keeper | storekeeper@demo.com | stock1234 |
| Technician | technician@demo.com | tech12345 |

Seed data: `powershell -File scripts\seed-demo-data.ps1` (medicines, suppliers,
customers, near-expiry + low-stock batches).
