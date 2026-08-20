"use client";

import {
  Mail,
  Pencil,
  Phone,
  Plus,
  Search,
  UserRound,
  UserRoundPlus,
  X,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import {
  Field,
  FormError,
  Input,
  Textarea,
} from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  type Customer,
  type CustomerInput,
  operationsGateway,
} from "@/features/operations/operations-gateway";
import { ApiClientError } from "@/lib/api-client";
import { formatDate } from "@/lib/format";

const emptyInput: CustomerInput = {
  address: null,
  email: null,
  firstName: "",
  lastName: null,
  notes: null,
  phoneNumber: null,
};

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function fullName(customer: Customer) {
  return [customer.firstName, customer.lastName].filter(Boolean).join(" ");
}

export function CustomersPage() {
  const canSell = usePermission(PERMISSIONS.POS_SELL);
  const canReadSales = usePermission(PERMISSIONS.SALE_READ);
  const allowed = canSell || canReadSales;
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Customer | null>(null);
  const [draft, setDraft] = useState<CustomerInput>(emptyInput);

  const loadCustomers = useCallback(async (search = "") => {
    setLoading(true);
    try {
      setCustomers(await operationsGateway.listCustomers(search));
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Customers could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!allowed) return;
    const timer = window.setTimeout(() => {
      void loadCustomers(query);
    }, 250);
    return () => window.clearTimeout(timer);
  }, [allowed, loadCustomers, query]);

  function openCreate() {
    setEditing(null);
    setDraft(emptyInput);
    setError(null);
    setFormOpen(true);
  }

  function openEdit(customer: Customer) {
    setEditing(customer);
    setDraft({
      address: customer.address,
      email: customer.email,
      firstName: customer.firstName,
      lastName: customer.lastName,
      notes: customer.notes,
      phoneNumber: customer.phoneNumber,
    });
    setError(null);
    setFormOpen(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function closeForm() {
    setFormOpen(false);
    setEditing(null);
    setDraft(emptyInput);
  }

  function update<K extends keyof CustomerInput>(
    key: K,
    value: CustomerInput[K],
  ) {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  async function saveCustomer(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const firstName = draft.firstName.trim();
    if (!firstName) {
      setError("Enter the customer's first name.");
      return;
    }
    const normalized: CustomerInput = {
      address: draft.address?.trim() || null,
      email: draft.email?.trim() || null,
      firstName,
      lastName: draft.lastName?.trim() || null,
      notes: draft.notes?.trim() || null,
      phoneNumber: draft.phoneNumber?.trim() || null,
    };
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        await operationsGateway.updateCustomer(editing.id, normalized);
      } else {
        await operationsGateway.createCustomer(normalized);
      }
      closeForm();
      await loadCustomers(query);
    } catch (caught) {
      setError(errorMessage(caught, "The customer could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  if (!allowed) {
    return <AccessRestricted />;
  }

  return (
    <div>
      <PageHeader
        title="Customers"
        description="Maintain customer contact details and loyalty balances for assisted sales."
        actions={
          <SecondaryButton
            type="button"
            onClick={formOpen ? closeForm : openCreate}
          >
            {formOpen ? (
              <X aria-hidden="true" size={17} />
            ) : (
              <UserRoundPlus aria-hidden="true" size={17} />
            )}
            {formOpen ? "Close" : "Add customer"}
          </SecondaryButton>
        }
      />

      {formOpen ? (
        <form
          onSubmit={saveCustomer}
          className="mb-6 rounded-md border border-[var(--border)] bg-white p-4 sm:p-6"
        >
          <div className="flex items-center gap-2">
            <UserRoundPlus
              aria-hidden="true"
              className="text-[var(--brand)]"
              size={18}
            />
            <h2 className="text-base font-semibold">
              {editing ? "Edit customer" : "New customer"}
            </h2>
          </div>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Field label="First name" required>
              <Input
                autoFocus
                value={draft.firstName}
                onChange={(event) => update("firstName", event.target.value)}
                required
              />
            </Field>
            <Field label="Last name">
              <Input
                value={draft.lastName ?? ""}
                onChange={(event) => update("lastName", event.target.value)}
              />
            </Field>
            <Field label="Phone">
              <Input
                type="tel"
                value={draft.phoneNumber ?? ""}
                onChange={(event) => update("phoneNumber", event.target.value)}
              />
            </Field>
            <Field label="Email">
              <Input
                type="email"
                value={draft.email ?? ""}
                onChange={(event) => update("email", event.target.value)}
              />
            </Field>
            <Field label="Address">
              <Input
                value={draft.address ?? ""}
                onChange={(event) => update("address", event.target.value)}
              />
            </Field>
            <Field label="Notes">
              <Textarea
                className="min-h-10"
                rows={1}
                value={draft.notes ?? ""}
                onChange={(event) => update("notes", event.target.value)}
              />
            </Field>
          </div>
          <div className="mt-4">
            <FormError message={error} />
          </div>
          <div className="mt-4 flex justify-end">
            <PrimaryButton type="submit" disabled={saving}>
              {editing ? (
                <Pencil aria-hidden="true" size={17} />
              ) : (
                <Plus aria-hidden="true" size={17} />
              )}
              {saving ? "Saving..." : editing ? "Save customer" : "Add customer"}
            </PrimaryButton>
          </div>
        </form>
      ) : null}

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <label className="relative w-full max-w-xl">
          <span className="sr-only">Search customers</span>
          <Search
            aria-hidden="true"
            className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-subtle)]"
            size={17}
          />
          <Input
            className="pl-9"
            placeholder="Search name, phone, or email"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
        <span className="text-xs text-[var(--text-muted)]">
          {loading ? "Loading..." : `${customers.length} customers`}
        </span>
      </div>

      {!formOpen && error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {customers.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Customer</th>
                  <th className="px-4 py-3 font-semibold">Contact</th>
                  <th className="px-4 py-3 font-semibold">Address</th>
                  <th className="px-4 py-3 text-right font-semibold">Points</th>
                  <th className="px-4 py-3 font-semibold">Added</th>
                  <th className="w-14 px-3 py-3">
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {customers.map((customer) => (
                  <tr key={customer.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand-strong)]">
                          <UserRound aria-hidden="true" size={17} />
                        </span>
                        <div className="min-w-0">
                          <p className="font-semibold">{fullName(customer)}</p>
                          <p className="max-w-64 truncate text-xs text-[var(--text-muted)]">
                            {customer.notes || "No notes"}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      <div className="space-y-1">
                        <p className="flex items-center gap-1.5">
                          <Phone aria-hidden="true" size={13} />
                          {customer.phoneNumber || "No phone"}
                        </p>
                        <p className="flex items-center gap-1.5">
                          <Mail aria-hidden="true" size={13} />
                          {customer.email || "No email"}
                        </p>
                      </div>
                    </td>
                    <td className="max-w-56 truncate px-4 py-3 text-[var(--text-muted)]">
                      {customer.address || "Not recorded"}
                    </td>
                    <td className="px-4 py-3 text-right font-semibold">
                      {customer.loyaltyPoints}
                    </td>
                    <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                      {formatDate(customer.createdAt.slice(0, 10))}
                    </td>
                    <td className="px-3 py-3">
                      <button
                        type="button"
                        title={`Edit ${fullName(customer)}`}
                        aria-label={`Edit ${fullName(customer)}`}
                        className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-white hover:text-[var(--text)]"
                        onClick={() => openEdit(customer)}
                      >
                        <Pencil aria-hidden="true" size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading customers...
          </div>
        ) : (
          <EmptyState
            icon={UserRound}
            title={query.trim() ? "No matching customers" : "No customers yet"}
            description={
              query.trim()
                ? "Try a different name or contact detail."
                : "Add the first customer to the directory."
            }
          />
        )}
      </section>
    </div>
  );
}
