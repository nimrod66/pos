"use client";

import {
  ArrowLeft,
  BadgeDollarSign,
  CreditCard,
  DollarSign,
  FileText,
  Plus,
  TrendingDown,
  TrendingUp,
} from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import {
  Field,
  FormError,
  Input,
  Select,
  Textarea,
} from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { formatKes } from "@/features/workspace/lib/money";
import { ApiClientError, apiRequest } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

interface CustomerBalance {
  customerId: string;
  balance: number;
  creditLimit: number;
  availableCredit: number;
}

interface CustomerTransaction {
  id: string;
  type: string;
  amount: number;
  description: string;
  createdAt: string;
}

interface OutstandingSale {
  id: string;
  invoiceNumber: string;
  totalAmount: number;
  amountPaid: number;
  amountOwed: number;
  completedAt: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

export function CustomerCreditPage({
  customerId,
}: {
  customerId: string;
}) {
  const canRead = usePermission(PERMISSIONS.CUSTOMER_ACCOUNT_READ);
  const canWrite = usePermission(PERMISSIONS.CUSTOMER_ACCOUNT_WRITE);
  const [balance, setBalance] = useState<CustomerBalance | null>(null);
  const [transactions, setTransactions] = useState<CustomerTransaction[]>([]);
  const [outstandingSales, setOutstandingSales] = useState<OutstandingSale[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [showPaymentDialog, setShowPaymentDialog] = useState(false);
  const [showAdjustDialog, setShowAdjustDialog] = useState(false);
  const [showCreditLimitDialog, setShowCreditLimitDialog] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [paymentAmount, setPaymentAmount] = useState("");
  const [paymentNotes, setPaymentNotes] = useState("");
  const [adjustAmount, setAdjustAmount] = useState("");
  const [adjustType, setAdjustType] = useState("DEBIT");
  const [adjustNotes, setAdjustNotes] = useState("");
  const [newCreditLimit, setNewCreditLimit] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [balRes, txnRes, outRes] = await Promise.all([
        apiRequest<CustomerBalance>(`/customers/${customerId}/balance`),
        apiRequest<CustomerTransaction[]>(
          `/customers/${customerId}/transactions?size=100&sort=createdAt,desc`,
        ),
        apiRequest<OutstandingSale[]>(
          `/customers/${customerId}/outstanding-sales`,
        ),
      ]);
      setBalance(balRes.data);
      setTransactions(txnRes.data ?? []);
      setOutstandingSales(outRes.data ?? []);
    } catch (caught) {
      setError(
        errorMessage(caught, "Customer account data could not be loaded."),
      );
    } finally {
      setLoading(false);
    }
  }, [customerId]);

  useEffect(() => {
    if (canRead) void load();
  }, [canRead, load]);

  async function handleRecordPayment() {
    const amount = parseFloat(paymentAmount);
    if (isNaN(amount) || amount <= 0) {
      setActionError("Enter a valid payment amount.");
      return;
    }
    setActionLoading(true);
    setActionError(null);
    try {
      await apiRequest(`/customers/${customerId}/payments`, {
        method: "POST",
        body: { amount, notes: paymentNotes || undefined },
      });
      setShowPaymentDialog(false);
      setPaymentAmount("");
      setPaymentNotes("");
      await load();
    } catch (caught) {
      setActionError(errorMessage(caught, "Payment could not be recorded."));
    } finally {
      setActionLoading(false);
    }
  }

  async function handleAdjust() {
    const amount = parseFloat(adjustAmount);
    if (isNaN(amount) || amount <= 0) {
      setActionError("Enter a valid adjustment amount.");
      return;
    }
    setActionLoading(true);
    setActionError(null);
    try {
      await apiRequest(`/customers/${customerId}/adjustments`, {
        method: "POST",
        body: { amount, type: adjustType, notes: adjustNotes || undefined },
      });
      setShowAdjustDialog(false);
      setAdjustAmount("");
      setAdjustNotes("");
      await load();
    } catch (caught) {
      setActionError(errorMessage(caught, "Adjustment could not be applied."));
    } finally {
      setActionLoading(false);
    }
  }

  async function handleUpdateCreditLimit() {
    const limit = parseFloat(newCreditLimit);
    if (isNaN(limit) || limit < 0) {
      setActionError("Enter a valid credit limit.");
      return;
    }
    setActionLoading(true);
    setActionError(null);
    try {
      await apiRequest(`/customers/${customerId}/credit-limit`, {
        method: "PUT",
        body: { creditLimit: limit },
      });
      setShowCreditLimitDialog(false);
      setNewCreditLimit("");
      await load();
    } catch (caught) {
      setActionError(
        errorMessage(caught, "Credit limit could not be updated."),
      );
    } finally {
      setActionLoading(false);
    }
  }

  if (!canRead) {
    return (
      <div className="max-w-7xl">
        <PageHeader
          eyebrow="Customers"
          title="Customer credit"
          description="Manage customer account balance, credit limit, and payments."
        />
        <EmptyState
          icon={BadgeDollarSign}
          title="Access restricted"
          description="You do not have permission to view customer account data."
        />
      </div>
    );
  }

  const txnTypeIcon = (type: string) => {
    switch (type) {
      case "PAYMENT":
        return <TrendingDown className="h-4 w-4 text-[var(--success)]" />;
      case "SALE":
        return <TrendingUp className="h-4 w-4 text-[var(--danger)]" />;
      default:
        return <DollarSign className="h-4 w-4 text-[var(--text-muted)]" />;
    }
  };

  return (
    <div className="max-w-7xl">
      <div className="mb-4">
        <Link
          href={`/customers/${customerId}`}
          className="inline-flex items-center gap-1.5 text-sm text-[var(--text-muted)] hover:text-[var(--text)]"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to customer
        </Link>
      </div>

      <PageHeader
        eyebrow="Customers"
        title="Customer credit"
        description="Manage account balance, credit limit, and payments."
      />

      {error && (
        <div className="mb-5 rounded-md border border-[var(--danger-border)] bg-[var(--danger-soft)] px-3 py-2.5 text-sm text-[var(--danger)]">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-[var(--text-muted)]">Loading account data...</p>
      ) : balance ? (
        <>
          {/* Balance cards */}
          <div className="mb-6 grid gap-4 sm:grid-cols-3">
            <div className="rounded-md border border-[var(--border)] bg-white p-4">
              <p className="text-xs font-medium text-[var(--text-muted)]">
                Account Balance
              </p>
              <p
                className={`mt-1 text-2xl font-bold ${
                  balance.balance > 0
                    ? "text-[var(--danger)]"
                    : "text-[var(--success)]"
                }`}
              >
                {formatKes(balance.balance)}
              </p>
              <p className="mt-1 text-xs text-[var(--text-muted)]">
                {balance.balance > 0 ? "Amount owed by customer" : "No outstanding balance"}
              </p>
            </div>
            <div className="rounded-md border border-[var(--border)] bg-white p-4">
              <p className="text-xs font-medium text-[var(--text-muted)]">
                Credit Limit
              </p>
              <p className="mt-1 text-2xl font-bold">
                {formatKes(balance.creditLimit)}
              </p>
              {canWrite && (
                <button
                  onClick={() => {
                    setNewCreditLimit(String(balance.creditLimit));
                    setShowCreditLimitDialog(true);
                  }}
                  className="mt-1 text-xs text-[var(--primary)] hover:underline"
                >
                  Change limit
                </button>
              )}
            </div>
            <div className="rounded-md border border-[var(--border)] bg-white p-4">
              <p className="text-xs font-medium text-[var(--text-muted)]">
                Available Credit
              </p>
              <p className="mt-1 text-2xl font-bold text-[var(--success)]">
                {formatKes(balance.availableCredit)}
              </p>
              <p className="mt-1 text-xs text-[var(--text-muted)]">
                Can be used for purchases
              </p>
            </div>
          </div>

          {/* Action buttons */}
          {canWrite && (
            <div className="mb-6 flex gap-3">
              <PrimaryButton onClick={() => setShowPaymentDialog(true)}>
                <Plus className="h-4 w-4" />
                Record Payment
              </PrimaryButton>
              <SecondaryButton onClick={() => setShowAdjustDialog(true)}>
                <FileText className="h-4 w-4" />
                Manual Adjustment
              </SecondaryButton>
            </div>
          )}

          {/* Outstanding sales */}
          {outstandingSales.length > 0 && (
            <div className="mb-6">
              <h3 className="mb-3 text-sm font-semibold">
                Outstanding Sales ({outstandingSales.length})
              </h3>
              <div className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[600px] text-left text-sm">
                    <thead className="bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                      <tr>
                        <th className="px-4 py-3 font-semibold">Invoice</th>
                        <th className="px-4 py-3 text-right font-semibold">Total</th>
                        <th className="px-4 py-3 text-right font-semibold">Paid</th>
                        <th className="px-4 py-3 text-right font-semibold">Owed</th>
                        <th className="px-4 py-3 font-semibold">Date</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[var(--border)]">
                      {outstandingSales.map((sale) => (
                        <tr key={sale.id}>
                          <td className="px-4 py-3 font-mono text-xs">
                            {sale.invoiceNumber ?? sale.id.slice(0, 8)}
                          </td>
                          <td className="px-4 py-3 text-right">
                            {formatKes(sale.totalAmount)}
                          </td>
                          <td className="px-4 py-3 text-right text-[var(--success)]">
                            {formatKes(sale.amountPaid)}
                          </td>
                          <td className="px-4 py-3 text-right font-semibold text-[var(--danger)]">
                            {formatKes(sale.amountOwed)}
                          </td>
                          <td className="px-4 py-3 text-[var(--text-muted)]">
                            {formatDateTime(sale.completedAt)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* Transaction history */}
          <div>
            <h3 className="mb-3 text-sm font-semibold">
              Transaction History ({transactions.length})
            </h3>
            {transactions.length ? (
              <div className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[500px] text-left text-sm">
                    <thead className="bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                      <tr>
                        <th className="px-4 py-3 font-semibold">Type</th>
                        <th className="px-4 py-3 font-semibold">Description</th>
                        <th className="px-4 py-3 text-right font-semibold">Amount</th>
                        <th className="px-4 py-3 font-semibold">Date</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[var(--border)]">
                      {transactions.map((txn) => (
                        <tr key={txn.id}>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-2">
                              {txnTypeIcon(txn.type)}
                              <span className="text-xs font-medium uppercase">
                                {txn.type}
                              </span>
                            </div>
                          </td>
                          <td className="px-4 py-3 text-[var(--text-muted)]">
                            {txn.description}
                          </td>
                          <td
                            className={`px-4 py-3 text-right font-semibold ${
                              txn.amount < 0
                                ? "text-[var(--success)]"
                                : "text-[var(--danger)]"
                            }`}
                          >
                            {txn.amount < 0 ? "-" : ""}
                            {formatKes(Math.abs(txn.amount))}
                          </td>
                          <td className="px-4 py-3 text-[var(--text-muted)]">
                            {formatDateTime(txn.createdAt)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <div className="rounded-md border border-[var(--border)] bg-white">
                <EmptyState
                  icon={BadgeDollarSign}
                  title="No transactions"
                  description="Account transactions will appear here."
                />
              </div>
            )}
          </div>
        </>
      ) : null}

      {/* Record Payment Dialog */}
      {showPaymentDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Record Payment"
            className="w-full max-w-md rounded-md border border-[var(--border)] bg-white shadow-xl"
          >
            <div className="flex items-center justify-between border-b border-[var(--border)] px-5 py-4">
              <h2 className="text-base font-semibold">Record Payment</h2>
              <button
                type="button"
                title="Close"
                onClick={() => { setShowPaymentDialog(false); setActionError(null); }}
                className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]"
              >
                ✕
              </button>
            </div>
            <div className="p-5 space-y-3">
              {actionError && <FormError message={actionError} />}
              <Field label="Amount (KES)">
                <Input
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  value={paymentAmount}
                  onChange={(e) => setPaymentAmount(e.target.value)}
                />
              </Field>
              <Field label="Notes (optional)">
                <Textarea
                  placeholder="Payment notes..."
                  value={paymentNotes}
                  onChange={(e) => setPaymentNotes(e.target.value)}
                />
              </Field>
            </div>
            <div className="flex justify-end gap-2 border-t border-[var(--border)] px-5 py-4">
              <SecondaryButton onClick={() => { setShowPaymentDialog(false); setActionError(null); }}>
                Cancel
              </SecondaryButton>
              <PrimaryButton disabled={actionLoading} onClick={handleRecordPayment}>
                {actionLoading ? "Recording..." : "Record Payment"}
              </PrimaryButton>
            </div>
          </div>
        </div>
      )}

      {/* Adjustment Dialog */}
      {showAdjustDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Manual Adjustment"
            className="w-full max-w-md rounded-md border border-[var(--border)] bg-white shadow-xl"
          >
            <div className="flex items-center justify-between border-b border-[var(--border)] px-5 py-4">
              <h2 className="text-base font-semibold">Manual Adjustment</h2>
              <button
                type="button"
                title="Close"
                onClick={() => { setShowAdjustDialog(false); setActionError(null); }}
                className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]"
              >
                ✕
              </button>
            </div>
            <div className="p-5 space-y-3">
              {actionError && <FormError message={actionError} />}
              <Field label="Type">
                <Select
                  value={adjustType}
                  onChange={(e) => setAdjustType(e.target.value)}
                >
                  <option value="DEBIT">Debit (increase balance owed)</option>
                  <option value="CREDIT">Credit (reduce balance owed)</option>
                </Select>
              </Field>
              <Field label="Amount (KES)">
                <Input
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  value={adjustAmount}
                  onChange={(e) => setAdjustAmount(e.target.value)}
                />
              </Field>
              <Field label="Reason (required)">
                <Textarea
                  placeholder="Reason for adjustment..."
                  value={adjustNotes}
                  onChange={(e) => setAdjustNotes(e.target.value)}
                />
              </Field>
            </div>
            <div className="flex justify-end gap-2 border-t border-[var(--border)] px-5 py-4">
              <SecondaryButton onClick={() => { setShowAdjustDialog(false); setActionError(null); }}>
                Cancel
              </SecondaryButton>
              <PrimaryButton disabled={actionLoading} onClick={handleAdjust}>
                {actionLoading ? "Applying..." : "Apply Adjustment"}
              </PrimaryButton>
            </div>
          </div>
        </div>
      )}

      {/* Credit Limit Dialog */}
      {showCreditLimitDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Update Credit Limit"
            className="w-full max-w-md rounded-md border border-[var(--border)] bg-white shadow-xl"
          >
            <div className="flex items-center justify-between border-b border-[var(--border)] px-5 py-4">
              <h2 className="text-base font-semibold">Update Credit Limit</h2>
              <button
                type="button"
                title="Close"
                onClick={() => { setShowCreditLimitDialog(false); setActionError(null); }}
                className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]"
              >
                ✕
              </button>
            </div>
            <div className="p-5 space-y-3">
              {actionError && <FormError message={actionError} />}
              <Field label="Credit Limit (KES)">
                <Input
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  value={newCreditLimit}
                  onChange={(e) => setNewCreditLimit(e.target.value)}
                />
              </Field>
            </div>
            <div className="flex justify-end gap-2 border-t border-[var(--border)] px-5 py-4">
              <SecondaryButton onClick={() => { setShowCreditLimitDialog(false); setActionError(null); }}>
                Cancel
              </SecondaryButton>
              <PrimaryButton disabled={actionLoading} onClick={handleUpdateCreditLimit}>
                {actionLoading ? "Saving..." : "Save"}
              </PrimaryButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
