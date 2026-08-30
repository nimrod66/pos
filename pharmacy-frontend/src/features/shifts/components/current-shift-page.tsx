"use client";

import { ArrowDownToLine, ArrowUpFromLine, Banknote, Clock3, LockKeyhole, Play, Square } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { Field, FormError, Input } from "@/components/ui/form-controls";
import { Modal } from "@/components/ui/modal";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { getLocalTerminalId, terminalGateway } from "@/features/terminals/terminal-gateway";
import { addMoney, centsToMoney, formatKes, moneyToCents } from "@/features/workspace/lib/money";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { formatDateTime } from "@/lib/format";
import { apiRequest } from "@/lib/api-client";
import { cn } from "@/lib/cn";

interface DrawerTransaction {
  id: string;
  transactionType: string;
  amount: number;
  remarks: string | null;
  createdAt: string;
}

export function CurrentShiftPage() {
  const currentShiftId = useWorkspaceQuery((state) => state.currentShiftId);
  const shifts = useWorkspaceQuery((state) => state.shifts);
  const canOpenShift = usePermission(PERMISSIONS.SHIFT_OPEN);
  const canCloseShift = usePermission(PERMISSIONS.SHIFT_CLOSE);
  const currentShift = shifts.find((shift) => shift.id === currentShiftId);
  const [openingFloat, setOpeningFloat] = useState("2000.00");
  const [actualCash, setActualCash] = useState(currentShift?.expectedCash ?? "");
  const [error, setError] = useState<string | null>(null);
  const [drawerTxns, setDrawerTxns] = useState<DrawerTransaction[]>([]);
  const [cashDialog, setCashDialog] = useState<"CASH_IN" | "CASH_OUT" | null>(null);
  const [cashAmount, setCashAmount] = useState("");
  const [cashReason, setCashReason] = useState("");
  const [cashBusy, setCashBusy] = useState(false);
  const [xReport, setXReport] = useState<Record<string, string | number> | null>(null);

  const loadDrawerTransactions = useCallback(async () => {
    if (!currentShiftId) return;
    try {
      const response = await apiRequest<{ drawerId: string; transactions: DrawerTransaction[] } | null>(
        "/cash-transactions/active-drawer",
        { cache: "no-store" },
      );
      setDrawerTxns(response.data?.transactions ?? []);
    } catch {
      setDrawerTxns([]);
    }
  }, [currentShiftId]);

  useEffect(() => {
    if (!currentShift) return;
    const initial = window.setTimeout(() => void loadDrawerTransactions(), 0);
    return () => window.clearTimeout(initial);
  }, [currentShift, loadDrawerTransactions]);

  async function submitCashMovement(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!cashDialog || cashBusy) return;
    setCashBusy(true);
    setError(null);
    try {
      await apiRequest("/cash-transactions", {
        method: "POST",
        body: {
          transactionType: cashDialog,
          amount: Number(cashAmount),
          remarks: cashReason.trim(),
        },
      });
      setCashDialog(null);
      setCashAmount("");
      setCashReason("");
      await loadDrawerTransactions();
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "The cash movement could not be recorded.",
      );
    } finally {
      setCashBusy(false);
    }
  }

  useEffect(() => {
    if (currentShift) return;
    const terminalId = getLocalTerminalId();
    if (!terminalId) return;
    let active = true;
    void terminalGateway.getCashRegisterConfig(terminalId)
      .then((config) => {
        if (active) setOpeningFloat(config.defaultOpeningFloat.toFixed(2));
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, [currentShift]);

  async function handleOpen(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!canOpenShift) {
      setError("Your active roles do not permit opening shifts.");
      return;
    }
    try {
      await workspaceGateway.openShift(openingFloat);
    } catch (caught) {
      setError(
        getWorkspaceErrorMessage(caught, "The shift could not be opened."),
      );
    }
  }

  async function handleClose(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!canCloseShift) {
      setError("Your active roles do not permit closing shifts.");
      return;
    }
    try {
      await workspaceGateway.closeShift(actualCash);
    } catch (caught) {
      setError(
        getWorkspaceErrorMessage(caught, "The shift could not be closed."),
      );
    }
  }

  const validActualCash = /^\d+(\.\d{1,2})?$/.test(actualCash);
  const previewVariance = currentShift && validActualCash
    ? centsToMoney(moneyToCents(actualCash) - moneyToCents(currentShift.expectedCash))
    : null;

  return (
    <div>
      <PageHeader
        title="Current shift"
        description="A shift controls sales access and reconciles the cash drawer at handover."
        actions={currentShift ? <StatusBadge tone="success">Open</StatusBadge> : <StatusBadge tone="neutral">Closed</StatusBadge>}
      />
      <FormError message={error} />

      {currentShift ? (
        <div className={cn("mt-6 grid gap-6", canCloseShift && "xl:grid-cols-[minmax(0,1.25fr)_minmax(320px,0.75fr)]")}>
          <div className="space-y-6">
            <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-sm text-[var(--text-muted)]">Opened by {currentShift.openedBy}</p>
                  <h2 className="mt-1 text-lg font-semibold">{formatDateTime(currentShift.openedAt)}</h2>
                </div>
                <StatusBadge tone="success"><Clock3 aria-hidden="true" className="mr-1" size={13} /> Active</StatusBadge>
              </div>
              <div className="mt-6 grid gap-px overflow-hidden rounded-md border border-[var(--border)] bg-[var(--border)] sm:grid-cols-2 lg:grid-cols-4">
                {[
                  ["Opening float", currentShift.openingFloat],
                  ["Cash sales", currentShift.cashSales],
                  ["M-Pesa sales", currentShift.mpesaSales],
                  ["Cash refunds", currentShift.cashRefunds],
                ].map(([label, value]) => (
                  <div className="bg-white p-4" key={label}>
                    <p className="text-xs text-[var(--text-muted)]">{label}</p>
                    <p className="mt-1 text-lg font-semibold">{formatKes(value)}</p>
                  </div>
                ))}
              </div>
              <div className="mt-5 flex items-center justify-between gap-4 border-t border-[var(--border)] pt-4">
                <span className="text-sm text-[var(--text-muted)]">Expected cash in drawer</span>
                <span className="text-xl font-semibold">{formatKes(currentShift.expectedCash)}</span>
              </div>

              <div className="mt-5 flex flex-wrap gap-2">
                <SecondaryButton
                  type="button"
                  onClick={() => void loadDrawerTransactions()}
                  title="Refresh drawer movements"
                >
                  <ArrowDownToLine aria-hidden="true" size={15} /> Refresh
                </SecondaryButton>
                <SecondaryButton
                  type="button"
                  onClick={async () => {
                    try {
                      const response = await apiRequest<Record<string, unknown>>(
                        `/reports/shift-z/${currentShiftId}`,
                        { cache: "no-store" },
                      );
                      setXReport(response.data as Record<string, string | number>);
                    } catch (caught) {
                      setError(caught instanceof Error ? caught.message : "X report failed.");
                    }
                  }}
                >
                  <ArrowUpFromLine aria-hidden="true" size={15} /> Print X report
                </SecondaryButton>
                <SecondaryButton
                  type="button"
                  onClick={() => { setCashDialog("CASH_IN"); setCashAmount(""); setCashReason(""); }}
                >
                  <ArrowDownToLine aria-hidden="true" size={15} /> Cash deposit
                </SecondaryButton>
                <SecondaryButton
                  type="button"
                  onClick={() => { setCashDialog("CASH_OUT"); setCashAmount(""); setCashReason(""); }}
                >
                  <ArrowUpFromLine aria-hidden="true" size={15} /> Cash pay-out
                </SecondaryButton>
              </div>

              {cashDialog ? (
                <form onSubmit={submitCashMovement} className="mt-4 rounded-md border border-[var(--border)] bg-[var(--surface-muted)] p-3">
                  <p className="text-sm font-semibold">
                    {cashDialog === "CASH_IN" ? "Record cash deposit" : "Record cash pay-out"}
                  </p>
                  <div className="mt-3 grid gap-3 sm:grid-cols-[140px_minmax(0,1fr)]">
                    <Field label="Amount (KES)" required>
                      <Input autoFocus inputMode="decimal" value={cashAmount} onChange={(event) => setCashAmount(event.target.value.replace(/[^\d.]/g, ""))} />
                    </Field>
                    <Field label="Reason" required>
                      <Input placeholder={cashDialog === "CASH_IN" ? "e.g. Change top-up" : "e.g. Petty cash, courier"} value={cashReason} onChange={(event) => setCashReason(event.target.value)} />
                    </Field>
                  </div>
                  <div className="mt-3 flex justify-end gap-2">
                    <SecondaryButton type="button" onClick={() => setCashDialog(null)}>Cancel</SecondaryButton>
                    <PrimaryButton type="submit" disabled={cashBusy || !cashAmount || !cashReason.trim()}>
                      {cashBusy ? "Saving..." : "Record"}
                    </PrimaryButton>
                  </div>
                </form>
              ) : null}

              {drawerTxns.length ? (
                <div className="mt-5 border-t border-[var(--border)] pt-4">
                  <h3 className="text-xs font-semibold uppercase text-[var(--text-muted)]">Drawer movements</h3>
                  <ul className="mt-2 space-y-1.5 text-sm">
                    {drawerTxns.slice(0, 6).map((txn) => (
                      <li key={txn.id} className="flex items-center justify-between gap-3">
                        <span className="min-w-0 truncate text-[var(--text-muted)]">
                          {formatDateTime(txn.createdAt)} - {txn.remarks}
                        </span>
                        <span className={txn.transactionType === "CASH_IN" ? "font-medium text-[var(--success)]" : "font-medium text-[var(--danger)]"}>
                          {txn.transactionType === "CASH_IN" ? "+" : "-"}{formatKes(txn.amount)}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
            </section>
          </div>

          {canCloseShift ? <form onSubmit={handleClose} className="h-fit rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
            <div className="flex size-10 items-center justify-center rounded-md bg-[var(--warning-soft)] text-[var(--warning)]"><LockKeyhole aria-hidden="true" size={19} /></div>
            <h2 className="mt-4 text-base font-semibold">Close and reconcile</h2>
            <p className="mt-1 text-sm text-[var(--text-muted)]">Count the physical cash drawer before closing.</p>
            <div className="mt-5">
              <Field label="Actual cash counted (KES)" required>
                <Input inputMode="decimal" value={actualCash} onChange={(event) => setActualCash(event.target.value.replace(/[^\d.]/g, ""))} />
              </Field>
            </div>
            <div className="mt-4 space-y-2 border-y border-[var(--border)] py-4 text-sm">
              <div className="flex justify-between gap-3"><span className="text-[var(--text-muted)]">Expected</span><span>{formatKes(currentShift.expectedCash)}</span></div>
              <div className="flex justify-between gap-3 font-semibold"><span>Variance</span><span className={previewVariance === null ? "text-[var(--text-muted)]" : moneyToCents(previewVariance) === 0 ? "text-[var(--success)]" : "text-[var(--danger)]"}>{previewVariance === null ? "Not counted" : formatKes(previewVariance)}</span></div>
            </div>
            <PrimaryButton type="submit" disabled={!validActualCash} className="mt-5 w-full bg-[var(--text)] hover:bg-[var(--brand-deep)]">
              <Square aria-hidden="true" size={16} /> Close shift
            </PrimaryButton>
          </form> : null}
        </div>
      ) : canOpenShift ? (
        <section className="mt-6 max-w-xl rounded-md border border-[var(--border)] bg-white p-5 sm:p-7">
          <div className="flex size-11 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand)]"><Banknote aria-hidden="true" size={21} /></div>
          <h2 className="mt-4 text-lg font-semibold">Open the cashier shift</h2>
          <p className="mt-1 text-sm text-[var(--text-muted)]">Sales stay locked until the starting cash float is recorded.</p>
          <form onSubmit={handleOpen} className="mt-5">
            <Field label="Opening cash float (KES)" required>
              <Input autoFocus inputMode="decimal" value={openingFloat} onChange={(event) => setOpeningFloat(event.target.value.replace(/[^\d.]/g, ""))} />
            </Field>
            <PrimaryButton type="submit" className="mt-5 w-full"><Play aria-hidden="true" size={17} /> Open shift</PrimaryButton>
          </form>
        </section>
      ) : (
        <section className="mt-6 max-w-xl rounded-md border border-[var(--border)] bg-white p-5 sm:p-7">
          <h2 className="text-lg font-semibold">No open shift</h2>
          <p className="mt-1 text-sm text-[var(--text-muted)]">
            Your active roles allow shift review but not shift opening.
          </p>
        </section>
      )}

      <Modal
        open={xReport !== null}
        onClose={() => setXReport(null)}
        title="X report"
        maxWidthClass="max-w-md"
        overlayClassName="print:static print:bg-white print:p-0"
        className="print:border-0 print:shadow-none"
        headerClassName="print:hidden"
        footerClassName="print:hidden"
        footer={
          <PrimaryButton type="button" onClick={() => window.print()}>
            Print X report
          </PrimaryButton>
        }
      >
        <dl className="space-y-2 text-sm">
          <p className="text-center text-sm font-bold uppercase">X daily report</p>
          <div className="flex justify-between"><dt className="text-[var(--text-muted)]">Sales receipts</dt><dd>{String(xReport?.salesCount ?? "-")}</dd></div>
          <div className="flex justify-between"><dt>Total sales</dt><dd className="font-semibold">{formatKes(String(xReport?.totalSales ?? "0"))}</dd></div>
          <div className="flex justify-between"><dt>Cash payments</dt><dd>{formatKes(String(xReport?.totalCashPayments ?? "0"))}</dd></div>
          <div className="flex justify-between"><dt>M-Pesa payments</dt><dd>{formatKes(String(xReport?.totalMpesaPayments ?? "0"))}</dd></div>
          <div className="flex justify-between"><dt>Opening float</dt><dd>{formatKes(String(xReport?.openingBalance ?? "0"))}</dd></div>
          <div className="flex justify-between"><dt>Expected in drawer</dt><dd>{formatKes(String(xReport?.expectedClosingBalance ?? "0"))}</dd></div>
        </dl>
      </Modal>
      <section className="mt-6 rounded-md border border-[var(--border)] bg-white">
        <div className="border-b border-[var(--border)] px-4 py-3.5"><h2 className="text-sm font-semibold">Shift history</h2></div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[840px] text-left text-sm">
            <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]"><tr><th className="px-4 py-3 font-semibold">Opened</th><th className="px-4 py-3 font-semibold">Operator</th><th className="px-4 py-3 text-right font-semibold">Sales</th><th className="px-4 py-3 text-right font-semibold">Expected cash</th><th className="px-4 py-3 text-right font-semibold">Variance</th><th className="px-4 py-3 font-semibold">State</th></tr></thead>
            <tbody className="divide-y divide-[var(--border)]">{shifts.map((shift) => <tr key={shift.id}><td className="px-4 py-3.5">{formatDateTime(shift.openedAt)}</td><td className="px-4 py-3.5 text-[var(--text-muted)]">{shift.openedBy}</td><td className="px-4 py-3.5 text-right">{formatKes(addMoney(shift.cashSales, shift.mpesaSales))}</td><td className="px-4 py-3.5 text-right">{formatKes(shift.expectedCash)}</td><td className="px-4 py-3.5 text-right">{shift.variance === null ? "—" : formatKes(shift.variance)}</td><td className="px-4 py-3.5"><StatusBadge tone={shift.status === "OPEN" ? "success" : "neutral"}>{shift.status === "OPEN" ? "Open" : "Closed"}</StatusBadge></td></tr>)}</tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
