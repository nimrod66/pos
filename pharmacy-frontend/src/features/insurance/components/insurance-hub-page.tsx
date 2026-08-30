"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { PageHeader } from "@/components/ui/page-header";
import { cn } from "@/lib/cn";
import { InsurersPage } from "@/features/insurance/components/insurers-page";
import { SchemesPage } from "@/features/insurance/components/schemes-page";
import { ClaimsPage } from "@/features/insurance/components/claims-page";
import { BatchesPage } from "@/features/insurance/components/batches-page";
import { PaymentsPage } from "@/features/insurance/components/payments-page";
import { ReconciliationPage } from "@/features/insurance/components/reconciliation-page";

type InsuranceTab =
  | "insurers"
  | "schemes"
  | "claims"
  | "batches"
  | "payments"
  | "reconciliation";

const tabs: Array<{ id: InsuranceTab; label: string }> = [
  { id: "insurers", label: "Insurers" },
  { id: "schemes", label: "Schemes" },
  { id: "claims", label: "Claims" },
  { id: "batches", label: "Batches" },
  { id: "payments", label: "Payments" },
  { id: "reconciliation", label: "Reconciliation" },
];

export function InsuranceHubPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const requestedTab = searchParams.get("tab");
  const [tab, setTab] = useState<InsuranceTab>(
    tabs.some((item) => item.id === requestedTab)
      ? (requestedTab as InsuranceTab)
      : "insurers",
  );

  function selectTab(id: InsuranceTab) {
    setTab(id);
    router.replace(`/insurance?tab=${id}`);
  }

  return (
    <div>
      <PageHeader
        title="Insurance"
        description="Manage insurers, schemes, claims, batches, payments, and reconciliation in one place."
      />

      <div className="border-b border-[var(--border)]">
        <div
          className="flex overflow-x-auto"
          role="tablist"
          aria-label="Insurance views"
        >
          {tabs.map((item) => (
            <button
              type="button"
              role="tab"
              aria-selected={tab === item.id}
              key={item.id}
              onClick={() => selectTab(item.id)}
              className={cn(
                "h-9 shrink-0 border-b-2 px-3 text-sm font-medium",
                tab === item.id
                  ? "border-[var(--brand)] text-[var(--brand-strong)]"
                  : "border-transparent text-[var(--text-muted)] hover:text-[var(--text)]",
              )}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-5">
        {tab === "insurers" ? <InsurersPage showHeader={false} /> : null}
        {tab === "schemes" ? <SchemesPage showHeader={false} /> : null}
        {tab === "claims" ? <ClaimsPage showHeader={false} /> : null}
        {tab === "batches" ? <BatchesPage showHeader={false} /> : null}
        {tab === "payments" ? <PaymentsPage showHeader={false} /> : null}
        {tab === "reconciliation" ? (
          <ReconciliationPage showHeader={false} />
        ) : null}
      </div>
    </div>
  );
}
