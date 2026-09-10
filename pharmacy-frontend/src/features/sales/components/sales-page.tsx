"use client";

import {
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type SortingState,
} from "@tanstack/react-table";
import { ArrowDownUp, ChevronDown, ChevronUp, ReceiptText, Search } from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { EmptyState } from "@/components/ui/empty-state";
import { Input, Select } from "@/components/ui/form-controls";
import { PaginationControls } from "@/components/ui/pagination";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { addMoney, formatKes } from "@/features/workspace/lib/money";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { formatDateTime } from "@/lib/format";
import type { Sale } from "@/features/workspace/types";

function saleStatusTone(status: string) {
  if (status === "COMPLETED") return "success" as const;
  if (status === "RETURNED" || status === "CANCELLED") return "danger" as const;
  return "warning" as const;
}

const reportableStatuses = new Set(["COMPLETED", "PARTIALLY_RETURNED", "RETURNED"]);
const rightAlignedColumns = new Set(["items", "netTotal"]);

const salesColumns: ColumnDef<Sale>[] = [
  {
    accessorKey: "receiptNumber",
    header: "Receipt",
    cell: ({ row }) => (
      <Link
        href={`/sales/${row.original.id}`}
        className="font-semibold text-[var(--brand-strong)] hover:underline"
      >
        {row.original.receiptNumber}
      </Link>
    ),
  },
  {
    accessorKey: "completedAt",
    header: "Completed",
    cell: ({ row }) => (
      <span className="whitespace-nowrap text-[var(--text-muted)]">
        {formatDateTime(row.original.completedAt)}
      </span>
    ),
  },
  { accessorKey: "cashierName", header: "Cashier" },
  {
    id: "payment",
    accessorFn: (sale) =>
      sale.payments.length > 1
        ? "Mixed"
        : sale.payments[0]?.method === "MPESA"
          ? "M-Pesa"
          : "Cash",
    header: "Payment",
    cell: ({ row }) => {
      const payment = row.original.payments[0];
      return (
        <>
          <p className="font-medium">{row.getValue("payment") as string}</p>
          {payment?.reference ? (
            <p className="mt-0.5 font-mono text-xs text-[var(--text-muted)]">
              {payment.reference}
            </p>
          ) : null}
        </>
      );
    },
  },
  {
    id: "items",
    accessorFn: (sale) => sale.items.reduce((sum, item) => sum + item.quantity, 0),
    header: "Items",
  },
  {
    id: "netTotal",
    accessorFn: (sale) => Number(addMoney(sale.total, `-${sale.refundTotal}`)),
    header: "Net total",
    cell: ({ row }) => (
      <span className="font-semibold">
        {formatKes(addMoney(row.original.total, `-${row.original.refundTotal}`))}
      </span>
    ),
  },
  {
    accessorKey: "status",
    header: "Status",
    enableSorting: false,
    cell: ({ row }) => (
      <StatusBadge tone={saleStatusTone(row.original.status)}>
        {row.original.status.replaceAll("_", " ").toLowerCase()}
      </StatusBadge>
    ),
  },
];

export function SalesPage() {
  const sales = useWorkspaceQuery((state) => state.sales);
  const [query, setQuery] = useState("");
  const [payment, setPayment] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [sorting, setSorting] = useState<SortingState>([]);
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
  const table = useReactTable({
    columns: salesColumns,
    data: visibleSales,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    initialState: { pagination: { pageSize: 25 } },
    onSortingChange: setSorting,
    state: { sorting },
  });
  useEffect(() => {
    table.setPageIndex(0);
  }, [payment, query, status, table]);
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
              <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
                {table.getHeaderGroups().map((headerGroup) => (
                  <tr key={headerGroup.id}>
                    {headerGroup.headers.map((header) => {
                      const align = rightAlignedColumns.has(header.column.id);
                      const sorted = header.column.getIsSorted();
                      return (
                        <th
                          key={header.id}
                          className={`px-4 py-3 font-semibold ${align ? "text-right" : ""}`}
                        >
                          {header.isPlaceholder ? null : header.column.getCanSort() ? (
                            <button
                              type="button"
                              className={`inline-flex items-center gap-1 hover:text-[var(--text)] ${align ? "ml-auto" : ""}`}
                              onClick={header.column.getToggleSortingHandler()}
                            >
                              {flexRender(
                                header.column.columnDef.header,
                                header.getContext(),
                              )}
                              {sorted === "asc" ? (
                                <ChevronUp aria-hidden="true" size={14} />
                              ) : sorted === "desc" ? (
                                <ChevronDown aria-hidden="true" size={14} />
                              ) : (
                                <ArrowDownUp aria-hidden="true" size={13} />
                              )}
                            </button>
                          ) : (
                            flexRender(header.column.columnDef.header, header.getContext())
                          )}
                        </th>
                      );
                    })}
                  </tr>
                ))}
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {table.getRowModel().rows.map((row) => (
                  <tr key={row.id} className="hover:bg-[var(--surface-muted)]/60">
                    {row.getVisibleCells().map((cell) => (
                      <td
                        key={cell.id}
                        className={`px-4 py-3.5 ${rightAlignedColumns.has(cell.column.id) ? "text-right" : ""}`}
                      >
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <EmptyState icon={ReceiptText} title="No sales found" description="Adjust the filters or complete a sale in the POS." />}
        <PaginationControls
          page={table.getState().pagination.pageIndex + 1}
          pageCount={table.getPageCount()}
          total={visibleSales.length}
          pageSize={table.getState().pagination.pageSize}
          onPage={(page) => table.setPageIndex(page - 1)}
        />
      </section>
    </div>
  );
}
