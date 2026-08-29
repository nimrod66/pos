"use client";

import { ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";

import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/form-controls";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { apiRequest } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

interface ControlledDrugRecord {
  id: string;
  medicineId: string;
  medicineName: string;
  prescriptionId: string | null;
  prescriptionNumber: string | null;
  userId: string;
  userName: string;
  quantityDispensed: number;
  createdAt: string;
}

export default function ControlledDrugsPage() {
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const [records, setRecords] = useState<ControlledDrugRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  async function load() {
    setLoading(true);
    try {
      const response = await apiRequest<ControlledDrugRecord[]>(
        "/controlled-drugs",
      );
      setRecords(response.data ?? []);
      setError(null);
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Failed to load",
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const t = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(t);
  }, []);

  const normalized = query.trim().toLowerCase();
  const filtered = normalized
    ? records.filter((r) =>
        [r.medicineName, r.userName, r.prescriptionNumber ?? ""]
          .join(" ")
          .toLowerCase()
          .includes(normalized),
      )
    : records;

  return (
    <div className="max-w-7xl">
      <PageHeader
        eyebrow="Regulatory"
        title="Controlled drugs register"
        description="Audit trail of all controlled substance dispensing linked to prescriptions and sales."
      />

      {error ? (
        <div className="mb-5 rounded-md border border-[var(--danger-border)] bg-[var(--danger-soft)] px-3 py-2.5 text-sm text-[var(--danger)]">
          {error}
        </div>
      ) : null}

      <label className="relative block max-w-md">
        <span className="sr-only">Search controlled drugs</span>
        <Input
          className="pl-9"
          placeholder="Search medicine, pharmacist, or prescription"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </label>

      {loading ? (
        <p className="mt-6 text-sm text-[var(--text-muted)]">
          Loading controlled drugs register...
        </p>
      ) : filtered.length ? (
        <div className="mt-4 overflow-hidden rounded-md border border-[var(--border)] bg-white">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Medicine</th>
                  <th className="px-4 py-3 font-semibold">Rx #</th>
                  <th className="px-4 py-3 font-semibold">Dispensed by</th>
                  <th className="px-4 py-3 text-right font-semibold">Qty</th>
                  <th className="px-4 py-3 font-semibold">Recorded</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {filtered.map((record) => {
                  const medicine = medicines.find(
                    (m) => m.id === record.medicineId,
                  );
                  return (
                    <tr key={record.id}>
                      <td className="px-4 py-3">
                        <p className="font-semibold">
                          {medicine?.brandName ?? record.medicineName}
                        </p>
                        <p className="text-xs text-[var(--text-muted)]">
                          {medicine?.genericName}
                        </p>
                      </td>
                      <td className="px-4 py-3 font-mono text-xs">
                        {record.prescriptionNumber ?? "—"}
                      </td>
                      <td className="px-4 py-3 text-[var(--text-muted)]">
                        {record.userName}
                      </td>
                      <td className="px-4 py-3 text-right font-semibold">
                        {record.quantityDispensed}
                      </td>
                      <td className="px-4 py-3 text-[var(--text-muted)]">
                        {formatDateTime(record.createdAt)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="mt-5 rounded-md border border-[var(--border)] bg-white">
          <EmptyState
            icon={ShieldCheck}
            title="No controlled drug records"
            description={
              query
                ? "Try a different search."
                : "Controlled substance dispensing records will appear here automatically when sales include controlled medicines."
            }
          />
        </div>
      )}
    </div>
  );
}
