"use client";

import {
  ArrowLeft,
  ChevronDown,
  ChevronRight,
  Clock,
  FileText,
  ShoppingBag,
  Star,
} from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { SecondaryLink } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import type {
  CustomerHistory,
  CustomerSale,
} from "@/features/workspace/types";
import { formatKes } from "@/features/workspace/lib/money";
import { ApiClientError, apiRequest } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

function prescriptionStatusTone(status: string) {
  switch (status) {
    case "APPROVED":
      return "success" as const;
    case "PENDING":
      return "warning" as const;
    case "REJECTED":
      return "danger" as const;
    default:
      return "neutral" as const;
  }
}

function prescriptionStatusLabel(status: string) {
  return status.replaceAll("_", " ").toLowerCase();
}

export function CustomerHistoryPage({
  customerId,
}: {
  customerId: string;
}) {
  const canRead = usePermission(PERMISSIONS.CUSTOMER_READ);
  const [history, setHistory] = useState<CustomerHistory | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedSales, setExpandedSales] = useState<Set<string>>(new Set());

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiRequest<CustomerHistory>(
        `/reports/customer-history/${customerId}`,
      );
      setHistory(response.data);
    } catch (caught) {
      setError(
        caught instanceof ApiClientError || caught instanceof Error
          ? caught.message
          : "Customer history could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, [customerId]);

  useEffect(() => {
    if (canRead) {
      void loadHistory();
    }
  }, [canRead, loadHistory]);

  function toggleSale(saleId: string) {
    setExpandedSales((prev) => {
      const next = new Set(prev);
      if (next.has(saleId)) {
        next.delete(saleId);
      } else {
        next.add(saleId);
      }
      return next;
    });
  }

  if (!canRead) {
    return (
      <div className="rounded-md border border-[var(--border)] bg-white p-6">
        <p className="text-sm text-[var(--text-muted)]">
          You do not have permission to view customer history.
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="rounded-md border border-[var(--border)] bg-white p-6">
        <h1 className="text-lg font-semibold">Loading customer history</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          Fetching customer details...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <PageHeader
          title="Customer history"
          actions={
            <SecondaryLink href="/customers">
              <ArrowLeft aria-hidden="true" size={17} />
              Back to customers
            </SecondaryLink>
          }
        />
        <FormError message={error} />
      </div>
    );
  }

  if (!history) {
    return (
      <div>
        <PageHeader
          title="Customer history"
          actions={
            <SecondaryLink href="/customers">
              <ArrowLeft aria-hidden="true" size={17} />
              Back to customers
            </SecondaryLink>
          }
        />
        <EmptyState
          icon={ShoppingBag}
          title="Customer not found"
          description="No history is available for this customer."
        />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        eyebrow="Customer history"
        title={history.fullName}
        description={
          history.phoneNumber
            ? `Phone: ${history.phoneNumber}`
            : undefined
        }
        actions={
          <SecondaryLink href="/customers">
            <ArrowLeft aria-hidden="true" size={17} />
            Back to customers
          </SecondaryLink>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <div className="flex items-center gap-2 text-xs text-[var(--text-muted)]">
            <Star aria-hidden="true" size={14} />
            Loyalty points
          </div>
          <p className="mt-1 text-2xl font-semibold">
            {history.loyaltyPoints}
          </p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <div className="flex items-center gap-2 text-xs text-[var(--text-muted)]">
            <ShoppingBag aria-hidden="true" size={14} />
            Total sales
          </div>
          <p className="mt-1 text-2xl font-semibold">
            {history.totalSales}
          </p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <div className="flex items-center gap-2 text-xs text-[var(--text-muted)]">
            <Clock aria-hidden="true" size={14} />
            Total spent
          </div>
          <p className="mt-1 text-2xl font-semibold">
            {formatKes(history.totalSpent)}
          </p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <div className="flex items-center gap-2 text-xs text-[var(--text-muted)]">
            <FileText aria-hidden="true" size={14} />
            Prescriptions
          </div>
          <p className="mt-1 text-2xl font-semibold">
            {history.prescriptions.length}
          </p>
        </div>
      </div>

      <section className="mb-6 overflow-hidden rounded-md border border-[var(--border)] bg-white">
        <div className="border-b border-[var(--border)] px-4 py-3">
          <h2 className="text-sm font-semibold">Recent sales</h2>
        </div>
        {history.recentSales.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[600px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="w-10 px-4 py-3">
                    <span className="sr-only">Expand</span>
                  </th>
                  <th className="px-4 py-3 font-semibold">Date</th>
                  <th className="px-4 py-3 font-semibold">Payment</th>
                  <th className="px-4 py-3 text-right font-semibold">Total</th>
                  <th className="w-20 px-4 py-3">
                    <span className="sr-only">View</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {history.recentSales.map((sale) => (
                  <SaleRow
                    key={sale.id}
                    sale={sale}
                    expanded={expandedSales.has(sale.id)}
                    onToggle={() => toggleSale(sale.id)}
                  />
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={ShoppingBag}
            title="No sales recorded"
            description="This customer has not made any purchases yet."
          />
        )}
      </section>

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        <div className="border-b border-[var(--border)] px-4 py-3">
          <h2 className="text-sm font-semibold">Prescriptions</h2>
        </div>
        {history.prescriptions.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[700px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Number</th>
                  <th className="px-4 py-3 font-semibold">Doctor</th>
                  <th className="px-4 py-3 font-semibold">Date</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Items</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {history.prescriptions.map((prescription) => (
                  <tr
                    key={prescription.id}
                    className="hover:bg-[var(--surface-muted)]/60"
                  >
                    <td className="px-4 py-3 font-medium">
                      {prescription.prescriptionNumber}
                    </td>
                    <td className="px-4 py-3 text-[var(--text-muted)]">
                      {prescription.doctorName}
                    </td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {formatDateTime(prescription.issuedDate)}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge
                        tone={prescriptionStatusTone(prescription.status)}
                      >
                        {prescriptionStatusLabel(prescription.status)}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {prescription.items.map((item) => item.medicineName).join(", ")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={FileText}
            title="No prescriptions"
            description="No prescriptions have been recorded for this customer."
          />
        )}
      </section>
    </div>
  );
}

function SaleRow({
  sale,
  expanded,
  onToggle,
}: {
  sale: CustomerSale;
  expanded: boolean;
  onToggle: () => void;
}) {
  return (
    <>
      <tr className="hover:bg-[var(--surface-muted)]/60">
        <td className="px-4 py-3">
          <button
            type="button"
            aria-label={expanded ? "Collapse items" : "Expand items"}
            className="flex size-7 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-white hover:text-[var(--text)]"
            onClick={onToggle}
          >
            {expanded ? (
              <ChevronDown aria-hidden="true" size={16} />
            ) : (
              <ChevronRight aria-hidden="true" size={16} />
            )}
          </button>
        </td>
        <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
          {formatDateTime(sale.completedAt)}
        </td>
        <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
          {sale.paymentMethod}
        </td>
        <td className="px-4 py-3 text-right font-semibold">
          {formatKes(sale.total)}
        </td>
        <td className="px-4 py-3 text-right">
          <Link
            href={`/sales/${sale.id}`}
            className="text-xs font-semibold text-[var(--brand-strong)] hover:underline"
          >
            View
          </Link>
        </td>
      </tr>
      {expanded ? (
        <tr>
          <td colSpan={5} className="bg-[var(--surface-muted)]/40 px-4 py-3">
            <table className="w-full text-xs">
              <thead>
                <tr className="text-[var(--text-muted)]">
                  <th className="pb-1 pr-4 text-left font-semibold">Item</th>
                  <th className="pb-1 pr-4 text-right font-semibold">Qty</th>
                  <th className="pb-1 pr-4 text-right font-semibold">Price</th>
                  <th className="pb-1 text-right font-semibold">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {sale.items.map((item, index) => (
                  <tr key={index}>
                    <td className="py-1.5 pr-4 font-medium">
                      {item.medicineName}
                    </td>
                    <td className="py-1.5 pr-4 text-right">{item.quantity}</td>
                    <td className="py-1.5 pr-4 text-right">
                      {formatKes(item.unitPrice)}
                    </td>
                    <td className="py-1.5 text-right font-medium">
                      {formatKes(item.lineTotal)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </td>
        </tr>
      ) : null}
    </>
  );
}
