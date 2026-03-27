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
  recurring: boolean;
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
  nonCreditExpenses: number;
  creditPurchases: number;
  billPayments: number;
  cashOutflow: number;
  goalAmount: number;
  netBalance: number;
  maxCreditCardBill: number;
  fixedExpensesOutsideCredit: number;
  creditCardClosingDay: number;
  currentStatement: {
    statementMonth: string;
    dueMonth: string;
    periodStart: string;
    periodEnd: string;
    amount: number;
    paid: boolean;
    paidAt: string | null;
    paymentExpenseId: number | null;
  };
  dueStatement: {
    statementMonth: string;
    dueMonth: string;
    periodStart: string;
    periodEnd: string;
    amount: number;
    paid: boolean;
    paidAt: string | null;
    paymentExpenseId: number | null;
  };
  creditUsage: {
    method: string;
    spent: number;
    limitAmount: number;
    remaining: number;
  };
  byCategory: { category: string; color: string; total: number }[];
  byPaymentMethod: { method: string; spent: number; limitAmount: number; remaining: number }[];
  incomes: Income[];
  expenses: Expense[];
}

export interface PlanningSettings {
  reserveGoal: number;
  creditLimit: number;
  creditCardClosingDay: number;
}
