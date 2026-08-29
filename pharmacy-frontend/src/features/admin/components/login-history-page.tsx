"use client";

import {
  FileClock,
  LogIn,
  RefreshCw,
  Search,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Input } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { apiRequest, ApiClientError } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

interface LoginHistoryEntry {
  id: string;
  userId: string;
  username: string;
  displayName: string;
  ipAddress: string;
  userAgent: string;
  status: string;
  failureReason: string;
  createdAt: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

export function LoginHistoryPage() {
  const canRead = usePermission(PERMISSIONS.USER_MANAGE);
  const [entries, setEntries] = useState<LoginHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiRequest<LoginHistoryEntry[]>("/login-history");
      setEntries(res.data);
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Login history could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    void apiRequest<LoginHistoryEntry[]>("/login-history")
      .then((res) => {
        if (active) {
          setEntries(res.data);
          setError(null);
        }
      })
      .catch((caught) => {
        if (active) setError(errorMessage(caught, "Login history could not be loaded."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canRead]);

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return entries;
    return entries.filter((entry) =>
      [entry.username, entry.displayName, entry.ipAddress, entry.status, entry.failureReason]
        .some((value) => value?.toLowerCase().includes(normalized)),
    );
  }, [entries, query]);

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      <PageHeader
        title="Login history"
        description="Review login attempts and session activity for staff accounts."
        actions={
          <SecondaryButton
            type="button"
            title="Refresh login history"
            aria-label="Refresh login history"
            className="px-3"
            disabled={loading}
            onClick={() => void load()}
          >
            <RefreshCw aria-hidden="true" size={17} />
          </SecondaryButton>
        }
      />

      <section className="mb-5 rounded-md border border-[var(--border)] bg-white p-4">
        <label className="relative block">
          <span className="sr-only">Search login history</span>
          <Search
            aria-hidden="true"
            className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]"
            size={17}
          />
          <Input
            className="pl-9"
            placeholder="Search by user, IP address, or status"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
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
                  <th className="px-4 py-3 font-semibold">IP Address</th>
                  <th className="px-4 py-3 font-semibold">User Agent</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Failure Reason</th>
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
                          <LogIn aria-hidden="true" size={15} />
                        </span>
                        <div>
                          <span className="block font-medium">{entry.displayName}</span>
                          <span className="block font-mono text-xs text-[var(--text-muted)]">
                            {entry.username}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-[var(--text-muted)]">
                      {entry.ipAddress || "-"}
                    </td>
                    <td className="max-w-48 truncate px-4 py-3 text-xs text-[var(--text-muted)]">
                      {entry.userAgent || "-"}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={entry.status === "SUCCESS" ? "success" : "danger"}>
                        {entry.status === "SUCCESS" ? "Success" : "Failed"}
                      </StatusBadge>
                    </td>
                    <td className="px-4 py-3 text-[var(--text-muted)]">
                      {entry.failureReason || "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading login history...
          </div>
        ) : (
          <EmptyState
            icon={FileClock}
            title={entries.length ? "No matching logins" : "No login history"}
            description={
              entries.length
                ? "Adjust the search filter."
                : "Login attempts will appear here."
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
