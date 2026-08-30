"use client";

import { useEffect, useState } from "react";
import { Shield, Plus, X } from "lucide-react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { insuranceGateway } from "@/features/insurance/insurance-gateway";
import type { InsuranceScheme, Insurer, CreateSchemeInput } from "@/features/insurance/types";

const SCHEME_TYPES = ["INDIVIDUAL", "FAMILY", "CORPORATE", "GROUP"];

export function SchemesPage({ showHeader = true }: { showHeader?: boolean }) {
  const canRead = usePermission(PERMISSIONS.INSURANCE_READ);
  const canWrite = usePermission(PERMISSIONS.INSURANCE_WRITE);

  const [schemes, setSchemes] = useState<InsuranceScheme[]>([]);
  const [insurers, setInsurers] = useState<Insurer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterInsurer, setFilterInsurer] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState<CreateSchemeInput>({
    name: "",
    code: "",
    schemeType: "INDIVIDUAL",
  });
  const [formInsurerId, setFormInsurerId] = useState("");

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    async function run() {
      setLoading(true);
      try {
        const [s, i] = await Promise.all([
          insuranceGateway.listSchemes(filterInsurer || undefined),
          insuranceGateway.listActiveInsurers(),
        ]);
        if (!active) return;
        setSchemes(s);
        setInsurers(i);
        setError(null);
      } catch {
        if (!active) return;
        setError("Failed to load schemes.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void run();
    return () => { active = false; };
  }, [canRead, filterInsurer]);

  function resetForm() {
    setForm({ name: "", code: "", schemeType: "INDIVIDUAL" });
    setFormInsurerId("");
    setShowForm(false);
  }

  async function handleSubmit() {
    if (!form.name.trim() || !form.code.trim() || !formInsurerId) {
      setError("Name, code, and insurer are required.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await insuranceGateway.createScheme(formInsurerId, form);
      resetForm();
      const data = await insuranceGateway.listSchemes(filterInsurer || undefined);
      setSchemes(data);
    } catch {
      setError("Failed to create scheme.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      {showHeader ? (
        <PageHeader
          title="Insurance schemes"
          description="Manage insurance schemes offered by insurers."
          actions={
            canWrite ? (
              <PrimaryButton type="button" onClick={() => { resetForm(); setShowForm(true); }}>
                <Plus aria-hidden="true" size={16} /> Add scheme
              </PrimaryButton>
            ) : undefined
          }
        />
      ) : null}

      {error ? <div className="mb-4"><FormError message={error} /></div> : null}

      {showForm ? (
        <div className="mb-5 rounded-md border border-[var(--brand)] bg-white p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold">New scheme</h3>
            <button type="button" onClick={resetForm} className="text-[var(--text-muted)] hover:text-[var(--danger)]"><X size={18} /></button>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Insurer *</span>
              <Select value={formInsurerId} onChange={(e) => setFormInsurerId(e.target.value)}>
                <option value="">Select insurer</option>
                {insurers.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
              </Select>
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Name *</span>
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="e.g. NHIF Supa" />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Code *</span>
              <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="e.g. NHIF-SUPA" />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Type</span>
              <Select value={form.schemeType ?? "INDIVIDUAL"} onChange={(e) => setForm({ ...form, schemeType: e.target.value })}>
                {SCHEME_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </Select>
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Co-pay %</span>
              <Input type="number" value={form.coPayPercentage ?? ""} onChange={(e) => setForm({ ...form, coPayPercentage: e.target.value ? Number(e.target.value) : undefined })} placeholder="e.g. 10" />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Max claim amount</span>
              <Input type="number" value={form.maxClaimAmount ?? ""} onChange={(e) => setForm({ ...form, maxClaimAmount: e.target.value ? Number(e.target.value) : undefined })} placeholder="e.g. 50000" />
            </label>
          </div>
          <div className="mt-3 flex gap-2">
            <PrimaryButton type="button" onClick={() => void handleSubmit()} disabled={submitting}>
              {submitting ? "Saving..." : "Create"}
            </PrimaryButton>
            <SecondaryButton type="button" onClick={resetForm}>Cancel</SecondaryButton>
          </div>
        </div>
      ) : null}

      <section className="mb-4 rounded-md border border-[var(--border)] bg-white p-4">
        <div className="flex gap-3">
          <label className="text-xs font-medium text-[var(--text-muted)]">
            <span className="mb-1 block">Insurer</span>
            <Select value={filterInsurer} onChange={(e) => setFilterInsurer(e.target.value)}>
              <option value="">All insurers</option>
              {insurers.map((i) => <option key={i.id} value={i.id}>{i.name}</option>)}
            </Select>
          </label>
        </div>
      </section>

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">Loading schemes...</div>
        ) : schemes.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[800px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Name</th>
                  <th className="px-4 py-3 font-semibold">Code</th>
                  <th className="px-4 py-3 font-semibold">Insurer</th>
                  <th className="px-4 py-3 font-semibold">Type</th>
                  <th className="px-4 py-3 font-semibold">Co-pay</th>
                  <th className="px-4 py-3 font-semibold">Preauth</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {schemes.map((scheme) => (
                  <tr key={scheme.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3 font-medium">{scheme.name}</td>
                    <td className="px-4 py-3 font-mono text-xs text-[var(--text-muted)]">{scheme.code}</td>
                    <td className="px-4 py-3">{scheme.insurerName || "-"}</td>
                    <td className="px-4 py-3">{scheme.schemeType || "-"}</td>
                    <td className="px-4 py-3">
                      {scheme.coPayPercentage ? `${scheme.coPayPercentage}%` : scheme.coPayFlat ? `KES ${scheme.coPayFlat}` : "-"}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={scheme.requiresPreauth ? "warning" : "neutral"}>
                        {scheme.requiresPreauth ? "Required" : "No"}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={scheme.status === "ACTIVE" ? "success" : "neutral"}>
                        {scheme.status ?? "ACTIVE"}
                      </StatusBadge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState icon={Shield} title="No schemes found" description="Add a scheme to start managing insurance coverage plans." />
        )}
      </section>
    </div>
  );
}
