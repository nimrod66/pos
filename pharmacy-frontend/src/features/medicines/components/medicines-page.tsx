"use client";

import {
  Archive,
  ArchiveRestore,
  PackageSearch,
  Pencil,
  Plus,
  Search,
  Trash2,
} from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";

import { PrimaryLink } from "@/components/ui/buttons";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { formatKes } from "@/features/workspace/lib/money";
import { stockForMedicine } from "@/features/workspace/lib/workspace-helpers";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import type { Medicine } from "@/features/workspace/types";

export function MedicinesPage() {
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const batches = useWorkspaceQuery((state) => state.batches);
  const movements = useWorkspaceQuery((state) => state.movements);
  const sales = useWorkspaceQuery((state) => state.sales);
  const categories = useWorkspaceQuery((state) => state.categories);
  const units = useWorkspaceQuery((state) => state.units);
  const canWrite = usePermission(PERMISSIONS.MEDICINE_WRITE);
  const canSetPrice = usePermission(PERMISSIONS.MEDICINE_PRICE_WRITE);
  const [query, setQuery] = useState("");
  const [categoryId, setCategoryId] = useState("ALL");
  const [status, setStatus] = useState("ACTIVE");
  const [busyMedicineId, setBusyMedicineId] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Medicine | null>(null);
  const [error, setError] = useState<string | null>(null);

  function hasHistory(medicineId: string) {
    return (
      batches.some((batch) => batch.medicineId === medicineId) ||
      movements.some((movement) => movement.medicineId === medicineId) ||
      sales.some((sale) =>
        sale.items.some((item) => item.medicineId === medicineId),
      )
    );
  }

  async function handleStatusChange(medicine: Medicine) {
    if (!canWrite || busyMedicineId) return;
    setError(null);
    setBusyMedicineId(medicine.id);
    try {
      await workspaceGateway.setMedicineStatus(
        medicine.id,
        medicine.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
      );
    } catch (caught) {
      setError(
        getWorkspaceErrorMessage(
          caught,
          "The medicine status could not be changed.",
        ),
      );
    } finally {
      setBusyMedicineId(null);
    }
  }

  async function handleDelete() {
    if (!canWrite || !deleteTarget || busyMedicineId) return;
    setError(null);
    setBusyMedicineId(deleteTarget.id);
    try {
      await workspaceGateway.deleteMedicine(deleteTarget.id);
      setDeleteTarget(null);
    } catch (caught) {
      setDeleteTarget(null);
      setError(
        getWorkspaceErrorMessage(caught, "The medicine could not be deleted."),
      );
    } finally {
      setBusyMedicineId(null);
    }
  }

  const visibleMedicines = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return medicines.filter((medicine) => {
      const matchesQuery =
        !normalized ||
        [
          medicine.brandName,
          medicine.genericName,
          medicine.sku,
          medicine.barcode,
          medicine.manufacturer,
        ].some((value) => value.toLowerCase().includes(normalized));
      return (
        matchesQuery &&
        (categoryId === "ALL" || medicine.categoryId === categoryId) &&
        (status === "ALL" || medicine.status === status)
      );
    });
  }, [categoryId, medicines, query, status]);

  return (
    <div>
      <PageHeader
        title="Medicines"
        description={`${medicines.length} catalogue records with pricing and dispensing controls.`}
        actions={canWrite && canSetPrice ? (
          <PrimaryLink href="/medicines/new">
            <Plus aria-hidden="true" size={17} />
            Add medicine
          </PrimaryLink>
        ) : undefined}
      />

      {error ? <div className="mb-4"><FormError message={error} /></div> : null}

      <section className="rounded-md border border-[var(--border)] bg-white">
        <div className="grid gap-3 border-b border-[var(--border)] p-4 md:grid-cols-[minmax(240px,1fr)_220px_170px]">
          <label className="relative block">
            <span className="sr-only">Search medicines</span>
            <Search
              aria-hidden="true"
              className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]"
              size={17}
            />
            <Input
              className="pl-9"
              placeholder="Search name, SKU or barcode"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
          <label>
            <span className="sr-only">Filter by category</span>
            <Select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              <option value="ALL">All categories</option>
              {categories.map((category) => (
                <option value={category.id} key={category.id}>
                  {category.name}
                </option>
              ))}
            </Select>
          </label>
          <label>
            <span className="sr-only">Filter by status</span>
            <Select value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="ALL">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </Select>
          </label>
        </div>

        {visibleMedicines.length === 0 ? (
          <EmptyState
            icon={PackageSearch}
            title="No medicines found"
            description="Adjust the search or filters to find a catalogue record."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1040px] text-left text-sm">
              <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Medicine</th>
                  <th className="px-4 py-3 font-semibold">SKU / barcode</th>
                  <th className="px-4 py-3 font-semibold">Category</th>
                  <th className="px-4 py-3 text-right font-semibold">Usable stock</th>
                  <th className="px-4 py-3 text-right font-semibold">Selling price</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  {canWrite ? (
                    <th className="px-4 py-3 text-right font-semibold">Actions</th>
                  ) : null}
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {visibleMedicines.map((medicine) => {
                  const stock = stockForMedicine(batches, medicine.id);
                  const category = categories.find((item) => item.id === medicine.categoryId);
                  const unit = units.find((item) => item.id === medicine.unitId);
                  const low = stock <= medicine.reorderLevel;
                  return (
                    <tr key={medicine.id} className="hover:bg-[var(--surface-muted)]/60">
                      <td className="px-4 py-3.5">
                        {canWrite ? (
                          <Link
                            href={`/medicines/${medicine.id}`}
                            className="font-semibold text-[var(--brand-strong)] hover:underline"
                          >
                            {medicine.brandName}
                          </Link>
                        ) : (
                          <p className="font-semibold">{medicine.brandName}</p>
                        )}
                        <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                          {medicine.genericName} · {medicine.manufacturer}
                        </p>
                      </td>
                      <td className="px-4 py-3.5">
                        <span className="font-medium">{medicine.sku}</span>
                        <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                          {medicine.barcode || "No barcode"}
                        </p>
                      </td>
                      <td className="px-4 py-3.5 text-[var(--text-muted)]">
                        {category?.name ?? "Uncategorised"}
                      </td>
                      <td className="px-4 py-3.5 text-right">
                        <span className={low ? "font-semibold text-[var(--danger)]" : "font-semibold"}>
                          {stock}
                        </span>{" "}
                        <span className="text-xs text-[var(--text-muted)]">{unit?.symbol}</span>
                      </td>
                      <td className="px-4 py-3.5 text-right font-medium">
                        {formatKes(medicine.sellingPrice)}
                      </td>
                      <td className="px-4 py-3.5">
                        <div className="flex items-center gap-2">
                          <StatusBadge tone={medicine.status === "ACTIVE" ? "success" : "neutral"}>
                            {medicine.status === "ACTIVE" ? "Active" : "Inactive"}
                          </StatusBadge>
                          {medicine.prescriptionRequired ? (
                            <StatusBadge tone="info">Rx</StatusBadge>
                          ) : null}
                        </div>
                      </td>
                      {canWrite ? (
                        <td className="px-4 py-3.5">
                          <div className="flex justify-end gap-1">
                            <Link
                              href={`/medicines/${medicine.id}`}
                              title={`Edit ${medicine.brandName}`}
                              aria-label={`Edit ${medicine.brandName}`}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
                            >
                              <Pencil aria-hidden="true" size={16} />
                            </Link>
                            <button
                              type="button"
                              title={
                                medicine.status === "ACTIVE"
                                  ? `Archive ${medicine.brandName}`
                                  : `Reactivate ${medicine.brandName}`
                              }
                              aria-label={
                                medicine.status === "ACTIVE"
                                  ? `Archive ${medicine.brandName}`
                                  : `Reactivate ${medicine.brandName}`
                              }
                              disabled={busyMedicineId === medicine.id}
                              onClick={() => void handleStatusChange(medicine)}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)] disabled:cursor-not-allowed disabled:opacity-40"
                            >
                              {medicine.status === "ACTIVE" ? (
                                <Archive aria-hidden="true" size={17} />
                              ) : (
                                <ArchiveRestore aria-hidden="true" size={17} />
                              )}
                            </button>
                            <button
                              type="button"
                              title={
                                hasHistory(medicine.id)
                                  ? "Medicines with stock or sales history cannot be deleted"
                                  : `Delete ${medicine.brandName}`
                              }
                              aria-label={`Delete ${medicine.brandName}`}
                              disabled={
                                hasHistory(medicine.id) ||
                                busyMedicineId === medicine.id
                              }
                              onClick={() => setDeleteTarget(medicine)}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)] disabled:cursor-not-allowed disabled:opacity-30"
                            >
                              <Trash2 aria-hidden="true" size={17} />
                            </button>
                          </div>
                        </td>
                      ) : null}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        <div className="border-t border-[var(--border)] px-4 py-3 text-xs text-[var(--text-muted)]">
          Showing {visibleMedicines.length} of {medicines.length} medicines
        </div>
      </section>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        busy={Boolean(deleteTarget && busyMedicineId === deleteTarget.id)}
        title="Delete unused medicine?"
        description={`Permanently delete ${deleteTarget?.brandName ?? "this medicine"}. This is only allowed before it has stock or sales history.`}
        confirmLabel="Delete medicine"
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
