import { AfterViewInit, Component, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FinanceService } from '../../core/finance.service';
import { ToastService } from '../../core/toast.service';
import { CurrencyMaskDirective } from '../../core/currency-mask.directive';

@Component({
  selector: 'app-goals-page',
  imports: [CommonModule, ReactiveFormsModule, CurrencyMaskDirective],
  templateUrl: './goals-page.component.html',
  styleUrl: './goals-page.component.scss'
})
export class GoalsPageComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);

  @ViewChild('reserveGoalInput') private reserveGoalInput?: ElementRef<HTMLInputElement>;
  @ViewChild('creditLimitInput') private creditLimitInput?: ElementRef<HTMLInputElement>;

  readonly form = this.fb.nonNullable.group({
    reserveGoal: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    creditLimit: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)])
  });

  constructor() {
    this.financeService.getPlanningSettings().subscribe({
      next: (settings) => {
        this.form.patchValue(settings);
        this.syncMaskedInputs();
      },
      error: () => this.toastService.error('Não foi possível carregar meta e limite.')
    });
  }

  ngAfterViewInit() {
    this.syncMaskedInputs();
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente a meta de reserva e o limite no crédito.');
      return;
    }

    this.financeService.savePlanningSettings(this.form.getRawValue() as any).subscribe({
      next: () => this.toastService.success('Meta e limite salvos com sucesso.'),
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar meta e limite.')
    });
  }

  private syncMaskedInputs() {
    setTimeout(() => {
      this.writeFormattedValue(this.reserveGoalInput, this.form.controls.reserveGoal.value);
      this.writeFormattedValue(this.creditLimitInput, this.form.controls.creditLimit.value);
    }, 0);
  }

  private writeFormattedValue(inputRef: ElementRef<HTMLInputElement> | undefined, value: number | null) {
    if (!inputRef) {
      return;
    }

    if (value === null || value === undefined) {
      inputRef.nativeElement.value = '';
      return;
    }

    inputRef.nativeElement.value = new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  }
}
