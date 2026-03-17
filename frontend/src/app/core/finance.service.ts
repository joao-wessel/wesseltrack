import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DashboardSummary, Category, Expense, Income, ManagedUser, MonthlyCloseResponse } from './models';

@Injectable({ providedIn: 'root' })
export class FinanceService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private readonly http: HttpClient) {}

  getDashboard(month: string) {
    return this.http.get<DashboardSummary>(`${this.apiUrl}/dashboard/monthly`, { params: { month } });
  }

  getCategories() {
    return this.http.get<Category[]>(`${this.apiUrl}/categories`);
  }

  createCategory(payload: { name: string; color: string }) {
    return this.http.post<Category>(`${this.apiUrl}/categories`, payload);
  }

  updateCategory(id: number, payload: { name: string; color: string }) {
    return this.http.put<Category>(`${this.apiUrl}/categories/${id}`, payload);
  }

  deleteCategory(id: number) {
    return this.http.delete<void>(`${this.apiUrl}/categories/${id}`);
  }

  getIncomes(month: string) {
    return this.http.get<Income[]>(`${this.apiUrl}/incomes`, { params: { month } });
  }

  createIncome(payload: { description: string; amount: number; receiveDate: string; expectedDay: number }) {
    return this.http.post<Income>(`${this.apiUrl}/incomes`, payload);
  }

  updateIncome(id: number, payload: { description: string; amount: number; receiveDate: string; expectedDay: number }) {
    return this.http.put<Income>(`${this.apiUrl}/incomes/${id}`, payload);
  }

  deleteIncome(id: number) {
    return this.http.delete<void>(`${this.apiUrl}/incomes/${id}`);
  }

  getExpenses(month: string) {
    return this.http.get<Expense[]>(`${this.apiUrl}/expenses`, { params: { month } });
  }

  createExpense(payload: {
    description: string;
    categoryId: number;
    type: 'FIXED' | 'VARIABLE' | 'INSTALLMENT';
    paymentMethod: 'DEBIT' | 'CREDIT' | 'CASH' | 'PIX';
    paymentSource: string;
    amount: number;
    dueDate: string;
    recurring: boolean;
    installmentCount?: number;
  }) {
    return this.http.post<Expense[]>(`${this.apiUrl}/expenses`, payload);
  }

  updateExpense(id: number, payload: {
    description: string;
    categoryId: number;
    type: 'FIXED' | 'VARIABLE' | 'INSTALLMENT';
    paymentMethod: 'DEBIT' | 'CREDIT' | 'CASH' | 'PIX';
    paymentSource: string;
    amount: number;
    dueDate: string;
    recurring: boolean;
    installmentCount?: number;
  }) {
    return this.http.put<Expense>(`${this.apiUrl}/expenses/${id}`, payload);
  }

  deleteExpense(id: number) {
    return this.http.delete<void>(`${this.apiUrl}/expenses/${id}`);
  }

  saveGoal(payload: { month: string; amount: number }) {
    return this.http.post<number>(`${this.apiUrl}/goals`, payload);
  }

  closeMonth(month: string) {
    return this.http.post<MonthlyCloseResponse>(`${this.apiUrl}/monthly-close`, null, { params: { month } });
  }

  getUsers() {
    return this.http.get<ManagedUser[]>(`${this.apiUrl}/users`);
  }

  createUser(payload: { name: string; username: string; password: string; role: 'ADMIN' | 'USER' }) {
    return this.http.post<ManagedUser>(`${this.apiUrl}/users`, payload);
  }
}
