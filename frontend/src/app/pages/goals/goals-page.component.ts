import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FinanceService } from '../../core/finance.service';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-goals-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './goals-page.component.html',
  styleUrl: './goals-page.component.scss'
})
export class GoalsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);

  readonly form = this.fb.nonNullable.group({
    month: this.fb.nonNullable.control(this.toYearMonth(new Date()), Validators.required),
    goalAmount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    creditLimit: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    debitLimit: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    pixLimit: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    cashLimit: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)])
  });

  constructor() {
    this.loadPlanning(this.form.controls.month.value);
  }

  changeMonth(month: string) {
    this.form.controls.month.setValue(month);
    this.loadPlanning(month);
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente a meta e os limites do mês.');
      return;
    }

    this.financeService.savePlanning(this.form.getRawValue() as any).subscribe({
      next: (planning) => {
        this.toastService.success('Meta e limites salvos com sucesso.');
        this.form.patchValue(planning as any);
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar meta e limites.')
    });
  }

  private loadPlanning(month: string) {
    this.financeService.getPlanning(month).subscribe({
      next: (planning) => this.form.patchValue(planning as any),
      error: () => this.toastService.error('Não foi possível carregar a configuração mensal.')
    });
  }

  private toYearMonth(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }
}
