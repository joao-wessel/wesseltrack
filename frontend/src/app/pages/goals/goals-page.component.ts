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
    creditLimit: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    creditCardDueDay: this.fb.control<number | null>(10, [Validators.required, Validators.min(1), Validators.max(31)])
  });

  constructor() {
    this.financeService.getPlanningSettings().subscribe({
      next: (settings) => {
        this.form.patchValue(settings);
        this.syncMaskedInputs();
      },
      error: () => this.toastService.error('N\u00E3o foi poss\u00EDvel carregar meta e limite.')
    });
  }

  ngAfterViewInit() {
    this.syncMaskedInputs();
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente a meta de reserva, o limite e o vencimento do cart\u00E3o.');
      return;
    }

    this.financeService.savePlanningSettings(this.form.getRawValue() as any).subscribe({
      next: () => {
        this.toastService.success('Meta, limite e vencimento salvos com sucesso.');
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar configura\u00E7\u00F5es do cart\u00E3o.')
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
