import { Component, ElementRef, OnDestroy, ViewChild, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FinanceService } from '../../core/finance.service';
import { ConfirmDialogService } from '../../core/confirm-dialog.service';
import { ToastService } from '../../core/toast.service';
import { Income } from '../../core/models';

@Component({
  selector: 'app-incomes-page',
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe],
  templateUrl: './incomes-page.component.html',
  styleUrl: './incomes-page.component.scss'
})
export class IncomesPageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly financeService = inject(FinanceService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly toastService = inject(ToastService);
  private resizeObserver?: ResizeObserver;
  private activeFormPanelElement?: HTMLElement;

  @ViewChild('monthlyAmountInput') private monthlyAmountInput?: ElementRef<HTMLInputElement>;
  @ViewChild('fixedAmountInput') private fixedAmountInput?: ElementRef<HTMLInputElement>;
  @ViewChild('activeFormPanel')
  set activeFormPanel(elementRef: ElementRef<HTMLElement> | undefined) {
    this.activeFormPanelElement = elementRef?.nativeElement;
    this.observeActiveFormPanel();
  }

  readonly month = signal(this.toYearMonth(new Date()));
  readonly incomes = signal<Income[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly editingRecurring = signal(false);
  readonly activeTab = signal<'monthly' | 'fixed'>('monthly');
  readonly panelHeight = signal<number | null>(null);
  readonly compactLayout = signal(this.isCompactViewport());
  readonly mobileFormOpen = signal(false);
  readonly listPanelHeight = computed(() => (this.compactLayout() ? null : this.panelHeight()));
  readonly showActiveForm = computed(() => !this.compactLayout() || this.mobileFormOpen());

  readonly monthlyForm = this.fb.nonNullable.group({
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    receiveDate: this.fb.nonNullable.control('', Validators.required)
  });

  readonly fixedForm = this.fb.nonNullable.group({
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    expectedDay: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(31)])
  });

  readonly recurringIncomes = computed(() => this.incomes().filter((item) => item.recurring));
  readonly monthlyIncomes = computed(() => this.incomes().filter((item) => !item.recurring));

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.handleViewportResize, { passive: true });
    }
    this.load();
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.handleViewportResize);
    }
  }

  changeMonth(value: string) {
    this.month.set(value);
    this.load();
  }

  saveMonthly() {
    if (this.monthlyForm.invalid) {
      this.mobileFormOpen.set(true);
      this.monthlyForm.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos da receita mensal.');
      return;
    }

    const payload = { ...this.monthlyForm.getRawValue(), expectedDay: null, recurring: false };
    if (this.editingId() && !this.editingRecurring()) {
      this.financeService.updateIncome(this.editingId()!, payload as any).subscribe({
        next: () => this.afterSave('Receita mensal salva com sucesso.', false),
        error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar receita mensal.')
      });
      return;
    }

    this.financeService.createIncome(payload as any).subscribe({
      next: () => this.afterSave('Receita mensal salva com sucesso.', false),
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar receita mensal.')
    });
  }

  saveFixed() {
    if (this.fixedForm.invalid) {
      this.mobileFormOpen.set(true);
      this.fixedForm.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos da receita fixa.');
      return;
    }

    const expectedDay = this.fixedForm.getRawValue().expectedDay;
    const payload = {
      ...this.fixedForm.getRawValue(),
      receiveDate: this.referenceDateForExpectedDay(expectedDay),
      recurring: true
    };
    if (this.editingId() && this.editingRecurring()) {
      this.financeService.updateIncome(this.editingId()!, payload as any).subscribe({
        next: () => this.afterSave('Receita fixa salva com sucesso.', true),
        error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar receita fixa.')
      });
      return;
    }

    this.financeService.createIncome(payload as any).subscribe({
      next: () => this.afterSave('Receita fixa salva com sucesso.', true),
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar receita fixa.')
    });
  }

  edit(item: Income) {
    this.editingId.set(item.id);
    this.editingRecurring.set(item.recurring);
    this.mobileFormOpen.set(true);

    const values = {
      description: item.description,
      amount: item.amount,
      receiveDate: item.receiveDate,
      expectedDay: item.expectedDay
    };

    if (item.recurring) {
      this.activeTab.set('fixed');
      this.fixedForm.patchValue({
        description: values.description,
        amount: values.amount,
        expectedDay: values.expectedDay
      });
      this.syncMaskedInput(this.fixedAmountInput, values.amount);
    } else {
      this.activeTab.set('monthly');
      this.monthlyForm.patchValue({
        description: values.description,
        amount: values.amount,
        receiveDate: values.receiveDate
      });
      this.syncMaskedInput(this.monthlyAmountInput, values.amount);
    }
  }

  async remove(item: Income) {
    const confirmed = await this.confirmDialog.open({
      title: 'Excluir receita',
      message: `Confirma a exclus\u00E3o de "${item.description}"? Esta a\u00E7\u00E3o n\u00E3o poder\u00E1 ser desfeita.`,
      confirmLabel: 'Excluir',
      cancelLabel: 'Cancelar',
      variant: 'danger'
    });

    if (!confirmed) {
      return;
    }

    this.financeService.deleteIncome(item.id, item.recurring ? this.month() : undefined).subscribe({
      next: () => {
        this.toastService.success('Receita exclu\u00EDda com sucesso.');
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao excluir receita.')
    });
  }

  cancel() {
    this.editingId.set(null);
    this.editingRecurring.set(false);
    this.monthlyForm.reset({ description: '', amount: null, receiveDate: '' });
    this.fixedForm.reset({ description: '', amount: null, expectedDay: null });
    if (this.compactLayout()) {
      this.mobileFormOpen.set(false);
    }
  }

  selectTab(tab: 'monthly' | 'fixed') {
    this.activeTab.set(tab);
    if (this.compactLayout() && !this.editingId()) {
      this.mobileFormOpen.set(false);
    }
  }

  toggleMobileForm() {
    this.mobileFormOpen.update((value) => !value);
  }

  private afterSave(message: string, recurring: boolean) {
    this.toastService.success(message);
    this.cancel();
    this.load();
  }

  private load() {
    this.financeService.getIncomes(this.month()).subscribe({
      next: (incomes) => this.incomes.set(incomes),
      error: () => this.toastService.error('N\u00E3o foi poss\u00EDvel carregar as receitas.')
    });
  }

  private toYearMonth(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  private referenceDateForExpectedDay(expectedDay: number | null): string | null {
    if (!expectedDay) {
      return null;
    }

    const [year, month] = this.month().split('-').map(Number);
    const reference = new Date(year, month - 1, Math.min(expectedDay, new Date(year, month, 0).getDate()));
    return `${reference.getFullYear()}-${String(reference.getMonth() + 1).padStart(2, '0')}-${String(reference.getDate()).padStart(2, '0')}`;
  }

  private syncMaskedInput(inputRef: ElementRef<HTMLInputElement> | undefined, value: number | null) {
    setTimeout(() => {
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
    }, 0);
  }

  private readonly handleViewportResize = () => {
    this.compactLayout.set(this.isCompactViewport());
  };

  private isCompactViewport(): boolean {
    return typeof window !== 'undefined' && window.innerWidth <= 900;
  }

  private observeActiveFormPanel() {
    this.resizeObserver?.disconnect();

    if (!this.activeFormPanelElement) {
      this.panelHeight.set(null);
      return;
    }

    const updateHeight = () => {
      this.panelHeight.set(this.activeFormPanelElement?.getBoundingClientRect().height ?? null);
    };

    updateHeight();
    this.resizeObserver = new ResizeObserver(() => updateHeight());
    this.resizeObserver.observe(this.activeFormPanelElement);
  }
}


