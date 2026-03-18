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

export interface UserFormPayload {
  name: string;
  username: string;
  password: string;
  role: 'ADMIN' | 'USER';
}

export interface UserUpdatePayload {
  name: string;
  username: string;
  password?: string;
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
  creditUsage: PaymentMethodUsage;
  byCategory: { category: string; color: string; total: number }[];
  byPaymentMethod: PaymentMethodUsage[];
  incomes: Income[];
  expenses: Expense[];
}

export interface PaymentMethodUsage {
  method: string;
  spent: number;
  limitAmount: number;
  remaining: number;
}

export interface MonthlyPlanning {
  month: string;
  goalAmount: number;
  creditLimit: number;
  debitLimit: number;
  pixLimit: number;
  cashLimit: number;
}

export interface MonthlyCloseResponse {
  sourceMonth: string;
  targetMonth: string;
  createdExpenses: number;
}
