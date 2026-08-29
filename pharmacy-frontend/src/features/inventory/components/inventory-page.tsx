"use client";

import { AlertTriangle, Boxes, ClipboardList, Layers3, Plus } from "lucide-react";
import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";

import { PrimaryButton, PrimaryLink, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { formatDate, formatDateTime } from "@/lib/format";
import { formatKes } from "@/features/workspace/lib/money";
import {
  daysUntil,
  stockForMedicine,
} from "@/features/workspace/lib/workspace-helpers";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { apiRequest } from "@/lib/api-client";
import { cn } from "@/lib/cn";

type InventoryTab = "stock" | "batches" | "movements" | "alerts";

const tabs: Array<{ id: InventoryTab; label: string }> = [
  { id: "stock", label: "Stock" },
  { id: "batches", label: "Batches" },
  { id: "movements", label: "Movements" },
  { id: "alerts", label: "Alerts" },
];

function expiryTone(days: number, nearExpiryDays: number) {
  if (days < 0) return "danger" as const;
  if (days <= nearExpiryDays) return "warning" as const;
  return "success" as const;
}

export function InventoryPage() {
  const router = useRouter();
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const batches = useWorkspaceQuery((state) => state.batches);
  const suppliers = useWorkspaceQuery((state) => state.suppliers);
  const movements = useWorkspaceQuery((state) => state.movements);
  const units = useWorkspaceQuery((state) => state.units);
  const settings = useWorkspaceQuery((state) => state.settings);
  const canReceiveStock = usePermission(PERMISSIONS.INVENTORY_RECEIVE);
  const canWriteOff = usePermission(PERMISSIONS.INVENTORY_ADJUST_APPROVE);
  const [tab, setTab] = useState<InventoryTab>("stock");
  const [query, setQuery] = useState("");
  const [writeOffTarget, setWriteOffTarget] = useState<{
    batch: { id: string; batchNumber: string; quantity: number };
    medicineName: string;
    remaining: number;
  } | null>(null);
  const [writeOffQty, setWriteOffQty] = useState("1");
  const [writeOffMethod, setWriteOffMethod] = useState("DISPOSAL");
  const [writeOffBusy, setWriteOffBusy] = useState(false);
  const [writeOffError, setWriteOffError] = useState<string | null>(null);

  async function submitWriteOff(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!writeOffTarget || writeOffBusy) return;
    setWriteOffBusy(true);
    setWriteOffError(null);
    try {
      await apiRequest("/expiry-logs", {
        method: "POST",
        body: {
          medicineBatchesId: writeOffTarget.batch.id,
          disposalMethod: writeOffMethod,
          quantityDisposed: Math.max(1, Math.floor(Number(writeOffQty) || 1)),
        },
      });
      setWriteOffTarget(null);
      window.location.reload();
    } catch (caught) {
      setWriteOffError(
        caught instanceof Error ? caught.message : "The write-off could not be recorded.",
      );
    } finally {
      setWriteOffBusy(false);
    }
  }

  const normalizedQuery = query.trim().toLowerCase();
  const stockRows = useMemo(
    () =>
      medicines
        .filter((medicine) =>
          [medicine.brandName, medicine.genericName, medicine.sku].some((value) =>
            value.toLowerCase().includes(normalizedQuery),
          ),
        )
        .map((medicine) => ({
          medicine,
          stock: stockForMedicine(batches, medicine.id),
          batches: batches.filter(
            (batch) => batch.medicineId === medicine.id && batch.quantity > 0,
          ).length,
        })),
    [batches, medicines, normalizedQuery],
  );
  const visibleBatches = useMemo(
    () =>
      [...batches]
        .filter((batch) => {
          const medicine = medicines.find((item) => item.id === batch.medicineId);
          return (
            batch.batchNumber.toLowerCase().includes(normalizedQuery) ||
            medicine?.brandName.toLowerCase().includes(normalizedQuery)
          );
        })
        .sort((left, right) => left.expiryDate.localeCompare(right.expiryDate)),
    [batches, medicines, normalizedQuery],
  );
  const visibleMovements = useMemo(
    () =>
      movements.filter((movement) => {
        const medicine = medicines.find((item) => item.id === movement.medicineId);
        return (
          movement.reference.toLowerCase().includes(normalizedQuery) ||
          medicine?.brandName.toLowerCase().includes(normalizedQuery)
        );
      }),
    [medicines, movements, normalizedQuery],
  );
  const lowStock = stockRows.filter(
    ({ medicine, stock }) => medicine.status === "ACTIVE" && stock <= medicine.reorderLevel,
  );
  const expiryAlerts = visibleBatches.filter(
    (batch) => daysUntil(batch.expiryDate) <= settings.nearExpiryDays,
  );

  return (
    <div>
      <PageHeader
        title="Inventory"
        description="Usable stock is calculated from non-expired batches and issued by earliest expiry first."
        actions={canReceiveStock ? (
          <PrimaryLink href="/procurement/grn/new">
            <Plus aria-hidden="true" size={17} />
            Receive stock
          </PrimaryLink>
        ) : undefined}
      />

      <section className="rounded-md border border-[var(--border)] bg-white">
        <div className="flex flex-col gap-3 border-b border-[var(--border)] p-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex overflow-x-auto" role="tablist" aria-label="Inventory views">
            {tabs.map((item) => (
              <button
                type="button"
                role="tab"
                aria-selected={tab === item.id}
                key={item.id}
                onClick={() => setTab(item.id)}
                className={cn(
                  "h-9 shrink-0 border-b-2 px-3 text-sm font-medium",
                  tab === item.id
                    ? "border-[var(--brand)] text-[var(--brand-strong)]"
                    : "border-transparent text-[var(--text-muted)] hover:text-[var(--text)]",
                )}
              >
                {item.label}
                {item.id === "alerts" && lowStock.length + expiryAlerts.length > 0 ? (
                  <span className="ml-2 rounded-full bg-[var(--danger-soft)] px-1.5 py-0.5 text-xs text-[var(--danger)]">
                    {lowStock.length + expiryAlerts.length}
                  </span>
                ) : null}
              </button>
            ))}
          </div>
          <label className="w-full lg:w-72">
            <span className="sr-only">Search inventory</span>
            <Input
              placeholder="Search medicine, batch or reference"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
        </div>

        {tab === "stock" ? (
          stockRows.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
                  <tr>
                    <th className="px-4 py-3 font-semibold">Medicine</th>
                    <th className="px-4 py-3 text-right font-semibold">Usable stock</th>
                    <th className="px-4 py-3 text-right font-semibold">Reorder level</th>
                    <th className="px-4 py-3 text-right font-semibold">Open batches</th>
                    <th className="px-4 py-3 font-semibold">State</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {stockRows.map(({ batches: batchCount, medicine, stock }) => {
                    const unit = units.find((item) => item.id === medicine.unitId);
                    const isLow = stock <= medicine.reorderLevel;
                    return (
                      <tr key={medicine.id}>
                        <td className="px-4 py-3.5">
                          <p className="font-semibold">{medicine.brandName}</p>
                          <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                            {medicine.genericName} · {medicine.sku}
                          </p>
                        </td>
                        <td className="px-4 py-3.5 text-right font-semibold">
                          {stock} <span className="text-xs font-normal text-[var(--text-muted)]">{unit?.symbol}</span>
                        </td>
                        <td className="px-4 py-3.5 text-right text-[var(--text-muted)]">
                          {medicine.reorderLevel}
                        </td>
                        <td className="px-4 py-3.5 text-right">{batchCount}</td>
                        <td className="px-4 py-3.5">
                          <StatusBadge tone={isLow ? "danger" : "success"}>
                            {stock === 0 ? "Out of stock" : isLow ? "Low stock" : "Healthy"}
                          </StatusBadge>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon={Boxes} title="No stock records found" description="Try a different search." />
          )
        ) : null}

        {tab === "batches" ? (
          visibleBatches.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[980px] text-left text-sm">
                <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
                  <tr>
                    <th className="px-4 py-3 font-semibold">Medicine</th>
                    <th className="px-4 py-3 font-semibold">Batch</th>
                    <th className="px-4 py-3 font-semibold">Supplier</th>
                    <th className="px-4 py-3 font-semibold">Expiry</th>
                    <th className="px-4 py-3 text-right font-semibold">Quantity</th>
                    <th className="px-4 py-3 text-right font-semibold">Unit cost</th>
                    <th className="px-4 py-3 font-semibold">Shelf</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {visibleBatches.map((batch) => {
                    const medicine = medicines.find((item) => item.id === batch.medicineId);
                    const supplier = suppliers.find((item) => item.id === batch.supplierId);
                    const expiryDays = daysUntil(batch.expiryDate);
                    return (
                      <tr key={batch.id}>
                        <td className="px-4 py-3.5 font-semibold">{medicine?.brandName ?? "Unknown"}</td>
                        <td className="px-4 py-3.5 font-mono text-xs">{batch.batchNumber}</td>
                        <td className="px-4 py-3.5 text-[var(--text-muted)]">{supplier?.name ?? "Unknown"}</td>
                        <td className="px-4 py-3.5">
                          <p>{formatDate(batch.expiryDate)}</p>
                          <StatusBadge tone={expiryTone(expiryDays, settings.nearExpiryDays)}>
                            {expiryDays < 0 ? "Expired" : expiryDays === 0 ? "Expires today" : `${expiryDays} days`}
                          </StatusBadge>
                        </td>
                        <td className="px-4 py-3.5 text-right font-semibold">{batch.quantity}</td>
                        <td className="px-4 py-3.5 text-right">{formatKes(batch.unitCost)}</td>
                        <td className="px-4 py-3.5 text-xs text-[var(--text-muted)]">{batch.shelfLocation || "—"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon={Layers3} title="No batches found" description="Try a different search or receive new stock." />
          )
        ) : null}

        {tab === "movements" ? (
          visibleMovements.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
                  <tr>
                    <th className="px-4 py-3 font-semibold">When</th>
                    <th className="px-4 py-3 font-semibold">Medicine</th>
                    <th className="px-4 py-3 font-semibold">Type</th>
                    <th className="px-4 py-3 text-right font-semibold">Change</th>
                    <th className="px-4 py-3 font-semibold">Reference</th>
                    <th className="px-4 py-3 font-semibold">Recorded by</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {visibleMovements.map((movement) => {
                    const medicine = medicines.find((item) => item.id === movement.medicineId);
                    return (
                      <tr key={movement.id}>
                        <td className="whitespace-nowrap px-4 py-3.5 text-[var(--text-muted)]">{formatDateTime(movement.occurredAt)}</td>
                        <td className="px-4 py-3.5 font-semibold">{medicine?.brandName ?? "Unknown"}</td>
                        <td className="px-4 py-3.5"><StatusBadge tone={movement.quantityDelta > 0 ? "success" : "info"}>{movement.type.replaceAll("_", " ")}</StatusBadge></td>
                        <td className={cn("px-4 py-3.5 text-right font-semibold", movement.quantityDelta > 0 ? "text-[var(--success)]" : "text-[var(--text)]")}>
                          {movement.quantityDelta > 0 ? "+" : ""}{movement.quantityDelta}
                        </td>
                        <td className="px-4 py-3.5 font-mono text-xs">{movement.reference}</td>
                        <td className="px-4 py-3.5 text-[var(--text-muted)]">{movement.actor}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon={ClipboardList} title="No movements found" description="Try a different search." />
          )
        ) : null}

        {tab === "alerts" ? (
          <div className="grid gap-6 p-4 lg:grid-cols-2 lg:p-6">
            <section>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="flex items-center gap-2 text-sm font-semibold">
                  <AlertTriangle aria-hidden="true" className="text-[var(--danger)]" size={17} />
                  Low stock ({lowStock.length})
                </h2>
                {lowStock.length && canWriteOff ? (
                  <SecondaryButton
                    type="button"
                    onClick={() => {
                      const draft = lowStock.map(({ medicine, stock }) => ({
                        medicineId: medicine.id,
                        brandName: medicine.brandName,
                        suggestedQty: Math.max(1, (medicine.reorderLevel ?? 0) - stock),
                        unitCost: medicine.buyingPrice,
                      }));
                      window.localStorage.setItem(
                        "pharmacy-pos:reorder-draft",
                        JSON.stringify(draft),
                      );
                      router.push("/procurement/purchase-orders?draft=1");
                    }}
                  >
                    Draft purchase order
                  </SecondaryButton>
                ) : null}
              </div>
              <div className="mt-3 divide-y divide-[var(--border)] border-y border-[var(--border)]">
                {lowStock.length ? lowStock.map(({ medicine, stock }) => (
                  <div className="flex items-center justify-between gap-4 py-3" key={medicine.id}>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">{medicine.brandName}</p>
                      <p className="text-xs text-[var(--text-muted)]">Reorder at {medicine.reorderLevel}</p>
                    </div>
                    <StatusBadge tone="danger">{stock} left</StatusBadge>
                  </div>
                )) : <p className="py-5 text-sm text-[var(--text-muted)]">All medicines are above their reorder levels.</p>}
              </div>
            </section>
            <section>
              <h2 className="flex items-center gap-2 text-sm font-semibold">
                <AlertTriangle aria-hidden="true" className="text-[var(--warning)]" size={17} />
                Expiry attention ({expiryAlerts.length})
              </h2>
              <div className="mt-3 divide-y divide-[var(--border)] border-y border-[var(--border)]">
                {expiryAlerts.length ? expiryAlerts.map((batch) => {
                  const medicine = medicines.find((item) => item.id === batch.medicineId);
                  const remaining = daysUntil(batch.expiryDate);
                  return (
                    <div className="flex items-center justify-between gap-4 py-3" key={batch.id}>
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold">{medicine?.brandName}</p>
                        <p className="text-xs text-[var(--text-muted)]">{batch.batchNumber} · {batch.quantity} units</p>
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        <StatusBadge tone={expiryTone(remaining, settings.nearExpiryDays)}>{remaining < 0 ? "Expired" : `${remaining} days`}</StatusBadge>
                        {canWriteOff ? (
                          <SecondaryButton
                            type="button"
                            title="Write off this batch as expired stock"
                            onClick={() =>
                              setWriteOffTarget({ batch, medicineName: medicine?.brandName ?? "Batch", remaining })
                            }
                          >
                            Write off
                          </SecondaryButton>
                        ) : null}
                      </div>
                    </div>
                  );
                }) : <p className="py-5 text-sm text-[var(--text-muted)]">No batches are near expiry.</p>}
              </div>
            </section>
          </div>
        ) : null}
      </section>

      {writeOffTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <form
            onSubmit={submitWriteOff}
            role="dialog"
            aria-modal="true"
            aria-label="Write off expired stock"
            className="w-full max-w-md rounded-md border border-[var(--border)] bg-white p-5 shadow-xl"
          >
            <h2 className="text-base font-semibold">Write off expired stock</h2>
            <p className="mt-1 text-sm text-[var(--text-muted)]">
              {writeOffTarget.medicineName} · batch {writeOffTarget.batch.batchNumber} ·{" "}
              {writeOffTarget.batch.quantity} units on hand. This removes units from
              sellable stock permanently and records a regulatory disposal log.
            </p>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Field label="Units to dispose" required>
                <Input
                  autoFocus
                  inputMode="numeric"
                  min={1}
                  max={writeOffTarget.batch.quantity}
                  type="number"
                  value={writeOffQty}
                  onChange={(event) => setWriteOffQty(event.target.value)}
                />
              </Field>
              <Field label="Disposal method" required>
                <Select
                  value={writeOffMethod}
                  onChange={(event) => setWriteOffMethod(event.target.value)}
                >
                  <option value="DISPOSAL">Disposal</option>
                  <option value="RETURN_TO_SUPPLIER">Return to supplier</option>
                  <option value="DESTRUCTION">Destruction</option>
                  <option value="DONATION">Donation (pre-expiry)</option>
                </Select>
              </Field>
            </div>
            <div className="mt-4">
              <FormError message={writeOffError} />
            </div>
            <div className="mt-4 flex justify-end gap-2">
              <SecondaryButton type="button" onClick={() => setWriteOffTarget(null)}>
                Cancel
              </SecondaryButton>
              <PrimaryButton
                type="submit"
                disabled={
                  writeOffBusy ||
                  !writeOffQty ||
                  Number(writeOffQty) < 1 ||
                  Number(writeOffQty) > writeOffTarget.batch.quantity
                }
              >
                {writeOffBusy ? "Saving..." : "Record write-off"}
              </PrimaryButton>
            </div>
          </form>
        </div>
      ) : null}
    </div>
  );
}
