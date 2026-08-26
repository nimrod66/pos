"use client";

import { apiRequest } from "@/lib/api-client";
import type {
  CreateCategoryInput,
  CreateExpenseInput,
  ExpenseCategory,
  ExpenseEntry,
  UpdateExpenseInput,
} from "./types";

async function getAll<T>(url: `/${string}`): Promise<T[]> {
  const response = await apiRequest<{ data: T[]; pagination: { totalElements: number } }>(url, {
    cache: "no-store",
  });
  return response.data.data ?? [];
}

export const expensesGateway = {
  async listExpenses(): Promise<ExpenseEntry[]> {
    return getAll<ExpenseEntry>("/expenses?size=500&sort=expenseDate,desc");
  },

  async createExpense(input: CreateExpenseInput): Promise<ExpenseEntry> {
    const response = await apiRequest<ExpenseEntry>("/expenses", {
      method: "POST",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  async updateExpense(id: string, input: UpdateExpenseInput): Promise<ExpenseEntry> {
    const response = await apiRequest<ExpenseEntry>(`/expenses/${id}`, {
      method: "PUT",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  async deleteExpense(id: string): Promise<void> {
    await apiRequest(`/expenses/${id}`, { method: "DELETE" });
  },

  async listCategories(): Promise<ExpenseCategory[]> {
    return getAll<ExpenseCategory>("/expense-categories?size=500");
  },

  async createCategory(input: CreateCategoryInput): Promise<ExpenseCategory> {
    const response = await apiRequest<ExpenseCategory>("/expense-categories", {
      method: "POST",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  async deleteCategory(id: string): Promise<void> {
    await apiRequest(`/expense-categories/${id}`, { method: "DELETE" });
  },
};
