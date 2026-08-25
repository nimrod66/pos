import { BookOpenText } from "lucide-react";
import type { Metadata } from "next";

import { PaymentSettingsPanel } from "@/features/system/components/payment-settings-panel";
import { SystemStatusPanel } from "@/features/system/components/system-status-panel";
import { API_BASE_URL, DEMO_AUTH_ENABLED } from "@/lib/api-config";

export const metadata: Metadata = {
  title: "System health",
};

export default function SystemPage() {
  return (
    <div>
      <header className="mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="mb-1 text-sm text-[var(--text-muted)]">Environment</p>
          <h1 className="text-2xl font-semibold">System health</h1>
        </div>
        {!DEMO_AUTH_ENABLED ? (
          <a
            className="inline-flex h-9 items-center gap-2 self-start rounded-md border border-[var(--border-strong)] bg-white px-3 text-sm font-medium transition-colors hover:bg-[var(--surface-muted)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand)]"
            href={`${new URL(API_BASE_URL).origin}/swagger-ui/index.html`}
            target="_blank"
            rel="noreferrer"
          >
            <BookOpenText aria-hidden="true" size={16} />
            API docs
          </a>
        ) : null}
      </header>

      <div className="mb-6">
        <PaymentSettingsPanel />
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <SystemStatusPanel />

        <aside className="rounded-md border border-[var(--border)] bg-white p-5">
          <h2 className="mb-4 text-sm font-semibold">Application boundary</h2>
          <dl className="space-y-4 text-sm">
            <div>
              <dt className="text-xs text-[var(--text-muted)]">Environment</dt>
              <dd className="mt-1 font-medium">
                {DEMO_AUTH_ENABLED ? "Local preview" : "API connected"}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-[var(--text-muted)]">Data source</dt>
              <dd className="mt-1 font-medium">
                {DEMO_AUTH_ENABLED
                  ? "Browser preview data"
                  : "Spring Boot and PostgreSQL"}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-[var(--text-muted)]">API contract</dt>
              <dd className="mt-1 font-mono text-xs">/api/v1</dd>
            </div>
            <div>
              <dt className="text-xs text-[var(--text-muted)]">Currency</dt>
              <dd className="mt-1 font-medium">KES</dd>
            </div>
            <div>
              <dt className="text-xs text-[var(--text-muted)]">
                Display timezone
              </dt>
              <dd className="mt-1 font-medium">Africa/Nairobi</dd>
            </div>
            <div>
              <dt className="text-xs text-[var(--text-muted)]">Checkout mode</dt>
              <dd className="mt-1 font-medium">
                {DEMO_AUTH_ENABLED ? "Preview transactions" : "Local pharmacy node"}
              </dd>
            </div>
          </dl>
        </aside>
      </div>
    </div>
  );
}
