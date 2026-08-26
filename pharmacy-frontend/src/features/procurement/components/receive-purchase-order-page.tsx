"use client";

import { ArrowLeft, PackageCheck, ReceiptText } from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import {
  PrimaryButton,
  SecondaryLink,
} from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { Field, FormError, Input } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  type PurchaseOrder,
  type PurchaseOrderReceipt,
  operationsGateway,
} from "@/features/operations/operations-gateway";
import {
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { formatKes } from "@/features/workspace/lib/money";
import { ApiClientError } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";
import { uuid } from "../../../lib/uuid";

interface ReceiveLineDraft {
  batchNumber: string;
  expiryDate: string;
  include: boolean;
  medicineId: string;
  medicineName: string;
  ordered: number;
  outstanding: number;
  purchaseOrderLineId: string;
  quantity: number;
  unitCost: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function buildReceiveLines(
  purchaseOrder: PurchaseOrder,
  received: PurchaseOrderReceipt[],
) {
  const receivedByLine = new Map<string, number>();
  for (const receipt of received) {
    for (const line of receipt.lines) {
      if (!line.purchaseOrderLineId) continue;
      receivedByLine.set(
        line.purchaseOrderLineId,
        (receivedByLine.get(line.purchaseOrderLineId) ?? 0) + line.quantity,
      );
    }
  }
  return purchaseOrder.items
    .map((item) => {
      const outstanding = Math.max(
        0,
        item.quantity - (receivedByLine.get(item.id) ?? 0),
      );
      return {
        batchNumber: "",
        expiryDate: "",
        include: outstanding > 0,
        medicineId: item.medicineId,
        medicineName: item.medicineName,
        ordered: item.quantity,
        outstanding,
        purchaseOrderLineId: item.id,
        quantity: outstanding,
        unitCost: Number(item.buyingPrice).toFixed(2),
      };
    })
    .filter((line) => line.outstanding > 0);
}

export function ReceivePurchaseOrderPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const canReceive = usePermission(PERMISSIONS.INVENTORY_RECEIVE);
  const suppliers = useWorkspaceQuery((state) => state.suppliers);
  const [order, setOrder] = useState<PurchaseOrder | null>(null);
  const [receipts, setReceipts] = useState<PurchaseOrderReceipt[]>([]);
  const [lines, setLines] = useState<ReceiveLineDraft[]>([]);
  const [supplierInvoiceNumber, setSupplierInvoiceNumber] = useState("");
  const [remarks, setRemarks] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(() =>
    uuid(),
  );
  const [minimumExpiryDate] = useState(() =>
    new Date(Date.now() + 86_400_000).toISOString().slice(0, 10),
  );

  useEffect(() => {
    if (!canReceive) return;
    let active = true;
    void Promise.all([
      operationsGateway.getPurchaseOrder(params.id),
      operationsGateway.listPurchaseOrderReceipts(params.id),
    ])
      .then(([purchaseOrder, received]) => {
        if (!active) return;
        setOrder(purchaseOrder);
        setReceipts(received);
        setLines(buildReceiveLines(purchaseOrder, received));
        setError(null);
      })
      .catch((caught) => {
        if (!active) return;
        setError(errorMessage(caught, "The purchase order could not be loaded."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canReceive, params.id]);

  const selectedLines = useMemo(
    () => lines.filter((line) => line.include),
    [lines],
  );
  const total = selectedLines.reduce(
    (sum, line) => sum + Number(line.unitCost || 0) * line.quantity,
    0,
  );

  function updateLine(
    purchaseOrderLineId: string,
    patch: Partial<ReceiveLineDraft>,
  ) {
    setLines((current) =>
      current.map((line) =>
        line.purchaseOrderLineId === purchaseOrderLineId
          ? { ...line, ...patch }
          : line,
      ),
    );
  }

  async function receive(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!order || selectedLines.length === 0) {
      setError("Select at least one delivered line.");
      return;
    }
    if (
      selectedLines.some(
        (line) =>
          !line.batchNumber.trim() ||
          !line.expiryDate ||
          line.quantity < 1 ||
          line.quantity > line.outstanding ||
          Number(line.unitCost) < 0,
      )
    ) {
      setError(
        "Enter a batch, future expiry, received quantity, and unit cost for each selected line.",
      );
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const receipt = await operationsGateway.receivePurchaseOrder(
        {
          lines: selectedLines.map((line) => ({
            batchNumber: line.batchNumber.trim().toUpperCase(),
            expiryDate: line.expiryDate,
            medicineId: line.medicineId,
            purchaseOrderLineId: line.purchaseOrderLineId,
            quantity: line.quantity,
            unitCost: Number(line.unitCost),
          })),
          purchaseOrdersId: order.id,
          remarks: remarks.trim() || null,
          supplierId: order.supplierId,
          supplierInvoiceNumber: supplierInvoiceNumber.trim() || null,
        },
        idempotencyKey,
      );
      await workspaceGateway.hydrate();
      setIdempotencyKey(uuid());
      router.push(
        `/procurement/purchase-orders?received=${encodeURIComponent(receipt.id)}`,
      );
    } catch (caught) {
      if (!(caught instanceof ApiClientError) || caught.status !== 0) {
        setIdempotencyKey(uuid());
      }
      setError(errorMessage(caught, "The delivery could not be received."));
    } finally {
      setSubmitting(false);
    }
  }

  if (!canReceive) return <AccessRestricted homePath="/inventory" />;

  if (loading) {
    return (
      <div className="rounded-md border border-[var(--border)] bg-white p-6 text-sm text-[var(--text-muted)]">
        Loading purchase order...
      </div>
    );
  }

  if (!order) {
    return (
      <div className="max-w-3xl">
        <PageHeader title="Receive purchase order" />
        <FormError message={error ?? "Purchase order not found."} />
        <SecondaryLink className="mt-4" href="/procurement/purchase-orders">
          <ArrowLeft aria-hidden="true" size={16} /> Back to purchase orders
        </SecondaryLink>
      </div>
    );
  }

  const supplier = suppliers.find((item) => item.id === order.supplierId);

  return (
    <div className="max-w-6xl">
      <PageHeader
        eyebrow="Goods received note"
        title={`Receive from ${supplier?.name ?? order.supplierName}`}
        description={`Purchase order created ${formatDateTime(order.orderDate)}. Receive full or partial quantities against its original lines.`}
        actions={
          <StatusBadge tone={order.status === "DELIVERED" ? "success" : "info"}>
            {order.status === "DELIVERED" ? "Delivered" : "Approved"}
          </StatusBadge>
        }
      />

      {lines.length ? (
        <form onSubmit={receive} className="space-y-5">
          <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Supplier invoice number">
                <Input
                  autoCapitalize="characters"
                  value={supplierInvoiceNumber}
                  onChange={(event) => setSupplierInvoiceNumber(event.target.value)}
                />
              </Field>
              <Field label="Receiving notes">
                <Input
                  value={remarks}
                  onChange={(event) => setRemarks(event.target.value)}
                />
              </Field>
            </div>
          </section>

          <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[980px] text-left text-sm">
                <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                  <tr>
                    <th className="w-12 px-3 py-3">
                      <span className="sr-only">Include</span>
                    </th>
                    <th className="px-3 py-3 font-semibold">Medicine</th>
                    <th className="w-32 px-3 py-3 font-semibold">Outstanding</th>
                    <th className="w-48 px-3 py-3 font-semibold">Batch number</th>
                    <th className="w-44 px-3 py-3 font-semibold">Expiry</th>
                    <th className="w-36 px-3 py-3 font-semibold">Received qty</th>
                    <th className="w-36 px-3 py-3 font-semibold">Unit cost</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {lines.map((line) => (
                    <tr
                      key={line.purchaseOrderLineId}
                      className={line.include ? "" : "bg-[var(--surface-muted)] opacity-65"}
                    >
                      <td className="px-3 py-3 text-center">
                        <input
                          type="checkbox"
                          className="size-4 accent-[var(--brand)]"
                          checked={line.include}
                          aria-label={`Receive ${line.medicineName}`}
                          onChange={(event) =>
                            updateLine(line.purchaseOrderLineId, {
                              include: event.target.checked,
                            })
                          }
                        />
                      </td>
                      <td className="px-3 py-3">
                        <p className="font-semibold">{line.medicineName}</p>
                        <p className="mt-1 text-xs text-[var(--text-muted)]">
                          {line.ordered} ordered
                        </p>
                      </td>
                      <td className="px-3 py-3 font-semibold">{line.outstanding}</td>
                      <td className="p-2">
                        <Input
                          disabled={!line.include}
                          autoCapitalize="characters"
                          value={line.batchNumber}
                          onChange={(event) =>
                            updateLine(line.purchaseOrderLineId, {
                              batchNumber: event.target.value,
                            })
                          }
                        />
                      </td>
                      <td className="p-2">
                        <Input
                          disabled={!line.include}
                          type="date"
                          min={minimumExpiryDate}
                          value={line.expiryDate}
                          onChange={(event) =>
                            updateLine(line.purchaseOrderLineId, {
                              expiryDate: event.target.value,
                            })
                          }
                        />
                      </td>
                      <td className="p-2">
                        <Input
                          disabled={!line.include}
                          type="number"
                          min={1}
                          max={line.outstanding}
                          step={1}
                          value={line.quantity}
                          onChange={(event) =>
                            updateLine(line.purchaseOrderLineId, {
                              quantity: Number(event.target.value),
                            })
                          }
                        />
                      </td>
                      <td className="p-2">
                        <Input
                          disabled={!line.include}
                          inputMode="decimal"
                          value={line.unitCost}
                          onChange={(event) =>
                            updateLine(line.purchaseOrderLineId, {
                              unitCost: event.target.value,
                            })
                          }
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between border-t border-[var(--border)] px-4 py-3 text-sm">
              <span className="text-[var(--text-muted)]">
                {selectedLines.length} selected lines
              </span>
              <span className="font-semibold">{formatKes(total.toFixed(2))}</span>
            </div>
          </section>

          <FormError message={error} />
          <div className="flex justify-end gap-2">
            <SecondaryLink href="/procurement/purchase-orders">
              Cancel
            </SecondaryLink>
            <PrimaryButton
              type="submit"
              disabled={submitting || selectedLines.length === 0}
            >
              <PackageCheck aria-hidden="true" size={17} />
              {submitting ? "Receiving..." : "Create GRN"}
            </PrimaryButton>
          </div>
        </form>
      ) : (
        <section className="rounded-md border border-[var(--border)] bg-white">
          <EmptyState
            icon={PackageCheck}
            title="Order fully received"
            description="Every purchase-order line has been matched to a goods received note."
          />
        </section>
      )}

      {receipts.length ? (
        <section className="mt-6 rounded-md border border-[var(--border)] bg-white">
          <div className="flex items-center gap-2 border-b border-[var(--border)] px-4 py-3.5">
            <ReceiptText aria-hidden="true" className="text-[var(--brand)]" size={17} />
            <h2 className="text-sm font-semibold">Receiving history</h2>
          </div>
          <div className="divide-y divide-[var(--border)]">
            {receipts.map((receipt) => (
              <div
                key={receipt.id}
                className="grid gap-1 px-4 py-3 text-sm sm:grid-cols-[minmax(0,1fr)_auto]"
              >
                <p className="font-semibold">
                  {receipt.supplierInvoiceNumber || "No supplier invoice"}
                </p>
                <p className="text-xs text-[var(--text-muted)]">
                  {formatDateTime(receipt.receivedAt)}
                </p>
                <p className="text-xs text-[var(--text-muted)]">
                  {receipt.lines.reduce((sum, line) => sum + line.quantity, 0)} units across {receipt.lines.length} lines
                </p>
              </div>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}
