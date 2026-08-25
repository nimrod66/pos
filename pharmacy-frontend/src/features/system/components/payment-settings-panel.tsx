"use client";

import { useCallback, useEffect, useState } from "react";

import { SecondaryButton } from "@/components/ui/buttons";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { apiRequest } from "@/lib/api-client";

interface BackendSetting {
  id: string;
  settingKey: string;
  settingValue: string;
  description: string | null;
  branchId: string | null;
  pharmacyId: string;
}

const FIELDS = [
  { key: "payment.mpesa_consumer_key", label: "Consumer key", secret: false },
  { key: "payment.mpesa_consumer_secret", label: "Consumer secret", secret: true },
  { key: "payment.mpesa_passkey", label: "Lipa na M-Pesa passkey", secret: true },
  { key: "payment.mpesa_shortcode", label: "Business shortcode", secret: false },
  { key: "payment.mpesa_callback_url", label: "Callback URL", secret: false },
] as const;

export function PaymentSettingsPanel() {
  const session = useAuthStore((state) => state.session);
  const canManage = usePermission(PERMISSIONS.SETTINGS_MANAGE);
  const [values, setValues] = useState<Record<string, string>>({});
  const [rows, setRows] = useState<Record<string, BackendSetting>>({});
  const [environment, setEnvironment] = useState("sandbox");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedAt, setSavedAt] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try {
      const response = await apiRequest<{ content?: BackendSetting[] } | BackendSetting[]>(
        `/system-settings?pharmacyId=${session.user.pharmacyId}`,
        { cache: "no-store" },
      );
      const data = Array.isArray(response.data)
        ? response.data
        : response.data.content ?? [];
      const nextValues: Record<string, string> = {};
      const nextRows: Record<string, BackendSetting> = {};
      for (const setting of data) {
        if (!setting.settingKey.startsWith("payment.mpesa_")) continue;
        nextRows[setting.settingKey] = setting;
        nextValues[setting.settingKey] =
          setting.settingKey === "payment.mpesa_consumer_secret" ||
          setting.settingKey === "payment.mpesa_passkey"
            ? ""
            : setting.settingValue;
      }
      setRows(nextRows);
      setValues(nextValues);
      setEnvironment(
        (nextRows["payment.mpesa_environment"]?.settingValue || "sandbox").toLowerCase(),
      );
      setError(null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Payment settings could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    const initial = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(initial);
  }, [load]);

  async function save() {
    if (!session) return;
    setSaving(true);
    setError(null);
    try {
      const keys = [
        ...FIELDS.map((field) => field.key),
        "payment.mpesa_environment",
      ];
      for (const key of keys) {
        const isSecret =
          key === "payment.mpesa_consumer_secret" || key === "payment.mpesa_passkey";
        let value = key === "payment.mpesa_environment" ? environment : values[key];
        if (isSecret && (value === undefined || value === "")) {
          continue; // keep the stored secret when the field was left blank
        }
        value = value ?? "";
        const existing = rows[key];
        const body = {
          settingKey: key,
          settingValue: value,
          description: existing?.description ?? null,
          branchId: null,
          pharmacyId: session.user.pharmacyId,
        };
        if (existing) {
          await apiRequest(`/system-settings/${existing.id}`, {
            method: "PUT",
            body,
          });
        } else {
          const created = await apiRequest<BackendSetting>("/system-settings", {
            method: "POST",
            body,
          });
          setRows((current) => ({ ...current, [key]: created.data }));
        }
      }
      setSavedAt(new Date().toLocaleTimeString());
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Saving payment settings failed.");
    } finally {
      setSaving(false);
    }
  }

  const stkReady =
    Boolean(rows["payment.mpesa_consumer_key"]?.settingValue) &&
    Boolean(rows["payment.mpesa_consumer_secret"]?.settingValue) &&
    Boolean(rows["payment.mpesa_passkey"]?.settingValue);

  if (!canManage) {
    return (
      <section className="rounded-md border border-[var(--border)] bg-white p-4">
        <h2 className="text-sm font-semibold">M-Pesa Daraja (STK Push)</h2>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          Payment gateway credentials are managed by the pharmacy owner.
        </p>
      </section>
    );
  }

  return (
    <section className="rounded-md border border-[var(--border)] bg-white">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--border)] px-4 py-3.5">
        <div>
          <h2 className="flex items-center gap-2 text-sm font-semibold">
            M-Pesa Daraja (STK Push)
            <StatusBadge tone={stkReady ? "success" : "warning"}>
              {stkReady ? "Automatic push enabled" : "Manual mode"}
            </StatusBadge>
          </h2>
          <p className="mt-0.5 text-xs text-[var(--text-muted)]">
            Per-pharmacy credentials — each pharmacy configures its own paybill here.
            Leave secrets blank to keep the saved values.
          </p>
        </div>
        <SecondaryButton type="button" disabled={saving || loading} onClick={() => void save()}>
          {saving ? "Saving..." : "Save payment settings"}
        </SecondaryButton>
      </div>
      {error ? (
        <div className="px-4 pt-4">
          <FormError message={error} />
        </div>
      ) : null}
      {savedAt && !error ? (
        <p role="status" className="px-4 pt-3 text-xs text-[var(--success)]">
          Saved at {savedAt}. New checkouts use these credentials immediately.
        </p>
      ) : null}
      <div className="grid gap-x-6 gap-y-4 p-4 md:grid-cols-2">
        {FIELDS.map((field) => (
          <Field key={field.key} label={field.label}>
            <Input
              type={field.secret ? "password" : "text"}
              autoComplete="off"
              disabled={loading}
              placeholder={
                field.secret && rows[field.key]?.settingValue ? "Saved - leave blank to keep" : ""
              }
              value={values[field.key] ?? ""}
              onChange={(event) =>
                setValues((current) => ({ ...current, [field.key]: event.target.value }))
              }
            />
          </Field>
        ))}
        <Field label="Daraja environment">
          <Select
            value={environment}
            onChange={(event) => setEnvironment(event.target.value)}
          >
            <option value="sandbox">Sandbox</option>
            <option value="production">Production</option>
          </Select>
        </Field>
      </div>
    </section>
  );
}
