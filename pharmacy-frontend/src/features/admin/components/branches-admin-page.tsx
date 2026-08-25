"use client";

import { Building2, Pencil, Plus, RefreshCw, Trash2 } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  PrimaryButton,
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
import { ApiClientError, apiRequest } from "@/lib/api-client";

interface BranchRow {
  id: string;
  branchName: string;
  branchCode: string;
  phoneNumber: string;
  email: string | null;
  location: string;
  status: string | null;
  pharmacyId: string;
  pharmacyName?: string | null;
}

const emptyForm = {
  branchName: "",
  branchCode: "",
  phoneNumber: "",
  email: "",
  location: "",
  status: "ACTIVE",
};

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

export function BranchesAdminPage() {
  const session = useAuthStore((state) => state.session);
  const canManage = usePermission(PERMISSIONS.SETTINGS_MANAGE);
  const [branches, setBranches] = useState<BranchRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState({ ...emptyForm });
  const [deleteTarget, setDeleteTarget] = useState<BranchRow | null>(null);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try {
      const response = await apiRequest<BranchRow[]>(
        `/branches?pharmacyId=${session.user.pharmacyId}`,
        { cache: "no-store" },
      );
      setBranches(response.data);
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Branches could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    if (!canManage) return;
    const initial = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(initial);
  }, [canManage, load]);

  if (!canManage) {
    return (
      <AccessRestricted homePath="/dashboard" />
    );
  }

  function openCreate() {
    setEditingId(null);
    setForm({ ...emptyForm });
    setFormError(null);
    setFormOpen(true);
  }

  function openEdit(branch: BranchRow) {
    setEditingId(branch.id);
    setForm({
      branchName: branch.branchName,
      branchCode: branch.branchCode,
      phoneNumber: branch.phoneNumber,
      email: branch.email ?? "",
      location: branch.location,
      status: branch.status ?? "ACTIVE",
    });
    setFormError(null);
    setFormOpen(true);
  }

  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) return;
    setSaving(true);
    setFormError(null);
    try {
      const body = { ...form, email: form.email || null, pharmacyId: session.user.pharmacyId };
      if (editingId) {
        await apiRequest(`/branches/${editingId}`, { method: "PUT", body });
        setNotice(`${form.branchName} was updated.`);
      } else {
        await apiRequest("/branches", { method: "POST", body });
        setNotice(`${form.branchName} was created. Assign staff and terminals to it next.`);
      }
      setFormOpen(false);
      await load();
    } catch (caught) {
      setFormError(errorMessage(caught, "The branch could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!deleteTarget) return;
    setSaving(true);
    try {
      await apiRequest(`/branches/${deleteTarget.id}`, { method: "DELETE" });
      setNotice(`${deleteTarget.branchName} was deleted.`);
      setDeleteTarget(null);
      await load();
    } catch (caught) {
      setDeleteTarget(null);
      setError(
        errorMessage(
          caught,
          "The branch could not be deleted - move its data first or deactivate it instead.",
        ),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Branches"
        description="Open new branches, update their contact details, and control which ones are active."
        actions={
          <>
            <SecondaryButton
              type="button"
              title="Refresh branches"
              aria-label="Refresh branches"
              className="px-3"
              disabled={loading}
              onClick={() => void load()}
            >
              <RefreshCw aria-hidden="true" size={17} />
            </SecondaryButton>
            <PrimaryButton type="button" onClick={openCreate}>
              <Plus aria-hidden="true" size={17} /> New branch
            </PrimaryButton>
          </>
        }
      />

      {error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}
      {notice ? (
        <div
          role="status"
          className="mb-4 rounded-md border border-[var(--success)]/30 bg-[var(--success-soft)] px-4 py-3 text-sm text-[var(--success)]"
        >
          {notice}
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {branches.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Branch</th>
                  <th className="px-4 py-3 font-semibold">Contact</th>
                  <th className="px-4 py-3 font-semibold">Location</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {branches.map((branch) => (
                  <tr key={branch.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <span className="flex size-9 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand-strong)]">
                          <Building2 aria-hidden="true" size={17} />
                        </span>
                        <div>
                          <p className="font-semibold">{branch.branchName}</p>
                          <p className="mt-0.5 font-mono text-xs text-[var(--text-muted)]">
                            {branch.branchCode}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <p>{branch.phoneNumber}</p>
                      {branch.email ? (
                        <p className="mt-0.5 text-xs text-[var(--text-muted)]">{branch.email}</p>
                      ) : null}
                    </td>
                    <td className="px-4 py-3">{branch.location}</td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={branch.status === "ACTIVE" ? "success" : "neutral"}>
                        {branch.status ?? "UNKNOWN"}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1">
                        <button
                          type="button"
                          title={`Edit ${branch.branchName}`}
                          aria-label={`Edit ${branch.branchName}`}
                          onClick={() => openEdit(branch)}
                          className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-white hover:text-[var(--text)]"
                        >
                          <Pencil aria-hidden="true" size={16} />
                        </button>
                        {branch.id !== session?.user.activeBranch.id ? (
                          <button
                            type="button"
                            title={`Delete ${branch.branchName}`}
                            aria-label={`Delete ${branch.branchName}`}
                            disabled={saving}
                            onClick={() => setDeleteTarget(branch)}
                            className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)] disabled:opacity-40"
                          >
                            <Trash2 aria-hidden="true" size={16} />
                          </button>
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
            Loading branches...
          </div>
        ) : (
          <EmptyState
            icon={Building2}
            title="No branches yet"
            description="Create your first branch - each branch has its own stock, staff, and registers."
            action={
              <PrimaryButton type="button" onClick={openCreate}>
                <Plus aria-hidden="true" size={17} /> New branch
              </PrimaryButton>
            }
          />
        )}
      </section>

      {formOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-label={editingId ? "Edit branch" : "New branch"}
            className="w-full max-w-lg rounded-md border border-[var(--border)] bg-white shadow-xl"
          >
            <div className="flex items-center justify-between border-b border-[var(--border)] px-5 py-4">
              <h2 className="text-base font-semibold">
                {editingId ? "Edit branch" : "New branch"}
              </h2>
              <button
                type="button"
                title="Close"
                aria-label="Close branch form"
                onClick={() => setFormOpen(false)}
                className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]"
              >
                ✕
              </button>
            </div>
            <form onSubmit={save}>
              <div className="grid gap-4 p-5 sm:grid-cols-2">
                <Field label="Branch name" required>
                  <Input
                    autoFocus
                    placeholder="e.g. Westlands branch"
                    value={form.branchName}
                    onChange={(event) =>
                      setForm((current) => ({ ...current, branchName: event.target.value }))
                    }
                  />
                </Field>
                <Field label="Branch code" required>
                  <Input
                    placeholder="e.g. WLD"
                    maxLength={20}
                    value={form.branchCode}
                    onChange={(event) =>
                      setForm((current) => ({ ...current, branchCode: event.target.value.toUpperCase() }))
                    }
                  />
                </Field>
                <Field label="Phone number" required>
                  <Input
                    placeholder="+254700000000"
                    value={form.phoneNumber}
                    onChange={(event) =>
                      setForm((current) => ({ ...current, phoneNumber: event.target.value }))
                    }
                  />
                </Field>
                <Field label="Email">
                  <Input
                    type="email"
                    placeholder="branch@pharmacy.co.ke"
                    value={form.email}
                    onChange={(event) =>
                      setForm((current) => ({ ...current, email: event.target.value }))
                    }
                  />
                </Field>
                <Field label="Location" required>
                  <Input
                    placeholder="e.g. Westlands, Nairobi"
                    value={form.location}
                    onChange={(event) =>
                      setForm((current) => ({ ...current, location: event.target.value }))
                    }
                  />
                </Field>
                <Field label="Status">
                  <Select
                    value={form.status}
                    onChange={(event) =>
                      setForm((current) => ({ ...current, status: event.target.value }))
                    }
                  >
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                  </Select>
                </Field>
              </div>
              <div className="px-5 pb-4">
                <FormError message={formError} />
              </div>
              <div className="flex justify-end gap-2 border-t border-[var(--border)] px-5 py-4">
                <SecondaryButton type="button" onClick={() => setFormOpen(false)}>
                  Cancel
                </SecondaryButton>
                <PrimaryButton type="submit" disabled={saving}>
                  {saving ? "Saving..." : editingId ? "Save changes" : "Create branch"}
                </PrimaryButton>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        busy={Boolean(deleteTarget && saving)}
        busyLabel="Deleting..."
        title={`Delete ${deleteTarget?.branchName ?? "branch"}?`}
        description="This permanently removes the branch record. Branches with existing stock, sales, or staff cannot be deleted - deactivate them instead."
        confirmLabel="Delete branch"
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => void remove()}
      />
    </div>
  );
}
