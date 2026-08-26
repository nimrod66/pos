export interface ExpenseEntry {
  id: string;
  expenseCategoryId: string | null;
  categoryName: string | null;
  cashDrawersId: string | null;
  userId: string | null;
  userName: string | null;
  amount: number;
  description: string | null;
  expenseDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExpenseCategory {
  id: string;
  categoryName: string;
  categoryDescription: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExpenseInput {
  expenseCategoryId: string;
  amount: number;
  description?: string;
  userId: string;
  cashDrawersId?: string;
  expenseDate?: string;
}

export interface UpdateExpenseInput {
  expenseCategoryId?: string;
  amount?: number;
  description?: string;
  userId?: string;
  expenseDate?: string;
}

export interface CreateCategoryInput {
  categoryName: string;
  categoryDescription?: string;
}
