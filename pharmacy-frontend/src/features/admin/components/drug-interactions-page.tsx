"use client";

import {
  AlertTriangle,
  ArrowRightLeft,
  RefreshCw,
  Search,
  ShieldAlert,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { apiRequest, ApiClientError } from "@/lib/api-client";

interface DrugInteraction {
  id: string;
  medicineAId: string;
  medicineAName: string;
  medicineBId: string;
  medicineBName: string;
  severity: string;
  description: string;
  clinicalEffect: string;
  recommendation: string;
  createdAt: string;
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function severityTone(severity: string) {
  const normalized = severity.toUpperCase();
  if (normalized === "CONTRAINDICATED" || normalized === "HIGH") return "danger" as const;
  if (normalized === "MODERATE" || normalized === "MEDIUM") return "warning" as const;
  if (normalized === "LOW" || normalized === "MINOR") return "info" as const;
  return "neutral" as const;
}

function severityLabel(severity: string) {
  return severity
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function DrugInteractionsPage() {
  const canRead = usePermission(PERMISSIONS.MEDICINE_READ);
  const [interactions, setInteractions] = useState<DrugInteraction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [severityFilter, setSeverityFilter] = useState("ALL");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiRequest<DrugInteraction[]>("/drug-interactions");
      setInteractions(res.data);
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Drug interactions could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    void apiRequest<DrugInteraction[]>("/drug-interactions")
      .then((res) => {
        if (active) {
          setInteractions(res.data);
          setError(null);
        }
      })
      .catch((caught) => {
        if (active) setError(errorMessage(caught, "Drug interactions could not be loaded."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canRead]);

  const severities = useMemo(
    () => [...new Set(interactions.map((i) => i.severity))].sort(),
    [interactions],
  );

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return interactions.filter((entry) => {
      const matchesSeverity =
        severityFilter === "ALL" || entry.severity === severityFilter;
      const matchesQuery =
        !normalized ||
        [
          entry.medicineAName,
          entry.medicineBName,
          entry.description,
          entry.clinicalEffect,
          entry.recommendation,
        ].some((value) => value?.toLowerCase().includes(normalized));
      return matchesSeverity && matchesQuery;
    });
  }, [interactions, query, severityFilter]);

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      <PageHeader
        title="Drug interactions"
        description="Review known drug interactions and their clinical severity."
        actions={
          <SecondaryButton
            type="button"
            title="Refresh interactions"
            aria-label="Refresh interactions"
            className="px-3"
            disabled={loading}
            onClick={() => void load()}
          >
            <RefreshCw aria-hidden="true" size={17} />
          </SecondaryButton>
        }
      />

      <section className="mb-5 rounded-md border border-[var(--border)] bg-white p-4">
        <div className="grid gap-3 md:grid-cols-[minmax(260px,1fr)_200px]">
          <label className="relative">
            <span className="sr-only">Search interactions</span>
            <Search
              aria-hidden="true"
              className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]"
              size={17}
            />
            <Input
              className="pl-9"
              placeholder="Search by medicine name, description, or recommendation"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
          <label>
            <span className="sr-only">Filter by severity</span>
            <Select
              value={severityFilter}
              onChange={(event) => setSeverityFilter(event.target.value)}
            >
              <option value="ALL">All severities</option>
              {severities.map((value) => (
                <option key={value} value={value}>
                  {severityLabel(value)}
                </option>
              ))}
            </Select>
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
            <table className="w-full min-w-[1000px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Medicine A</th>
                  <th className="px-4 py-3 font-semibold">Medicine B</th>
                  <th className="px-4 py-3 font-semibold">Severity</th>
                  <th className="px-4 py-3 font-semibold">Clinical Effect</th>
                  <th className="px-4 py-3 font-semibold">Description</th>
                  <th className="px-4 py-3 font-semibold">Recommendation</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {filtered.map((entry) => (
                  <tr key={entry.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3.5 font-semibold">{entry.medicineAName}</td>
                    <td className="px-4 py-3.5 font-semibold">{entry.medicineBName}</td>
                    <td className="px-4 py-3.5">
                      <StatusBadge tone={severityTone(entry.severity)}>
                        {severityLabel(entry.severity)}
                      </StatusBadge>
                    </td>
                    <td className="max-w-48 truncate px-4 py-3.5 text-[var(--text-muted)]">
                      {entry.clinicalEffect || "-"}
                    </td>
                    <td className="max-w-56 truncate px-4 py-3.5 text-[var(--text-muted)]">
                      {entry.description || "-"}
                    </td>
                    <td className="max-w-56 truncate px-4 py-3.5 text-[var(--text-muted)]">
                      {entry.recommendation || "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading drug interactions...
          </div>
        ) : (
          <EmptyState
            icon={ShieldAlert}
            title={interactions.length ? "No matching interactions" : "No drug interactions"}
            description={
              interactions.length
                ? "Adjust the search or severity filter."
                : "Drug interactions will appear here."
            }
          />
        )}
      </section>
      <p className="mt-3 text-right text-xs text-[var(--text-muted)]">
        {filtered.length} of {interactions.length} interactions
      </p>
    </div>
  );
}
