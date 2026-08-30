"use client";

import {
  FileText,
  Plus,
} from "lucide-react";
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
import { Modal } from "@/components/ui/modal";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { formatKes } from "@/features/workspace/lib/money";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { ApiClientError, apiRequest } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

interface SupplierInvoice {
  id: string;
  supplierId: string;
  supplierName: string;
  invoiceNumber: string;
  totalAmount: number;
  paidAmount: number;
  status: string;
  dueDate: string | null;
  notes: string | null;
  createdAt: string;
}

interface SupplierPayment {
  id: string;
  invoiceId: string;
  amount: number;
  paymentMethod: string;
  reference: string | null;
  notes: string | null;
  createdAt: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function statusTone(status: string) {
  switch (status) {
    case "PAID":
      return "success" as const;
    case "PARTIAL":
      return "warning" as const;
    case "OVERDUE":
      return "danger" as const;
    case "PENDING":
      return "neutral" as const;
    default:
      return "neutral" as const;
  }
}

export function SupplierInvoicesPage() {
  const suppliers = useWorkspaceQuery((state) => state.suppliers);
  const canWrite = usePermission(PERMISSIONS.SUPPLIER_WRITE);

  const [invoices, setInvoices] = useState<SupplierInvoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [supplierFilter, setSupplierFilter] = useState("");

  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showPaymentDialog, setShowPaymentDialog] = useState(false);
  const [showPaymentsDialog, setShowPaymentsDialog] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [newInvoice, setNewInvoice] = useState({
    supplierId: "",
    invoiceNumber: "",
    totalAmount: "",
    dueDate: "",
    notes: "",
  });

  const [paymentInvoiceId, setPaymentInvoiceId] = useState("");
  const [payment, setPayment] = useState({
    amount: "",
    paymentMethod: "CASH",
    reference: "",
    notes: "",
  });

  const [selectedInvoice, setSelectedInvoice] = useState<SupplierInvoice | null>(null);
  const [invoicePayments, setInvoicePayments] = useState<SupplierPayment[]>([]);
  const [paymentsLoading, setPaymentsLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ size: "200", sort: "createdAt,desc" });
      if (supplierFilter) params.set("supplierId", supplierFilter);
      const response = await apiRequest<SupplierInvoice[]>(
        `/supplier-invoices?${params}`,
      );
      setInvoices(response.data ?? []);
    } catch (caught) {
      setError(errorMessage(caught, "Invoices could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, [supplierFilter]);

  useEffect(() => {
    void load();
  }, [load]);

  const normalized = query.trim().toLowerCase();
  const filtered = normalized
    ? invoices.filter(
        (inv) =>
          inv.invoiceNumber.toLowerCase().includes(normalized) ||
          inv.supplierName.toLowerCase().includes(normalized),
      )
    : invoices;

  async function handleCreateInvoice() {
    if (!newInvoice.supplierId) {
      setActionError("Select a supplier.");
      return;
    }
    if (!newInvoice.invoiceNumber.trim()) {
      setActionError("Enter an invoice number.");
      return;
    }
    const amount = parseFloat(newInvoice.totalAmount);
    if (isNaN(amount) || amount <= 0) {
      setActionError("Enter a valid amount.");
      return;
    }
    setActionLoading(true);
    setActionError(null);
    try {
      await apiRequest("/supplier-invoices", {
        method: "POST",
        body: {
          supplierId: newInvoice.supplierId,
          invoiceNumber: newInvoice.invoiceNumber.trim(),
          totalAmount: amount,
          dueDate: newInvoice.dueDate || undefined,
          notes: newInvoice.notes || undefined,
        },
      });
      setShowCreateDialog(false);
      setNewInvoice({
        supplierId: "",
        invoiceNumber: "",
        totalAmount: "",
        dueDate: "",
        notes: "",
      });
      await load();
    } catch (caught) {
      setActionError(errorMessage(caught, "Invoice could not be created."));
    } finally {
      setActionLoading(false);
    }
  }

  async function handleRecordPayment() {
    const amount = parseFloat(payment.amount);
    if (isNaN(amount) || amount <= 0) {
      setActionError("Enter a valid payment amount.");
      return;
    }
    setActionLoading(true);
    setActionError(null);
    try {
      await apiRequest("/supplier-payments", {
        method: "POST",
        body: {
          invoiceId: paymentInvoiceId,
          amount,
          paymentMethod: payment.paymentMethod,
          reference: payment.reference || undefined,
          notes: payment.notes || undefined,
        },
      });
      setShowPaymentDialog(false);
      setPayment({ amount: "", paymentMethod: "CASH", reference: "", notes: "" });
      await load();
    } catch (caught) {
      setActionError(errorMessage(caught, "Payment could not be recorded."));
    } finally {
      setActionLoading(false);
    }
  }

  async function loadInvoicePayments(invoice: SupplierInvoice) {
    setSelectedInvoice(invoice);
    setShowPaymentsDialog(true);
    setPaymentsLoading(true);
    try {
      const response = await apiRequest<SupplierPayment[]>(
        `/supplier-payments?invoiceId=${invoice.id}&size=100&sort=createdAt,desc`,
      );
      setInvoicePayments(response.data ?? []);
    } catch {
      setInvoicePayments([]);
    } finally {
      setPaymentsLoading(false);
    }
  }

  return (
    <div className="max-w-7xl">
      <PageHeader
        eyebrow="Procurement"
        title="Supplier invoices"
        description="Track supplier invoices and record payments."
        actions={
          canWrite ? (
            <PrimaryButton onClick={() => setShowCreateDialog(true)}>
              <Plus className="h-4 w-4" />
              New Invoice
            </PrimaryButton>
          ) : undefined
        }
      />

      {error && (
        <div className="mb-5 rounded-md border border-[var(--danger-border)] bg-[var(--danger-soft)] px-3 py-2.5 text-sm text-[var(--danger)]">
          {error}
        </div>
      )}

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <label className="relative block max-w-md flex-1">
          <span className="sr-only">Search invoices</span>
          <Input
            className="pl-9"
            placeholder="Search invoice number or supplier..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </label>
        <Select
          value={supplierFilter}
          onChange={(e) => setSupplierFilter(e.target.value)}
          className="max-w-xs"
        >
          <option value="">All suppliers</option>
          {suppliers.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </Select>
      </div>

      {loading ? (
        <p className="text-sm text-[var(--text-muted)]">Loading invoices...</p>
      ) : filtered.length ? (
        <div className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[800px] text-left text-sm">
              <thead className="bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Invoice #</th>
                  <th className="px-4 py-3 font-semibold">Supplier</th>
                  <th className="px-4 py-3 text-right font-semibold">Total</th>
                  <th className="px-4 py-3 text-right font-semibold">Paid</th>
                  <th className="px-4 py-3 text-right font-semibold">Balance</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Due Date</th>
                  <th className="px-4 py-3 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {filtered.map((inv) => {
                  const balance = inv.totalAmount - inv.paidAmount;
                  return (
                    <tr key={inv.id}>
                      <td className="px-4 py-3 font-mono text-xs font-semibold">
                        {inv.invoiceNumber}
                      </td>
                      <td className="px-4 py-3">{inv.supplierName}</td>
                      <td className="px-4 py-3 text-right">
                        {formatKes(inv.totalAmount)}
                      </td>
                      <td className="px-4 py-3 text-right text-[var(--success)]">
                        {formatKes(inv.paidAmount)}
                      </td>
                      <td
                        className={`px-4 py-3 text-right font-semibold ${
                          balance > 0
                            ? "text-[var(--danger)]"
                            : "text-[var(--text-muted)]"
                        }`}
                      >
                        {formatKes(balance)}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge tone={statusTone(inv.status)}>
                          {inv.status}
                        </StatusBadge>
                      </td>
                      <td className="px-4 py-3 text-[var(--text-muted)]">
                        {inv.dueDate ? formatDateTime(inv.dueDate) : "—"}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => loadInvoicePayments(inv)}
                            className="text-xs text-[var(--primary)] hover:underline"
                          >
                            Payments
                          </button>
                          {canWrite && balance > 0 && (
                            <button
                              onClick={() => {
                                setPaymentInvoiceId(inv.id);
                                setPayment({
                                  ...payment,
                                  amount: String(balance),
                                });
                                setShowPaymentDialog(true);
                              }}
                              className="text-xs text-[var(--success)] hover:underline"
                            >
                              Pay
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="rounded-md border border-[var(--border)] bg-white">
          <EmptyState
            icon={FileText}
            title="No invoices"
            description={
              query
                ? "Try a different search."
                : "Supplier invoices will appear here."
            }
          />
        </div>
      )}

      {/* Create Invoice Dialog */}
      <Modal
        open={showCreateDialog}
        onClose={() => { setShowCreateDialog(false); setActionError(null); }}
        title="New Supplier Invoice"
        maxWidthClass="max-w-lg"
      >
        <div className="grid gap-4">
          {actionError && <FormError message={actionError} />}
          <Field label="Supplier">
            <Select
              value={newInvoice.supplierId}
              onChange={(e) =>
                setNewInvoice({ ...newInvoice, supplierId: e.target.value })
              }
            >
              <option value="">Select supplier...</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Invoice Number">
            <Input
              placeholder="INV-001"
              value={newInvoice.invoiceNumber}
              onChange={(e) =>
                setNewInvoice({ ...newInvoice, invoiceNumber: e.target.value })
              }
            />
          </Field>
          <Field label="Total Amount (KES)">
            <Input
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              value={newInvoice.totalAmount}
              onChange={(e) =>
                setNewInvoice({ ...newInvoice, totalAmount: e.target.value })
              }
            />
          </Field>
          <Field label="Due Date (optional)">
            <Input
              type="date"
              value={newInvoice.dueDate}
              onChange={(e) =>
                setNewInvoice({ ...newInvoice, dueDate: e.target.value })
              }
            />
          </Field>
          <Field label="Notes (optional)">
            <Textarea
              placeholder="Invoice notes..."
              value={newInvoice.notes}
              onChange={(e) =>
                setNewInvoice({ ...newInvoice, notes: e.target.value })
              }
            />
          </Field>
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <SecondaryButton onClick={() => { setShowCreateDialog(false); setActionError(null); }}>
            Cancel
          </SecondaryButton>
          <PrimaryButton disabled={actionLoading} onClick={handleCreateInvoice}>
            {actionLoading ? "Creating..." : "Create Invoice"}
          </PrimaryButton>
        </div>
      </Modal>

      {/* Record Payment Dialog */}
      <Modal
        open={showPaymentDialog}
        onClose={() => { setShowPaymentDialog(false); setActionError(null); }}
        title="Record Supplier Payment"
      >
        <div className="grid gap-4">
          {actionError && <FormError message={actionError} />}
          <Field label="Amount (KES)">
            <Input
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              value={payment.amount}
              onChange={(e) =>
                setPayment({ ...payment, amount: e.target.value })
              }
            />
          </Field>
          <Field label="Payment Method">
            <Select
              value={payment.paymentMethod}
              onChange={(e) =>
                setPayment({ ...payment, paymentMethod: e.target.value })
              }
            >
              <option value="CASH">Cash</option>
              <option value="BANK_TRANSFER">Bank Transfer</option>
              <option value="MPESA">M-Pesa</option>
              <option value="CHEQUE">Cheque</option>
            </Select>
          </Field>
          <Field label="Reference (optional)">
            <Input
              placeholder="Transaction reference..."
              value={payment.reference}
              onChange={(e) =>
                setPayment({ ...payment, reference: e.target.value })
              }
            />
          </Field>
          <Field label="Notes (optional)">
            <Textarea
              placeholder="Payment notes..."
              value={payment.notes}
              onChange={(e) =>
                setPayment({ ...payment, notes: e.target.value })
              }
            />
          </Field>
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <SecondaryButton onClick={() => { setShowPaymentDialog(false); setActionError(null); }}>
            Cancel
          </SecondaryButton>
          <PrimaryButton disabled={actionLoading} onClick={handleRecordPayment}>
            {actionLoading ? "Recording..." : "Record Payment"}
          </PrimaryButton>
        </div>
      </Modal>

      {/* View Payments Dialog */}
      {showPaymentsDialog && selectedInvoice ? (
        <Modal
          open={showPaymentsDialog}
          onClose={() => setShowPaymentsDialog(false)}
          title={`Payments for ${selectedInvoice.invoiceNumber}`}
          maxWidthClass="max-w-lg"
        >
          {paymentsLoading ? (
            <p className="text-sm text-[var(--text-muted)]">Loading payments...</p>
          ) : invoicePayments.length ? (
            <div className="max-h-64 overflow-y-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-xs text-[var(--text-muted)]">
                  <tr>
                    <th className="pb-2 font-semibold">Amount</th>
                    <th className="pb-2 font-semibold">Method</th>
                    <th className="pb-2 font-semibold">Reference</th>
                    <th className="pb-2 font-semibold">Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {invoicePayments.map((p) => (
                    <tr key={p.id}>
                      <td className="py-2 font-semibold text-[var(--success)]">
                        {formatKes(p.amount)}
                      </td>
                      <td className="py-2 text-[var(--text-muted)]">
                        {p.paymentMethod}
                      </td>
                      <td className="py-2 text-[var(--text-muted)]">
                        {p.reference ?? "—"}
                      </td>
                      <td className="py-2 text-[var(--text-muted)]">
                        {formatDateTime(p.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-sm text-[var(--text-muted)]">No payments recorded yet.</p>
          )}
          <div className="mt-4 flex justify-end">
            <SecondaryButton onClick={() => setShowPaymentsDialog(false)}>
              Close
            </SecondaryButton>
          </div>
        </Modal>
      ) : null}
    </div>
  );
}
