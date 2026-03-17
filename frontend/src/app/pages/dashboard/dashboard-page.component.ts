import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { SummaryCardComponent } from '../../components/summary-card/summary-card.component';
import { FinanceService } from '../../core/finance.service';
import { DashboardSummary } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [CommonModule, CurrencyPipe, DatePipe, SummaryCardComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent {
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);

  readonly activeMonth = signal(this.toYearMonth(new Date()));
  readonly dashboard = signal<DashboardSummary | null>(null);
  readonly totalAvailable = computed(() => this.dashboard()?.netBalance ?? 0);

  constructor() {
    this.load();
  }

  changeMonth(offset: number) {
    const [year, month] = this.activeMonth().split('-').map(Number);
    const next = new Date(year, month - 1 + offset, 1);
    this.activeMonth.set(this.toYearMonth(next));
    this.load();
  }

  closeMonth() {
    this.financeService.closeMonth(this.activeMonth()).subscribe({
      next: (response) => {
        this.toastService.success(`Fechamento concluído. ${response.createdExpenses} despesas fixas foram lançadas em ${response.targetMonth}.`);
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao fechar o mês.')
    });
  }

  private load() {
    this.financeService.getDashboard(this.activeMonth()).subscribe({
      next: (dashboard) => this.dashboard.set(dashboard),
      error: () => this.toastService.error('Não foi possível carregar o painel.')
    });
  }

  private toYearMonth(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }
}
