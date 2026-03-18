import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FinanceService } from '../../core/finance.service';
import { ToastService } from '../../core/toast.service';
import { Category, Expense, PaymentMethod } from '../../core/models';

@Component({
  selector: 'app-expenses-page',
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './expenses-page.component.html',
  styleUrl: './expenses-page.component.scss'
})
export class ExpensesPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);

  readonly month = signal(this.toYearMonth(new Date()));
  readonly expenses = signal<Expense[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly form = this.fb.nonNullable.group({
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(140)]),
    categoryId: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    type: this.fb.nonNullable.control<'FIXED' | 'VARIABLE' | 'INSTALLMENT'>('VARIABLE', Validators.required),
    paymentMethod: this.fb.nonNullable.control<'DEBIT' | 'CREDIT' | 'CASH' | 'PIX'>('DEBIT', Validators.required),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    dueDate: this.fb.nonNullable.control('', Validators.required),
    recurring: this.fb.nonNullable.control(false),
    installmentCount: this.fb.control<number | null>(null, [Validators.min(1), Validators.max(60)])
  });

  readonly isFixed = computed(() => this.form.controls.type.value === 'FIXED');
  readonly isInstallment = computed(() => this.form.controls.type.value === 'INSTALLMENT');

  constructor() {
    this.load();
  }

  changeMonth(value: string) {
    this.month.set(value);
    this.load();
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos da despesa.');
      return;
    }

    const value = this.form.getRawValue();
    const payload = {
      ...value,
      recurring: value.type === 'FIXED' ? value.recurring : false,
      installmentCount: value.type === 'INSTALLMENT' ? value.installmentCount ?? 1 : undefined
    };

    if (this.editingId()) {
      this.financeService.updateExpense(this.editingId()!, payload as any).subscribe({
        next: () => {
          this.toastService.success('Despesa salva com sucesso.');
          this.cancel();
          this.load();
        },
        error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar despesa.')
      });
      return;
    }

    this.financeService.createExpense(payload as any).subscribe({
      next: () => {
        this.toastService.success('Despesa salva com sucesso.');
        this.cancel();
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar despesa.')
    });
  }

  edit(item: Expense) {
    const category = this.categories().find((entry) => entry.name === item.category);
    this.editingId.set(item.id);
    this.form.patchValue({
      description: item.description,
      categoryId: category?.id ?? null,
      type: item.type,
      paymentMethod: item.paymentMethod,
      amount: item.amount,
      dueDate: item.dueDate,
      recurring: item.recurring,
      installmentCount: item.installmentCount
    });
  }

  remove(item: Expense) {
    this.financeService.deleteExpense(item.id).subscribe({
      next: () => {
        this.toastService.success('Despesa excluída com sucesso.');
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao excluir despesa.')
    });
  }

  cancel() {
    this.editingId.set(null);
    this.form.reset({
      description: '',
      categoryId: null,
      type: 'VARIABLE',
      paymentMethod: 'DEBIT',
      amount: null,
      dueDate: '',
      recurring: false,
      installmentCount: null
    });
  }

  private load() {
    this.financeService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.toastService.error('Não foi possível carregar as categorias.')
    });

    this.financeService.getExpenses(this.month()).subscribe({
      next: (expenses) => this.expenses.set(expenses),
      error: () => this.toastService.error('Não foi possível carregar as despesas.')
    });
  }

  private toYearMonth(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  paymentMethodLabel(method: PaymentMethod): string {
    switch (method) {
      case 'CREDIT':
        return 'Crédito';
      case 'DEBIT':
        return 'Débito';
      case 'PIX':
        return 'PIX';
      case 'CASH':
        return 'Dinheiro';
    }
  }
}
