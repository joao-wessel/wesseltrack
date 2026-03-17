export interface UserProfile {
  id: number;
  name: string;
  username: string;
  role: 'ADMIN' | 'USER';
}

export interface ManagedUser {
  id: number;
  name: string;
  username: string;
  role: 'ADMIN' | 'USER';
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserProfile;
}

export interface Category {
  id: number;
  name: string;
  color: string;
}

export interface Income {
  id: number;
  description: string;
  amount: number;
  receiveDate: string;
  expectedDay: number;
}

export type ExpenseType = 'FIXED' | 'VARIABLE' | 'INSTALLMENT';
export type PaymentMethod = 'DEBIT' | 'CREDIT' | 'CASH' | 'PIX';

export interface Expense {
  id: number;
  description: string;
  category: string;
  categoryColor: string;
  type: ExpenseType;
  paymentMethod: PaymentMethod;
  paymentSource: string;
  amount: number;
  originalAmount: number;
  dueDate: string;
  recurring: boolean;
  installmentNumber: number;
  installmentCount: number;
}

export interface DashboardSummary {
  month: string;
  totalIncome: number;
  totalExpenses: number;
  goalAmount: number;
  netBalance: number;
  debitExpenses: number;
  creditExpenses: number;
  byCategory: { category: string; color: string; total: number }[];
  byPaymentSource: { source: string; total: number }[];
  incomes: Income[];
  expenses: Expense[];
}

export interface MonthlyCloseResponse {
  sourceMonth: string;
  targetMonth: string;
  createdExpenses: number;
}
