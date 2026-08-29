"use client";

import { useEffect, useState } from "react";
import { RefreshCw, Plus, X } from "lucide-react";

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
import type { Reconciliation, Insurer } from "@/features/insurance/types";

function statusTone(status: string | null) {
  switch (status) {
    case "COMPLETED": return "success" as const;
    case "PENDING": return "warning" as const;
    case "IN_PROGRESS": return "info" as const;
    default: return "neutral" as const;
  }
}

export function ReconciliationPage() {
  const canRead = usePermission(PERMISSIONS.INSURANCE_READ);
  const canWrite = usePermission(PERMISSIONS.INSURANCE_WRITE);

  const [reconciliations, setReconciliations] = useState<Reconciliation[]>([]);
  const [insurers, setInsurers] = useState<Insurer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [formInsurerId, setFormInsurerId] = useState("");
  const [periodFrom, setPeriodFrom] = useState("");
  const [periodTo, setPeriodTo] = useState("");

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    async function run() {
      setLoading(true);
      try {
        const [r, i] = await Promise.all([
          insuranceGateway.listReconciliations(),
          insuranceGateway.listActiveInsurers(),
        ]);
        if (!active) return;
        setReconciliations(r);
        setInsurers(i);
        setError(null);
      } catch {
        if (!active) return;
        setError("Failed to load reconciliations.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void run();
    return () => { active = false; };
  }, [canRead]);

  function resetForm() {
    setFormInsurerId("");
    setPeriodFrom("");
    setPeriodTo("");
    setShowForm(false);
  }

  async function handleSubmit() {
    if (!formInsurerId || !periodFrom || !periodTo) {
      setError("Insurer and date range are required.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await insuranceGateway.runReconciliation(formInsurerId, periodFrom, periodTo);
      resetForm();
      const data = await insuranceGateway.listReconciliations();
      setReconciliations(data);
    } catch {
      setError("Failed to run reconciliation.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!canRead) return <AccessRestricted />;

  const totalOutstanding = reconciliations.reduce((s, r) => s + Number(r.outstandingAmount ?? 0), 0);

  return (
    <div>
      <PageHeader
        title="Reconciliation"
        description="Reconcile insurance claims with payments received from insurers."
        actions={
          canWrite ? (
            <PrimaryButton type="button" onClick={() => { resetForm(); setShowForm(true); }}>
              <Plus aria-hidden="true" size={16} /> Run reconciliation
            </PrimaryButton>
          ) : undefined
        }
      />

      {error ? <div className="mb-4"><FormError message={error} /></div> : null}

      <div className="mb-4 grid gap-3 sm:grid-cols-2">
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Total outstanding</p>
          <p className="mt-1 text-xl font-semibold text-[var(--warning)]">{formatKes(totalOutstanding)}</p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Reconciliations</p>
          <p className="mt-1 text-xl font-semibold">{reconciliations.length}</p>
        </div>
      </div>

      {showForm ? (
        <div className="mb-5 rounded-md border border-[var(--brand)] bg-white p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold">Run reconciliation</h3>
            <button type="button" onClick={resetForm} className="text-[var(--text-muted)] hover:text-[var(--danger)]"><X size={18} /></button>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Insurer *</span>
              <Select value={formInsurerId} onChange={(e) => setFormInsurerId(e.target.value)}>
                <option value="">Select insurer</option>
                {insurers.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
              </Select>
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Period from *</span>
              <Input type="date" value={periodFrom} onChange={(e) => setPeriodFrom(e.target.value)} />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Period to *</span>
              <Input type="date" value={periodTo} onChange={(e) => setPeriodTo(e.target.value)} />
            </label>
          </div>
          <div className="mt-3 flex gap-2">
            <PrimaryButton type="button" onClick={() => void handleSubmit()} disabled={submitting}>
              {submitting ? "Running..." : "Run reconciliation"}
            </PrimaryButton>
            <SecondaryButton type="button" onClick={resetForm}>Cancel</SecondaryButton>
          </div>
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">Loading reconciliations...</div>
        ) : reconciliations.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1000px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Insurer</th>
                  <th className="px-4 py-3 font-semibold">Period</th>
                  <th className="px-4 py-3 text-right font-semibold">Claims</th>
                  <th className="px-4 py-3 text-right font-semibold">Claimed</th>
                  <th className="px-4 py-3 text-right font-semibold">Approved</th>
                  <th className="px-4 py-3 text-right font-semibold">Paid</th>
                  <th className="px-4 py-3 text-right font-semibold">Outstanding</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {reconciliations.map((rec) => (
                  <tr key={rec.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3 font-medium">{rec.insurerName || "-"}</td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {rec.periodFrom && rec.periodTo
                        ? `${formatDateTime(rec.periodFrom)} - ${formatDateTime(rec.periodTo)}`
                        : "-"}
                    </td>
                    <td className="px-4 py-3 text-right">{rec.totalClaims ?? "-"}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      {rec.totalClaimedAmount ? formatKes(rec.totalClaimedAmount) : "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      {rec.totalApprovedAmount ? formatKes(rec.totalApprovedAmount) : "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      {rec.totalPaidAmount ? formatKes(rec.totalPaidAmount) : "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-semibold text-[var(--warning)]">
                      {rec.outstandingAmount ? formatKes(rec.outstandingAmount) : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={statusTone(rec.status)}>
                        {(rec.status ?? "PENDING").replace(/_/g, " ")}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {rec.createdAt ? formatDateTime(rec.createdAt) : "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState icon={RefreshCw} title="No reconciliations found" description="Run a reconciliation to compare claims against payments." />
        )}
      </section>
    </div>
  );
}
