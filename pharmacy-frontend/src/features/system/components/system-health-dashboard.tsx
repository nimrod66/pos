"use client";

import {
  Activity,
  AlertTriangle,
  ArrowUpCircle,
  CheckCircle2,
  Database,
  HardDrive,
  MemoryStick,
  Monitor,
  RefreshCw,
  Server,
  Wifi,
  XCircle,
  type LucideIcon,
} from "lucide-react";
import { useEffect, useState } from "react";

import {
  terminalGateway,
  type SystemHealthCheck,
  type HealthComponent,
} from "@/features/terminals/terminal-gateway";
import { cn } from "@/lib/cn";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

type LoadState =
  | { kind: "loading" }
  | { kind: "ready"; health: SystemHealthCheck }
  | { kind: "error" };

type Tone = "up" | "down" | "warn" | "unknown";

function toneOf(status: string): Tone {
  switch (status) {
    case "UP":
      return "up";
    case "DOWN":
      return "down";
    case "WARNING":
      return "warn";
    default:
      return "unknown";
  }
}

const toneStyles: Record<Tone, { bg: string; text: string; icon: LucideIcon }> =
  {
    up: {
      bg: "bg-emerald-50 border-emerald-200",
      text: "text-emerald-700",
      icon: CheckCircle2,
    },
    down: {
      bg: "bg-red-50 border-red-200",
      text: "text-red-700",
      icon: XCircle,
    },
    warn: {
      bg: "bg-amber-50 border-amber-200",
      text: "text-amber-700",
      icon: AlertTriangle,
    },
    unknown: {
      bg: "bg-gray-50 border-gray-200",
      text: "text-gray-500",
      icon: AlertTriangle,
    },
  };

function StatusBadge({ status }: { status: string }) {
  const t = toneOf(status);
  const s = toneStyles[t];
  const Icon = s.icon;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium",
        s.bg,
        s.text,
      )}
    >
      <Icon size={12} />
      {status}
    </span>
  );
}

function HealthCard({
  title,
  icon: Icon,
  component,
  children,
}: {
  title: string;
  icon: LucideIcon;
  component: HealthComponent;
  children?: React.ReactNode;
}) {
  const t = toneOf(component.status);
  const s = toneStyles[t];

  return (
    <div className={cn("rounded-lg border p-4", s.bg)}>
      <div className="mb-2 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Icon size={16} className={s.text} />
          <span className="text-sm font-semibold">{title}</span>
        </div>
        <StatusBadge status={component.status} />
      </div>
      {children}
    </div>
  );
}

function KV({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between text-xs">
      <span className="text-gray-500">{label}</span>
      <span className="font-medium">{String(value ?? "N/A")}</span>
    </div>
  );
}

function DiskBar({ percent }: { percent: number }) {
  const color =
    percent > 90 ? "bg-red-500" : percent > 80 ? "bg-amber-500" : "bg-emerald-500";
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
      <div
        className={cn("h-full rounded-full transition-all", color)}
        style={{ width: `${Math.min(percent, 100)}%` }}
      />
    </div>
  );
}

export function SystemHealthDashboard() {
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [autoRefresh, setAutoRefresh] = useState(true);

  async function refresh() {
    setState({ kind: "loading" });
    try {
      const health = await terminalGateway.getHealthCheck();
      setState({ kind: "ready", health });
    } catch {
      setState({ kind: "error" });
    }
  }

  useEffect(() => {
    void refresh();
  }, []);

  useEffect(() => {
    if (!autoRefresh) return;
    const id = setInterval(refresh, 30_000);
    return () => clearInterval(id);
  }, [autoRefresh]);

  const health = state.kind === "ready" ? state.health : null;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">System Health</h2>
          <p className="text-xs text-gray-500">
            {health
              ? `Last checked: ${new Date(health.checkedAt).toLocaleTimeString()}`
              : "Loading..."}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setAutoRefresh(!autoRefresh)}
            className={cn(
              "rounded-md border px-3 py-1.5 text-xs font-medium transition-colors",
              autoRefresh
                ? "border-emerald-300 bg-emerald-50 text-emerald-700"
                : "border-gray-300 bg-white text-gray-600",
            )}
          >
            Auto-refresh {autoRefresh ? "ON" : "OFF"}
          </button>
          <button
            type="button"
            onClick={() => void refresh()}
            disabled={state.kind === "loading"}
            className="flex items-center gap-1.5 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <RefreshCw
              size={12}
              className={state.kind === "loading" ? "animate-spin" : ""}
            />
            Refresh
          </button>
        </div>
      </div>

      {state.kind === "error" && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          Failed to load system health. The API may be unreachable.
        </div>
      )}

      {health && (
        <>
          <div
            className={cn(
              "flex items-center gap-3 rounded-lg border p-4",
              health.status === "HEALTHY"
                ? "border-emerald-200 bg-emerald-50"
                : "border-amber-200 bg-amber-50",
            )}
          >
            {health.status === "HEALTHY" ? (
              <CheckCircle2 size={20} className="text-emerald-600" />
            ) : (
              <AlertTriangle size={20} className="text-amber-600" />
            )}
            <div>
              <p className="text-sm font-semibold">
                System {health.status}
              </p>
              <p className="text-xs text-gray-500">
                {health.status === "HEALTHY"
                  ? "All components are operating normally."
                  : "One or more components need attention."}
              </p>
            </div>
          </div>

           <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-4">
            <HealthCard title="API" icon={Server} component={health.api}>
              <KV label="Uptime" value={String((health.api as Record<string, unknown>).uptime ?? "N/A")} />
            </HealthCard>

            <HealthCard title="Database" icon={Database} component={health.database}>
              <KV label="Name" value={String((health.database as Record<string, unknown>).database ?? "N/A")} />
              <KV label="Medicines" value={String((health.database as Record<string, unknown>).medicineCount ?? "N/A")} />
            </HealthCard>

            <HealthCard title="Disk" icon={HardDrive} component={health.disk}>
              <DiskBar percent={health.disk.usedPercent} />
              <div className="mt-1.5 flex justify-between text-xs text-gray-500">
                <span>{health.disk.freeGB}GB free</span>
                <span>{health.disk.usedPercent}% used</span>
              </div>
            </HealthCard>

            <HealthCard title="Memory" icon={MemoryStick} component={health.memory}>
              <DiskBar percent={health.memory.heapUsedPercent} />
              <div className="mt-1.5 flex justify-between text-xs text-gray-500">
                <span>{health.memory.heapUsedMB}MB used</span>
                <span>{health.memory.heapMaxMB}MB max</span>
              </div>
            </HealthCard>

            <HealthCard
              title="Connection Pool"
              icon={Activity}
              component={health.connectionPool}
            >
              <KV
                label="Active"
                value={`${health.connectionPool.activeConnections} / ${health.connectionPool.totalConnections}`}
              />
              <KV label="Idle" value={health.connectionPool.idleConnections} />
              <KV label="Waiting" value={health.connectionPool.threadsAwaiting} />
            </HealthCard>

            <HealthCard title="Backup" icon={ArrowUpCircle} component={health.backup}>
              <KV label="Count" value={health.backup.count} />
              <KV
                label="Last backup"
                value={
                  health.backup.hoursSinceBackup != null
                    ? `${health.backup.hoursSinceBackup}h ago`
                    : "Never"
                }
              />
              <KV
                label="Size"
                value={
                  health.backup.lastBackupSize
                    ? `${(health.backup.lastBackupSize / 1024 / 1024).toFixed(1)}MB`
                    : "N/A"
                }
              />
            </HealthCard>

            <HealthCard title="Sync" icon={Wifi} component={health.sync}>
              <KV label="Mode" value={health.sync.mode ?? "N/A"} />
              <KV
                label="Latency"
                value={
                  health.sync.latencyMs != null
                    ? `${health.sync.latencyMs}ms`
                    : "N/A"
                }
              />
            </HealthCard>

            <HealthCard title="Terminals" icon={Monitor} component={health.terminals}>
              <KV
                label="Online"
                value={`${health.terminals.active} / ${health.terminals.total}`}
              />
            </HealthCard>
          </div>
        </>
      )}
    </div>
  );
}
