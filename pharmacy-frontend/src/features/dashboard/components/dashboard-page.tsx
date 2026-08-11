"use client";

import {
  Banknote,
  CalendarClock,
  CircleAlert,
  Clock3,
  PackageSearch,
  ReceiptText,
} from "lucide-react";
import Link from "next/link";

import { useAuthStore } from "@/features/auth/store/auth-store";
import { formatTime } from "@/lib/format";
import { addMoney, formatKes } from "@/features/workspace/lib/money";
import {
  daysUntil,
  stockForMedicine,
} from "@/features/workspace/lib/workspace-helpers";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";

export function DashboardPage() {
  const session = useAuthStore((state) => state.session);
  const batches = useWorkspaceQuery((state) => state.batches);
  const currentShiftId = useWorkspaceQuery((state) => state.currentShiftId);
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const sales = useWorkspaceQuery((state) => state.sales);
  const shifts = useWorkspaceQuery((state) => state.shifts);

  if (!session) {
    return null;
  }

  const currentShift = shifts.find((shift) => shift.id === currentShiftId);
  const currentSales = currentShift
    ? sales.filter((sale) => sale.shiftId === currentShift.id)
    : [];
  const salesTotal = currentSales.reduce(
    (total, sale) => addMoney(total, sale.total, `-${sale.refundTotal}`),
    "0.00",
  );
  const lowStock = medicines.filter(
    (medicine) =>
      medicine.status === "ACTIVE" &&
      stockForMedicine(batches, medicine.id) <= medicine.reorderLevel,
  );
  const nearExpiry = batches.filter((batch) => {
    const days = daysUntil(batch.expiryDate);
    return batch.quantity > 0 && days >= 0 && days <= 90;
  });
  const expired = batches.filter(
    (batch) => batch.quantity > 0 && daysUntil(batch.expiryDate) < 0,
  );
  const summary = [
    {
      detail: `${currentSales.length} completed sale${currentSales.length === 1 ? "" : "s"}`,
      icon: Banknote,
      label: "Current shift sales",
      tone: "var(--brand)",
      value: formatKes(salesTotal),
    },
    {
      detail: `${currentSales.length} receipt${currentSales.length === 1 ? "" : "s"} issued`,
      icon: ReceiptText,
      label: "Transactions",
      tone: "var(--accent)",
      value: String(currentSales.length),
    },
    {
      detail: "At or below reorder level",
      icon: CircleAlert,
      label: "Low stock",
      tone: "var(--warning)",
      value: String(lowStock.length),
    },
    {
      detail: "Next 90 days",
      icon: CalendarClock,
      label: "Near expiry",
      tone: "var(--danger)",
      value: String(nearExpiry.length),
    },
  ];

  return (
    <div>
      <header className="mb-7 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="mb-1 text-sm text-[var(--text-muted)]">
            Welcome back, {session.user.displayName}
          </p>
          <h1 className="text-2xl font-semibold">Dashboard</h1>
        </div>
        <div className="flex items-center gap-2 text-sm text-[var(--text-muted)]">
          <Clock3 aria-hidden="true" size={16} />
          <span>{currentShift ? "Shift open" : "Shift not open"}</span>
        </div>
      </header>

      <section
        aria-label="Today's summary"
        className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4"
      >
        {summary.map(({ detail, icon: Icon, label, tone, value }) => (
          <article
            key={label}
            className="min-h-32 rounded-md border border-[var(--border)] bg-white p-4"
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-sm text-[var(--text-muted)]">{label}</p>
                <p className="mt-3 text-2xl font-semibold">{value}</p>
              </div>
              <span
                className="flex size-9 items-center justify-center rounded-md bg-[var(--surface-muted)]"
                style={{ color: tone }}
              >
                <Icon aria-hidden="true" size={18} />
              </span>
            </div>
            <p className="mt-2 text-xs text-[var(--text-subtle)]">{detail}</p>
          </article>
        ))}
      </section>

      <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
        <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
          <div className="flex h-14 items-center justify-between border-b border-[var(--border)] px-4 sm:px-5">
            <div>
              <h2 className="text-sm font-semibold">Recent sales</h2>
              <p className="text-xs text-[var(--text-muted)]">Today</p>
            </div>
            <ReceiptText
              aria-hidden="true"
              className="text-[var(--text-subtle)]"
              size={18}
            />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[34rem] border-collapse text-left text-sm">
              <thead className="bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="h-10 px-5 font-medium" scope="col">
                    Time
                  </th>
                  <th className="h-10 px-5 font-medium" scope="col">
                    Receipt
                  </th>
                  <th className="h-10 px-5 font-medium" scope="col">
                    Payment
                  </th>
                  <th className="h-10 px-5 text-right font-medium" scope="col">
                    Total
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {currentSales.slice(0, 6).map((sale) => (
                  <tr key={sale.id} className="hover:bg-[var(--surface-muted)]">
                    <td className="h-12 px-5 text-[var(--text-muted)]">
                      {formatTime(sale.completedAt)}
                    </td>
                    <td className="h-12 px-5 font-medium">
                      <Link className="hover:text-[var(--brand)]" href={`/sales/${sale.id}`}>
                        {sale.receiptNumber}
                      </Link>
                    </td>
                    <td className="h-12 px-5 text-[var(--text-muted)]">
                      {sale.payments[0]?.method === "MPESA" ? "M-Pesa" : "Cash"}
                    </td>
                    <td className="h-12 px-5 text-right font-medium">
                      {formatKes(sale.total)}
                    </td>
                  </tr>
                ))}
                {currentSales.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="h-48 px-5 text-center">
                      <div className="mx-auto flex max-w-xs flex-col items-center">
                        <span className="flex size-10 items-center justify-center rounded-md bg-[var(--surface-muted)] text-[var(--text-subtle)]">
                          <ReceiptText aria-hidden="true" size={19} />
                        </span>
                        <p className="mt-3 text-sm font-medium">No sales in this shift</p>
                        <p className="mt-1 text-xs text-[var(--text-muted)]">
                          Completed transactions will appear here.
                        </p>
                      </div>
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        <aside className="space-y-4">
          <section className="rounded-md border border-[var(--border)] bg-white p-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold">Current shift</h2>
              <span className="rounded-full bg-[var(--warning-soft)] px-2 py-1 text-xs font-medium text-[var(--warning)]">
                {currentShift ? "Open" : "Not open"}
              </span>
            </div>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex items-center justify-between gap-3">
                <dt className="text-[var(--text-muted)]">Cashier</dt>
                <dd className="truncate font-medium">{session.user.displayName}</dd>
              </div>
              <div className="flex items-center justify-between gap-3">
                <dt className="text-[var(--text-muted)]">Branch</dt>
                <dd className="truncate font-medium">
                  {session.user.activeBranch.name}
                </dd>
              </div>
            </dl>
          </section>

          <section className="rounded-md border border-[var(--border)] bg-white p-4">
            <div className="flex items-center gap-2">
              <PackageSearch
                aria-hidden="true"
                className="text-[var(--brand)]"
                size={18}
              />
              <h2 className="text-sm font-semibold">Stock attention</h2>
            </div>
            <dl className="mt-4 divide-y divide-[var(--border)] text-sm">
              <div className="flex h-10 items-center justify-between">
                <dt className="text-[var(--text-muted)]">Low stock</dt>
                <dd className="font-semibold">{lowStock.length}</dd>
              </div>
              <div className="flex h-10 items-center justify-between">
                <dt className="text-[var(--text-muted)]">Near expiry</dt>
                <dd className="font-semibold">{nearExpiry.length}</dd>
              </div>
              <div className="flex h-10 items-center justify-between">
                <dt className="text-[var(--text-muted)]">Expired</dt>
                <dd className="font-semibold">{expired.length}</dd>
              </div>
            </dl>
          </section>
        </aside>
      </div>
    </div>
  );
}
