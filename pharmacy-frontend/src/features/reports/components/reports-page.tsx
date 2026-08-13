"use client";

import {
  Banknote,
  BarChart3,
  Boxes,
  CreditCard,
  ReceiptText,
  RotateCcw,
  Smartphone,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { FormError, Input } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import {
  getWorkspaceErrorMessage,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { addMoney, formatKes, moneyToCents } from "@/features/workspace/lib/money";
import { todayIsoDate } from "@/features/workspace/lib/workspace-helpers";
import type {
  InventoryReport,
  SalesReport,
} from "@/features/workspace/types";
import { cn } from "@/lib/cn";

export function ReportsPage() {
  const branchId = useAuthStore(
    (state) => state.session?.user.activeBranch.id ?? null,
  );
  const canViewSales = usePermission(PERMISSIONS.REPORT_SALES_READ);
  const canViewInventory = usePermission(PERMISSIONS.REPORT_INVENTORY_READ);
  const [from, setFrom] = useState(todayIsoDate());
  const [to, setTo] = useState(todayIsoDate());
  const [salesReport, setSalesReport] = useState<SalesReport | null>(null);
  const [inventoryReport, setInventoryReport] =
    useState<InventoryReport | null>(null);
  const [salesError, setSalesError] = useState<string | null>(null);
  const [inventoryError, setInventoryError] = useState<string | null>(null);

  useEffect(() => {
    if (!branchId || !canViewSales || !from || !to || to < from) return;
    let active = true;
    void workspaceGateway
      .getSalesReport(from, to)
      .then((report) => {
        if (!active) return;
        setSalesReport(report);
        setSalesError(null);
      })
      .catch((error) => {
        if (!active) return;
        setSalesError(
          getWorkspaceErrorMessage(error, "The sales report could not be loaded."),
        );
      });
    return () => {
      active = false;
    };
  }, [branchId, canViewSales, from, to]);

  useEffect(() => {
    if (!branchId || !canViewInventory) return;
    let active = true;
    void workspaceGateway
      .getInventoryReport()
      .then((report) => {
        if (!active) return;
        setInventoryReport(report);
        setInventoryError(null);
      })
      .catch((error) => {
        if (!active) return;
        setInventoryError(
          getWorkspaceErrorMessage(
            error,
            "The inventory report could not be loaded.",
          ),
        );
      });
    return () => {
      active = false;
    };
  }, [branchId, canViewInventory]);

  const paymentRows = useMemo(() => {
    if (!salesReport) return [];
    return [
      {
        amount: addMoney(salesReport.cashPayments, `-${salesReport.cashRefunds}`),
        color: "bg-[var(--success)]",
        icon: Banknote,
        label: "Cash",
      },
      {
        amount: addMoney(
          salesReport.mpesaPayments,
          `-${salesReport.mpesaRefunds}`,
        ),
        color: "bg-[var(--accent)]",
        icon: Smartphone,
        label: "M-Pesa",
      },
      {
        amount: addMoney(
          salesReport.otherPayments,
          `-${salesReport.otherRefunds}`,
        ),
        color: "bg-[var(--warning)]",
        icon: CreditCard,
        label: "Other",
      },
    ].filter((row) => moneyToCents(row.amount) !== 0);
  }, [salesReport]);
  const paymentTotal = Math.max(
    paymentRows.reduce(
      (total, row) => total + Math.max(0, moneyToCents(row.amount)),
      0,
    ),
    1,
  );
  const maxRevenue = Math.max(
    ...(salesReport?.topProducts.map((product) =>
      moneyToCents(product.netRevenue),
    ) ?? []),
    1,
  );

  if (!canViewSales && !canViewInventory) {
    return <AccessRestricted />;
  }

  const summaryCards = [
    ...(canViewSales
      ? [
          {
            detail: `${salesReport?.completedSalesCount ?? 0} receipts`,
            icon: BarChart3,
            label: "Net sales",
            tone: "text-[var(--brand)] bg-[var(--brand-soft)]",
            value: salesReport ? formatKes(salesReport.netSales) : "-",
          },
          {
            detail: "Completed returns in range",
            icon: RotateCcw,
            label: "Refunds",
            tone: "text-[var(--danger)] bg-[var(--danger-soft)]",
            value: salesReport ? formatKes(salesReport.refunds) : "-",
          },
        ]
      : []),
    ...(canViewInventory
      ? [
          {
            detail: inventoryReport?.pharmacyWide
              ? `${inventoryReport.batchCount} batches across all branches`
              : `${inventoryReport?.batchCount ?? 0} stocked batches`,
            icon: Boxes,
            label: "Stock at cost",
            tone: "text-[var(--accent)] bg-[var(--accent-soft)]",
            value: inventoryReport ? formatKes(inventoryReport.stockValue) : "-",
          },
          {
            detail: inventoryReport?.pharmacyWide
              ? "Branch-level reorder alerts"
              : "At or below reorder level",
            icon: ReceiptText,
            label: "Low stock",
            tone: "text-[var(--warning)] bg-[var(--warning-soft)]",
            value: inventoryReport ? String(inventoryReport.lowStockCount) : "-",
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

      <FormError
        message={
          to < from
            ? "The end date must be on or after the start date."
            : salesError ?? inventoryError
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
            <h2 className="text-sm font-semibold">Top products by net revenue</h2>
            {salesReport?.topProducts.length ? (
              <div className="mt-5 space-y-4">
                {salesReport.topProducts.map((product, index) => {
                  const revenue = moneyToCents(product.netRevenue);
                  return (
                    <div key={product.medicineId}>
                      <div className="mb-1.5 flex items-center justify-between gap-4 text-sm">
                        <span className="truncate">
                          <span className="mr-2 text-xs text-[var(--text-subtle)]">
                            {index + 1}
                          </span>
                          <strong>{product.medicineName}</strong>{" "}
                          <span className="text-xs text-[var(--text-muted)]">
                            - {product.quantity} net sold
                          </span>
                        </span>
                        <span className="shrink-0 font-semibold">
                          {formatKes(product.netRevenue)}
                        </span>
                      </div>
                      <div className="h-2 overflow-hidden rounded-full bg-[var(--surface-muted)]">
                        <div
                          className="h-full rounded-full bg-[var(--brand)]"
                          style={{
                            width: `${Math.max(5, (Math.max(0, revenue) / maxRevenue) * 100)}%`,
                          }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="mt-6 text-sm text-[var(--text-muted)]">
                No completed sales fall within this date range.
              </p>
            )}
          </section>

          <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
            <h2 className="text-sm font-semibold">Payment mix after refunds</h2>
            {paymentRows.length ? (
              <div className="mt-5 space-y-5">
                {paymentRows.map(({ amount, color, icon: Icon, label }) => (
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
                          width: `${Math.max(0, Math.min(100, (Math.max(0, moneyToCents(amount)) / paymentTotal) * 100))}%`,
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="mt-5 text-sm text-[var(--text-muted)]">
                No completed payments fall within this date range.
              </p>
            )}
            <div className="mt-6 border-t border-[var(--border)] pt-4">
              <div className="flex items-center justify-between text-sm">
                <span className="text-[var(--text-muted)]">Gross sales</span>
                <span>{salesReport ? formatKes(salesReport.grossSales) : "-"}</span>
              </div>
              <div className="mt-2 flex items-center justify-between text-sm font-semibold">
                <span>Net sales</span>
                <span>{salesReport ? formatKes(salesReport.netSales) : "-"}</span>
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
          {inventoryReport?.lowStockItems.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[650px] text-left text-sm">
                <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
                  <tr>
                    {inventoryReport.pharmacyWide ? (
                      <th className="px-4 py-3 font-semibold">Branch</th>
                    ) : null}
                    <th className="px-4 py-3 font-semibold">Medicine</th>
                    <th className="px-4 py-3 text-right font-semibold">Available</th>
                    <th className="px-4 py-3 text-right font-semibold">Reorder at</th>
                    <th className="px-4 py-3 font-semibold">State</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {inventoryReport.lowStockItems.map((item) => (
                    <tr key={`${item.branchId}-${item.medicineId}`}>
                      {inventoryReport.pharmacyWide ? (
                        <td className="px-4 py-3.5 font-medium">
                          {item.branchName}
                        </td>
                      ) : null}
                      <td className="px-4 py-3.5">
                        <p className="font-semibold">{item.medicineName}</p>
                        <p className="text-xs text-[var(--text-muted)]">{item.sku}</p>
                      </td>
                      <td className="px-4 py-3.5 text-right font-semibold">
                        {item.available}
                      </td>
                      <td className="px-4 py-3.5 text-right">{item.reorderLevel}</td>
                      <td className="px-4 py-3.5">
                        <StatusBadge tone="danger">
                          {item.available === 0 ? "Out of stock" : "Low stock"}
                        </StatusBadge>
                      </td>
                    </tr>
                  ))}
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
