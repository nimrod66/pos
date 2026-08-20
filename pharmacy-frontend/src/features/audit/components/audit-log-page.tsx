"use client";

import {
  Download,
  FileClock,
  RefreshCw,
  Search,
  ShieldCheck,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  type AuditLogEntry,
  operationsGateway,
} from "@/features/operations/operations-gateway";
import { ApiClientError } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function actionLabel(action: string) {
  return action
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function actionTone(action: string) {
  const normalized = action.toUpperCase();
  if (normalized.includes("DELETE") || normalized.includes("DISABLE")) {
    return "danger" as const;
  }
  if (normalized.includes("CREATE") || normalized.includes("APPROVE")) {
    return "success" as const;
  }
  if (normalized.includes("UPDATE") || normalized.includes("CHANGE")) {
    return "info" as const;
  }
  return "neutral" as const;
}

function csvCell(value: string | null) {
  return `"${(value ?? "").replace(/"/g, '""')}"`;
}

export function AuditLogPage() {
  const canRead = usePermission(PERMISSIONS.AUDIT_READ);
  const [entries, setEntries] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [entity, setEntity] = useState("ALL");
  const [action, setAction] = useState("ALL");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  const load = useCallback(async () => {
    await Promise.resolve();
    setLoading(true);
    try {
      setEntries(await operationsGateway.listAuditLogs());
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Audit history could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    void operationsGateway
      .listAuditLogs()
      .then((rows) => {
        if (!active) return;
        setEntries(rows);
        setError(null);
      })
      .catch((caught) => {
        if (!active) return;
        setError(errorMessage(caught, "Audit history could not be loaded."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canRead]);

  const entities = useMemo(
    () => [...new Set(entries.map((entry) => entry.tableName))].sort(),
    [entries],
  );
  const actions = useMemo(
    () => [...new Set(entries.map((entry) => entry.action))].sort(),
    [entries],
  );
  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return entries.filter((entry) => {
      const date = entry.createdAt.slice(0, 10);
      return (
        (entity === "ALL" || entry.tableName === entity) &&
        (action === "ALL" || entry.action === action) &&
        (!from || date >= from) &&
        (!to || date <= to) &&
        (!normalized ||
          [
            entry.userName,
            entry.tableName,
            entry.action,
            entry.recordId,
          ].some((value) => value?.toLowerCase().includes(normalized)))
      );
    });
  }, [action, entries, entity, from, query, to]);

  function exportCsv() {
    const header = ["Time", "User", "Action", "Entity", "Record ID"];
    const rows = filtered.map((entry) => [
      entry.createdAt,
      entry.userName,
      entry.action,
      entry.tableName,
      entry.recordId,
    ]);
    const csv = [header, ...rows]
      .map((row) => row.map((cell) => csvCell(cell)).join(","))
      .join("\r\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `pharmacy-audit-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      <PageHeader
        title="Audit log"
        description="Review recorded account and business-data changes across the pharmacy."
        actions={
          <>
            <SecondaryButton
              type="button"
              title="Refresh audit log"
              aria-label="Refresh audit log"
              className="px-3"
              disabled={loading}
              onClick={() => void load()}
            >
              <RefreshCw aria-hidden="true" size={17} />
            </SecondaryButton>
            <SecondaryButton
              type="button"
              disabled={filtered.length === 0}
              onClick={exportCsv}
            >
              <Download aria-hidden="true" size={17} /> Export
            </SecondaryButton>
          </>
        }
      />

      <section className="mb-5 rounded-md border border-[var(--border)] bg-white p-4">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(260px,1fr)_200px_220px_170px_170px]">
          <label className="relative">
            <span className="sr-only">Search audit log</span>
            <Search
              aria-hidden="true"
              className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]"
              size={17}
            />
            <Input
              className="pl-9"
              placeholder="Search user, action, entity, or record"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
          <label>
            <span className="sr-only">Entity</span>
            <Select value={entity} onChange={(event) => setEntity(event.target.value)}>
              <option value="ALL">All entities</option>
              {entities.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </Select>
          </label>
          <label>
            <span className="sr-only">Action</span>
            <Select value={action} onChange={(event) => setAction(event.target.value)}>
              <option value="ALL">All actions</option>
              {actions.map((value) => (
                <option key={value} value={value}>
                  {actionLabel(value)}
                </option>
              ))}
            </Select>
          </label>
          <label>
            <span className="sr-only">From date</span>
            <Input
              type="date"
              value={from}
              max={to || undefined}
              onChange={(event) => setFrom(event.target.value)}
            />
          </label>
          <label>
            <span className="sr-only">To date</span>
            <Input
              type="date"
              value={to}
              min={from || undefined}
              onChange={(event) => setTo(event.target.value)}
            />
          </label>
        </div>
      </section>

      {error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {filtered.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Time</th>
                  <th className="px-4 py-3 font-semibold">User</th>
                  <th className="px-4 py-3 font-semibold">Action</th>
                  <th className="px-4 py-3 font-semibold">Entity</th>
                  <th className="px-4 py-3 font-semibold">Record ID</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {filtered.map((entry) => (
                  <tr key={entry.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="whitespace-nowrap px-4 py-3 text-xs text-[var(--text-muted)]">
                      {formatDateTime(entry.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <span className="flex size-8 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand-strong)]">
                          <ShieldCheck aria-hidden="true" size={15} />
                        </span>
                        <span className="font-medium">
                          {entry.userName || "System"}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={actionTone(entry.action)}>
                        {actionLabel(entry.action)}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3 font-medium">{entry.tableName}</td>
                    <td className="max-w-72 truncate px-4 py-3 font-mono text-xs text-[var(--text-muted)]">
                      {entry.recordId || "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading audit history...
          </div>
        ) : (
          <EmptyState
            icon={FileClock}
            title={entries.length ? "No matching activity" : "No audit entries"}
            description={
              entries.length
                ? "Adjust the search or date filters."
                : "Recorded changes will appear here."
            }
          />
        )}
      </section>
      <p className="mt-3 text-right text-xs text-[var(--text-muted)]">
        {filtered.length} of {entries.length} entries
      </p>
    </div>
  );
}
