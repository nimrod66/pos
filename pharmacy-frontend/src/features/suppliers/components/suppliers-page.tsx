"use client";

import {
  Archive,
  ArchiveRestore,
  Pencil,
  Plus,
  Save,
  Trash2,
  Truck,
  X,
} from "lucide-react";
import { useState } from "react";

import {
  PrimaryButton,
  PrimaryLink,
  SecondaryButton,
} from "@/components/ui/buttons";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { Field, FormError, Input } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { formatKes } from "@/features/workspace/lib/money";
import type { Supplier, SupplierInput } from "@/features/workspace/types";
import { formatDateTime } from "@/lib/format";

export function SuppliersPage() {
  const suppliers = useWorkspaceQuery((state) => state.suppliers);
  const batches = useWorkspaceQuery((state) => state.batches);
  const goodsReceipts = useWorkspaceQuery((state) => state.goodsReceipts);
  const canWriteSupplier = usePermission(PERMISSIONS.SUPPLIER_WRITE);
  const canReceiveStock = usePermission(PERMISSIONS.INVENTORY_RECEIVE);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [busySupplierId, setBusySupplierId] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Supplier | null>(null);

  function hasHistory(supplierId: string) {
    return (
      batches.some((batch) => batch.supplierId === supplierId) ||
      goodsReceipts.some((receipt) => receipt.supplierId === supplierId)
    );
  }

  function resetForm() {
    setEditingId(null);
    setName("");
    setPhone("");
    setEmail("");
    setError(null);
  }

  function openCreateForm() {
    resetForm();
    setShowForm(true);
  }

  function openEditForm(supplier: Supplier) {
    setEditingId(supplier.id);
    setName(supplier.name);
    setPhone(supplier.phone);
    setEmail(supplier.email);
    setError(null);
    setShowForm(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function closeForm() {
    setShowForm(false);
    resetForm();
  }

  async function handleSupplierSubmit(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();
    setError(null);
    if (!canWriteSupplier) {
      setError("Your active roles do not permit supplier changes.");
      return;
    }
    if (name.trim().length < 2 || phone.trim().length < 7) {
      setError("Enter a supplier name and a valid contact number.");
      return;
    }

    const input: SupplierInput = {
      name: name.trim(),
      phone: phone.trim(),
      email: email.trim(),
    };
    setSubmitting(true);
    try {
      if (editingId) {
        await workspaceGateway.updateSupplier(editingId, input);
      } else {
        await workspaceGateway.addSupplier(input);
      }
      closeForm();
    } catch (caught) {
      setError(
        getWorkspaceErrorMessage(
          caught,
          `The supplier could not be ${editingId ? "updated" : "added"}.`,
        ),
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatusChange(supplier: Supplier) {
    if (!canWriteSupplier || busySupplierId) return;
    setError(null);
    setBusySupplierId(supplier.id);
    try {
      await workspaceGateway.setSupplierStatus(
        supplier.id,
        supplier.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
      );
    } catch (caught) {
      setError(
        getWorkspaceErrorMessage(
          caught,
          "The supplier status could not be changed.",
        ),
      );
    } finally {
      setBusySupplierId(null);
    }
  }

  async function handleDelete() {
    if (!canWriteSupplier || !deleteTarget || busySupplierId) return;
    setError(null);
    setBusySupplierId(deleteTarget.id);
    try {
      await workspaceGateway.deleteSupplier(deleteTarget.id);
      setDeleteTarget(null);
    } catch (caught) {
      setDeleteTarget(null);
      setError(
        getWorkspaceErrorMessage(caught, "The supplier could not be deleted."),
      );
    } finally {
      setBusySupplierId(null);
    }
  }

  return (
    <div>
      <PageHeader
        title="Suppliers & goods received"
        description="Manage wholesale contacts and retain a receiving trail for every stock increase."
        actions={
          canWriteSupplier || canReceiveStock ? (
            <>
              {canWriteSupplier ? (
                <SecondaryButton
                  type="button"
                  onClick={showForm ? closeForm : openCreateForm}
                >
                  {showForm ? (
                    <X aria-hidden="true" size={17} />
                  ) : (
                    <Plus aria-hidden="true" size={17} />
                  )}
                  {showForm ? "Close" : "Add supplier"}
                </SecondaryButton>
              ) : null}
              {canReceiveStock ? (
                <PrimaryLink href="/procurement/grn/new">
                  <Truck aria-hidden="true" size={17} />
                  Receive stock
                </PrimaryLink>
              ) : null}
            </>
          ) : undefined
        }
      />

      {showForm && canWriteSupplier ? (
        <form
          onSubmit={handleSupplierSubmit}
          className="mb-6 rounded-md border border-[var(--border)] bg-white p-4 sm:p-6"
        >
          <h2 className="text-base font-semibold">
            {editingId ? "Edit supplier" : "New supplier"}
          </h2>
          <div className="mt-4 grid gap-4 md:grid-cols-3">
            <Field label="Supplier name" required>
              <Input
                autoFocus
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </Field>
            <Field label="Phone" required>
              <Input
                type="tel"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
              />
            </Field>
            <Field label="Email">
              <Input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </Field>
          </div>
          <div className="mt-4">
            <FormError message={error} />
          </div>
          <div className="mt-4 flex justify-end">
            <PrimaryButton type="submit" disabled={submitting}>
              {editingId ? (
                <Save aria-hidden="true" size={17} />
              ) : (
                <Plus aria-hidden="true" size={17} />
              )}
              {submitting
                ? "Saving..."
                : editingId
                  ? "Save supplier"
                  : "Add supplier"}
            </PrimaryButton>
          </div>
        </form>
      ) : null}

      {!showForm && error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.05fr)_minmax(0,1fr)]">
        <section className="rounded-md border border-[var(--border)] bg-white">
          <div className="border-b border-[var(--border)] px-4 py-3.5">
            <h2 className="text-sm font-semibold">Supplier directory</h2>
          </div>
          {suppliers.length ? (
            <div className="divide-y divide-[var(--border)]">
              {suppliers.map((supplier) => (
                <div
                  key={supplier.id}
                  className="grid grid-cols-[minmax(0,1fr)_auto] gap-4 p-4"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold">
                      {supplier.name}
                    </p>
                    <p className="mt-1 text-xs text-[var(--text-muted)]">
                      {supplier.phone}
                    </p>
                    <p className="mt-0.5 truncate text-xs text-[var(--text-muted)]">
                      {supplier.email || "No email recorded"}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    <StatusBadge
                      tone={supplier.status === "ACTIVE" ? "success" : "neutral"}
                    >
                      {supplier.status === "ACTIVE" ? "Active" : "Inactive"}
                    </StatusBadge>
                    {canWriteSupplier ? (
                      <div className="flex gap-1">
                        <button
                          type="button"
                          title={`Edit ${supplier.name}`}
                          aria-label={`Edit ${supplier.name}`}
                          onClick={() => openEditForm(supplier)}
                          className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
                        >
                          <Pencil aria-hidden="true" size={16} />
                        </button>
                        <button
                          type="button"
                          title={
                            supplier.status === "ACTIVE"
                              ? `Archive ${supplier.name}`
                              : `Reactivate ${supplier.name}`
                          }
                          aria-label={
                            supplier.status === "ACTIVE"
                              ? `Archive ${supplier.name}`
                              : `Reactivate ${supplier.name}`
                          }
                          disabled={busySupplierId === supplier.id}
                          onClick={() => void handleStatusChange(supplier)}
                          className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)] disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          {supplier.status === "ACTIVE" ? (
                            <Archive aria-hidden="true" size={17} />
                          ) : (
                            <ArchiveRestore aria-hidden="true" size={17} />
                          )}
                        </button>
                        <button
                          type="button"
                          title={
                            hasHistory(supplier.id)
                              ? "Suppliers with receiving history cannot be deleted"
                              : `Delete ${supplier.name}`
                          }
                          aria-label={`Delete ${supplier.name}`}
                          disabled={
                            hasHistory(supplier.id) ||
                            busySupplierId === supplier.id
                          }
                          onClick={() => setDeleteTarget(supplier)}
                          className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)] disabled:cursor-not-allowed disabled:opacity-30"
                        >
                          <Trash2 aria-hidden="true" size={17} />
                        </button>
                      </div>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={Truck}
              title="No suppliers yet"
              description="Add the first wholesale supplier."
            />
          )}
        </section>

        <section className="rounded-md border border-[var(--border)] bg-white">
          <div className="border-b border-[var(--border)] px-4 py-3.5">
            <h2 className="text-sm font-semibold">Goods received notes</h2>
          </div>
          {goodsReceipts.length ? (
            <div className="divide-y divide-[var(--border)]">
              {goodsReceipts.map((receipt) => {
                const supplier = suppliers.find(
                  (item) => item.id === receipt.supplierId,
                );
                return (
                  <div
                    key={receipt.id}
                    className="grid grid-cols-[1fr_auto] gap-x-4 gap-y-1 p-4 text-sm"
                  >
                    <p className="font-semibold">{receipt.number}</p>
                    <p className="text-right font-semibold">
                      {formatKes(receipt.totalCost)}
                    </p>
                    <p className="truncate text-xs text-[var(--text-muted)]">
                      {supplier?.name ?? "Unknown supplier"}
                    </p>
                    <p className="text-right text-xs text-[var(--text-muted)]">
                      {receipt.itemCount} item
                    </p>
                    <p className="col-span-2 mt-1 text-xs text-[var(--text-subtle)]">
                      {formatDateTime(receipt.receivedAt)}
                    </p>
                  </div>
                );
              })}
            </div>
          ) : (
            <EmptyState
              icon={Truck}
              title="No stock received"
              description="Create the first GRN to add a batch."
            />
          )}
        </section>
      </div>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        busy={Boolean(deleteTarget && busySupplierId === deleteTarget.id)}
        title="Delete unused supplier?"
        description={`Permanently delete ${deleteTarget?.name ?? "this supplier"}. This is only allowed before a GRN or stock batch references it.`}
        confirmLabel="Delete supplier"
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
