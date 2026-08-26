"use client";

import { useEffect, useMemo, useState } from "react";
import {
  DollarSign,
  Pencil,
  Plus,
  Trash2,
  X,
} from "lucide-react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { EmptyState } from "@/components/ui/empty-state";
import { FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { formatKes } from "@/features/workspace/lib/money";
import { expensesGateway } from "@/features/expenses/expenses-gateway";
import type {
  ExpenseCategory,
  ExpenseEntry,
} from "@/features/expenses/types";

export function ExpensesPage() {
  const canWrite = usePermission(PERMISSIONS.EXPENSE_WRITE);
  const canRead = usePermission(PERMISSIONS.EXPENSE_READ);
  const userId = useAuthStore((s) => s.session?.user.id ?? "");

  const [expenses, setExpenses] = useState<ExpenseEntry[]>([]);
  const [categories, setCategories] = useState<ExpenseCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<ExpenseEntry | null>(null);

  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [expenseDate, setExpenseDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [submitting, setSubmitting] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    async function run() {
      setLoading(true);
      try {
        const [exp, cats] = await Promise.all([
          expensesGateway.listExpenses(),
          expensesGateway.listCategories(),
        ]);
        if (!active) return;
        setExpenses(exp);
        setCategories(cats);
        setError(null);
      } catch {
        if (!active) return;
        setError("Failed to load expenses.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void run();
    return () => {
      active = false;
    };
  }, [canRead]);

  async function reload() {
    try {
      const [exp, cats] = await Promise.all([
        expensesGateway.listExpenses(),
        expensesGateway.listCategories(),
      ]);
      setExpenses(exp);
      setCategories(cats);
      setError(null);
    } catch {
      setError("Failed to load expenses.");
    }
  }

  const categoryMap = useMemo(
    () => new Map(categories.map((c) => [c.id, c.categoryName])),
    [categories],
  );

  function resetForm() {
    setAmount("");
    setDescription("");
    setCategoryId("");
    setExpenseDate(new Date().toISOString().slice(0, 10));
    setEditing(null);
    setShowForm(false);
  }

  function startEdit(expense: ExpenseEntry) {
    setEditing(expense);
    setAmount(String(expense.amount));
    setDescription(expense.description ?? "");
    setCategoryId(expense.expenseCategoryId ?? "");
    setExpenseDate(expense.expenseDate?.slice(0, 10) ?? new Date().toISOString().slice(0, 10));
    setShowForm(true);
  }

  async function handleSubmit() {
    const parsed = parseFloat(amount);
    if (isNaN(parsed) || parsed <= 0) {
      setError("Enter a valid positive amount.");
      return;
    }
    if (!categoryId) {
      setError("Select a category.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (editing) {
        await expensesGateway.updateExpense(editing.id, {
          expenseCategoryId: categoryId,
          amount: parsed,
          description: description || undefined,
          userId,
        });
      } else {
        await expensesGateway.createExpense({
          expenseCategoryId: categoryId,
          amount: parsed,
          description: description || undefined,
          userId,
          cashDrawersId: undefined,
        });
      }
      resetForm();
      await reload();
    } catch {
      setError("Failed to save expense.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!deleteId) return;
    try {
      await expensesGateway.deleteExpense(deleteId);
      setDeleteId(null);
      await reload();
    } catch {
      setError("Failed to delete expense.");
    }
  }

  if (!canRead) return <AccessRestricted />;

  const total = expenses.reduce((sum, e) => sum + Number(e.amount), 0);

  return (
    <div>
      <PageHeader
        title="Expenses"
        description="Track and manage pharmacy expenses."
        actions={
          canWrite ? (
            <PrimaryButton
              type="button"
              onClick={() => {
                resetForm();
                setShowForm(true);
              }}
            >
              <Plus aria-hidden="true" size={16} /> Add expense
            </PrimaryButton>
          ) : undefined
        }
      />

      {error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}

      {/* Summary */}
      <div className="mb-4 rounded-md border border-[var(--border)] bg-white p-4">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-md bg-[var(--danger-soft)] text-[var(--danger)]">
            <DollarSign size={18} />
          </div>
          <div>
            <p className="text-xs text-[var(--text-muted)]">Total expenses</p>
            <p className="text-xl font-semibold">{formatKes(total)}</p>
          </div>
          <span className="ml-auto text-xs text-[var(--text-muted)]">
            {expenses.length} records
          </span>
        </div>
      </div>

      {/* Inline form */}
      {showForm ? (
        <div className="mb-5 rounded-md border border-[var(--brand)] bg-white p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold">
              {editing ? "Edit expense" : "New expense"}
            </h3>
            <button
              type="button"
              onClick={resetForm}
              className="text-[var(--text-muted)] hover:text-[var(--danger)]"
            >
              <X size={18} />
            </button>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Amount (KES)</span>
              <Input
                type="number"
                min="0.01"
                step="0.01"
                inputMode="decimal"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0.00"
              />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Category</span>
              <Select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
                <option value="">Select category</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.categoryName}
                  </option>
                ))}
              </Select>
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Date</span>
              <Input
                type="date"
                value={expenseDate}
                onChange={(e) => setExpenseDate(e.target.value)}
              />
            </label>
            <label className="text-xs font-medium text-[var(--text-muted)]">
              <span className="mb-1 block">Description</span>
              <Input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What was this expense for?"
              />
            </label>
          </div>
          <div className="mt-3 flex gap-2">
            <PrimaryButton
              type="button"
              onClick={() => void handleSubmit()}
              disabled={submitting}
            >
              {submitting ? "Saving..." : editing ? "Update" : "Record expense"}
            </PrimaryButton>
            <SecondaryButton type="button" onClick={resetForm}>
              Cancel
            </SecondaryButton>
          </div>
        </div>
      ) : null}

      {/* Table */}
      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading expenses...
          </div>
        ) : expenses.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[700px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Date</th>
                  <th className="px-4 py-3 font-semibold">Category</th>
                  <th className="px-4 py-3 font-semibold">Description</th>
                  <th className="px-4 py-3 text-right font-semibold">Amount</th>
                  <th className="px-4 py-3 font-semibold">Recorded by</th>
                  {canWrite ? <th className="px-4 py-3 font-semibold">Actions</th> : null}
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {expenses.map((expense) => (
                  <tr key={expense.id} className="hover:bg-[var(--surface-muted)]/60">
                    <td className="whitespace-nowrap px-4 py-3 text-xs text-[var(--text-muted)]">
                      {expense.expenseDate
                        ? new Date(expense.expenseDate).toLocaleDateString()
                        : "-"}
                    </td>
                    <td className="px-4 py-3 font-medium">
                      {expense.expenseCategoryId ? (categoryMap.get(expense.expenseCategoryId) ?? "Uncategorized") : "Uncategorized"}
                    </td>
                    <td className="max-w-52 truncate px-4 py-3 text-[var(--text-muted)]">
                      {expense.description || "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-semibold text-[var(--danger)]">
                      {formatKes(expense.amount)}
                    </td>
                    <td className="px-4 py-3 text-[var(--text-muted)]">
                      {expense.userName ?? "Unknown"}
                    </td>
                    {canWrite ? (
                      <td className="px-4 py-3">
                        <div className="flex gap-1">
                          <button
                            type="button"
                            onClick={() => startEdit(expense)}
                            className="rounded p-1 text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--brand)]"
                            title="Edit"
                          >
                            <Pencil size={15} />
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeleteId(expense.id)}
                            className="rounded p-1 text-[var(--text-muted)] hover:bg-[var(--danger-soft)] hover:text-[var(--danger)]"
                            title="Delete"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={DollarSign}
            title="No expenses recorded"
            description="Track your pharmacy expenses to understand your cash flow."
          />
        )}
      </section>

      {/* Delete confirmation */}
      {deleteId ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-sm rounded-md bg-white p-5 shadow-lg">
            <h3 className="text-sm font-semibold">Delete expense?</h3>
            <p className="mt-2 text-sm text-[var(--text-muted)]">
              This action cannot be undone.
            </p>
            <div className="mt-4 flex gap-2">
              <PrimaryButton
                type="button"
                onClick={() => void handleDelete()}
                className="bg-[var(--danger)] hover:bg-[var(--danger)]/90"
              >
                Delete
              </PrimaryButton>
              <SecondaryButton type="button" onClick={() => setDeleteId(null)}>
                Cancel
              </SecondaryButton>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
