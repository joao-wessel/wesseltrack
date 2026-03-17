import { Component, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-summary-card',
  imports: [CurrencyPipe],
  template: `
    <article class="summary-card">
      <span>{{ label() }}</span>
      <strong>{{ value() | currency:'BRL':'symbol':'1.2-2' }}</strong>
      <small>{{ hint() }}</small>
    </article>
  `,
  styles: [`
    .summary-card {
      padding: 1.2rem;
      border-radius: 1.25rem;
      background: rgba(255,255,255,0.9);
      border: 1px solid rgba(15, 23, 42, 0.08);
      box-shadow: 0 20px 45px rgba(148, 163, 184, 0.16);
      display: grid;
      gap: 0.35rem;
    }
    span { color: #64748b; font-size: 0.85rem; }
    strong { font-size: 1.45rem; color: #0f172a; }
    small { color: #475569; }
  `]
})
export class SummaryCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly hint = input<string>('');
}
