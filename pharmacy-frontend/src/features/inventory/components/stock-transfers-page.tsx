"use client";

import {
  ArrowRightLeft,
  CheckCircle,
  Package,
  Plus,
  Search,
  Truck,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";

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
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { inventoryGateway } from "@/features/inventory/inventory-gateway";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { apiRequest, ApiClientError } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";
import type {
  StockTransfer,
} from "@/features/workspace/types";

interface BranchSummary {
  id: string;
  branchName: string;
  branchCode: string;
  status: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function statusTone(status: StockTransfer["status"]) {
  switch (status) {
    case "PENDING":
      return "warning" as const;
    case "APPROVED":
      return "info" as const;
    case "REJECTED":
      return "danger" as const;
    case "IN_TRANSIT":
      return "info" as const;
    case "RECEIVED":
      return "success" as const;
  }
}

export function StockTransfersPage() {
  const canRead = usePermission(PERMISSIONS.STOCK_TRANSFER_READ);
  const canWrite = usePermission(PERMISSIONS.STOCK_TRANSFER_WRITE);
  const branchId = useAuthStore((state) => state.session?.user.activeBranch.id);
  const user = useAuthStore((state) => state.session?.user);
  const batches = useWorkspaceQuery((state) => state.batches);
  const medicines = useWorkspaceQuery((state) => state.medicines);

  const [transfers, setTransfers] = useState<StockTransfer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [branches, setBranches] = useState<BranchSummary[]>([]);

  const [formOpen, setFormOpen] = useState(false);
  const [destBranchId, setDestBranchId] = useState("");
  const [notes, setNotes] = useState("");
  const [selectedItems, setSelectedItems] = useState<
    Array<{ medicineBatchId: string; quantity: string }>
  >([]);
  const [saving, setSaving] = useState(false);

  const [confirmAction, setConfirmAction] = useState<{
    type: "approve" | "receive";
    transfer: StockTransfer;
  } | null>(null);
  const [actionBusy, setActionBusy] = useState(false);

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

  const branchOptions = useMemo(
    () => branches.filter((b) => b.id !== branchId),
    [branches, branchId],
  );

  async function loadBranches() {
    if (!user) return;
    try {
      const response = await apiRequest<BranchSummary[]>(
        `/branches?pharmacyId=${encodeURIComponent(user.pharmacyId)}`,
        { cache: "no-store" },
      );
      setBranches(response.data);
    } catch {
      // Branches will remain empty array
    }
  }

  const filteredTransfers = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return transfers;
    return transfers.filter(
      (transfer) =>
        transfer.transferNumber.toLowerCase().includes(normalized) ||
        transfer.sourceBranchName.toLowerCase().includes(normalized) ||
        transfer.destBranchName.toLowerCase().includes(normalized) ||
        transfer.requestedByName.toLowerCase().includes(normalized),
    );
  }, [transfers, query]);

  async function loadTransfers() {
    setLoading(true);
    try {
      setTransfers(await inventoryGateway.listStockTransfers());
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Stock transfers could not be loaded."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!canRead) return;
    void loadTransfers();
    void loadBranches();
  }, [canRead]);

  function openCreate() {
    setFormOpen(true);
    setDestBranchId("");
    setNotes("");
    setSelectedItems([]);
    setError(null);
  }

  function closeForm() {
    setFormOpen(false);
    setDestBranchId("");
    setNotes("");
    setSelectedItems([]);
  }

  function addLine() {
    setSelectedItems((current) => [
      ...current,
      { medicineBatchId: "", quantity: "" },
    ]);
  }

  function removeLine(index: number) {
    setSelectedItems((current) => current.filter((_, i) => i !== index));
  }

  function updateLine(
    index: number,
    field: "medicineBatchId" | "quantity",
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
    if (!destBranchId) {
      setError("Select a destination branch.");
      return;
    }
    if (
      selectedItems.length === 0 ||
      selectedItems.some((item) => !item.medicineBatchId || !item.quantity)
    ) {
      setError("Add at least one item with a valid quantity.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await inventoryGateway.createStockTransfer({
        destBranchId,
        notes: notes.trim() || undefined,
        items: selectedItems.map((item) => ({
          medicineBatchId: item.medicineBatchId,
          quantity: Number(item.quantity) || 1,
        })),
      });
      closeForm();
      await loadTransfers();
    } catch (caught) {
      setError(errorMessage(caught, "The stock transfer could not be created."));
    } finally {
      setSaving(false);
    }
  }

  async function confirmApprove() {
    if (!confirmAction || confirmAction.type !== "approve" || actionBusy) return;
    setActionBusy(true);
    try {
      await inventoryGateway.approveStockTransfer(confirmAction.transfer.id);
      setConfirmAction(null);
      await loadTransfers();
    } catch (caught) {
      setError(errorMessage(caught, "The transfer could not be approved."));
      setConfirmAction(null);
    } finally {
      setActionBusy(false);
    }
  }

  async function confirmReceive() {
    if (!confirmAction || confirmAction.type !== "receive" || actionBusy) return;
    setActionBusy(true);
    try {
      await inventoryGateway.receiveStockTransfer(confirmAction.transfer.id);
      setConfirmAction(null);
      await loadTransfers();
    } catch (caught) {
      setError(errorMessage(caught, "The transfer could not be marked as received."));
      setConfirmAction(null);
    } finally {
      setActionBusy(false);
    }
  }

  if (!canRead) {
    return <AccessRestricted />;
  }

  return (
    <div>
      <PageHeader
        title="Stock transfers"
        description="Transfer inventory between branches and track shipment status."
        actions={
          canWrite ? (
            <PrimaryButton type="button" onClick={formOpen ? closeForm : openCreate}>
              {formOpen ? (
                <>
                  <ArrowRightLeft aria-hidden="true" size={17} />
                  Close
                </>
              ) : (
                <>
                  <Plus aria-hidden="true" size={17} />
                  New transfer
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
            <ArrowRightLeft
              aria-hidden="true"
              className="text-[var(--brand)]"
              size={18}
            />
            <h2 className="text-base font-semibold">New stock transfer</h2>
          </div>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <Field label="Destination branch" required>
              <Select
                required
                value={destBranchId}
                onChange={(event) => setDestBranchId(event.target.value)}
              >
                <option value="">Select branch</option>
                {branchOptions.map((branch) => (
                  <option key={branch.id} value={branch.id}>
                    {branch.branchName}
                  </option>
                ))}
              </Select>
            </Field>
            <div className="sm:col-span-2">
              <Field label="Notes">
                <Textarea
                  className="min-h-10"
                  rows={1}
                  placeholder="Optional notes about this transfer"
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                />
              </Field>
            </div>
          </div>
          <div className="mt-5 border-t border-[var(--border)] pt-5">
            <div className="mb-3 flex items-center justify-between gap-3">
              <h2 className="text-sm font-semibold">Items to transfer</h2>
              <SecondaryButton type="button" onClick={addLine}>
                <Plus aria-hidden="true" size={15} /> Add line
              </SecondaryButton>
            </div>
            {selectedItems.length === 0 ? (
              <p className="py-4 text-center text-sm text-[var(--text-muted)]">
                Click &quot;Add line&quot; to include batches in this transfer.
              </p>
            ) : (
              <div className="space-y-3">
                {selectedItems.map((item, index) => (
                  <div
                    key={index}
                    className="grid items-end gap-3 sm:grid-cols-[minmax(0,1fr)_120px_40px]"
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
                    <Field label="Quantity" required>
                      <Input
                        required
                        type="number"
                        min={1}
                        step={1}
                        value={item.quantity}
                        onChange={(event) =>
                          updateLine(index, "quantity", event.target.value)
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
                      <Truck aria-hidden="true" size={16} />
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
              disabled={saving || selectedItems.length === 0 || !destBranchId}
            >
              <ArrowRightLeft aria-hidden="true" size={17} />
              {saving ? "Creating..." : "Create transfer"}
            </PrimaryButton>
          </div>
        </form>
      ) : null}

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <label className="relative w-full max-w-xl">
          <span className="sr-only">Search stock transfers</span>
          <Search
            aria-hidden="true"
            className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]"
            size={17}
          />
          <Input
            className="pl-9"
            placeholder="Search by transfer number, branch, or requester"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
        <span className="text-xs text-[var(--text-muted)]">
          {loading ? "Loading..." : `${filteredTransfers.length} transfers`}
        </span>
      </div>

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {filteredTransfers.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Transfer #</th>
                  <th className="px-4 py-3 font-semibold">From</th>
                  <th className="px-4 py-3 font-semibold">To</th>
                  <th className="px-4 py-3 text-right font-semibold">Items</th>
                  <th className="px-4 py-3 font-semibold">Requested by</th>
                  <th className="px-4 py-3 font-semibold">Created</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="w-28 px-3 py-3">
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {filteredTransfers.map((transfer) => (
                  <tr key={transfer.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3 font-mono text-xs font-semibold">
                      {transfer.transferNumber}
                    </td>
                    <td className="px-4 py-3">{transfer.sourceBranchName}</td>
                    <td className="px-4 py-3">{transfer.destBranchName}</td>
                    <td className="px-4 py-3 text-right">{transfer.itemCount}</td>
                    <td className="px-4 py-3">{transfer.requestedByName}</td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {formatDateTime(transfer.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={statusTone(transfer.status)}>
                        {transfer.status.replace("_", " ").toLowerCase()}
                      </StatusBadge>
                    </td>
                    <td className="px-3 py-3">
                      <div className="flex justify-end gap-1">
                        {canWrite && transfer.status === "PENDING" ? (
                          <SecondaryButton
                            type="button"
                            title={`Approve ${transfer.transferNumber}`}
                            onClick={() =>
                              setConfirmAction({ type: "approve", transfer })
                            }
                          >
                            <CheckCircle aria-hidden="true" size={15} />
                            Approve
                          </SecondaryButton>
                        ) : null}
                        {canWrite && transfer.status === "APPROVED" ? (
                          <SecondaryButton
                            type="button"
                            title={`Receive ${transfer.transferNumber}`}
                            onClick={() =>
                              setConfirmAction({ type: "receive", transfer })
                            }
                          >
                            <Package aria-hidden="true" size={15} />
                            Receive
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
            Loading stock transfers...
          </div>
        ) : (
          <EmptyState
            icon={ArrowRightLeft}
            title={query.trim() ? "No matching transfers" : "No stock transfers yet"}
            description={
              query.trim()
                ? "Try a different search term."
                : "Create a new transfer to move inventory between branches."
            }
          />
        )}
      </section>

      {confirmAction ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-label={`${confirmAction.type === "approve" ? "Approve" : "Receive"} transfer`}
            className="w-full max-w-md rounded-md border border-[var(--border)] bg-white p-5 shadow-xl"
          >
            <h2 className="text-base font-semibold">
              {confirmAction.type === "approve"
                ? "Approve transfer?"
                : "Mark as received?"}
            </h2>
            <p className="mt-1 text-sm text-[var(--text-muted)]">
              {confirmAction.type === "approve"
                ? `Approve transfer ${confirmAction.transfer.transferNumber} from ${confirmAction.transfer.sourceBranchName} to ${confirmAction.transfer.destBranchName}?`
                : `Confirm receipt of transfer ${confirmAction.transfer.transferNumber}? This indicates the items have been received at ${confirmAction.transfer.destBranchName}.`}
            </p>
            <div className="mt-4 flex justify-end gap-2">
              <SecondaryButton type="button" onClick={() => setConfirmAction(null)}>
                Cancel
              </SecondaryButton>
              <PrimaryButton
                type="button"
                disabled={actionBusy}
                onClick={
                  confirmAction.type === "approve"
                    ? () => void confirmApprove()
                    : () => void confirmReceive()
                }
              >
                {actionBusy
                  ? confirmAction.type === "approve"
                    ? "Approving..."
                    : "Receiving..."
                  : confirmAction.type === "approve"
                    ? "Approve transfer"
                    : "Confirm receipt"}
              </PrimaryButton>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
