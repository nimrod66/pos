"use client";

import { useEffect, useState } from "react";
import { ClipboardList, Send } from "lucide-react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { formatKes } from "@/features/workspace/lib/money";
import { formatDateTime } from "@/lib/format";
import { insuranceGateway } from "@/features/insurance/insurance-gateway";
import type { ClaimBatch, Insurer } from "@/features/insurance/types";

function statusTone(status: string | null) {
  switch (status) {
    case "PAID": return "success" as const;
    case "REJECTED": return "danger" as const;
    case "SUBMITTED": return "warning" as const;
    case "DRAFT": return "neutral" as const;
    default: return "info" as const;
  }
}

export function BatchesPage({ showHeader = true }: { showHeader?: boolean }) {
  const canRead = usePermission(PERMISSIONS.INSURANCE_READ);
  const canWrite = usePermission(PERMISSIONS.INSURANCE_WRITE);

  const [batches, setBatches] = useState<ClaimBatch[]>([]);
  const [insurers, setInsurers] = useState<Insurer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterInsurer, setFilterInsurer] = useState("");
  const [submitBatchId, setSubmitBatchId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    async function run() {
      setLoading(true);
      try {
        const [b, i] = await Promise.all([
          insuranceGateway.listBatches(filterInsurer || undefined),
          insuranceGateway.listActiveInsurers(),
        ]);
        if (!active) return;
        setBatches(b);
        setInsurers(i);
        setError(null);
      } catch {
        if (!active) return;
        setError("Failed to load claim batches.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void run();
    return () => { active = false; };
  }, [canRead, filterInsurer]);

  async function handleSubmitBatch() {
    if (!submitBatchId) return;
    setSubmitting(true);
    try {
      const updated = await insuranceGateway.submitBatch(submitBatchId);
      setBatches((prev) => prev.map((b) => (b.id === submitBatchId ? updated : b)));
      setSubmitBatchId(null);
    } catch {
      setError("Failed to submit batch.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!canRead) return <AccessRestricted />;

  const totalAmount = batches.reduce((s, b) => s + Number(b.totalAmount ?? 0), 0);
  const totalApproved = batches.reduce((s, b) => s + Number(b.approvedAmount ?? 0), 0);
  const totalPaid = batches.reduce((s, b) => s + Number(b.paidAmount ?? 0), 0);

  return (
    <div>
      {showHeader ? (
        <PageHeader
          title="Claim batches"
          description="Group claims into batches for submission to insurers."
        />
      ) : null}

      {error ? <div className="mb-4"><FormError message={error} /></div> : null}

      <div className="mb-4 grid gap-3 sm:grid-cols-4">
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Total claimed</p>
          <p className="mt-1 text-xl font-semibold">{formatKes(totalAmount)}</p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Total approved</p>
          <p className="mt-1 text-xl font-semibold text-[var(--success)]">{formatKes(totalApproved)}</p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Total paid</p>
          <p className="mt-1 text-xl font-semibold">{formatKes(totalPaid)}</p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Batches</p>
          <p className="mt-1 text-xl font-semibold">{batches.length}</p>
        </div>
      </div>

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
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">Loading batches...</div>
        ) : batches.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Reference</th>
                  <th className="px-4 py-3 font-semibold">Insurer</th>
                  <th className="px-4 py-3 text-right font-semibold">Claims</th>
                  <th className="px-4 py-3 text-right font-semibold">Amount</th>
                  <th className="px-4 py-3 text-right font-semibold">Approved</th>
                  <th className="px-4 py-3 text-right font-semibold">Paid</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Created</th>
                  {canWrite ? <th className="px-4 py-3 font-semibold">Action</th> : null}
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {batches.map((batch) => (
                  <tr key={batch.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3 font-mono text-xs">{batch.batchReference || "-"}</td>
                    <td className="px-4 py-3">{batch.insurerName || "-"}</td>
                    <td className="px-4 py-3 text-right">{batch.claimCount ?? "-"}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-semibold">
                      {formatKes(batch.totalAmount ?? 0)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      {batch.approvedAmount ? (
                        <span className="font-semibold text-[var(--success)]">{formatKes(batch.approvedAmount)}</span>
                      ) : "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      {batch.paidAmount ? formatKes(batch.paidAmount) : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={statusTone(batch.status)}>
                        {(batch.status ?? "DRAFT").replace(/_/g, " ")}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {batch.createdAt ? formatDateTime(batch.createdAt) : "-"}
                    </td>
                    {canWrite ? (
                      <td className="px-4 py-3">
                        {batch.status === "DRAFT" ? (
                          <SecondaryButton
                            type="button"
                            className="flex items-center gap-1 px-2 py-1 text-xs"
                            onClick={() => setSubmitBatchId(batch.id)}
                          >
                            <Send size={12} /> Submit
                          </SecondaryButton>
                        ) : null}
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState icon={ClipboardList} title="No batches found" description="Claim batches will appear here when claims are grouped for submission." />
        )}
      </section>

      <ConfirmDialog
        open={Boolean(submitBatchId)}
        title="Submit claim batch?"
        description="This will submit all claims in this batch to the insurer for processing."
        confirmLabel="Submit batch"
        busyLabel="Submitting..."
        busy={submitting}
        onConfirm={() => void handleSubmitBatch()}
        onCancel={() => setSubmitBatchId(null)}
      />
    </div>
  );
}
