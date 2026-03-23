import { Component, ElementRef, OnDestroy, ViewChild, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FinanceService } from '../../core/finance.service';
import { ConfirmDialogService } from '../../core/confirm-dialog.service';
import { ToastService } from '../../core/toast.service';
import { Category, Expense } from '../../core/models';
import { CurrencyMaskDirective } from '../../core/currency-mask.directive';

@Component({
  selector: 'app-expenses-page',
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe, CurrencyMaskDirective],
  templateUrl: './expenses-page.component.html',
  styleUrl: './expenses-page.component.scss'
})
export class ExpensesPageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly financeService = inject(FinanceService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly toastService = inject(ToastService);
  private resizeObserver?: ResizeObserver;
  private activeFormPanelElement?: HTMLElement;

  @ViewChild('quickAmountInput') private quickAmountInput?: ElementRef<HTMLInputElement>;
  @ViewChild('fixedAmountInput') private fixedAmountInput?: ElementRef<HTMLInputElement>;
  @ViewChild('installmentAmountInput') private installmentAmountInput?: ElementRef<HTMLInputElement>;
  @ViewChild('activeFormPanel')
  set activeFormPanel(elementRef: ElementRef<HTMLElement> | undefined) {
    this.activeFormPanelElement = elementRef?.nativeElement;
    this.observeActiveFormPanel();
  }

  readonly month = signal(this.toYearMonth(new Date()));
  readonly expenses = signal<Expense[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly editingMode = signal<'quick' | 'fixed' | 'installment' | null>(null);
  readonly activeTab = signal<'quick' | 'fixed' | 'installment'>('quick');
  readonly panelHeight = signal<number | null>(null);
  readonly compactLayout = signal(this.isCompactViewport());
  readonly mobileFormOpen = signal(false);
  readonly listPanelHeight = computed(() => (this.compactLayout() ? null : this.panelHeight()));
  readonly showActiveForm = computed(() => !this.compactLayout() || this.mobileFormOpen());

  readonly quickForm = this.fb.nonNullable.group({
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(140)]),
    categoryId: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    paymentMethod: this.fb.nonNullable.control<'DEBIT' | 'CREDIT' | 'CASH' | 'PIX'>('DEBIT', Validators.required),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    dueDate: this.fb.nonNullable.control('', Validators.required)
  });

  readonly fixedForm = this.fb.nonNullable.group({
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(140)]),
    categoryId: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    paymentMethod: this.fb.nonNullable.control<'DEBIT' | 'CREDIT' | 'CASH' | 'PIX'>('DEBIT', Validators.required),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    dueDate: this.fb.nonNullable.control('', Validators.required)
  });

  readonly installmentForm = this.fb.nonNullable.group({
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(140)]),
    categoryId: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    dueDate: this.fb.nonNullable.control('', Validators.required),
    installmentCount: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(60)]),
    firstInstallmentNextMonth: this.fb.nonNullable.control(false)
  });

  readonly quickExpenses = computed(() => this.expenses().filter((item) => item.type === 'VARIABLE'));
  readonly fixedExpenses = computed(() => this.expenses().filter((item) => item.type === 'FIXED'));
  readonly installmentExpenses = computed(() => this.expenses().filter((item) => item.type === 'INSTALLMENT'));

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

  saveQuick() {
    if (this.quickForm.invalid) {
      this.mobileFormOpen.set(true);
      this.quickForm.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos do lan\u00E7amento r\u00E1pido.');
      return;
    }

    const payload = { ...this.quickForm.getRawValue(), type: 'VARIABLE', recurring: false };
    this.saveExpense(payload as any, 'quick', 'Despesa r\u00E1pida salva com sucesso.');
  }

  saveFixed() {
    if (this.fixedForm.invalid) {
      this.mobileFormOpen.set(true);
      this.fixedForm.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos da despesa fixa.');
      return;
    }

    const payload = {
      ...this.fixedForm.getRawValue(),
      dueDate: this.referenceDateForSelectedMonth(this.fixedForm.getRawValue().dueDate),
      type: 'FIXED',
      recurring: true
    };
    this.saveExpense(payload as any, 'fixed', 'Despesa fixa salva com sucesso.');
  }

  saveInstallment() {
    if (this.installmentForm.invalid) {
      this.mobileFormOpen.set(true);
      this.installmentForm.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos da compra parcelada.');
      return;
    }

    const payload = {
      ...this.installmentForm.getRawValue(),
      type: 'INSTALLMENT',
      paymentMethod: 'CREDIT',
      recurring: false
    };
    this.saveExpense(payload as any, 'installment', 'Compra parcelada salva com sucesso.');
  }

  edit(item: Expense) {
    this.editingId.set(item.id);
    this.mobileFormOpen.set(true);
    const category = this.categories().find((entry) => entry.name === item.category);

    if (item.type === 'VARIABLE') {
      this.activeTab.set('quick');
      this.editingMode.set('quick');
      this.quickForm.patchValue({
        description: item.description,
        categoryId: category?.id ?? null,
        paymentMethod: item.paymentMethod,
        amount: item.amount,
        dueDate: item.dueDate
      });
      this.syncMaskedInput(this.quickAmountInput, item.amount);
      return;
    }

    if (item.type === 'FIXED') {
      this.activeTab.set('fixed');
      this.editingMode.set('fixed');
      this.fixedForm.patchValue({
        description: item.description,
        categoryId: category?.id ?? null,
        paymentMethod: item.paymentMethod,
        amount: item.amount,
        dueDate: item.dueDate
      });
      this.syncMaskedInput(this.fixedAmountInput, item.amount);
      return;
    }

    this.activeTab.set('installment');
    this.editingMode.set('installment');
    this.installmentForm.patchValue({
      description: item.description,
      categoryId: category?.id ?? null,
      amount: item.originalAmount,
      dueDate: item.dueDate,
      installmentCount: item.installmentCount,
      firstInstallmentNextMonth: false
    });
    this.syncMaskedInput(this.installmentAmountInput, item.originalAmount);
  }

  async remove(item: Expense) {
    const confirmed = await this.confirmDialog.open({
      title: 'Excluir despesa',
      message: `Confirma a exclus\u00E3o de "${item.description}"? Esta a\u00E7\u00E3o n\u00E3o poder\u00E1 ser desfeita.`,
      confirmLabel: 'Excluir',
      cancelLabel: 'Cancelar',
      variant: 'danger'
    });

    if (!confirmed) {
      return;
    }

    this.financeService.deleteExpense(item.id, item.recurring ? this.month() : undefined).subscribe({
      next: () => {
        this.toastService.success('Despesa exclu\u00EDda com sucesso.');
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao excluir despesa.')
    });
  }

  cancel() {
    this.editingId.set(null);
    this.editingMode.set(null);
    this.quickForm.reset({ description: '', categoryId: null, paymentMethod: 'DEBIT', amount: null, dueDate: '' });
    this.fixedForm.reset({ description: '', categoryId: null, paymentMethod: 'DEBIT', amount: null, dueDate: '' });
    this.installmentForm.reset({ description: '', categoryId: null, amount: null, dueDate: '', installmentCount: null, firstInstallmentNextMonth: false });
    if (this.compactLayout()) {
      this.mobileFormOpen.set(false);
    }
  }

  selectTab(tab: 'quick' | 'fixed' | 'installment') {
    this.activeTab.set(tab);
    if (this.compactLayout() && !this.editingId()) {
      this.mobileFormOpen.set(false);
    }
  }

  toggleMobileForm() {
    this.mobileFormOpen.update((value) => !value);
  }

  paymentMethodLabel(method: Expense['paymentMethod']) {
    switch (method) {
      case 'CREDIT':
        return 'Cr\u00E9dito';
      case 'DEBIT':
        return 'D\u00E9bito';
      case 'PIX':
        return 'PIX';
      case 'CASH':
        return 'Dinheiro';
    }
  }

  private saveExpense(payload: any, mode: 'quick' | 'fixed' | 'installment', successMessage: string) {
    if (this.editingId() && this.editingMode() === mode) {
      this.financeService.updateExpense(this.editingId()!, payload).subscribe({
        next: () => {
          this.toastService.success(successMessage);
          this.cancel();
          this.load();
        },
        error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar despesa.')
      });
      return;
    }

    this.financeService.createExpense(payload).subscribe({
      next: () => {
        this.toastService.success(successMessage);
        this.cancel();
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar despesa.')
    });
  }

  private load() {
    this.financeService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.toastService.error('N\u00E3o foi poss\u00EDvel carregar as categorias.')
    });

    this.financeService.getExpenses(this.month()).subscribe({
      next: (expenses) => this.expenses.set(expenses),
      error: () => this.toastService.error('N\u00E3o foi poss\u00EDvel carregar as despesas.')
    });
  }

  private toYearMonth(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  private referenceDateForSelectedMonth(dateValue: string): string {
    const referenceDate = dateValue ? new Date(`${dateValue}T00:00:00`) : new Date();
    const [year, month] = this.month().split('-').map(Number);
    const day = Math.min(referenceDate.getDate(), new Date(year, month, 0).getDate());
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
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


