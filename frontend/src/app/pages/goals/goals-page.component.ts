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
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)])
  });

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos da meta mensal.');
      return;
    }

    this.financeService.saveGoal(this.form.getRawValue() as any).subscribe({
      next: () => this.toastService.success('Meta mensal salva com sucesso.'),
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar meta mensal.')
    });
  }

  private toYearMonth(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }
}

