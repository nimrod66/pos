"use client";

import { useEffect, useState } from "react";
import { FileText } from "lucide-react";

import { SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { formatKes } from "@/features/workspace/lib/money";
import { insuranceGateway } from "@/features/insurance/insurance-gateway";
import type { InsuranceClaim, Insurer } from "@/features/insurance/types";

const STATUSES = ["PENDING", "SUBMITTED", "ACKNOWLEDGED", "PARTIALLY_PAID", "PAID", "REJECTED", "WRITTEN_OFF"];

function statusTone(status: string | null) {
  switch (status) {
    case "PAID": return "success" as const;
    case "REJECTED": case "WRITTEN_OFF": return "danger" as const;
    case "PENDING": case "SUBMITTED": return "warning" as const;
    default: return "info" as const;
  }
}

export function ClaimsPage({ showHeader = true }: { showHeader?: boolean }) {
  const canRead = usePermission(PERMISSIONS.INSURANCE_READ);
  const canWrite = usePermission(PERMISSIONS.INSURANCE_WRITE);

  const [claims, setClaims] = useState<InsuranceClaim[]>([]);
  const [insurers, setInsurers] = useState<Insurer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterInsurer, setFilterInsurer] = useState("");
  const [filterStatus, setFilterStatus] = useState("");

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    async function run() {
      setLoading(true);
      try {
        const [c, i] = await Promise.all([
          insuranceGateway.listClaims(filterInsurer || undefined, filterStatus || undefined),
          insuranceGateway.listActiveInsurers(),
        ]);
        if (!active) return;
        setClaims(c);
        setInsurers(i);
        setError(null);
      } catch {
        if (!active) return;
        setError("Failed to load claims.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void run();
    return () => { active = false; };
  }, [canRead, filterInsurer, filterStatus]);

  async function handleStatusUpdate(claimId: string, newStatus: string) {
    try {
      const updated = await insuranceGateway.updateClaimStatus(claimId, newStatus);
      setClaims((prev) => prev.map((c) => (c.id === claimId ? updated : c)));
    } catch {
      setError("Failed to update claim status.");
    }
  }

  if (!canRead) return <AccessRestricted />;

  const totalClaimed = claims.reduce((s, c) => s + Number(c.claimAmount ?? 0), 0);
  const totalApproved = claims.reduce((s, c) => s + Number(c.approvedAmount ?? 0), 0);

  return (
    <div>
      {showHeader ? (
        <PageHeader
          title="Insurance claims"
          description="Track claims submitted to insurers and their settlement status."
        />
      ) : null}

      {error ? <div className="mb-4"><FormError message={error} /></div> : null}

      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Total claimed</p>
          <p className="mt-1 text-xl font-semibold">{formatKes(totalClaimed)}</p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Total approved</p>
          <p className="mt-1 text-xl font-semibold text-[var(--success)]">{formatKes(totalApproved)}</p>
        </div>
        <div className="rounded-md border border-[var(--border)] bg-white p-4">
          <p className="text-xs text-[var(--text-muted)]">Claims count</p>
          <p className="mt-1 text-xl font-semibold">{claims.length}</p>
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
          <label className="text-xs font-medium text-[var(--text-muted)]">
            <span className="mb-1 block">Status</span>
            <Select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
              <option value="">All statuses</option>
              {STATUSES.map((s) => <option key={s} value={s}>{s.replace(/_/g, " ")}</option>)}
            </Select>
          </label>
        </div>
      </section>

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">Loading claims...</div>
        ) : claims.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Patient</th>
                  <th className="px-4 py-3 font-semibold">Insurer</th>
                  <th className="px-4 py-3 font-semibold">Reference</th>
                  <th className="px-4 py-3 text-right font-semibold">Amount</th>
                  <th className="px-4 py-3 text-right font-semibold">Approved</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  {canWrite ? <th className="px-4 py-3 font-semibold">Action</th> : null}
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {claims.map((claim) => (
                  <tr key={claim.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3">
                      <p className="font-medium">{claim.patientName || "Walk-in"}</p>
                      {claim.patientMembershipId ? (
                        <p className="text-xs text-[var(--text-muted)]">#{claim.patientMembershipId}</p>
                      ) : null}
                    </td>
                    <td className="px-4 py-3">{claim.insurerName || "-"}</td>
                    <td className="max-w-40 truncate px-4 py-3 font-mono text-xs text-[var(--text-muted)]">
                      {claim.claimReference || "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-semibold">
                      {formatKes(claim.claimAmount ?? 0)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      {claim.approvedAmount ? (
                        <span className="font-semibold text-[var(--success)]">{formatKes(claim.approvedAmount)}</span>
                      ) : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={statusTone(claim.claimStatus)}>
                        {(claim.claimStatus ?? "PENDING").replace(/_/g, " ")}
                      </StatusBadge>
                    </td>
                    {canWrite ? (
                      <td className="px-4 py-3">
                        {claim.claimStatus === "PENDING" ? (
                          <SecondaryButton
                            type="button"
                            className="px-2 py-1 text-xs"
                            onClick={() => void handleStatusUpdate(claim.id, "SUBMITTED")}
                          >
                            Submit
                          </SecondaryButton>
                        ) : claim.claimStatus === "SUBMITTED" ? (
                          <SecondaryButton
                            type="button"
                            className="px-2 py-1 text-xs"
                            onClick={() => void handleStatusUpdate(claim.id, "ACKNOWLEDGED")}
                          >
                            Ack
                          </SecondaryButton>
                        ) : claim.claimStatus === "ACKNOWLEDGED" ? (
                          <SecondaryButton
                            type="button"
                            className="px-2 py-1 text-xs"
                            onClick={() => void handleStatusUpdate(claim.id, "PAID")}
                          >
                            Mark paid
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
          <EmptyState icon={FileText} title="No claims found" description="Claims will appear here when patients use insurance." />
        )}
      </section>
    </div>
  );
}
