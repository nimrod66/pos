"use client";

import { ReceiptText, Search } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";

import { EmptyState } from "@/components/ui/empty-state";
import { Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { addMoney, formatKes } from "@/features/workspace/lib/money";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { formatDateTime } from "@/lib/format";

function saleStatusTone(status: string) {
  if (status === "COMPLETED") return "success" as const;
  if (status === "RETURNED" || status === "CANCELLED") return "danger" as const;
  return "warning" as const;
}

const reportableStatuses = new Set(["COMPLETED", "PARTIALLY_RETURNED", "RETURNED"]);

export function SalesPage() {
  const sales = useWorkspaceQuery((state) => state.sales);
  const [query, setQuery] = useState("");
  const [payment, setPayment] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const normalized = query.trim().toLowerCase();
  const visibleSales = useMemo(
    () =>
      sales.filter((sale) =>
        (!normalized || [sale.receiptNumber, sale.cashierName, ...sale.items.map((item) => item.medicineName)].some((value) => value.toLowerCase().includes(normalized))) &&
        (payment === "ALL" || sale.payments.some((item) => item.method === payment)) &&
        (status === "ALL" || sale.status === status),
      ),
    [normalized, payment, sales, status],
  );
  const completedSales = sales.filter((sale) => reportableStatuses.has(sale.status));
  const netSales = addMoney(...completedSales.map((sale) => addMoney(sale.total, `-${sale.refundTotal}`)));
  const cashSales = addMoney(...completedSales.flatMap((sale) => sale.payments.filter((item) => item.method === "CASH").map((item) => item.amount)));
  const mpesaSales = addMoney(...completedSales.flatMap((sale) => sale.payments.filter((item) => item.method === "MPESA").map((item) => item.amount)));

  return (
    <div>
      <PageHeader title="Sales & receipts" description="Review completed transactions, payment references, receipts, and item returns." />
      <div className="mb-6 grid gap-px overflow-hidden rounded-md border border-[var(--border)] bg-[var(--border)] sm:grid-cols-2 xl:grid-cols-4">
        {[["Net sales", formatKes(netSales)], ["Receipts", String(sales.length)], ["Cash", formatKes(cashSales)], ["M-Pesa", formatKes(mpesaSales)]].map(([label, value]) => (
          <div className="bg-white p-4" key={label}><p className="text-xs text-[var(--text-muted)]">{label}</p><p className="mt-1 text-xl font-semibold">{value}</p></div>
        ))}
      </div>
      <section className="rounded-md border border-[var(--border)] bg-white">
        <div className="grid gap-3 border-b border-[var(--border)] p-4 md:grid-cols-[minmax(240px,1fr)_180px_200px]">
          <label className="relative"><span className="sr-only">Search sales</span><Search aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]" size={17} /><Input className="pl-9" placeholder="Receipt, cashier, or medicine" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
          <label><span className="sr-only">Payment method</span><Select value={payment} onChange={(event) => setPayment(event.target.value)}><option value="ALL">All payments</option><option value="CASH">Cash</option><option value="MPESA">M-Pesa</option></Select></label>
          <label><span className="sr-only">Sale status</span><Select value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All statuses</option><option value="COMPLETED">Completed</option><option value="PARTIALLY_RETURNED">Partially returned</option><option value="RETURNED">Returned</option><option value="SUSPENDED">Suspended</option><option value="CANCELLED">Cancelled</option><option value="UNKNOWN">Unknown</option></Select></label>
        </div>
        {visibleSales.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[940px] text-left text-sm">
              <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]"><tr><th className="px-4 py-3 font-semibold">Receipt</th><th className="px-4 py-3 font-semibold">Completed</th><th className="px-4 py-3 font-semibold">Cashier</th><th className="px-4 py-3 font-semibold">Payment</th><th className="px-4 py-3 text-right font-semibold">Items</th><th className="px-4 py-3 text-right font-semibold">Net total</th><th className="px-4 py-3 font-semibold">Status</th></tr></thead>
              <tbody className="divide-y divide-[var(--border)]">{visibleSales.map((sale) => (
                <tr key={sale.id} className="hover:bg-[var(--surface-muted)]/60">
                  <td className="px-4 py-3.5"><Link href={`/sales/${sale.id}`} className="font-semibold text-[var(--brand-strong)] hover:underline">{sale.receiptNumber}</Link></td>
                  <td className="whitespace-nowrap px-4 py-3.5 text-[var(--text-muted)]">{formatDateTime(sale.completedAt)}</td>
                  <td className="px-4 py-3.5">{sale.cashierName}</td>
                  <td className="px-4 py-3.5"><p className="font-medium">{sale.payments.length > 1 ? "Mixed" : sale.payments[0]?.method === "MPESA" ? "M-Pesa" : "Cash"}</p>{sale.payments[0]?.reference ? <p className="mt-0.5 font-mono text-xs text-[var(--text-muted)]">{sale.payments[0].reference}</p> : null}</td>
                  <td className="px-4 py-3.5 text-right">{sale.items.reduce((sum, item) => sum + item.quantity, 0)}</td>
                  <td className="px-4 py-3.5 text-right font-semibold">{formatKes(addMoney(sale.total, `-${sale.refundTotal}`))}</td>
                  <td className="px-4 py-3.5"><StatusBadge tone={saleStatusTone(sale.status)}>{sale.status.replaceAll("_", " ").toLowerCase()}</StatusBadge></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : <EmptyState icon={ReceiptText} title="No sales found" description="Adjust the filters or complete a sale in the POS." />}
        <div className="border-t border-[var(--border)] px-4 py-3 text-xs text-[var(--text-muted)]">Showing {visibleSales.length} of {sales.length} receipts</div>
      </section>
    </div>
  );
}
