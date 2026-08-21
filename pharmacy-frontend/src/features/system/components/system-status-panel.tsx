"use client";

import {
  CheckCircle2,
  CircleMinus,
  Database,
  MonitorCheck,
  RefreshCw,
  Server,
  TriangleAlert,
  type LucideIcon,
} from "lucide-react";
import { useEffect, useState } from "react";

import {
  terminalGateway,
  type SystemStatusSnapshot,
} from "@/features/terminals/terminal-gateway";
import { cn } from "@/lib/cn";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

type LoadState =
  | { kind: "loading" }
  | { kind: "ready"; snapshot: SystemStatusSnapshot }
  | { kind: "error" };

type StatusTone = "danger" | "loading" | "neutral" | "success";

interface StatusRow {
  detail: string;
  icon: LucideIcon;
  label: string;
  state: string;
  tone: StatusTone;
}

const statusPresentation: Record<
  StatusTone,
  { color: string; icon: LucideIcon }
> = {
  danger: { color: "var(--danger)", icon: TriangleAlert },
  loading: { color: "var(--text-muted)", icon: RefreshCw },
  neutral: { color: "var(--text-muted)", icon: CircleMinus },
  success: { color: "var(--success)", icon: CheckCircle2 },
};

export function SystemStatusPanel() {
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  async function refreshStatus() {
    setState({ kind: "loading" });

    try {
      const snapshot = await terminalGateway.getSystemStatus();
      setState({ kind: "ready", snapshot });
    } catch {
      setState({ kind: "error" });
    }
  }

  useEffect(() => {
    const controller = new AbortController();

    void terminalGateway
      .getSystemStatus(controller.signal)
      .then((snapshot) => setState({ kind: "ready", snapshot }))
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && error.name === "AbortError")) {
          setState({ kind: "error" });
        }
      });

    return () => controller.abort();
  }, []);

  const ready = state.kind === "ready";
  const failed = state.kind === "error";
  const status = ready ? state.snapshot.status : null;
  const serviceState = ready
    ? { state: "Available", tone: "success" as const }
    : failed
      ? { state: "Unavailable", tone: "danger" as const }
      : { state: "Checking", tone: "loading" as const };

  const rows: StatusRow[] = DEMO_AUTH_ENABLED
    ? [
        {
          label: "Web application",
          detail: "Next.js 16.3.0",
          state: "Available",
          icon: MonitorCheck,
          tone: "success",
        },
        {
          label: "Backend API",
          detail: "Not required for local preview",
          state: "Not connected",
          icon: Server,
          tone: "neutral",
        },
        {
          label: "Local preview data",
          detail: "Browser storage",
          state: "Ready",
          icon: Database,
          tone: "success",
        },
      ]
    : [
        {
          label: "Web application",
          detail: "Next.js 16.3.0",
          state: "Available",
          icon: MonitorCheck,
          tone: "success",
        },
        {
          label: "REST API",
          detail: status ? "Version " + status.version : "Spring Boot service",
          state: serviceState.state,
          icon: Server,
          tone: serviceState.tone,
        },
        {
          label: "PostgreSQL",
          detail: status?.databaseName ?? "pharmacy_pos",
          state: serviceState.state,
          icon: Database,
          tone: serviceState.tone,
        },
      ];

  return (
    <section
      className="overflow-hidden rounded-md border border-[var(--border)] bg-white"
      aria-live="polite"
    >
      <div className="flex min-h-14 items-center justify-between gap-3 border-b border-[var(--border)] px-5 py-2.5">
        <div>
          <h2 className="text-sm font-semibold">Runtime readiness</h2>
          <p className="text-xs text-[var(--text-muted)]">
            {DEMO_AUTH_ENABLED
              ? "Frontend preview environment"
              : "Frontend, API, and database"}
          </p>
        </div>
        <button
          type="button"
          className="flex size-9 items-center justify-center rounded-md border border-[var(--border)] text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-muted)] hover:text-[var(--text)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand)] disabled:cursor-wait disabled:opacity-60"
          aria-label="Refresh system status"
          title="Refresh system status"
          disabled={state.kind === "loading"}
          onClick={() => void refreshStatus()}
        >
          <RefreshCw
            aria-hidden="true"
            className={state.kind === "loading" ? "animate-spin" : ""}
            size={16}
          />
        </button>
      </div>

      <div>
        {rows.map(({ label, detail, state: rowState, icon: Icon, tone }, index) => {
          const presentation = statusPresentation[tone];
          const StateIcon = presentation.icon;

          return (
            <div
              className={cn(
                "grid min-h-20 grid-cols-[2.25rem_minmax(0,1fr)_auto] items-center gap-3 px-5",
                index > 0 && "border-t border-[var(--border)]",
              )}
              key={label}
            >
              <div className="flex size-9 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand)]">
                <Icon aria-hidden="true" size={18} />
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{label}</p>
                <p className="truncate text-xs text-[var(--text-muted)]">
                  {detail}
                </p>
              </div>
              <div
                className="flex items-center gap-1.5 text-xs font-medium"
                style={{ color: presentation.color }}
              >
                <StateIcon
                  aria-hidden="true"
                  className={tone === "loading" ? "animate-spin" : ""}
                  size={14}
                />
                {rowState}
              </div>
            </div>
          );
        })}
      </div>

      <div className="min-h-10 border-t border-[var(--border)] bg-[var(--surface-muted)] px-5 py-2 text-xs text-[var(--text-muted)]">
        {ready
          ? state.snapshot.requestId
            ? "Request " + state.snapshot.requestId
            : "Preview mode runs without Docker, Spring Boot, or PostgreSQL."
            : failed
              ? "The API or database is not responding."
              : "Checking the local services..."}
      </div>
    </section>
  );
}
