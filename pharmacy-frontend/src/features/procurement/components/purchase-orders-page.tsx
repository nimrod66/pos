"use client";

import {
  Check,
  ChevronDown,
  ClipboardList,
  PackageCheck,
  Plus,
  RefreshCw,
  Trash2,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  PrimaryButton,
  PrimaryLink,
  SecondaryButton,
} from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import {
  type PurchaseOrder,
  operationsGateway,
} from "@/features/operations/operations-gateway";
import { formatKes } from "@/features/workspace/lib/money";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { ApiClientError } from "@/lib/api-client";
import { formatDate, formatDateTime } from "@/lib/format";
import { uuid } from "../../../lib/uuid";

interface DraftLine {
  key: string;
  medicineId: string;
  quantity: number;
  buyingPrice: string;
  discount: string;
  tax: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function newLine(medicineId = "", buyingPrice = "0.00"): DraftLine {
  return {
    buyingPrice,
    discount: "0.00",
    key: uuid(),
    medicineId,
    quantity: 1,
    tax: "0.00",
  };
}

function statusTone(status: PurchaseOrder["status"]) {
  if (status === "DELIVERED") return "success" as const;
  if (status === "FAILED") return "danger" as const;
  if (status === "IN_PROGRESS") return "info" as const;
  return "warning" as const;
}

function statusLabel(status: PurchaseOrder["status"]) {
  if (status === "IN_PROGRESS") return "Approved";
  return status.charAt(0) + status.slice(1).toLowerCase();
}

export function PurchaseOrdersPage() {
  const session = useAuthStore((state) => state.session);
  const suppliers = useWorkspaceQuery((state) => state.suppliers);
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const canRead = usePermission(PERMISSIONS.PURCHASE_ORDER_READ);
  const canCreate = usePermission(PERMISSIONS.PURCHASE_ORDER_WRITE);
  const canApprove = usePermission(PERMISSIONS.INVENTORY_ADJUST_APPROVE);
  const canReceive = usePermission(PERMISSIONS.INVENTORY_RECEIVE);
  const [orders, setOrders] = useState<PurchaseOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [supplierId, setSupplierId] = useState("");
  const [expectedDeliveryDate, setExpectedDeliveryDate] = useState("");
  const [lines, setLines] = useState<DraftLine[]>([]);
  const [minimumDeliveryDate] = useState(() =>
    new Date(Date.now() + 86_400_000).toISOString().slice(0, 10),
  );

  const activeSuppliers = useMemo(
    () => suppliers.filter((supplier) => supplier.status === "ACTIVE"),
    [suppliers],
  );
  const activeMedicines = useMemo(
    () => medicines.filter((medicine) => medicine.status === "ACTIVE"),
    [medicines],
  );

  const loadOrders = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try {
      setOrders(
        await operationsGateway.listPurchaseOrders(
          session.user.activeBranch.id,
        ),
      );
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Purchase orders could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    if (!canRead || !session) return;
    let active = true;
    window.queueMicrotask(() => {
      if (active) void loadOrders();
    });
    return () => {
      active = false;
    };
  }, [canRead, loadOrders, session]);

  // Prefill a purchase order from a low-stock reorder draft (inventory page).
  useEffect(() => {
    const initial = window.setTimeout(() => {
      const raw = window.localStorage.getItem("pharmacy-pos:reorder-draft");
      if (!raw) return;
      window.localStorage.removeItem("pharmacy-pos:reorder-draft");
      type ReorderItem = { medicineId: string; suggestedQty: number; unitCost: number };
      let draft: ReorderItem[] = [];
      try {
        draft = JSON.parse(raw) as ReorderItem[];
      } catch {
        return;
      }
      const mapped = draft
        .map((item) =>
          newLine(
            item.medicineId,
            Number(item.unitCost ?? 0).toFixed(2),
          ),
        )
        .map((line, index) => ({ ...line, quantity: Math.max(1, draft[index].suggestedQty) }));
      if (mapped.length) {
        setLines(mapped);
        setFormOpen(true);
      }
    }, 0);
    return () => window.clearTimeout(initial);
  }, []);

  function resetForm() {
    const medicine = activeMedicines[0];
    setSupplierId(activeSuppliers[0]?.id ?? "");
    setExpectedDeliveryDate("");
    setLines([
      newLine(medicine?.id ?? "", medicine?.buyingPrice ?? "0.00"),
    ]);
    setError(null);
  }

  function openForm() {
    resetForm();
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setLines([]);
    setError(null);
  }

  function updateLine(key: string, patch: Partial<DraftLine>) {
    setLines((current) =>
      current.map((line) => (line.key === key ? { ...line, ...patch } : line)),
    );
  }

  function chooseMedicine(key: string, medicineId: string) {
    const medicine = activeMedicines.find((item) => item.id === medicineId);
    updateLine(key, {
      buyingPrice: medicine?.buyingPrice ?? "0.00",
      medicineId,
    });
  }

  async function createOrder(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !canCreate) return;
    if (!supplierId || lines.length === 0) {
      setError("Choose a supplier and add at least one medicine.");
      return;
    }
    if (new Set(lines.map((line) => line.medicineId)).size !== lines.length) {
      setError("A medicine may appear only once on a purchase order.");
      return;
    }
    if (
      lines.some(
        (line) =>
          !line.medicineId ||
          !Number.isInteger(line.quantity) ||
          line.quantity < 1 ||
          Number(line.buyingPrice) <= 0 ||
          Number(line.discount) < 0 ||
          Number(line.tax) < 0,
      )
    ) {
      setError("Check each medicine, quantity, cost, discount, and tax amount.");
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await operationsGateway.createPurchaseOrder({
        branchId: session.user.activeBranch.id,
        expectedDeliveryDate: expectedDeliveryDate
          ? `${expectedDeliveryDate}T12:00:00`
          : null,
        items: lines.map((line) => ({
          buyingPrice: Number(line.buyingPrice),
          discount: Number(line.discount || 0),
          medicineId: line.medicineId,
          quantity: line.quantity,
          tax: Number(line.tax || 0),
        })),
        orderedById: session.user.id,
        supplierId,
      });
      closeForm();
      await loadOrders();
    } catch (caught) {
      setError(errorMessage(caught, "The purchase order could not be created."));
    } finally {
      setSaving(false);
    }
  }

  async function approve(order: PurchaseOrder) {
    if (!session || !canApprove || busyId) return;
    setBusyId(order.id);
    setError(null);
    try {
      await operationsGateway.approvePurchaseOrder(order.id, session.user.id);
      await loadOrders();
    } catch (caught) {
      setError(errorMessage(caught, "The purchase order could not be approved."));
    } finally {
      setBusyId(null);
    }
  }

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      <PageHeader
        title="Purchase orders"
        description="Prepare supplier orders, approve them, and receive delivered batches through a linked GRN."
        actions={
          <>
            <SecondaryButton
              type="button"
              title="Refresh purchase orders"
              aria-label="Refresh purchase orders"
              onClick={() => void loadOrders()}
              disabled={loading}
              className="px-3"
            >
              <RefreshCw aria-hidden="true" size={17} />
            </SecondaryButton>
            {canCreate ? (
              <PrimaryButton
                type="button"
                onClick={formOpen ? closeForm : openForm}
              >
                {formOpen ? (
                  <X aria-hidden="true" size={17} />
                ) : (
                  <Plus aria-hidden="true" size={17} />
                )}
                {formOpen ? "Close" : "New order"}
              </PrimaryButton>
            ) : null}
          </>
        }
      />

      {formOpen ? (
        <form
          onSubmit={createOrder}
          className="mb-6 rounded-md border border-[var(--border)] bg-white p-4 sm:p-6"
        >
          <h2 className="text-base font-semibold">New purchase order</h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <Field label="Supplier" required>
              <Select
                value={supplierId}
                onChange={(event) => setSupplierId(event.target.value)}
                required
              >
                <option value="">Choose supplier</option>
                {activeSuppliers.map((supplier) => (
                  <option key={supplier.id} value={supplier.id}>
                    {supplier.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Expected delivery">
              <Input
                type="date"
                min={minimumDeliveryDate}
                value={expectedDeliveryDate}
                onChange={(event) => setExpectedDeliveryDate(event.target.value)}
              />
            </Field>
          </div>

          <div className="mt-5 overflow-x-auto rounded-md border border-[var(--border)]">
            <table className="w-full min-w-[850px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-3 py-2.5 font-semibold">Medicine</th>
                  <th className="w-28 px-3 py-2.5 font-semibold">Quantity</th>
                  <th className="w-36 px-3 py-2.5 font-semibold">Unit cost</th>
                  <th className="w-32 px-3 py-2.5 font-semibold">Discount</th>
                  <th className="w-32 px-3 py-2.5 font-semibold">Tax</th>
                  <th className="w-14 px-2 py-2.5" />
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {lines.map((line) => (
                  <tr key={line.key}>
                    <td className="p-2">
                      <Select
                        value={line.medicineId}
                        onChange={(event) =>
                          chooseMedicine(line.key, event.target.value)
                        }
                        required
                      >
                        <option value="">Choose medicine</option>
                        {activeMedicines.map((medicine) => (
                          <option key={medicine.id} value={medicine.id}>
                            {medicine.brandName} - {medicine.sku}
                          </option>
                        ))}
                      </Select>
                    </td>
                    <td className="p-2">
                      <Input
                        type="number"
                        min={1}
                        step={1}
                        value={line.quantity}
                        onChange={(event) =>
                          updateLine(line.key, {
                            quantity: Number(event.target.value),
                          })
                        }
                        required
                      />
                    </td>
                    <td className="p-2">
                      <Input
                        inputMode="decimal"
                        value={line.buyingPrice}
                        onChange={(event) =>
                          updateLine(line.key, {
                            buyingPrice: event.target.value,
                          })
                        }
                        required
                      />
                    </td>
                    <td className="p-2">
                      <Input
                        inputMode="decimal"
                        value={line.discount}
                        onChange={(event) =>
                          updateLine(line.key, { discount: event.target.value })
                        }
                      />
                    </td>
                    <td className="p-2">
                      <Input
                        inputMode="decimal"
                        value={line.tax}
                        onChange={(event) =>
                          updateLine(line.key, { tax: event.target.value })
                        }
                      />
                    </td>
                    <td className="p-2">
                      <button
                        type="button"
                        title="Remove line"
                        aria-label="Remove line"
                        disabled={lines.length === 1}
                        onClick={() =>
                          setLines((current) =>
                            current.filter((item) => item.key !== line.key),
                          )
                        }
                        className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)] disabled:opacity-30"
                      >
                        <Trash2 aria-hidden="true" size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-3 flex flex-wrap items-start justify-between gap-3">
            <SecondaryButton
              type="button"
              onClick={() => {
                const medicine = activeMedicines.find(
                  (item) => !lines.some((line) => line.medicineId === item.id),
                );
                setLines((current) => [
                  ...current,
                  newLine(medicine?.id, medicine?.buyingPrice),
                ]);
              }}
            >
              <Plus aria-hidden="true" size={16} /> Add line
            </SecondaryButton>
            <div className="text-right">
              <p className="text-xs text-[var(--text-muted)]">Order total</p>
              <p className="text-lg font-semibold">
                {formatKes(
                  lines
                    .reduce(
                      (total, line) =>
                        total +
                        Number(line.buyingPrice || 0) * line.quantity -
                        Number(line.discount || 0) +
                        Number(line.tax || 0),
                      0,
                    )
                    .toFixed(2),
                )}
              </p>
            </div>
          </div>
          <div className="mt-4">
            <FormError message={error} />
          </div>
          <div className="mt-4 flex justify-end">
            <PrimaryButton type="submit" disabled={saving}>
              <ClipboardList aria-hidden="true" size={17} />
              {saving ? "Creating..." : "Create order"}
            </PrimaryButton>
          </div>
        </form>
      ) : null}

      {!formOpen && error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {orders.length ? (
          <div className="divide-y divide-[var(--border)]">
            {orders.map((order) => {
              const total = order.items.reduce(
                (sum, item) => sum + Number(item.total),
                0,
              );
              return (
                <details key={order.id} className="group">
                  <summary className="grid min-h-20 list-none grid-cols-[minmax(0,1fr)_auto] items-center gap-4 px-4 py-3 hover:bg-[var(--surface-muted)] sm:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)_auto_auto]">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">
                        {order.supplierName}
                      </p>
                      <p className="mt-1 text-xs text-[var(--text-muted)]">
                        {formatDateTime(order.orderDate)} by {order.orderedByName}
                      </p>
                    </div>
                    <div className="hidden text-sm sm:block">
                      <p className="font-semibold">{formatKes(total.toFixed(2))}</p>
                      <p className="mt-1 text-xs text-[var(--text-muted)]">
                        {order.items.length} {order.items.length === 1 ? "line" : "lines"}
                      </p>
                    </div>
                    <StatusBadge tone={statusTone(order.status)}>
                      {statusLabel(order.status)}
                    </StatusBadge>
                    <ChevronDown
                      aria-hidden="true"
                      className="text-[var(--text-muted)] transition-transform group-open:rotate-180"
                      size={17}
                    />
                  </summary>
                  <div className="border-t border-[var(--border)] bg-[var(--surface-muted)]/50 px-4 py-4">
                    <div className="overflow-x-auto rounded-md border border-[var(--border)] bg-white">
                      <table className="w-full min-w-[650px] text-left text-sm">
                        <thead className="border-b border-[var(--border)] text-xs text-[var(--text-muted)]">
                          <tr>
                            <th className="px-3 py-2.5 font-semibold">Medicine</th>
                            <th className="px-3 py-2.5 text-right font-semibold">Qty</th>
                            <th className="px-3 py-2.5 text-right font-semibold">Unit cost</th>
                            <th className="px-3 py-2.5 text-right font-semibold">Line total</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-[var(--border)]">
                          {order.items.map((item) => (
                            <tr key={item.id}>
                              <td className="px-3 py-2.5 font-medium">
                                {item.medicineName}
                              </td>
                              <td className="px-3 py-2.5 text-right">{item.quantity}</td>
                              <td className="px-3 py-2.5 text-right">
                                {formatKes(item.buyingPrice.toFixed(2))}
                              </td>
                              <td className="px-3 py-2.5 text-right font-semibold">
                                {formatKes(item.total.toFixed(2))}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-xs text-[var(--text-muted)]">
                      <span>
                        Expected: {order.expectedDeliveryDate ? formatDate(order.expectedDeliveryDate.slice(0, 10)) : "Not set"}
                      </span>
                      <div className="flex gap-2">
                        {canApprove && order.status === "ORDERED" ? (
                          <SecondaryButton
                            type="button"
                            disabled={busyId === order.id}
                            onClick={() => void approve(order)}
                          >
                            <Check aria-hidden="true" size={16} />
                            {busyId === order.id ? "Approving..." : "Approve"}
                          </SecondaryButton>
                        ) : null}
                        {canReceive && order.status === "IN_PROGRESS" ? (
                          <PrimaryLink
                            href={`/procurement/purchase-orders/${order.id}/receive`}
                          >
                            <PackageCheck aria-hidden="true" size={16} /> Receive
                          </PrimaryLink>
                        ) : null}
                      </div>
                    </div>
                  </div>
                </details>
              );
            })}
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading purchase orders...
          </div>
        ) : (
          <EmptyState
            icon={ClipboardList}
            title="No purchase orders"
            description="Create an order when stock needs to be requested from a supplier."
          />
        )}
      </section>
    </div>
  );
}
