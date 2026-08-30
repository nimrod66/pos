"use client";

import { PackagePlus, Plus, Trash2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { PrimaryButton, SecondaryLink } from "@/components/ui/buttons";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { addMoney, formatKes, multiplyMoney } from "@/features/workspace/lib/money";
import { todayIsoDate } from "@/features/workspace/lib/workspace-helpers";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { ApiClientError } from "@/lib/api-client";
import type { ReceiveStockLine } from "@/features/workspace/types";
import { uuid } from "../../../lib/uuid";

interface GrnLine extends ReceiveStockLine {
  key: string;
}

function emptyLine(medicineId: string, unitCost: string): GrnLine {
  return { key: uuid(), medicineId, batchNumber: "", expiryDate: "", quantity: 1, unitCost };
}

export function ReceiveStockForm() {
  const router = useRouter();
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const suppliers = useWorkspaceQuery((state) => state.suppliers);
  const canReceiveStock = usePermission(PERMISSIONS.INVENTORY_RECEIVE);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [idempotencyKey, setIdempotencyKey] = useState(() => uuid());

  const activeMedicines = medicines.filter((medicine) => medicine.status === "ACTIVE");
  const activeSuppliers = suppliers.filter((supplier) => supplier.status === "ACTIVE");

  const [supplierId, setSupplierId] = useState("");
  const [supplierInvoiceNumber, setSupplierInvoiceNumber] = useState("");
  const [remarks, setRemarks] = useState("");
  const [lines, setLines] = useState<GrnLine[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!supplierId && activeSuppliers[0]) setSupplierId(activeSuppliers[0].id);
    if (lines.length === 0 && activeMedicines[0]) {
      setLines([emptyLine(activeMedicines[0].id, activeMedicines[0].buyingPrice)]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSuppliers.length, activeMedicines.length]);

  function updateLine(key: string, updates: Partial<GrnLine>) {
    setLines((prev) => prev.map((line) => (line.key === key ? { ...line, ...updates } : line)));
  }

  function addLine() {
    const first = activeMedicines[0];
    setLines((prev) => [...prev, emptyLine(first?.id ?? "", first?.buyingPrice ?? "0.00")]);
  }

  function removeLine(key: string) {
    setLines((prev) => (prev.length > 1 ? prev.filter((line) => line.key !== key) : prev));
  }

  const estimatedTotal = addMoney(
    ...lines.map((line) =>
      line.quantity > 0 && /^\d+(\.\d{1,2})?$/.test(line.unitCost ?? "")
        ? multiplyMoney(line.unitCost, line.quantity)
        : "0.00",
    ),
  );

  function validate(): boolean {
    const next: Record<string, string> = {};
    if (!supplierId) next.supplierId = "Choose a supplier.";
    lines.forEach((line, index) => {
      const label = `Line ${index + 1}`;
      if (!line.medicineId) next[line.key] = `${label}: choose a medicine.`;
      else if (line.batchNumber.trim().length < 2) next[line.key] = `${label}: enter the supplier batch number.`;
      else if (!line.expiryDate) next[line.key] = `${label}: choose an expiry date.`;
      else if (line.expiryDate <= todayIsoDate()) next[line.key] = `${label}: expiry must be after today.`;
      else if (!Number.isInteger(line.quantity) || line.quantity < 1) next[line.key] = `${label}: quantity must be at least one.`;
      else if (!/^\d+(\.\d{1,2})?$/.test(line.unitCost ?? "")) next[line.key] = `${label}: enter a valid unit cost.`;
    });
    setFieldErrors(next);
    return Object.keys(next).length === 0;
  }

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError(null);
    if (!canReceiveStock) {
      setSubmitError("Your active roles do not permit stock receiving.");
      return;
    }
    if (!validate()) return;
    setSubmitting(true);
    try {
      const grn = await workspaceGateway.receiveStock({
        idempotencyKey,
        supplierId,
        medicineId: lines[0]?.medicineId ?? "",
        batchNumber: lines[0]?.batchNumber ?? "",
        expiryDate: lines[0]?.expiryDate ?? "",
        quantity: lines[0]?.quantity ?? 0,
        unitCost: lines[0]?.unitCost ?? "0.00",
        lines: lines.map(({ medicineId, batchNumber, expiryDate, quantity, unitCost }) => ({
          medicineId,
          batchNumber,
          expiryDate,
          quantity,
          unitCost,
        })),
        supplierInvoiceNumber,
        remarks,
      });
      setIdempotencyKey(uuid());
      router.push(`/inventory?received=${encodeURIComponent(grn)}`);
    } catch (error) {
      if (!(error instanceof ApiClientError) || error.status !== 0) {
        setIdempotencyKey(uuid());
      }
      setSubmitError(
        getWorkspaceErrorMessage(error, "Stock could not be received."),
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (!canReceiveStock) {
    return <AccessRestricted homePath="/inventory" />;
  }

  return (
    <div className="max-w-5xl">
      <PageHeader
        eyebrow="Goods received note"
        title="Receive stock"
        description="Add one or many medicine batches. The system records each supplier batch, cost, expiry, and stock movement."
      />
      <form onSubmit={onSubmit} className="space-y-6">
        <FormError message={submitError} />
        <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Supplier" required error={fieldErrors.supplierId}>
              <Select value={supplierId} onChange={(event) => setSupplierId(event.target.value)}>
                {activeSuppliers.map((supplier) => (
                  <option key={supplier.id} value={supplier.id}>{supplier.name}</option>
                ))}
              </Select>
            </Field>
            <Field label="Supplier invoice number">
              <Input autoCapitalize="characters" value={supplierInvoiceNumber} onChange={(event) => setSupplierInvoiceNumber(event.target.value)} />
            </Field>
          </div>
        </section>

        <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold">Batches ({lines.length})</h2>
            <button
              type="button"
              onClick={addLine}
              className="flex h-9 items-center gap-1.5 rounded-md border border-[var(--border-strong)] px-3 text-xs font-semibold text-[var(--brand-strong)] hover:bg-[var(--surface-muted)]"
            >
              <Plus aria-hidden="true" size={15} />
              Add batch
            </button>
          </div>
          <div className="space-y-4">
            {lines.map((line, index) => (
              <div key={line.key} className="rounded-md border border-[var(--border)] p-3">
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-xs font-semibold text-[var(--text-muted)]">Batch {index + 1}</p>
                  {lines.length > 1 ? (
                    <button
                      type="button"
                      aria-label={`Remove batch ${index + 1}`}
                      onClick={() => removeLine(line.key)}
                      className="flex size-8 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)]"
                    >
                      <Trash2 aria-hidden="true" size={15} />
                    </button>
                  ) : null}
                </div>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
                  <Field label="Medicine" required className="sm:col-span-2">
                    <Select
                      value={line.medicineId}
                      onChange={(event) => {
                        const medicine = activeMedicines.find((m) => m.id === event.target.value);
                        updateLine(line.key, { medicineId: event.target.value, unitCost: medicine?.buyingPrice ?? line.unitCost });
                      }}
                    >
                      {activeMedicines.map((medicine) => (
                        <option key={medicine.id} value={medicine.id}>{medicine.brandName} · {medicine.sku}</option>
                      ))}
                    </Select>
                  </Field>
                  <Field label="Batch number" required>
                    <Input autoCapitalize="characters" value={line.batchNumber} onChange={(event) => updateLine(line.key, { batchNumber: event.target.value })} />
                  </Field>
                  <Field label="Expiry date" required>
                    <Input type="date" min={todayIsoDate()} value={line.expiryDate} onChange={(event) => updateLine(line.key, { expiryDate: event.target.value })} />
                  </Field>
                  <div className="grid grid-cols-2 gap-2">
                    <Field label="Qty" required>
                      <Input type="number" min={1} step={1} value={line.quantity} onChange={(event) => updateLine(line.key, { quantity: Math.floor(Number(event.target.value) || 0) })} />
                    </Field>
                    <Field label="Unit cost" required>
                      <Input inputMode="decimal" placeholder="0.00" value={line.unitCost} onChange={(event) => updateLine(line.key, { unitCost: event.target.value.replace(/[^\d.]/g, "") })} />
                    </Field>
                  </div>
                </div>
                {fieldErrors[line.key] ? (
                  <p className="mt-2 text-xs text-[var(--danger)]">{fieldErrors[line.key]}</p>
                ) : null}
              </div>
            ))}
          </div>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <Field label="Receiving notes">
              <Input value={remarks} onChange={(event) => setRemarks(event.target.value)} />
            </Field>
            <div className="flex items-end justify-end">
              <div className="text-right text-sm">
                <span className="text-[var(--text-muted)]">Estimated GRN total</span>
                <p className="text-lg font-semibold">{formatKes(estimatedTotal)}</p>
              </div>
            </div>
          </div>
        </section>
        <div className="flex justify-end gap-2">
          <SecondaryLink href="/inventory">Cancel</SecondaryLink>
          <PrimaryButton type="submit" disabled={submitting || lines.length === 0}>
            <PackagePlus aria-hidden="true" size={17} />
            {submitting ? "Receiving..." : `Receive ${lines.length} batch${lines.length === 1 ? "" : "es"}`}
          </PrimaryButton>
        </div>
      </form>
    </div>
  );
}
