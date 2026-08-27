"use client";

import {
  AlertTriangle,
  ClipboardCheck,
  ClipboardList,
  Eye,
  FileCheck,
  ListChecks,
  Plus,
  Search,
  Trash2,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
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
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  inventoryGateway,
} from "@/features/inventory/inventory-gateway";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { ApiClientError } from "@/lib/api-client";
import { formatDate, formatDateTime } from "@/lib/format";
import type {
  StockCount,
  StockCountItem,
} from "@/features/workspace/types";

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function statusTone(status: StockCount["status"]) {
  switch (status) {
    case "DRAFT":
      return "neutral" as const;
    case "IN_PROGRESS":
      return "warning" as const;
    case "COMPLETED":
      return "info" as const;
    case "RECONCILED":
      return "success" as const;
  }
}

function varianceTone(variance: number | null) {
  if (variance === null) return "neutral" as const;
  if (variance === 0) return "success" as const;
  if (variance > 0) return "warning" as const;
  return "danger" as const;
}

export function StockCountsPage() {
  const canRead = usePermission(PERMISSIONS.STOCK_COUNT_READ);
  const canWrite = usePermission(PERMISSIONS.STOCK_COUNT_WRITE);
  const batches = useWorkspaceQuery((state) => state.batches);
  const medicines = useWorkspaceQuery((state) => state.medicines);

  const [counts, setCounts] = useState<StockCount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  const [formOpen, setFormOpen] = useState(false);
  const [notes, setNotes] = useState("");
  const [selectedItems, setSelectedItems] = useState<
    Array<{ medicineBatchId: string; countedQuantity: string }>
  >([]);
  const [saving, setSaving] = useState(false);

  const [completingId, setCompletingId] = useState<string | null>(null);
  const [confirmAction, setConfirmAction] = useState<{
    type: "complete" | "reconcile";
    count: StockCount;
  } | null>(null);
  const [actionBusy, setActionBusy] = useState(false);

  const [detailCount, setDetailCount] = useState<StockCount | null>(null);

  const batchOptions = useMemo(
    () =>
      batches
        .filter((batch) => batch.quantity > 0)
        .map((batch) => {
          const medicine = medicines.find((m) => m.id === batch.medicineId);
          return {
            id: batch.id,
            batchNumber: batch.batchNumber,
            medicineName: medicine?.brandName ?? "Unknown",
            quantity: batch.quantity,
          };
        }),
    [batches, medicines],
  );

  const filteredCounts = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return counts;
    return counts.filter(
      (count) =>
        count.countNumber.toLowerCase().includes(normalized) ||
        count.branchName.toLowerCase().includes(normalized) ||
        count.countedByName.toLowerCase().includes(normalized),
    );
  }, [counts, query]);

  async function loadCounts() {
    setLoading(true);
    try {
      setCounts(await inventoryGateway.listStockCounts());
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Stock counts could not be loaded."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!canRead) return;
    void loadCounts();
  }, [canRead]);

  function openCreate() {
    setFormOpen(true);
    setNotes("");
    setSelectedItems([]);
    setError(null);
  }

  function closeForm() {
    setFormOpen(false);
    setNotes("");
    setSelectedItems([]);
    setDetailCount(null);
  }

  function addLine() {
    setSelectedItems((current) => [
      ...current,
      { medicineBatchId: "", countedQuantity: "" },
    ]);
  }

  function removeLine(index: number) {
    setSelectedItems((current) => current.filter((_, i) => i !== index));
  }

  function updateLine(
    index: number,
    field: "medicineBatchId" | "countedQuantity",
    value: string,
  ) {
    setSelectedItems((current) =>
      current.map((item, i) =>
        i === index ? { ...item, [field]: value } : item,
      ),
    );
  }

  async function submitCreate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canWrite) return;
    if (
      selectedItems.length === 0 ||
      selectedItems.some((item) => !item.medicineBatchId)
    ) {
      setError("Select at least one batch to count.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await inventoryGateway.createStockCount({
        notes: notes.trim() || undefined,
        items: selectedItems.map((item) => ({
          medicineBatchId: item.medicineBatchId,
          countedQuantity: Number(item.countedQuantity) || 0,
        })),
      });
      closeForm();
      await loadCounts();
    } catch (caught) {
      setError(errorMessage(caught, "The stock count could not be created."));
    } finally {
      setSaving(false);
    }
  }

  async function confirmComplete() {
    if (!confirmAction || confirmAction.type !== "complete" || actionBusy) return;
    setActionBusy(true);
    try {
      await inventoryGateway.completeStockCount(confirmAction.count.id);
      setConfirmAction(null);
      setDetailCount(null);
      await loadCounts();
    } catch (caught) {
      setError(errorMessage(caught, "The stock count could not be completed."));
      setConfirmAction(null);
    } finally {
      setActionBusy(false);
    }
  }

  async function confirmReconcile() {
    if (!confirmAction || confirmAction.type !== "reconcile" || actionBusy) return;
    setActionBusy(true);
    try {
      await inventoryGateway.reconcileStockCount(confirmAction.count.id);
      setConfirmAction(null);
      setDetailCount(null);
      await loadCounts();
    } catch (caught) {
      setError(errorMessage(caught, "The stock count could not be reconciled."));
      setConfirmAction(null);
    } finally {
      setActionBusy(false);
    }
  }

  async function viewDetail(count: StockCount) {
    setCompletingId(count.id);
    try {
      const detail = await inventoryGateway.getStockCount(count.id);
      setDetailCount(detail);
    } catch (caught) {
      setError(errorMessage(caught, "Could not load stock count details."));
    } finally {
      setCompletingId(null);
    }
  }

  if (!canRead) {
    return <AccessRestricted />;
  }

  return (
    <div>
      <PageHeader
        title="Stock counts"
        description="Track physical inventory counts and reconcile variances with system stock."
        actions={
          canWrite ? (
            <PrimaryButton type="button" onClick={formOpen ? closeForm : openCreate}>
              {formOpen ? (
                <>
                  <ClipboardList aria-hidden="true" size={17} />
                  Close
                </>
              ) : (
                <>
                  <Plus aria-hidden="true" size={17} />
                  New count
                </>
              )}
            </PrimaryButton>
          ) : undefined
        }
      />

      {error && !formOpen ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}

      {formOpen ? (
        <form
          onSubmit={submitCreate}
          className="mb-6 rounded-md border border-[var(--border)] bg-white p-4 sm:p-6"
        >
          <div className="flex items-center gap-2">
            <ListChecks
              aria-hidden="true"
              className="text-[var(--brand)]"
              size={18}
            />
            <h2 className="text-base font-semibold">New stock count</h2>
          </div>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <Field label="Notes">
                <Textarea
                  className="min-h-10"
                  rows={1}
                  placeholder="Optional notes about this count"
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                />
              </Field>
            </div>
          </div>
          <div className="mt-5 border-t border-[var(--border)] pt-5">
            <div className="mb-3 flex items-center justify-between gap-3">
              <h2 className="text-sm font-semibold">Batch items to count</h2>
              <SecondaryButton type="button" onClick={addLine}>
                <Plus aria-hidden="true" size={15} /> Add line
              </SecondaryButton>
            </div>
            {selectedItems.length === 0 ? (
              <p className="py-4 text-center text-sm text-[var(--text-muted)]">
                Click &quot;Add line&quot; to include batches in this count.
              </p>
            ) : (
              <div className="space-y-3">
                {selectedItems.map((item, index) => (
                  <div
                    key={index}
                    className="grid items-end gap-3 sm:grid-cols-[minmax(0,1fr)_140px_40px]"
                  >
                    <Field label={`Batch ${index + 1}`} required>
                      <Select
                        required
                        value={item.medicineBatchId}
                        onChange={(event) =>
                          updateLine(index, "medicineBatchId", event.target.value)
                        }
                      >
                        <option value="">Select batch</option>
                        {batchOptions.map((batch) => (
                          <option key={batch.id} value={batch.id}>
                            {batch.medicineName} - {batch.batchNumber} ({batch.quantity} in stock)
                          </option>
                        ))}
                      </Select>
                    </Field>
                    <Field label="Counted quantity" required>
                      <Input
                        required
                        type="number"
                        min={0}
                        step={1}
                        value={item.countedQuantity}
                        onChange={(event) =>
                          updateLine(index, "countedQuantity", event.target.value)
                        }
                      />
                    </Field>
                    <button
                      type="button"
                      title="Remove line"
                      aria-label={`Remove line ${index + 1}`}
                      disabled={selectedItems.length === 1}
                      onClick={() => removeLine(index)}
                      className="flex size-10 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)] disabled:opacity-30"
                    >
                      <Trash2 aria-hidden="true" size={16} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div className="mt-4">
            <FormError message={error} />
          </div>
          <div className="mt-4 flex justify-end">
            <PrimaryButton
              type="submit"
              disabled={saving || selectedItems.length === 0}
            >
              <ClipboardCheck aria-hidden="true" size={17} />
              {saving ? "Creating..." : "Create stock count"}
            </PrimaryButton>
          </div>
        </form>
      ) : null}

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <label className="relative w-full max-w-xl">
          <span className="sr-only">Search stock counts</span>
          <Search
            aria-hidden="true"
            className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]"
            size={17}
          />
          <Input
            className="pl-9"
            placeholder="Search by count number, branch, or counter"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
        <span className="text-xs text-[var(--text-muted)]">
          {loading ? "Loading..." : `${filteredCounts.length} counts`}
        </span>
      </div>

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {filteredCounts.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[860px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Count #</th>
                  <th className="px-4 py-3 font-semibold">Branch</th>
                  <th className="px-4 py-3 font-semibold">Counted by</th>
                  <th className="px-4 py-3 text-right font-semibold">Items</th>
                  <th className="px-4 py-3 text-right font-semibold">Variances</th>
                  <th className="px-4 py-3 font-semibold">Created</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="w-28 px-3 py-3">
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {filteredCounts.map((count) => (
                  <tr key={count.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3 font-mono text-xs font-semibold">
                      {count.countNumber}
                    </td>
                    <td className="px-4 py-3">{count.branchName}</td>
                    <td className="px-4 py-3">{count.countedByName}</td>
                    <td className="px-4 py-3 text-right">{count.itemCount}</td>
                    <td className="px-4 py-3 text-right">
                      <StatusBadge tone={count.varianceCount > 0 ? "warning" : "success"}>
                        {count.varianceCount}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {formatDate(count.createdAt.slice(0, 10))}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={statusTone(count.status)}>
                        {count.status.replace("_", " ").toLowerCase()}
                      </StatusBadge>
                    </td>
                    <td className="px-3 py-3">
                      <div className="flex justify-end gap-1">
                        <button
                          type="button"
                          title={`View ${count.countNumber}`}
                          aria-label={`View ${count.countNumber}`}
                          disabled={completingId === count.id}
                          className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-white hover:text-[var(--text)]"
                          onClick={() => void viewDetail(count)}
                        >
                          <Eye aria-hidden="true" size={16} />
                        </button>
                        {canWrite && count.status === "DRAFT" ? (
                          <SecondaryButton
                            type="button"
                            title={`Complete ${count.countNumber}`}
                            onClick={() =>
                              setConfirmAction({ type: "complete", count })
                            }
                          >
                            <FileCheck aria-hidden="true" size={15} />
                            Complete
                          </SecondaryButton>
                        ) : null}
                        {canWrite && count.status === "COMPLETED" ? (
                          <SecondaryButton
                            type="button"
                            title={`Reconcile ${count.countNumber}`}
                            onClick={() =>
                              setConfirmAction({ type: "reconcile", count })
                            }
                          >
                            <AlertTriangle aria-hidden="true" size={15} />
                            Reconcile
                          </SecondaryButton>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading stock counts...
          </div>
        ) : (
          <EmptyState
            icon={ClipboardList}
            title={query.trim() ? "No matching stock counts" : "No stock counts yet"}
            description={
              query.trim()
                ? "Try a different search term."
                : "Create a new stock count to start tracking physical inventory."
            }
          />
        )}
      </section>

      {detailCount ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-label={`Stock count ${detailCount.countNumber}`}
            className="flex max-h-[90vh] w-full max-w-2xl flex-col rounded-md border border-[var(--border)] bg-white p-5 shadow-xl"
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="text-base font-semibold">
                  {detailCount.countNumber}
                </h2>
                <p className="mt-1 text-sm text-[var(--text-muted)]">
                  {detailCount.branchName} &middot; counted by{" "}
                  {detailCount.countedByName}
                </p>
              </div>
              <SecondaryButton type="button" onClick={closeForm}>
                Close
              </SecondaryButton>
            </div>
            {detailCount.notes ? (
              <p className="mt-2 text-sm text-[var(--text-muted)]">
                {detailCount.notes}
              </p>
            ) : null}
            <div className="mt-3 flex items-center gap-3 text-xs text-[var(--text-muted)]">
              <span>Created {formatDateTime(detailCount.createdAt)}</span>
              {detailCount.completedAt ? (
                <span>Completed {formatDateTime(detailCount.completedAt)}</span>
              ) : null}
              <StatusBadge tone={statusTone(detailCount.status)}>
                {detailCount.status.replace("_", " ").toLowerCase()}
              </StatusBadge>
            </div>
            <div className="mt-4 flex-1 overflow-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                  <tr>
                    <th className="px-3 py-2 font-semibold">Medicine</th>
                    <th className="px-3 py-2 font-semibold">Batch</th>
                    <th className="px-3 py-2 text-right font-semibold">System</th>
                    <th className="px-3 py-2 text-right font-semibold">Counted</th>
                    <th className="px-3 py-2 text-right font-semibold">Variance</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {detailCount.items.map((item: StockCountItem) => (
                    <tr key={item.id}>
                      <td className="px-3 py-2.5 font-medium">
                        {item.medicineName}
                      </td>
                      <td className="px-3 py-2.5 font-mono text-xs">
                        {item.batchNumber}
                      </td>
                      <td className="px-3 py-2.5 text-right">
                        {item.systemQuantity}
                      </td>
                      <td className="px-3 py-2.5 text-right font-semibold">
                        {item.countedQuantity ?? "—"}
                      </td>
                      <td className="px-3 py-2.5 text-right">
                        {item.variance !== null ? (
                          <StatusBadge tone={varianceTone(item.variance)}>
                            {item.variance > 0 ? "+" : ""}
                            {item.variance}
                          </StatusBadge>
                        ) : (
                          "—"
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {detailCount.status === "COMPLETED" && canWrite ? (
              <div className="mt-4 flex justify-end border-t border-[var(--border)] pt-4">
                <SecondaryButton
                  type="button"
                  onClick={() =>
                    setConfirmAction({ type: "reconcile", count: detailCount })
                  }
                >
                  <AlertTriangle aria-hidden="true" size={15} />
                  Reconcile variances
                </SecondaryButton>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}

      <ConfirmDialog
        open={Boolean(confirmAction)}
        busy={actionBusy}
        busyLabel={
          confirmAction?.type === "complete" ? "Completing..." : "Reconciling..."
        }
        title={
          confirmAction?.type === "complete"
            ? "Complete stock count?"
            : "Reconcile stock count?"
        }
        description={
          confirmAction?.type === "complete"
            ? `Mark ${confirmAction?.count.countNumber} as completed. This locks the counted quantities for reconciliation.`
            : `Reconcile variances for ${confirmAction?.count.countNumber}. This adjusts system stock to match the physical count.`
        }
        confirmLabel={
          confirmAction?.type === "complete" ? "Complete count" : "Reconcile now"
        }
        onCancel={() => setConfirmAction(null)}
        onConfirm={
          confirmAction?.type === "complete"
            ? () => void confirmComplete()
            : () => void confirmReconcile()
        }
      />
    </div>
  );
}
