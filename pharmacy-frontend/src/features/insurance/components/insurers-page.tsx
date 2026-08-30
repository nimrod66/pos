"use client";

import { useEffect, useState } from "react";
import { Shield, Plus, Pencil, Trash2, X } from "lucide-react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { insuranceGateway } from "@/features/insurance/insurance-gateway";
import type { Insurer, CreateInsurerInput } from "@/features/insurance/types";

const TYPES = ["GOVERNMENT", "PRIVATE", "CORPORATE", "SELF_PAY"];

export function InsurersPage({ showHeader = true }: { showHeader?: boolean }) {
  const canRead = usePermission(PERMISSIONS.INSURANCE_READ);
  const canWrite = usePermission(PERMISSIONS.INSURANCE_WRITE);

  const [insurers, setInsurers] = useState<Insurer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Insurer | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState<CreateInsurerInput>({
    name: "",
    code: "",
    insurerType: "PRIVATE",
  });

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    async function run() {
      setLoading(true);
      try {
        const data = await insuranceGateway.listInsurers();
        if (!active) return;
        setInsurers(data);
        setError(null);
      } catch {
        if (!active) return;
        setError("Failed to load insurers.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void run();
    return () => {
      active = false;
    };
  }, [canRead]);

  function resetForm() {
    setForm({ name: "", code: "", insurerType: "PRIVATE" });
    setEditing(null);
    setShowForm(false);
  }

  function startEdit(insurer: Insurer) {
    setEditing(insurer);
    setForm({
      name: insurer.name,
      code: insurer.code,
      insurerType: insurer.insurerType,
      contactPerson: insurer.contactPerson ?? undefined,
      phoneNumber: insurer.phoneNumber ?? undefined,
      email: insurer.email ?? undefined,
      requiresPreauth: insurer.requiresPreauth,
    });
    setShowForm(true);
  }

  async function handleSubmit() {
    if (!form.name.trim() || !form.code.trim()) {
      setError("Name and code are required.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (editing) {
        await insuranceGateway.updateInsurer(editing.id, form);
      } else {
        await insuranceGateway.createInsurer(form);
      }
      resetForm();
      const data = await insuranceGateway.listInsurers();
      setInsurers(data);
    } catch {
      setError("Failed to save insurer.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!deleteId) return;
    try {
      await insuranceGateway.deleteInsurer(deleteId);
      setDeleteId(null);
      setInsurers((prev) => prev.filter((i) => i.id !== deleteId));
    } catch {
      setError("Failed to delete insurer.");
    }
  }

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      {showHeader ? (
        <PageHeader
          title="Insurance providers"
          description="Manage insurers, schemes, and patient enrollment."
          actions={
            canWrite ? (
              <PrimaryButton type="button" onClick={() => { resetForm(); setShowForm(true); }}>
                <Plus aria-hidden="true" size={16} /> Add insurer
              </PrimaryButton>
            ) : undefined
          }
        />
      ) : null}

      {error ? <div className="mb-4"><FormError message={error} /></div> : null}

      {showForm ? (
        <div className="mb-5 rounded-md border border-[var(--brand)] bg-white p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold">{editing ? "Edit insurer" : "New insurer"}</h3>
            <button type="button" onClick={resetForm} className="text-[var(--text-muted)] hover:text-[var(--danger)]"><X size={18} /></button>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Name *</span>
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="e.g. NHIF" />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Code *</span>
              <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="e.g. NHIF001" />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Type *</span>
              <Select value={form.insurerType} onChange={(e) => setForm({ ...form, insurerType: e.target.value })}>
                {TYPES.map((t) => <option key={t} value={t}>{t.replace("_", " ")}</option>)}
              </Select>
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Contact person</span>
              <Input value={form.contactPerson ?? ""} onChange={(e) => setForm({ ...form, contactPerson: e.target.value })} />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Phone</span>
              <Input value={form.phoneNumber ?? ""} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Email</span>
              <Input value={form.email ?? ""} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>
          </div>
          <div className="mt-3 flex gap-2">
            <PrimaryButton type="button" onClick={() => void handleSubmit()} disabled={submitting}>
              {submitting ? "Saving..." : editing ? "Update" : "Create"}
            </PrimaryButton>
            <SecondaryButton type="button" onClick={resetForm}>Cancel</SecondaryButton>
          </div>
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">Loading insurers...</div>
        ) : insurers.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[800px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Name</th>
                  <th className="px-4 py-3 font-semibold">Code</th>
                  <th className="px-4 py-3 font-semibold">Type</th>
                  <th className="px-4 py-3 font-semibold">Contact</th>
                  <th className="px-4 py-3 font-semibold">Co-pay</th>
                  <th className="px-4 py-3 font-semibold">Preauth</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  {canWrite ? <th className="px-4 py-3 font-semibold">Actions</th> : null}
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {insurers.map((insurer) => (
                  <tr key={insurer.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3 font-medium">{insurer.name}</td>
                    <td className="px-4 py-3 font-mono text-xs text-[var(--text-muted)]">{insurer.code}</td>
                    <td className="px-4 py-3">{insurer.insurerType?.replace("_", " ")}</td>
                    <td className="px-4 py-3 text-[var(--text-muted)]">{insurer.contactPerson || insurer.phoneNumber || "-"}</td>
                    <td className="px-4 py-3">
                      {insurer.defaultCoPayPercentage ? `${insurer.defaultCoPayPercentage}%` : insurer.defaultCoPayFlat ? `KES ${insurer.defaultCoPayFlat}` : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={insurer.requiresPreauth ? "warning" : "neutral"}>
                        {insurer.requiresPreauth ? "Required" : "No"}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={insurer.status === "ACTIVE" ? "success" : "neutral"}>
                        {insurer.status ?? "ACTIVE"}
                      </StatusBadge>
                    </td>
                    {canWrite ? (
                      <td className="px-4 py-3">
                        <div className="flex gap-1">
                          <button type="button" onClick={() => startEdit(insurer)} className="rounded p-1 text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--brand)]" title="Edit"><Pencil size={15} /></button>
                          <button type="button" onClick={() => setDeleteId(insurer.id)} className="rounded p-1 text-[var(--text-muted)] hover:bg-[var(--danger-soft)] hover:text-[var(--danger)]" title="Delete"><Trash2 size={15} /></button>
                        </div>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState icon={Shield} title="No insurers configured" description="Add your first insurance provider to start processing claims." />
        )}
      </section>

      {deleteId ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-sm rounded-md bg-white p-5 shadow-lg">
            <h3 className="text-sm font-semibold">Delete insurer?</h3>
            <p className="mt-2 text-sm text-[var(--text-muted)]">This may affect linked claims and members.</p>
            <div className="mt-4 flex gap-2">
              <PrimaryButton type="button" onClick={() => void handleDelete()} className="bg-[var(--danger)] hover:bg-[var(--danger)]/90">Delete</PrimaryButton>
              <SecondaryButton type="button" onClick={() => setDeleteId(null)}>Cancel</SecondaryButton>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
