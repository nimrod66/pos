"use client";

import {
  ClipboardPlus,
  FileText,
  Plus,
  Search,
  ShieldCheck,
  ShoppingCart,
  Trash2,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useCartStore } from "@/features/pos/store/cart-store";
import {
  type Prescription,
  prescriptionGateway,
} from "@/features/prescriptions/prescription-gateway";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { ApiClientError } from "@/lib/api-client";
import { cn } from "@/lib/cn";

interface DraftItem {
  id: string;
  dosage: string;
  medicineId: string;
  quantity: number;
}

function newItem(): DraftItem {
  return { dosage: "", id: crypto.randomUUID(), medicineId: "", quantity: 1 };
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-KE", {
    dateStyle: "medium",
    timeZone: "Africa/Nairobi",
  }).format(new Date(`${value.slice(0, 10)}T12:00:00+03:00`));
}

function errorMessage(error: unknown) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : "The prescription could not be saved.";
}

export function PrescriptionsPage() {
  const router = useRouter();
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const canApprove = usePermission(PERMISSIONS.PRESCRIPTION_APPROVE);
  const canSell = usePermission(PERMISSIONS.POS_SELL);
  const setPrescriptionReferenceId = useCartStore(
    (state) => state.setPrescriptionReferenceId,
  );
  const [rows, setRows] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [view, setView] = useState<"records" | "medicines">("records");
  const [customerName, setCustomerName] = useState("");
  const [doctorName, setDoctorName] = useState("");
  const [doctorLicenseNumber, setDoctorLicenseNumber] = useState("");
  const [hospitalName, setHospitalName] = useState("");
  const [prescriptionNumber, setPrescriptionNumber] = useState("");
  const [diagnosis, setDiagnosis] = useState("");
  const [issuedDate, setIssuedDate] = useState(today());
  const [items, setItems] = useState<DraftItem[]>([newItem()]);

  const rxMedicines = useMemo(
    () => medicines.filter((medicine) => medicine.prescriptionRequired),
    [medicines],
  );
  const filteredRows = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return rows;
    return rows.filter((row) =>
      [row.prescriptionNumber, row.customerName, row.doctorName]
        .join(" ")
        .toLowerCase()
        .includes(normalized),
    );
  }, [query, rows]);

  async function load() {
    setLoading(true);
    try {
      setRows(await prescriptionGateway.list());
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const initial = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(initial);
  }, []);

  function resetForm() {
    setCustomerName("");
    setDoctorName("");
    setDoctorLicenseNumber("");
    setHospitalName("");
    setPrescriptionNumber("");
    setDiagnosis("");
    setIssuedDate(today());
    setItems([newItem()]);
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canApprove) return;
    if (items.some((item) => !item.medicineId || item.quantity < 1)) {
      setError("Choose a medicine and quantity for every prescription line.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await prescriptionGateway.create({
        customerName: customerName.trim(),
        diagnosis: diagnosis.trim() || null,
        doctorLicenseNumber: doctorLicenseNumber.trim(),
        doctorName: doctorName.trim(),
        hospitalName: hospitalName.trim() || null,
        issuedDate,
        items: items.map((item) => ({
          dosage: item.dosage.trim() || null,
          medicineId: item.medicineId,
          quantity: item.quantity,
        })),
        prescriptionNumber: prescriptionNumber.trim(),
      });
      resetForm();
      setFormOpen(false);
      await load();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  function sendToPos(id: string) {
    setPrescriptionReferenceId(id);
    router.push("/pos");
  }

  async function markDispensed(id: string) {
    setError(null);
    try {
      await prescriptionGateway.dispense(id);
      await load();
    } catch (caught) {
      setError(errorMessage(caught));
    }
  }

  return (
    <div className="max-w-7xl">
      <PageHeader
        eyebrow="Clinical workspace"
        title="Prescriptions"
        description="Review active prescriptions and the medicines that require pharmacist control."
        actions={
          canApprove ? (
            <PrimaryButton type="button" onClick={() => setFormOpen((open) => !open)}>
              <ClipboardPlus aria-hidden="true" size={16} />
              {formOpen ? "Close form" : "New prescription"}
            </PrimaryButton>
          ) : undefined
        }
      />

      {error ? <div className="mb-5"><FormError message={error} /></div> : null}

      <div className="mb-5 flex w-full max-w-md rounded-md bg-[var(--surface-muted)] p-1" role="tablist" aria-label="Prescription views">
        <button type="button" role="tab" aria-selected={view === "records"} onClick={() => setView("records")} className={cn("h-9 flex-1 rounded text-sm font-semibold", view === "records" ? "bg-white text-[var(--text)] shadow-sm" : "text-[var(--text-muted)]")}>Prescriptions</button>
        <button type="button" role="tab" aria-selected={view === "medicines"} onClick={() => setView("medicines")} className={cn("h-9 flex-1 rounded text-sm font-semibold", view === "medicines" ? "bg-white text-[var(--text)] shadow-sm" : "text-[var(--text-muted)]")}>Rx medicines</button>
      </div>

      {formOpen ? (
        <form onSubmit={submit} className="mb-6 border-y border-[var(--border)] bg-white py-5">
          <div className="grid gap-4 px-4 sm:grid-cols-2 lg:grid-cols-4">
            <Field label="Patient name" required><Input required value={customerName} onChange={(event) => setCustomerName(event.target.value)} /></Field>
            <Field label="Prescription number" required><Input required autoCapitalize="characters" value={prescriptionNumber} onChange={(event) => setPrescriptionNumber(event.target.value.toUpperCase())} /></Field>
            <Field label="Doctor" required><Input required value={doctorName} onChange={(event) => setDoctorName(event.target.value)} /></Field>
            <Field label="License number" required><Input required value={doctorLicenseNumber} onChange={(event) => setDoctorLicenseNumber(event.target.value)} /></Field>
            <Field label="Hospital or clinic"><Input value={hospitalName} onChange={(event) => setHospitalName(event.target.value)} /></Field>
            <Field label="Issued date" required><Input required type="date" max={today()} value={issuedDate} onChange={(event) => setIssuedDate(event.target.value)} /></Field>
            <div className="sm:col-span-2"><Field label="Diagnosis"><Input value={diagnosis} onChange={(event) => setDiagnosis(event.target.value)} /></Field></div>
          </div>
          <div className="mt-5 border-t border-[var(--border)] px-4 pt-5">
            <div className="mb-3 flex items-center justify-between gap-3">
              <h2 className="text-sm font-semibold">Prescription items</h2>
              <SecondaryButton type="button" onClick={() => setItems((current) => [...current, newItem()])}><Plus aria-hidden="true" size={15} /> Add line</SecondaryButton>
            </div>
            <div className="space-y-3">
              {items.map((item, index) => (
                <div key={item.id} className="grid items-end gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(160px,0.45fr)_110px_40px]">
                  <Field label={`Medicine ${index + 1}`} required>
                    <Select required value={item.medicineId} onChange={(event) => setItems((current) => current.map((candidate) => candidate.id === item.id ? { ...candidate, medicineId: event.target.value } : candidate))}>
                      <option value="">Select medicine</option>
                      {rxMedicines.map((medicine) => <option key={medicine.id} value={medicine.id}>{medicine.brandName} ({medicine.genericName})</option>)}
                    </Select>
                  </Field>
                  <Field label="Dosage"><Input placeholder="e.g. 1 tablet twice daily" value={item.dosage} onChange={(event) => setItems((current) => current.map((candidate) => candidate.id === item.id ? { ...candidate, dosage: event.target.value } : candidate))} /></Field>
                  <Field label="Quantity" required><Input required type="number" min={1} step={1} value={item.quantity} onChange={(event) => setItems((current) => current.map((candidate) => candidate.id === item.id ? { ...candidate, quantity: Number(event.target.value) } : candidate))} /></Field>
                  <button type="button" title="Remove line" aria-label={`Remove prescription line ${index + 1}`} disabled={items.length === 1} onClick={() => setItems((current) => current.filter((candidate) => candidate.id !== item.id))} className="flex size-10 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)] disabled:opacity-30"><Trash2 aria-hidden="true" size={16} /></button>
                </div>
              ))}
            </div>
            <div className="mt-5 flex justify-end"><PrimaryButton disabled={saving || rxMedicines.length === 0} type="submit"><ShieldCheck aria-hidden="true" size={16} />{saving ? "Saving..." : "Approve prescription"}</PrimaryButton></div>
          </div>
        </form>
      ) : null}

      {view === "records" ? (
        <section>
          <label className="relative block max-w-md">
            <span className="sr-only">Search prescriptions</span>
            <Search aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]" size={17} />
            <Input className="pl-9" placeholder="Search patient, doctor, or reference" value={query} onChange={(event) => setQuery(event.target.value)} />
          </label>
          {loading ? <p className="mt-6 text-sm text-[var(--text-muted)]">Loading prescriptions...</p> : filteredRows.length ? (
            <div className="mt-4 overflow-hidden rounded-md border border-[var(--border)] bg-white">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] text-left text-sm">
                  <thead className="bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]"><tr><th className="px-4 py-3 font-semibold">Reference</th><th className="px-4 py-3 font-semibold">Patient</th><th className="px-4 py-3 font-semibold">Prescriber</th><th className="px-4 py-3 font-semibold">Issued</th><th className="px-4 py-3 font-semibold">Items</th><th className="px-4 py-3 font-semibold">Status</th><th className="px-4 py-3"><span className="sr-only">Actions</span></th></tr></thead>
                  <tbody className="divide-y divide-[var(--border)]">
                    {filteredRows.map((row) => <tr key={row.id}><td className="px-4 py-3 font-mono text-xs font-semibold">{row.prescriptionNumber}</td><td className="px-4 py-3 font-medium">{row.customerName}</td><td className="px-4 py-3"><span className="block">{row.doctorName}</span><span className="text-xs text-[var(--text-muted)]">{row.doctorLicenseNumber}</span></td><td className="px-4 py-3 text-[var(--text-muted)]">{formatDate(row.issuedDate)}</td><td className="px-4 py-3">{row.items.length}</td><td className="px-4 py-3"><StatusBadge tone={row.status === "ACTIVE" ? "success" : "neutral"}>{row.status.toLowerCase()}</StatusBadge></td><td className="px-4 py-3 text-right"><div className="flex justify-end gap-2">{row.status === "ACTIVE" && canSell && canApprove ? <SecondaryButton type="button" onClick={() => sendToPos(row.id)}><ShoppingCart aria-hidden="true" size={15} /> Use in POS</SecondaryButton> : null}{row.status === "ACTIVE" && canApprove ? <SecondaryButton type="button" title="Mark this prescription as dispensed without a sale" onClick={() => void markDispensed(row.id)}>Mark dispensed</SecondaryButton> : null}</div></td></tr>)}
                  </tbody>
                </table>
              </div>
            </div>
          ) : <div className="mt-5 rounded-md border border-[var(--border)] bg-white"><EmptyState icon={FileText} title="No prescriptions found" description={query ? "Try a different search." : "Approved prescriptions will appear here."} /></div>}
        </section>
      ) : (
        <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
          {rxMedicines.length ? <div className="divide-y divide-[var(--border)]">{rxMedicines.map((medicine) => <div key={medicine.id} className="grid gap-2 px-4 py-3 sm:grid-cols-[minmax(0,1fr)_160px_100px] sm:items-center"><div><p className="font-semibold">{medicine.brandName}</p><p className="mt-0.5 text-xs text-[var(--text-muted)]">{medicine.genericName} - {medicine.sku}</p></div><span className="text-xs text-[var(--text-muted)]">{medicine.manufacturer}</span><StatusBadge tone="warning">Rx required</StatusBadge></div>)}</div> : <EmptyState icon={ShieldCheck} title="No prescription medicines" description="Medicines marked as prescription-required will appear here." />}
        </section>
      )}
    </div>
  );
}
