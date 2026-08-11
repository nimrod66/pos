"use client";

import {
  Banknote,
  BarChart3,
  Boxes,
  ReceiptText,
  RotateCcw,
  Smartphone,
} from "lucide-react";
import { useMemo, useState } from "react";

import { Input } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  addMoney,
  centsToMoney,
  formatKes,
  moneyToCents,
} from "@/features/workspace/lib/money";
import {
  stockForMedicine,
  stockValue,
  todayIsoDate,
} from "@/features/workspace/lib/workspace-helpers";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { cn } from "@/lib/cn";

export function ReportsPage() {
  const sales = useWorkspaceQuery((state) => state.sales);
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const batches = useWorkspaceQuery((state) => state.batches);
  const canViewSales = usePermission(PERMISSIONS.REPORT_SALES_READ);
  const canViewInventory = usePermission(PERMISSIONS.REPORT_INVENTORY_READ);
  const [from, setFrom] = useState(todayIsoDate());
  const [to, setTo] = useState(todayIsoDate());

  const filteredSales = useMemo(
    () =>
      sales.filter(
        (sale) =>
          sale.completedAt.slice(0, 10) >= from &&
          sale.completedAt.slice(0, 10) <= to,
      ),
    [from, sales, to],
  );
  const grossSales = addMoney(...filteredSales.map((sale) => sale.total));
  const refunds = addMoney(
    ...filteredSales.map((sale) => sale.refundTotal),
  );
  const netSales = addMoney(grossSales, `-${refunds}`);
  const cash = addMoney(
    ...filteredSales
      .filter((sale) => sale.payments[0]?.method === "CASH")
      .map((sale) => addMoney(sale.total, `-${sale.refundTotal}`)),
  );
  const mpesa = addMoney(
    ...filteredSales
      .filter((sale) => sale.payments[0]?.method === "MPESA")
      .map((sale) => addMoney(sale.total, `-${sale.refundTotal}`)),
  );
  const inventoryValue = centsToMoney(stockValue(medicines, batches));
  const lowStock = medicines.filter(
    (medicine) =>
      stockForMedicine(batches, medicine.id) <= medicine.reorderLevel,
  );
  const topProducts = useMemo(() => {
    const totals = new Map<
      string,
      { name: string; quantity: number; revenueCents: number }
    >();
    for (const sale of filteredSales) {
      for (const item of sale.items) {
        const current = totals.get(item.medicineId) ?? {
          name: item.medicineName,
          quantity: 0,
          revenueCents: 0,
        };
        const netQuantity = item.quantity - item.returnedQuantity;
        current.quantity += netQuantity;
        current.revenueCents += moneyToCents(item.unitPrice) * netQuantity;
        totals.set(item.medicineId, current);
      }
    }
    return [...totals.values()]
      .sort((left, right) => right.revenueCents - left.revenueCents)
      .slice(0, 6);
  }, [filteredSales]);
  const maxRevenue = Math.max(
    ...topProducts.map((product) => product.revenueCents),
    1,
  );
  const paymentTotal = Math.max(moneyToCents(netSales), 1);

  if (!canViewSales && !canViewInventory) {
    return <AccessRestricted />;
  }

  const summaryCards = [
    ...(canViewSales
      ? [
          {
            label: "Net sales",
            value: formatKes(netSales),
            detail: `${filteredSales.length} receipts`,
            icon: BarChart3,
            tone: "text-[var(--brand)] bg-[var(--brand-soft)]",
          },
          {
            label: "Refunds",
            value: formatKes(refunds),
            detail: "Recorded returns",
            icon: RotateCcw,
            tone: "text-[var(--danger)] bg-[var(--danger-soft)]",
          },
        ]
      : []),
    ...(canViewInventory
      ? [
          {
            label: "Stock at cost",
            value: formatKes(inventoryValue),
            detail: `${batches.length} batches`,
            icon: Boxes,
            tone: "text-[var(--accent)] bg-[var(--accent-soft)]",
          },
          {
            label: "Low stock",
            value: String(lowStock.length),
            detail: "Needs attention",
            icon: ReceiptText,
            tone: "text-[var(--warning)] bg-[var(--warning-soft)]",
          },
        ]
      : []),
  ];

  return (
    <div>
      <PageHeader
        title="Reports"
        description={
          canViewSales && canViewInventory
            ? "Sales, payments, returns, and stock exposure."
            : canViewSales
              ? "Sales, payments, and returns."
              : "Stock value and reorder exposure."
        }
        actions={
          canViewSales ? (
            <div className="flex items-end gap-2">
              <label className="text-xs font-medium text-[var(--text-muted)]">
                <span className="mb-1 block">From</span>
                <Input
                  type="date"
                  value={from}
                  onChange={(event) => setFrom(event.target.value)}
                  className="min-h-9"
                />
              </label>
              <label className="text-xs font-medium text-[var(--text-muted)]">
                <span className="mb-1 block">To</span>
                <Input
                  type="date"
                  min={from}
                  value={to}
                  onChange={(event) => setTo(event.target.value)}
                  className="min-h-9"
                />
              </label>
            </div>
          ) : undefined
        }
      />

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {summaryCards.map(({ detail, icon: Icon, label, tone, value }) => (
          <div
            key={label}
            className="rounded-md border border-[var(--border)] bg-white p-4"
          >
            <div
              className={cn(
                "flex size-9 items-center justify-center rounded-md",
                tone,
              )}
            >
              <Icon aria-hidden="true" size={18} />
            </div>
            <p className="mt-4 text-xs text-[var(--text-muted)]">{label}</p>
            <p className="mt-1 text-xl font-semibold">{value}</p>
            <p className="mt-1 text-xs text-[var(--text-subtle)]">{detail}</p>
          </div>
        ))}
      </div>

      {canViewSales ? (
        <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1.1fr)_minmax(320px,0.9fr)]">
          <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
            <h2 className="text-sm font-semibold">
              Top products by net revenue
            </h2>
            {topProducts.length ? (
              <div className="mt-5 space-y-4">
                {topProducts.map((product, index) => (
                  <div key={product.name}>
                    <div className="mb-1.5 flex items-center justify-between gap-4 text-sm">
                      <span className="truncate">
                        <span className="mr-2 text-xs text-[var(--text-subtle)]">
                          {index + 1}
                        </span>
                        <strong>{product.name}</strong>{" "}
                        <span className="text-xs text-[var(--text-muted)]">
                          - {product.quantity} sold
                        </span>
                      </span>
                      <span className="shrink-0 font-semibold">
                        {formatKes(centsToMoney(product.revenueCents))}
                      </span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-[var(--surface-muted)]">
                      <div
                        className="h-full rounded-full bg-[var(--brand)]"
                        style={{
                          width: `${Math.max(5, (product.revenueCents / maxRevenue) * 100)}%`,
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="mt-6 text-sm text-[var(--text-muted)]">
                No sales fall within this date range.
              </p>
            )}
          </section>

          <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
            <h2 className="text-sm font-semibold">Payment mix</h2>
            <div className="mt-5 space-y-5">
              {[
                {
                  label: "Cash",
                  amount: cash,
                  icon: Banknote,
                  color: "bg-[var(--success)]",
                },
                {
                  label: "M-Pesa",
                  amount: mpesa,
                  icon: Smartphone,
                  color: "bg-[var(--accent)]",
                },
              ].map(({ amount, color, icon: Icon, label }) => (
                <div key={label}>
                  <div className="mb-2 flex items-center justify-between text-sm">
                    <span className="flex items-center gap-2 font-medium">
                      <Icon aria-hidden="true" size={16} /> {label}
                    </span>
                    <strong>{formatKes(amount)}</strong>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-[var(--surface-muted)]">
                    <div
                      className={cn("h-full rounded-full", color)}
                      style={{
                        width: `${Math.max(0, Math.min(100, (moneyToCents(amount) / paymentTotal) * 100))}%`,
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
            <div className="mt-6 border-t border-[var(--border)] pt-4">
              <div className="flex items-center justify-between text-sm">
                <span className="text-[var(--text-muted)]">Gross sales</span>
                <span>{formatKes(grossSales)}</span>
              </div>
              <div className="mt-2 flex items-center justify-between text-sm font-semibold">
                <span>Net sales</span>
                <span>{formatKes(netSales)}</span>
              </div>
            </div>
          </section>
        </div>
      ) : null}

      {canViewInventory ? (
        <section className="mt-6 rounded-md border border-[var(--border)] bg-white">
          <div className="border-b border-[var(--border)] px-4 py-3.5">
            <h2 className="text-sm font-semibold">Stock requiring reorder</h2>
          </div>
          {lowStock.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[650px] text-left text-sm">
                <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
                  <tr>
                    <th className="px-4 py-3 font-semibold">Medicine</th>
                    <th className="px-4 py-3 text-right font-semibold">
                      Available
                    </th>
                    <th className="px-4 py-3 text-right font-semibold">
                      Reorder at
                    </th>
                    <th className="px-4 py-3 font-semibold">State</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {lowStock.map((medicine) => {
                    const stock = stockForMedicine(batches, medicine.id);
                    return (
                      <tr key={medicine.id}>
                        <td className="px-4 py-3.5">
                          <p className="font-semibold">{medicine.brandName}</p>
                          <p className="text-xs text-[var(--text-muted)]">
                            {medicine.sku}
                          </p>
                        </td>
                        <td className="px-4 py-3.5 text-right font-semibold">
                          {stock}
                        </td>
                        <td className="px-4 py-3.5 text-right">
                          {medicine.reorderLevel}
                        </td>
                        <td className="px-4 py-3.5">
                          <StatusBadge tone="danger">
                            {stock === 0 ? "Out of stock" : "Low stock"}
                          </StatusBadge>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="p-5 text-sm text-[var(--text-muted)]">
              All active medicines are above their reorder levels.
            </p>
          )}
        </section>
      ) : null}
    </div>
  );
}
