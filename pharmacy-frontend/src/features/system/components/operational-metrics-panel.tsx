"use client";

import { useEffect, useState } from "react";
import { AlertTriangle, RefreshCw } from "lucide-react";

import { apiRequest } from "@/lib/api-client";

type MetricSummary = {
  from: string;
  to: string;
  events: Record<string, Record<string, number>>;
  checkout?: {
    attempted: number;
    success: number;
    failed: number;
    successRatePercent: number | null;
  };
  checkoutFailureReasons?: Record<string, number>;
  paymentFailureReasons?: Record<string, number>;
  hardwareFailureReasons?: Record<string, number>;
};

function count(summary: MetricSummary | null, type: string, status: string) {
  return summary?.events?.[type]?.[status] ?? 0;
}

function ReasonList({ title, reasons }: { title: string; reasons?: Record<string, number> }) {
  const entries = Object.entries(reasons ?? {}).slice(0, 5);
  return (
    <div className="rounded-md border border-[var(--border)] bg-white p-4">
      <h3 className="mb-2 text-sm font-semibold">{title}</h3>
      {entries.length ? (
        <ul className="space-y-1 text-xs">
          {entries.map(([reason, total]) => (
            <li key={reason} className="flex justify-between gap-3">
              <span className="truncate text-[var(--text-muted)]">{reason}</span>
              <span className="font-semibold">{total}</span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-xs text-[var(--text-muted)]">No failures in this window.</p>
      )}
    </div>
  );
}

export function OperationalMetricsPanel() {
  const [summary, setSummary] = useState<MetricSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const response = await apiRequest<MetricSummary>("/operations/metrics", { cache: "no-store" });
      setSummary(response.data);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Failed to load operational metrics.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const checkoutRate = summary?.checkout?.successRatePercent;

  return (
    <section className="rounded-lg border border-[var(--border)] bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Operational readiness metrics</h2>
          <p className="text-xs text-[var(--text-muted)]">Last 7 days: checkout, payment, backup, offline, security, and hardware events.</p>
        </div>
        <button
          type="button"
          onClick={() => void load()}
          disabled={loading}
          className="inline-flex items-center gap-1.5 rounded-md border border-[var(--border-strong)] px-3 py-1.5 text-xs font-medium disabled:opacity-50"
        >
          <RefreshCw size={12} className={loading ? "animate-spin" : ""} />
          Refresh
        </button>
      </div>

      {error ? (
        <div className="mb-4 flex items-center gap-2 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-700">
          <AlertTriangle size={16} /> {error}
        </div>
      ) : null}

      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-md bg-[var(--surface-muted)] p-4">
          <p className="text-xs text-[var(--text-muted)]">Checkout success</p>
          <p className="mt-1 text-2xl font-semibold">{checkoutRate == null ? "—" : `${checkoutRate}%`}</p>
          <p className="text-xs text-[var(--text-muted)]">Failed: {summary?.checkout?.failed ?? 0}</p>
        </div>
        <div className="rounded-md bg-[var(--surface-muted)] p-4">
          <p className="text-xs text-[var(--text-muted)]">Payment failures</p>
          <p className="mt-1 text-2xl font-semibold">{count(summary, "PAYMENT", "FAILED")}</p>
          <p className="text-xs text-[var(--text-muted)]">Pending: {count(summary, "PAYMENT", "PENDING")}</p>
        </div>
        <div className="rounded-md bg-[var(--surface-muted)] p-4">
          <p className="text-xs text-[var(--text-muted)]">Offline queue issues</p>
          <p className="mt-1 text-2xl font-semibold">{count(summary, "OFFLINE_QUEUE", "FAILED")}</p>
          <p className="text-xs text-[var(--text-muted)]">Queued: {count(summary, "OFFLINE_QUEUE", "PENDING")}</p>
        </div>
        <div className="rounded-md bg-[var(--surface-muted)] p-4">
          <p className="text-xs text-[var(--text-muted)]">Hardware failures</p>
          <p className="mt-1 text-2xl font-semibold">{count(summary, "HARDWARE", "FAILED")}</p>
          <p className="text-xs text-[var(--text-muted)]">Warnings: {count(summary, "HARDWARE", "WARNING")}</p>
        </div>
      </div>

      <div className="mt-3 grid gap-3 md:grid-cols-3">
        <ReasonList title="Checkout failure reasons" reasons={summary?.checkoutFailureReasons} />
        <ReasonList title="Payment failure reasons" reasons={summary?.paymentFailureReasons} />
        <ReasonList title="Hardware failure reasons" reasons={summary?.hardwareFailureReasons} />
      </div>
    </section>
  );
}
