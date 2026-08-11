"use client";

import { Check, RotateCcw, Save, Settings2 } from "lucide-react";
import { useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import {
  Field,
  FormError,
  Input,
  Select,
  Textarea,
} from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useCartStore } from "@/features/pos/store/cart-store";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import type { PharmacySettings } from "@/features/workspace/types";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

export function SettingsPage() {
  const settings = useWorkspaceQuery((state) => state.settings);
  const loadStatus = useWorkspaceQuery((state) => state.loadStatus);

  if (loadStatus !== "ready") {
    return (
      <div className="rounded-md border border-[var(--border)] bg-white p-6">
        <h1 className="text-lg font-semibold">Loading settings</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          Fetching branch configuration from the pharmacy node.
        </p>
      </div>
    );
  }

  return <SettingsForm settings={settings} />;
}

function SettingsForm({ settings }: { settings: PharmacySettings }) {
  const clearCart = useCartStore((state) => state.clear);
  const canManageSettings = usePermission(PERMISSIONS.SETTINGS_MANAGE);
  const [draft, setDraft] = useState<PharmacySettings>(settings);
  const [saved, setSaved] = useState(false);
  const [resetArmed, setResetArmed] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function update<K extends keyof PharmacySettings>(key: K, value: PharmacySettings[K]) {
    setSaved(false);
    setError(null);
    setDraft((current) => ({ ...current, [key]: value }));
  }

  async function handleSave(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canManageSettings) return;
    setError(null);
    setSaving(true);
    const normalized = {
      ...draft,
      receiptPrefix: draft.receiptPrefix.trim().toUpperCase().replace(/-+$/, ""),
    };
    try {
      await workspaceGateway.updateSettings(normalized);
      setDraft(normalized);
      setSaved(true);
    } catch (caught) {
      setSaved(false);
      setError(
        getWorkspaceErrorMessage(caught, "Settings could not be saved."),
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleReset() {
    if (!canManageSettings) return;
    setError(null);
    try {
      await workspaceGateway.resetWorkspace();
      clearCart();
      setDraft(workspaceGateway.getSnapshot().settings);
      setResetArmed(false);
      setSaved(false);
    } catch (caught) {
      setError(
        getWorkspaceErrorMessage(caught, "Preview data could not be reset."),
      );
    }
  }

  if (!canManageSettings) {
    return <AccessRestricted />;
  }

  return (
    <div className="max-w-6xl">
      <PageHeader title="Pharmacy settings" description="Configure branch identity, stock alerts, and receipt details." />
      {saved ? <div role="status" className="mb-6 flex items-center gap-2 rounded-md bg-[var(--success-soft)] px-3 py-2.5 text-sm text-[var(--success)]"><Check aria-hidden="true" size={17} /> Settings saved.</div> : null}
      {error ? <div className="mb-6"><FormError message={error} /></div> : null}
      <form onSubmit={handleSave} className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-6">
          <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
            <div className="flex items-center gap-2"><Settings2 aria-hidden="true" className="text-[var(--brand)]" size={18} /><h2 className="text-base font-semibold">Branch identity</h2></div>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Field label="Pharmacy name" required><Input value={draft.pharmacyName} onChange={(event) => update("pharmacyName", event.target.value)} required /></Field>
              <Field label="Branch name" required><Input value={draft.branchName} onChange={(event) => update("branchName", event.target.value)} required /></Field>
              <Field label="Phone" required><Input type="tel" value={draft.phone} onChange={(event) => update("phone", event.target.value)} required /></Field>
              <Field label="Timezone"><Select value={draft.timezone} disabled><option value="Africa/Nairobi">Africa/Nairobi</option></Select></Field>
              <Field label="Currency"><Select value={draft.currency} disabled><option value="KES">Kenyan shilling (KES)</option></Select></Field>
              <Field label="Near-expiry alert (days)" required><Input type="number" min={1} max={730} step={1} value={draft.nearExpiryDays} onChange={(event) => update("nearExpiryDays", Number(event.target.value))} /></Field>
            </div>
          </section>
          <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
            <h2 className="text-base font-semibold">Receipt</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Field label="Receipt prefix" required hint="Used as PREFIX-000001."><Input maxLength={8} autoCapitalize="characters" value={draft.receiptPrefix} onChange={(event) => update("receiptPrefix", event.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, ""))} required /></Field>
              <Field label="Receipt paper" required><Select value={draft.receiptPaperWidth} onChange={(event) => update("receiptPaperWidth", event.target.value as PharmacySettings["receiptPaperWidth"])}><option value="80MM">80 mm thermal</option><option value="58MM">58 mm thermal</option></Select></Field>
              <Field label="Receipt footer" required><Textarea value={draft.receiptFooter} onChange={(event) => update("receiptFooter", event.target.value)} required /></Field>
            </div>
          </section>
          <div className="flex justify-end"><PrimaryButton type="submit" disabled={saving}><Save aria-hidden="true" size={17} /> {saving ? "Saving..." : "Save settings"}</PrimaryButton></div>
        </div>
        <aside className="space-y-6">
          <section className="rounded-md border border-[var(--border)] bg-white p-5">
            <h2 className="text-sm font-semibold">Receipt preview</h2>
            <div className={`${draft.receiptPaperWidth === "58MM" ? "max-w-[220px]" : "max-w-[300px]"} mx-auto mt-4 border-y border-dashed border-[var(--border-strong)] py-5 text-center transition-[max-width]`}><p className="font-bold">{draft.pharmacyName || "Pharmacy name"}</p><p className="mt-1 text-sm">{draft.branchName || "Branch"}</p><p className="mt-0.5 text-xs text-[var(--text-muted)]">{draft.phone || "Phone number"}</p><p className="my-5 font-mono text-xs">{draft.receiptPrefix || "POS"}-000001</p><p className="text-xs text-[var(--text-muted)]">{draft.receiptFooter || "Receipt footer"}</p></div>
          </section>
          {DEMO_AUTH_ENABLED ? <section className="rounded-md border border-[var(--danger-border)] bg-white p-5">
            <h2 className="text-sm font-semibold text-[var(--danger)]">Reset preview data</h2>
            <p className="mt-2 text-sm text-[var(--text-muted)]">Restore sample medicines, stock, sales, shifts, staff, and settings. This cannot be undone.</p>
            {resetArmed ? <div className="mt-4 rounded-md bg-[var(--danger-soft)] p-3"><p className="text-sm font-medium text-[var(--danger)]">Confirm the full local reset.</p><div className="mt-3 flex gap-2"><SecondaryButton type="button" className="min-h-9 flex-1" onClick={() => setResetArmed(false)}>Cancel</SecondaryButton><PrimaryButton type="button" className="min-h-9 flex-1 bg-[var(--danger)] hover:bg-[#a9342b]" onClick={() => void handleReset()}>Confirm reset</PrimaryButton></div></div> : <SecondaryButton type="button" className="mt-4 w-full text-[var(--danger)]" onClick={() => setResetArmed(true)}><RotateCcw aria-hidden="true" size={17} /> Reset preview</SecondaryButton>}
          </section> : null}
        </aside>
      </form>
    </div>
  );
}
