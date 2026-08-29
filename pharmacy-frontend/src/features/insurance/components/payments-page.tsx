"use client";

import { useEffect, useState } from "react";
import { Wallet, Plus, Link as LinkIcon, X } from "lucide-react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { formatKes } from "@/features/workspace/lib/money";
import { formatDateTime } from "@/lib/format";
import { insuranceGateway } from "@/features/insurance/insurance-gateway";
import type { InsurancePayment, Insurer, ClaimBatch, CreatePaymentInput } from "@/features/insurance/types";

function statusTone(status: string | null) {
  switch (status) {
    case "LINKED": case "RECONCILED": return "success" as const;
    case "PENDING": return "warning" as const;
    case "PARTIAL": return "info" as const;
    default: return "neutral" as const;
  }
}

const PAYMENT_METHODS = ["BANK_TRANSFER", "CHEQUE", "CASH", "MOBILE_MONEY"];

export function PaymentsPage() {
  const canRead = usePermission(PERMISSIONS.INSURANCE_READ);
  const canWrite = usePermission(PERMISSIONS.INSURANCE_WRITE);

  const [payments, setPayments] = useState<InsurancePayment[]>([]);
  const [insurers, setInsurers] = useState<Insurer[]>([]);
  const [batches, setBatches] = useState<ClaimBatch[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterInsurer, setFilterInsurer] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState<CreatePaymentInput>({
    paymentReference: "",
    paymentAmount: undefined,
    paymentDate: new Date().toISOString().split("T")[0],
    paymentMethod: "BANK_TRANSFER",
    notes: "",
  });
  const [formInsurerId, setFormInsurerId] = useState("");
  const [formBatchId, setFormBatchId] = useState("");

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    async function run() {
      setLoading(true);
      try {
        const [p, i] = await Promise.all([
          insuranceGateway.listPayments(filterInsurer || undefined),
          insuranceGateway.listActiveInsurers(),
        ]);
        if (!active) return;
        setPayments(p);
        setInsurers(i);
        setError(null);
      } catch {
        if (!active) return;
        setError("Failed to load payments.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void run();
    return () => { active = false; };
  }, [canRead, filterInsurer]);

  useEffect(() => {
    if (!formInsurerId) {
      setBatches([]);
      return;
    }
    let active = true;
    insuranceGateway.listBatches(formInsurerId).then((b) => {
      if (active) setBatches(b);
    }).catch(() => {
      if (active) setBatches([]);
    });
    return () => { active = false; };
  }, [formInsurerId]);

  function resetForm() {
    setForm({
      paymentReference: "",
      paymentAmount: undefined,
      paymentDate: new Date().toISOString().split("T")[0],
      paymentMethod: "BANK_TRANSFER",
      notes: "",
    });
    setFormInsurerId("");
    setFormBatchId("");
    setShowForm(false);
  }

  async function handleSubmit() {
    if (!formInsurerId || !form.paymentAmount) {
      setError("Insurer and payment amount are required.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await insuranceGateway.createPayment(formInsurerId, {
        ...form,
        batchId: formBatchId || undefined,
      });
      resetForm();
      const data = await insuranceGateway.listPayments(filterInsurer || undefined);
      setPayments(data);
    } catch {
      setError("Failed to record payment.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!canRead) return <AccessRestricted />;

  const totalPaid = payments.reduce((s, p) => s + Number(p.paymentAmount ?? 0), 0);
  const linkedCount = payments.filter((p) => p.status === "LINKED" || p.status === "RECONCILED").length;

  return (
    <div>
      <PageHeader
        title="Insurance payments"
        description="Record and track payments received from insurers."
        actions={
          canWrite ? (
            <PrimaryButton type="button" onClick={() => { resetForm(); setShowForm(true); }}>
              <Plus aria-hidden="true" size={16} /> Record payment
            </PrimaryButton>
          ) : undefined
        }
      />

      {error ? <div className="mb-4"><FormError message={error} /></div> : null}

      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Total received</p>
          <p className="mt-1 text-xl font-semibold text-[var(--success)]">{formatKes(totalPaid)}</p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Payments</p>
          <p className="mt-1 text-xl font-semibold">{payments.length}</p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Linked to claims</p>
          <p className="mt-1 text-xl font-semibold">{linkedCount}</p>
        </div>
      </div>

      {showForm ? (
        <div className="mb-5 rounded-md border border-[var(--brand)] bg-white p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold">Record payment</h3>
            <button type="button" onClick={resetForm} className="text-[var(--text-muted)] hover:text-[var(--danger)]"><X size={18} /></button>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Insurer *</span>
              <Select value={formInsurerId} onChange={(e) => setFormInsurerId(e.target.value)}>
                <option value="">Select insurer</option>
                {insurers.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
              </Select>
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Batch</span>
              <Select value={formBatchId} onChange={(e) => setFormBatchId(e.target.value)}>
                <option value="">No specific batch</option>
                {batches.map((b) => <option key={b.id} value={b.id}>{b.batchReference || b.id}</option>)}
              </Select>
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Reference</span>
              <Input value={form.paymentReference ?? ""} onChange={(e) => setForm({ ...form, paymentReference: e.target.value })} placeholder="e.g. CHEQUE-001" />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Amount *</span>
              <Input type="number" value={form.paymentAmount ?? ""} onChange={(e) => setForm({ ...form, paymentAmount: e.target.value ? Number(e.target.value) : undefined })} placeholder="e.g. 50000" />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Date</span>
              <Input type="date" value={form.paymentDate ?? ""} onChange={(e) => setForm({ ...form, paymentDate: e.target.value })} />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Method</span>
              <Select value={form.paymentMethod ?? "BANK_TRANSFER"} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}>
                {PAYMENT_METHODS.map((m) => <option key={m} value={m}>{m.replace(/_/g, " ")}</option>)}
              </Select>
            </label>
          </div>
          <label className="mt-3 block text-xs font-medium text-[var(--text-muted)]">
            <span className="mb-1 block">Notes</span>
            <Input value={form.notes ?? ""} onChange={(e) => setForm({ ...form, notes: e.target.value })} placeholder="Optional notes" />
          </label>
          <div className="mt-3 flex gap-2">
            <PrimaryButton type="button" onClick={() => void handleSubmit()} disabled={submitting}>
              {submitting ? "Saving..." : "Record payment"}
            </PrimaryButton>
            <SecondaryButton type="button" onClick={resetForm}>Cancel</SecondaryButton>
          </div>
        </div>
      ) : null}

      <section className="mb-4 rounded-md border border-[var(--border)] bg-white p-4">
        <div className="flex gap-3">
          <label className="text-xs font-medium text-[var(--text-muted)]">
            <span className="mb-1 block">Insurer</span>
            <Select value={filterInsurer} onChange={(e) => setFilterInsurer(e.target.value)}>
              <option value="">All insurers</option>
              {insurers.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
            </Select>
          </label>
        </div>
      </section>

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">Loading payments...</div>
        ) : payments.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Reference</th>
                  <th className="px-4 py-3 font-semibold">Insurer</th>
                  <th className="px-4 py-3 font-semibold">Batch</th>
                  <th className="px-4 py-3 text-right font-semibold">Amount</th>
                  <th className="px-4 py-3 font-semibold">Method</th>
                  <th className="px-4 py-3 font-semibold">Date</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Claims linked</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {payments.map((payment) => (
                  <tr key={payment.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3 font-mono text-xs">{payment.paymentReference || "-"}</td>
                    <td className="px-4 py-3">{payment.insurerName || "-"}</td>
                    <td className="px-4 py-3 font-mono text-xs">{payment.batchRef || "-"}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-semibold text-[var(--success)]">
                      {formatKes(payment.paymentAmount ?? 0)}
                    </td>
                    <td className="px-4 py-3">{(payment.paymentMethod ?? "-").replace(/_/g, " ")}</td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {payment.paymentDate ? formatDateTime(payment.paymentDate) : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={statusTone(payment.status)}>
                        {(payment.status ?? "PENDING").replace(/_/g, " ")}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3 text-right">{payment.linkedClaimCount ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState icon={Wallet} title="No payments found" description="Payments from insurers will appear here." />
        )}
      </section>
    </div>
  );
}
