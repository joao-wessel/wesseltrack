import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { SummaryCardComponent } from '../../components/summary-card/summary-card.component';
import { FinanceService } from '../../core/finance.service';
import { DashboardSummary } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [CommonModule, CurrencyPipe, SummaryCardComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent {
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);
  private readonly monthFormatter = new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric'
  });

  readonly activeMonth = signal(this.toYearMonth(new Date()));
  readonly activeMonthLabel = computed(() => this.formatMonthLabel(this.activeMonth()));
  readonly dashboard = signal<DashboardSummary | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly totalAvailable = computed(() => this.dashboard()?.netBalance ?? 0);
  readonly totalCategoryExpenses = computed(() =>
    (this.dashboard()?.byCategory ?? []).reduce((sum, item) => sum + item.total, 0)
  );
  readonly totalPaymentExpenses = computed(() =>
    (this.dashboard()?.byPaymentMethod ?? []).reduce((sum, item) => sum + item.spent, 0)
  );
  readonly pieChartStyle = computed(() => this.buildPieChartStyle());
  readonly creditUsageShare = computed(() => {
    const data = this.dashboard();
    if (!data || data.maxCreditCardBill <= 0) {
      return 0;
    }

    return Math.min((data.creditUsage.spent / data.maxCreditCardBill) * 100, 100);
  });
  readonly creditUsageDelta = computed(() => {
    const data = this.dashboard();
    if (!data) {
      return 0;
    }

    return data.maxCreditCardBill - data.creditUsage.spent;
  });

  constructor() {
    this.load();
  }

  changeMonth(offset: number) {
    const [year, month] = this.activeMonth().split('-').map(Number);
    const next = new Date(year, month - 1 + offset, 1);
    this.activeMonth.set(this.toYearMonth(next));
    this.load();
  }

  categoryShare(total: number) {
    const grandTotal = this.totalCategoryExpenses();
    return grandTotal === 0 ? 0 : (total / grandTotal) * 100;
  }

  paymentShare(total: number) {
    const grandTotal = this.totalPaymentExpenses();
    return grandTotal === 0 ? 0 : (total / grandTotal) * 100;
  }

  paymentMethodColor(method: string) {
    switch (this.normalizeMethod(method)) {
      case 'CREDIT':
      case 'CREDITO':
        return '#0f766e';
      case 'DEBIT':
      case 'DEBITO':
        return '#0f172a';
      case 'PIX':
        return '#0891b2';
      case 'CASH':
      case 'DINHEIRO':
        return '#b45309';
      default:
        return '#64748b';
    }
  }

  paymentMethodLabel(method: string) {
    switch (this.normalizeMethod(method)) {
      case 'CREDIT':
      case 'CREDITO':
        return 'Cr\u00E9dito';
      case 'DEBIT':
      case 'DEBITO':
        return 'D\u00E9bito';
      case 'PIX':
        return 'PIX';
      case 'CASH':
      case 'DINHEIRO':
        return 'Dinheiro';
      default:
        return method;
    }
  }

  creditBillStatus() {
    const data = this.dashboard();
    if (!data) {
      return 'Sem dados';
    }

    const ratio = data.maxCreditCardBill <= 0 ? 0 : data.creditUsage.spent / data.maxCreditCardBill;
    if (ratio <= 0.75) {
      return 'Dentro do ideal';
    }
    if (ratio <= 1) {
      return 'Aten\u00E7\u00E3o';
    }
    return 'Acima do ideal';
  }

  creditBillStatusTone() {
    const status = this.creditBillStatus();
    if (status === 'Dentro do ideal') {
      return '#0f766e';
    }
    if (status === 'Aten\u00E7\u00E3o') {
      return '#b45309';
    }
    return '#b91c1c';
  }

  private normalizeMethod(method: string) {
    return method
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase();
  }

  private buildPieChartStyle() {
    const items = this.dashboard()?.byCategory ?? [];
    const total = this.totalCategoryExpenses();

    if (!items.length || total <= 0) {
      return 'conic-gradient(#e2e8f0 0deg 360deg)';
    }

    let current = 0;
    const segments = items.map((item) => {
      const slice = (item.total / total) * 360;
      const start = current;
      const end = current + slice;
      current = end;
      return `${item.color} ${start}deg ${end}deg`;
    });

    return `conic-gradient(${segments.join(', ')})`;
  }

  private load() {
    this.loadError.set(null);
    this.financeService.getDashboard(this.activeMonth()).subscribe({
      next: (dashboard) => {
        this.dashboard.set(dashboard);
        this.loadError.set(null);
      },
      error: (error: any) => {
        this.dashboard.set(null);
        const message = error?.error?.error ?? 'N\u00E3o foi poss\u00EDvel carregar o painel.';
        this.loadError.set(message);
        this.toastService.error(message);
      }
    });
  }

  private toYearMonth(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  private formatMonthLabel(value: string): string {
    const [year, month] = value.split('-').map(Number);
    return this.monthFormatter.format(new Date(year, month - 1, 1));
  }
}
