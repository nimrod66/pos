"use client";

import {
  AlertTriangle,
  Database,
  RefreshCw,
  RotateCw,
  Trash2,
  XCircle,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { apiRequest, ApiClientError } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

interface DeadLetterEvent {
  eventId: string;
  eventType: string;
  payload: unknown;
  error: string;
  retryCount: number;
  maxRetries: number;
  createdAt: string;
  lastAttemptAt: string;
}

interface DeadLetterStats {
  total: number;
  byEventType: Record<string, number>;
  oldestEvent: string;
  newestEvent: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function eventTypeTone(eventType: string) {
  const normalized = eventType.toUpperCase();
  if (normalized.includes("DELETE")) return "danger" as const;
  if (normalized.includes("CREATE")) return "success" as const;
  if (normalized.includes("UPDATE")) return "info" as const;
  return "neutral" as const;
}

function eventTypeLabel(eventType: string) {
  return eventType
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function SyncQueuePage() {
  const canRead = usePermission(PERMISSIONS.SETTINGS_MANAGE);
  const [events, setEvents] = useState<DeadLetterEvent[]>([]);
  const [stats, setStats] = useState<DeadLetterStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryingAll, setRetryingAll] = useState(false);
  const [retryingId, setRetryingId] = useState<string | null>(null);
  const [discardTarget, setDiscardTarget] = useState<DeadLetterEvent | null>(null);
  const [discarding, setDiscarding] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [eventsRes, statsRes] = await Promise.all([
        apiRequest<DeadLetterEvent[]>("/sync/dead-letter"),
        apiRequest<DeadLetterStats>("/sync/dead-letter/stats"),
      ]);
      setEvents(eventsRes.data);
      setStats(statsRes.data);
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Sync queue data could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    Promise.all([
      apiRequest<DeadLetterEvent[]>("/sync/dead-letter"),
      apiRequest<DeadLetterStats>("/sync/dead-letter/stats"),
    ])
      .then(([eventsRes, statsRes]) => {
        if (active) {
          setEvents(eventsRes.data);
          setStats(statsRes.data);
          setError(null);
        }
      })
      .catch((caught) => {
        if (active) setError(errorMessage(caught, "Sync queue data could not be loaded."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canRead]);

  async function handleRetryAll() {
    setRetryingAll(true);
    try {
      await apiRequest("/sync/dead-letter/retry-all", { method: "POST" });
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "Could not retry all events."));
    } finally {
      setRetryingAll(false);
    }
  }

  async function handleRetryOne(eventId: string) {
    setRetryingId(eventId);
    try {
      await apiRequest(`/sync/dead-letter/${eventId}/retry`, { method: "POST" });
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "Could not retry the event."));
    } finally {
      setRetryingId(null);
    }
  }

  async function handleDiscard() {
    if (!discardTarget) return;
    setDiscarding(true);
    try {
      await apiRequest(`/sync/dead-letter/${discardTarget.eventId}`, { method: "DELETE" });
      setDiscardTarget(null);
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "Could not discard the event."));
    } finally {
      setDiscarding(false);
    }
  }

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      <PageHeader
        title="Sync queue"
        description="Review and manage failed sync events in the dead-letter queue."
        actions={
          <>
            <SecondaryButton
              type="button"
              title="Refresh queue"
              aria-label="Refresh queue"
              className="px-3"
              disabled={loading}
              onClick={() => void load()}
            >
              <RefreshCw aria-hidden="true" size={17} />
            </SecondaryButton>
            <PrimaryButton
              type="button"
              disabled={retryingAll || events.length === 0}
              onClick={() => void handleRetryAll()}
            >
              <RotateCw
                aria-hidden="true"
                size={17}
                className={retryingAll ? "animate-spin" : ""}
              />
              {retryingAll ? "Retrying..." : "Retry all"}
            </PrimaryButton>
          </>
        }
      />

      {stats ? (
        <section className="mb-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-md border border-[var(--border)] bg-white p-4">
            <p className="text-xs font-medium text-[var(--text-muted)]">Total failed</p>
            <p className="mt-1 text-2xl font-semibold">{stats.total}</p>
          </div>
          <div className="rounded-md border border-[var(--border)] bg-white p-4">
            <p className="text-xs font-medium text-[var(--text-muted)]">Event types</p>
            <p className="mt-1 text-2xl font-semibold">
              {Object.keys(stats.byEventType).length}
            </p>
          </div>
          <div className="rounded-md border border-[var(--border)] bg-white p-4">
            <p className="text-xs font-medium text-[var(--text-muted)]">Oldest event</p>
            <p className="mt-1 text-sm font-medium">
              {stats.oldestEvent ? formatDateTime(stats.oldestEvent) : "-"}
            </p>
          </div>
          <div className="rounded-md border border-[var(--border)] bg-white p-4">
            <p className="text-xs font-medium text-[var(--text-muted)]">Newest event</p>
            <p className="mt-1 text-sm font-medium">
              {stats.newestEvent ? formatDateTime(stats.newestEvent) : "-"}
            </p>
          </div>
        </section>
      ) : null}

      {error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {events.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1000px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Event ID</th>
                  <th className="px-4 py-3 font-semibold">Event Type</th>
                  <th className="px-4 py-3 font-semibold">Error</th>
                  <th className="px-4 py-3 font-semibold">Retries</th>
                  <th className="px-4 py-3 font-semibold">Created</th>
                  <th className="px-4 py-3 font-semibold">Last Attempt</th>
                  <th className="px-4 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {events.map((event) => (
                  <tr key={event.eventId} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="max-w-36 truncate px-4 py-3.5 font-mono text-xs text-[var(--text-muted)]">
                      {event.eventId}
                    </td>
                    <td className="px-4 py-3.5">
                      <StatusBadge tone={eventTypeTone(event.eventType)}>
                        {eventTypeLabel(event.eventType)}
                      </StatusBadge>
                    </td>
                    <td className="max-w-64 truncate px-4 py-3.5 text-[var(--text-muted)]">
                      {event.error || "-"}
                    </td>
                    <td className="px-4 py-3.5 font-mono text-xs">
                      {event.retryCount}/{event.maxRetries}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3.5 text-xs text-[var(--text-muted)]">
                      {formatDateTime(event.createdAt)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3.5 text-xs text-[var(--text-muted)]">
                      {event.lastAttemptAt ? formatDateTime(event.lastAttemptAt) : "-"}
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="flex justify-end gap-1">
                        <button
                          type="button"
                          title="Retry this event"
                          aria-label="Retry this event"
                          disabled={retryingId === event.eventId}
                          onClick={() => void handleRetryOne(event.eventId)}
                          className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)] disabled:cursor-not-allowed disabled:opacity-35"
                        >
                          <RotateCw
                            aria-hidden="true"
                            size={16}
                            className={retryingId === event.eventId ? "animate-spin" : ""}
                          />
                        </button>
                        <button
                          type="button"
                          title="Discard this event"
                          aria-label="Discard this event"
                          onClick={() => setDiscardTarget(event)}
                          className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--danger-soft)] hover:text-[var(--danger)]"
                        >
                          <Trash2 aria-hidden="true" size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading sync queue...
          </div>
        ) : (
          <EmptyState
            icon={Database}
            title="No failed events"
            description="The dead-letter queue is empty. Failed sync events will appear here."
          />
        )}
      </section>
      <p className="mt-3 text-right text-xs text-[var(--text-muted)]">
        {events.length} event{events.length !== 1 ? "s" : ""} in queue
      </p>

      <ConfirmDialog
        busy={discarding}
        busyLabel="Discarding..."
        confirmLabel="Discard event"
        description={`This will permanently remove event "${discardTarget?.eventId}" (${discardTarget ? eventTypeLabel(discardTarget.eventType) : ""}). This action cannot be undone.`}
        onCancel={() => setDiscardTarget(null)}
        onConfirm={() => void handleDiscard()}
        open={Boolean(discardTarget)}
        title="Discard dead-letter event"
      />
    </div>
  );
}
