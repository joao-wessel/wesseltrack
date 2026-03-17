import { Component, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FinanceService } from '../../core/finance.service';
import { ToastService } from '../../core/toast.service';
import { Income } from '../../core/models';

@Component({
  selector: 'app-incomes-page',
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './incomes-page.component.html',
  styleUrl: './incomes-page.component.scss'
})
export class IncomesPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);

  readonly month = signal(this.toYearMonth(new Date()));
  readonly incomes = signal<Income[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly form = this.fb.nonNullable.group({
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    receiveDate: this.fb.nonNullable.control('', Validators.required),
    expectedDay: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(31)])
  });

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
      this.toastService.error('Preencha corretamente os campos da receita.');
      return;
    }

    const payload = this.form.getRawValue() as any;
    const request = this.editingId()
      ? this.financeService.updateIncome(this.editingId()!, payload)
      : this.financeService.createIncome(payload);

    request.subscribe({
      next: () => {
        this.toastService.success('Receita salva com sucesso.');
        this.cancel();
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar receita.')
    });
  }

  edit(item: Income) {
    this.editingId.set(item.id);
    this.form.patchValue(item);
  }

  remove(item: Income) {
    this.financeService.deleteIncome(item.id).subscribe({
      next: () => {
        this.toastService.success('Receita excluída com sucesso.');
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao excluir receita.')
    });
  }

  cancel() {
    this.editingId.set(null);
    this.form.reset({ description: '', amount: null, receiveDate: '', expectedDay: null });
  }

  private load() {
    this.financeService.getIncomes(this.month()).subscribe({
      next: (incomes) => this.incomes.set(incomes),
      error: () => this.toastService.error('Não foi possível carregar as receitas.')
    });
  }

  private toYearMonth(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }
}
