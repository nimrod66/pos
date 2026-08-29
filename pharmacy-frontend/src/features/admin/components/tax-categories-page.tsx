"use client";

import {
  Pencil,
  Plus,
  Trash2,
  X,
  Receipt,
  Percent,
  ToggleLeft,
  ToggleRight,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { apiRequest, ApiClientError } from "@/lib/api-client";

interface TaxCategory {
  id: string;
  name: string;
  code: string;
  rate: number;
  type: string;
  description: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

export function TaxCategoriesPage() {
  const canWrite = usePermission(PERMISSIONS.SETTINGS_MANAGE);
  const canRead = usePermission(PERMISSIONS.SETTINGS_MANAGE);
  const [categories, setCategories] = useState<TaxCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [rate, setRate] = useState("");
  const [type, setType] = useState("EXCLUSIVE");
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<TaxCategory | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiRequest<TaxCategory[]>("/tax-categories");
      setCategories(res.data);
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Tax categories could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    void apiRequest<TaxCategory[]>("/tax-categories")
      .then((res) => {
        if (active) {
          setCategories(res.data);
          setError(null);
        }
      })
      .catch((caught) => {
        if (active) setError(errorMessage(caught, "Tax categories could not be loaded."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canRead]);

  function resetForm() {
    setName("");
    setCode("");
    setRate("");
    setType("EXCLUSIVE");
    setDescription("");
    setEditingId(null);
    setError(null);
  }

  function openCreateForm() {
    resetForm();
    setShowForm(true);
  }

  function openEditForm(cat: TaxCategory) {
    setName(cat.name);
    setCode(cat.code);
    setRate(String(cat.rate));
    setType(cat.type);
    setDescription(cat.description);
    setEditingId(cat.id);
    setError(null);
    setShowForm(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function closeForm() {
    setShowForm(false);
    resetForm();
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!canWrite) {
      setError("Your active roles do not permit tax category changes.");
      return;
    }
    if (name.trim().length < 1 || code.trim().length < 1) {
      setError("Enter a name and code for the tax category.");
      return;
    }
    const parsedRate = Number.parseFloat(rate);
    if (Number.isNaN(parsedRate) || parsedRate < 0) {
      setError("Enter a valid tax rate (0 or greater).");
      return;
    }

    setSubmitting(true);
    try {
      const body = {
        name: name.trim(),
        code: code.trim().toUpperCase(),
        rate: parsedRate,
        type,
        description: description.trim(),
      };
      if (editingId) {
        await apiRequest(`/tax-categories/${editingId}`, {
          method: "PUT",
          body,
        });
      } else {
        await apiRequest("/tax-categories", {
          method: "POST",
          body,
        });
      }
      closeForm();
      await load();
    } catch (caught) {
      setError(
        errorMessage(
          caught,
          `The tax category could not be ${editingId ? "updated" : "created"}.`,
        ),
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggle(cat: TaxCategory) {
    if (!canWrite || togglingId) return;
    setTogglingId(cat.id);
    try {
      await apiRequest(`/tax-categories/${cat.id}/toggle`, { method: "PATCH" });
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "The tax category status could not be toggled."));
    } finally {
      setTogglingId(null);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await apiRequest(`/tax-categories/${deleteTarget.id}`, { method: "DELETE" });
      setDeleteTarget(null);
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "The tax category could not be deleted."));
    } finally {
      setDeleting(false);
    }
  }

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      <PageHeader
        title="Tax categories"
        description="Manage tax categories and rates applied to medicine sales."
        actions={
          canWrite ? (
            <SecondaryButton
              type="button"
              onClick={showForm ? closeForm : openCreateForm}
            >
              {showForm ? (
                <X aria-hidden="true" size={17} />
              ) : (
                <Plus aria-hidden="true" size={17} />
              )}
              {showForm ? "Close" : "Add tax category"}
            </SecondaryButton>
          ) : undefined
        }
      />

      {showForm ? (
        <form
          onSubmit={handleSubmit}
          className="mb-6 rounded-md border border-[var(--border)] bg-white p-4 sm:p-6"
        >
          <div className="flex items-center gap-2">
            <Percent
              aria-hidden="true"
              className="text-[var(--brand)]"
              size={18}
            />
            <h2 className="text-base font-semibold">
              {editingId ? "Edit tax category" : "New tax category"}
            </h2>
          </div>

          <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Field label="Name" required>
              <Input
                autoFocus
                placeholder="e.g. VAT"
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </Field>
            <Field label="Code" required>
              <Input
                placeholder="e.g. VAT16"
                value={code}
                onChange={(event) => setCode(event.target.value)}
              />
            </Field>
            <Field label="Rate (%)" required>
              <Input
                inputMode="decimal"
                min="0"
                placeholder="e.g. 16"
                step="0.01"
                type="number"
                value={rate}
                onChange={(event) => setRate(event.target.value)}
              />
            </Field>
            <Field label="Type" required>
              <Select value={type} onChange={(event) => setType(event.target.value)}>
                <option value="EXCLUSIVE">Exclusive</option>
                <option value="INCLUSIVE">Inclusive</option>
              </Select>
            </Field>
          </div>

          <div className="mt-4 grid gap-4 md:grid-cols-1">
            <Field label="Description">
              <Input
                placeholder="Brief description of this tax category"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </Field>
          </div>

          <div className="mt-4">
            <FormError message={error} />
          </div>
          <div className="mt-4 flex justify-end">
            <PrimaryButton type="submit" disabled={submitting}>
              {editingId ? (
                <Pencil aria-hidden="true" size={17} />
              ) : (
                <Plus aria-hidden="true" size={17} />
              )}
              {submitting
                ? "Saving..."
                : editingId
                  ? "Save changes"
                  : "Add tax category"}
            </PrimaryButton>
          </div>
        </form>
      ) : null}

      {!showForm ? <FormError message={error} /> : null}

      <section
        className={`${!showForm && error ? "mt-4 " : ""}rounded-md border border-[var(--border)] bg-white`}
      >
        {categories.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Name</th>
                  <th className="px-4 py-3 font-semibold">Code</th>
                  <th className="px-4 py-3 font-semibold">Rate</th>
                  <th className="px-4 py-3 font-semibold">Type</th>
                  <th className="px-4 py-3 font-semibold">Description</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {categories.map((cat) => (
                  <tr key={cat.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3.5 font-semibold">{cat.name}</td>
                    <td className="px-4 py-3.5 font-mono text-xs text-[var(--text-muted)]">
                      {cat.code}
                    </td>
                    <td className="px-4 py-3.5 font-mono text-sm">{cat.rate}%</td>
                    <td className="px-4 py-3.5">
                      <StatusBadge tone={cat.type === "INCLUSIVE" ? "info" : "neutral"}>
                        {cat.type === "INCLUSIVE" ? "Inclusive" : "Exclusive"}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3.5 text-[var(--text-muted)]">
                      {cat.description || "-"}
                    </td>
                    <td className="px-4 py-3.5">
                      <StatusBadge tone={cat.active ? "success" : "neutral"}>
                        {cat.active ? "Active" : "Inactive"}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3.5">
                      {canWrite ? (
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            title={cat.active ? `Deactivate ${cat.name}` : `Activate ${cat.name}`}
                            aria-label={cat.active ? `Deactivate ${cat.name}` : `Activate ${cat.name}`}
                            disabled={togglingId === cat.id}
                            onClick={() => void handleToggle(cat)}
                            className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)] disabled:cursor-not-allowed disabled:opacity-35"
                          >
                            {cat.active ? (
                              <ToggleRight aria-hidden="true" size={18} className="text-[var(--success)]" />
                            ) : (
                              <ToggleLeft aria-hidden="true" size={18} />
                            )}
                          </button>
                          <button
                            type="button"
                            title={`Edit ${cat.name}`}
                            aria-label={`Edit ${cat.name}`}
                            onClick={() => openEditForm(cat)}
                            className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
                          >
                            <Pencil aria-hidden="true" size={16} />
                          </button>
                          <button
                            type="button"
                            title={`Delete ${cat.name}`}
                            aria-label={`Delete ${cat.name}`}
                            onClick={() => setDeleteTarget(cat)}
                            className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--danger-soft)] hover:text-[var(--danger)]"
                          >
                            <Trash2 aria-hidden="true" size={16} />
                          </button>
                        </div>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading tax categories...
          </div>
        ) : (
          <EmptyState
            icon={Receipt}
            title="No tax categories"
            description="Tax categories will appear here once created."
          />
        )}
      </section>

      <ConfirmDialog
        busy={deleting}
        busyLabel="Deleting..."
        confirmLabel="Delete tax category"
        description={`This will permanently remove "${deleteTarget?.name}" (${deleteTarget?.code}). This action cannot be undone.`}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
        open={Boolean(deleteTarget)}
        title="Delete tax category"
      />
    </div>
  );
}
