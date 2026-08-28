"use client";

import { FileText, History, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import {
  type BranchSummary,
  terminalGateway,
} from "@/features/terminals/terminal-gateway";
import { formatDateTime } from "@/lib/format";
import { ApiClientError, apiRequest } from "@/lib/api-client";

interface ShiftRow {
  id: string;
  shiftName: string | null;
  shiftNumber: number | null;
  status: string | null;
  branchId: string | null;
  branchName: string | null;
  userName: string | null;
  shiftStartTime: string | null;
  shiftEndTime: string | null;
  openingFloat: number;
  cashSales: number;
  mpesaSales: number;
  cashRefunds: number;
  expectedCash: number;
  actualCash: number | null;
  variance: number | null;
}

interface ZReport {
  shiftId: string;
  shiftName: string | null;
  branchName: string | null;
  cashierName: string | null;
  openedAt: string | null;
  closedAt: string | null;
  status: string | null;
  openingBalance: number;
  expectedClosingBalance: number;
  actualClosingBalance: number | null;
  variance: number | null;
  salesCount: number;
  totalSales: number;
  totalCashPayments: number;
  totalMpesaPayments: number;
  totalCardPayments: number;
  totalRefunds: number;
  refundCount: number;
  paymentBreakdown: Array<Record<string, unknown>>;
}

function money(value: number) {
  return value.toFixed(2);
}

export function ShiftHistoryPage() {
  const session = useAuthStore((state) => state.session);
  const canReconcile = usePermission(PERMISSIONS.SHIFT_VARIANCE_APPROVE);
  const [branches, setBranches] = useState<BranchSummary[]>([]);
  const [branchFilter, setBranchFilter] = useState("ALL");
  const [shifts, setShifts] = useState<ShiftRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [zReport, setZReport] = useState<ZReport | null>(null);
  const [resolveTarget, setResolveTarget] = useState<ShiftRow | null>(null);
  const [resolveNote, setResolveNote] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!session || !canReconcile) return;
    setLoading(true);
    try {
      const query =
        branchFilter === "ALL"
          ? ""
          : `?branchId=${encodeURIComponent(branchFilter)}`;
      const response = await apiRequest<ShiftRow[]>(
        `/shifts/history${query}`,
        { cache: "no-store" },
      );
      setShifts(response.data);
      setError(null);
    } catch (caught) {
      setError(
        caught instanceof ApiClientError || caught instanceof Error
          ? caught.message
          : "Shift history could not be loaded.",
      );
    } finally {
      setLoading(false);
    }
  }, [branchFilter, canReconcile, session]);

  useEffect(() => {
    if (!session?.user.pharmacyId) return;
    let active = true;
    void terminalGateway
      .listBranches(session.user.pharmacyId)
      .then((rows) => {
        if (active) setBranches(rows.filter((item) => item.status === "ACTIVE"));
      })
      .catch(() => {
        if (active) setBranches([]);
      });
    return () => {
      active = false;
    };
  }, [session]);

  useEffect(() => {
    const initial = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(initial);
  }, [load]);

  const totals = useMemo(() => {
    let expected = 0;
    let actual = 0;
    let variance = 0;
    let varianceCount = 0;
    for (const shift of shifts) {
      expected += Number(shift.expectedCash ?? 0);
      actual += Number(shift.actualCash ?? 0);
      const diff = Number(shift.variance ?? 0);
      variance += diff;
      if (Math.abs(diff) > 0.004) varianceCount += 1;
    }
    return { expected, actual, variance, varianceCount };
  }, [shifts]);

  async function openZReport(shiftId: string) {
    setError(null);
    try {
      const response = await apiRequest<ZReport>(
        `/reports/shift-z/${shiftId}`,
        { cache: "no-store" },
      );
      setZReport(response.data);
    } catch (caught) {
      setError(
        caught instanceof ApiClientError || caught instanceof Error
          ? caught.message
          : "The Z report could not be generated.",
      );
    }
  }

  async function resolveVariance(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!resolveTarget || busy) return;
    setBusy(true);
    setError(null);
    try {
      await apiRequest(`/shifts/${resolveTarget.id}/variance-review`, {
        method: "PATCH",
        body: { remarks: resolveNote.trim() || null, status: null, actualCash: null },
      });
      setResolveTarget(null);
      setResolveNote("");
      await load();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError || caught instanceof Error
          ? caught.message
          : "The variance could not be resolved.",
      );
    } finally {
      setBusy(false);
    }
  }

  if (!canReconcile) {
    return (
      <AccessRestricted homePath="/dashboard" />
    );
  }

  return (
    <div>
      <PageHeader
        title="Shift reconciliation"
        description="Every cashier shift across all branches with drawer counts and variances - the owner's view of who closed with what."
        actions={
          <SecondaryButton
            type="button"
            title="Refresh shifts"
            aria-label="Refresh shifts"
            className="px-3"
            disabled={loading}
            onClick={() => void load()}
          >
            <RefreshCw aria-hidden="true" size={17} />
          </SecondaryButton>
        }
      />

      <div className="mb-5 grid gap-3 md:grid-cols-[260px_minmax(0,1fr)] md:items-end">
        <Field label="Branch">
          <Select
            value={branchFilter}
            onChange={(event) => setBranchFilter(event.target.value)}
          >
            <option value="ALL">All branches</option>
            {branches.map((branch) => (
              <option key={branch.id} value={branch.id}>
                {branch.branchName} ({branch.branchCode})
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="rounded-md border border-[var(--border)] bg-white p-3">
            <p className="text-xs text-[var(--text-muted)]">Shifts</p>
            <p className="mt-1 text-lg font-semibold">{shifts.length}</p>
          </div>
          <div className="rounded-md border border-[var(--border)] bg-white p-3">
            <p className="text-xs text-[var(--text-muted)]">Expected cash</p>
            <p className="mt-1 text-lg font-semibold">{money(totals.expected)}</p>
          </div>
          <div className="rounded-md border border-[var(--border)] bg-white p-3">
            <p className="text-xs text-[var(--text-muted)]">Counted cash</p>
            <p className="mt-1 text-lg font-semibold">
              {money(totals.actual)}
            </p>
          </div>
          <div className="rounded-md border border-[var(--border)] bg-white p-3">
            <p className="text-xs text-[var(--text-muted)]">
              Variances ({totals.varianceCount})
            </p>
            <p
              className={`mt-1 text-lg font-semibold ${Math.abs(totals.variance) > 0.004 ? "text-[var(--danger)]" : "text-[var(--success)]"}`}
            >
              {money(totals.variance)}
            </p>
          </div>
        </div>
      </div>

      {error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {shifts.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1080px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Branch</th>
                  <th className="px-4 py-3 font-semibold">Cashier</th>
                  <th className="px-4 py-3 font-semibold">Opened</th>
                  <th className="px-4 py-3 font-semibold">Closed</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 text-right font-semibold">Float</th>
                  <th className="px-4 py-3 text-right font-semibold">Cash sales</th>
                  <th className="px-4 py-3 text-right font-semibold">M-Pesa</th>
                  <th className="px-4 py-3 text-right font-semibold">Expected</th>
                  <th className="px-4 py-3 text-right font-semibold">Counted</th>
                  <th className="px-4 py-3 text-right font-semibold">Variance</th>
                  <th className="px-4 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {shifts.map((shift) => {
                  const variance = Number(shift.variance ?? 0);
                  const hasVariance = Math.abs(variance) > 0.004;
                  return (
                    <tr key={shift.id} className="hover:bg-[var(--surface-muted)]/60">
                      <td className="px-4 py-3 font-medium">
                        {shift.branchName ?? "Unknown"}
                      </td>
                      <td className="px-4 py-3">
                        {shift.userName ?? "Unknown"}
                        {shift.shiftName ? (
                          <span className="ml-2 text-xs text-[var(--text-muted)]">
                            {shift.shiftName}
                          </span>
                        ) : null}
                      </td>
                      <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                        {shift.shiftStartTime
                          ? formatDateTime(shift.shiftStartTime)
                          : "-"}
                      </td>
                      <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                        {shift.shiftEndTime
                          ? formatDateTime(shift.shiftEndTime)
                          : "-"}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge
                          tone={
                            shift.status === "ACTIVE"
                              ? "info"
                              : shift.status === "REVIEWED"
                                ? "success"
                                : hasVariance
                                  ? "danger"
                                  : "success"
                          }
                        >
                          {shift.status === "ACTIVE"
                            ? "Open"
                            : shift.status === "CANCELLED"
                              ? "Cancelled"
                              : shift.status === "REVIEWED"
                                ? "Reviewed"
                                : hasVariance
                                  ? `Variance ${money(variance)}`
                                  : "Reconciled"}
                        </StatusBadge>
                      </td>
                      <td className="px-4 py-3 text-right">
                        {money(Number(shift.openingFloat ?? 0))}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {money(Number(shift.cashSales ?? 0))}
                      </td>
                      <td className="px-4 py-3 text-right text-[var(--text-muted)]">
                        {money(Number(shift.mpesaSales ?? 0))}
                      </td>
                      <td className="px-4 py-3 text-right font-medium">
                        {money(Number(shift.expectedCash ?? 0))}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {shift.actualCash == null ? "-" : money(Number(shift.actualCash))}
                      </td>
                      <td
                        className={`px-4 py-3 text-right font-semibold ${hasVariance ? "text-[var(--danger)]" : "text-[var(--success)]"}`}
                      >
                        {shift.variance == null ? "-" : money(variance)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex justify-end gap-2">
                          <SecondaryButton
                            type="button"
                            className="min-h-9 gap-1.5 px-3"
                            disabled={busy}
                            onClick={() => void openZReport(shift.id)}
                          >
                            <FileText aria-hidden="true" size={14} /> Z report
                          </SecondaryButton>
                          {shift.status === "CLOSED" && hasVariance ? (
                            <SecondaryButton
                              type="button"
                              className="min-h-9 px-3"
                              disabled={busy}
                              onClick={() => {
                                setResolveTarget(shift);
                                setResolveNote("");
                              }}
                            >
                              Resolve
                            </SecondaryButton>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading shift history...
          </div>
        ) : (
          <div className="flex min-h-48 flex-col items-center justify-center px-5 py-10 text-center">
            <History aria-hidden="true" size={20} className="text-[var(--text-subtle)]" />
            <p className="mt-3 text-sm font-medium">No shifts recorded yet</p>
            <p className="mt-1 max-w-sm text-xs text-[var(--text-muted)]">
              Shifts appear here as soon as cashiers open and close their tills.
            </p>
          </div>
        )}
      </section>

      {zReport ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4 print:static print:bg-white print:p-0">
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Z report"
            className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-md border border-[var(--border)] bg-white p-6 shadow-xl print:max-h-none print:border-0 print:shadow-none"
          >
            <div className="flex items-start justify-between gap-3 print:hidden">
              <h2 className="text-base font-semibold">Z report - {zReport.branchName}</h2>
              <button
                type="button"
                aria-label="Close Z report"
                onClick={() => setZReport(null)}
                className="text-sm text-[var(--text-muted)] hover:text-[var(--text)]"
              >
                Close
              </button>
            </div>
            <dl className="relative mt-4 space-y-2 text-sm">
              <p className="text-center text-sm font-bold uppercase">Z daily report</p>
              <div className="flex justify-between"><dt className="text-[var(--text-muted)]">Branch</dt><dd>{zReport.branchName}</dd></div>
              <div className="flex justify-between"><dt className="text-[var(--text-muted)]">Cashier</dt><dd>{zReport.cashierName ?? "-"}</dd></div>
              <div className="flex justify-between"><dt className="text-[var(--text-muted)]">Opened</dt><dd>{zReport.openedAt ? formatDateTime(zReport.openedAt) : "-"}</dd></div>
              <div className="flex justify-between"><dt className="text-[var(--text-muted)]">Closed</dt><dd>{zReport.closedAt ? formatDateTime(zReport.closedAt) : "-"}</dd></div>
              <div className="flex justify-between border-t border-[var(--border)] pt-2"><dt>Sales receipts</dt><dd>{zReport.salesCount}</dd></div>
              <div className="flex justify-between"><dt>Total sales (incl. tax)</dt><dd className="font-semibold">{money(Number(zReport.totalSales ?? 0))}</dd></div>
              <div className="flex justify-between"><dt>Cash payments</dt><dd>{money(Number(zReport.totalCashPayments ?? 0))}</dd></div>
              <div className="flex justify-between"><dt>M-Pesa payments</dt><dd>{money(Number(zReport.totalMpesaPayments ?? 0))}</dd></div>
              {Number(zReport.totalCardPayments ?? 0) > 0 ? (
                <div className="flex justify-between"><dt>Card payments</dt><dd>{money(Number(zReport.totalCardPayments))}</dd></div>
              ) : null}
              <div className="flex justify-between"><dt>Credit notes / refunds</dt><dd>{money(Number(zReport.totalRefunds ?? 0))} ({zReport.refundCount})</dd></div>
              <div className="flex justify-between border-t border-[var(--border)] pt-2"><dt>Opening float</dt><dd>{money(Number(zReport.openingBalance ?? 0))}</dd></div>
              <div className="flex justify-between"><dt>Expected in drawer</dt><dd>{money(Number(zReport.expectedClosingBalance ?? 0))}</dd></div>
              <div className="flex justify-between"><dt>Counted</dt><dd>{zReport.actualClosingBalance == null ? "-" : money(Number(zReport.actualClosingBalance))}</dd></div>
              <div className={`flex justify-between font-semibold ${Math.abs(Number(zReport.variance ?? 0)) > 0.004 ? "text-[var(--danger)]" : "text-[var(--success)]"}`}><dt>Variance</dt><dd>{zReport.variance == null ? "-" : money(Number(zReport.variance))}</dd></div>
            </dl>
            <div className="mt-5 flex justify-end print:hidden">
              <PrimaryButton type="button" onClick={() => window.print()}>
                Print Z report
              </PrimaryButton>
            </div>
          </div>
        </div>
      ) : null}

      {resolveTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <form
            onSubmit={resolveVariance}
            role="dialog"
            aria-modal="true"
            aria-label="Resolve variance"
            className="w-full max-w-md rounded-md border border-[var(--border)] bg-white p-5 shadow-xl"
          >
            <h2 className="text-base font-semibold">
              Resolve variance for {resolveTarget.userName ?? "shift"}
            </h2>
            <p className="mt-1 text-sm text-[var(--text-muted)]">
              Variance of {money(Number(resolveTarget.variance ?? 0))} on{" "}
              {resolveTarget.branchName}. Add a review note explaining the outcome.
            </p>
            <div className="mt-4">
              <Field label="Review note" required>
                <Input
                  autoFocus
                  placeholder="e.g. Counting error corrected with cashier"
                  value={resolveNote}
                  onChange={(event) => setResolveNote(event.target.value)}
                />
              </Field>
            </div>
            <div className="mt-4 flex justify-end gap-2">
              <SecondaryButton type="button" onClick={() => setResolveTarget(null)}>
                Cancel
              </SecondaryButton>
              <PrimaryButton type="submit" disabled={busy || !resolveNote.trim()}>
                {busy ? "Saving..." : "Record resolution"}
              </PrimaryButton>
            </div>
          </form>
        </div>
      ) : null}
    </div>
  );
}
