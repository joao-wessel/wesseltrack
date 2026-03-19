import { AfterViewInit, Directive, ElementRef, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';
import { distinctUntilChanged, startWith } from 'rxjs';

@Directive({
  selector: 'input[appCurrencyMask]',
  standalone: true
})
export class CurrencyMaskDirective implements AfterViewInit {
  private readonly element = inject(ElementRef<HTMLInputElement>);
  private readonly ngControl = inject(NgControl, { optional: true, self: true });

  constructor() {
    queueMicrotask(() => this.scheduleSync());
    this.ngControl?.control?.valueChanges
      ?.pipe(startWith(this.ngControl.control.value), distinctUntilChanged())
      .subscribe(() => this.scheduleSync());
  }

  ngAfterViewInit() {
    this.scheduleSync();
    requestAnimationFrame(() => this.scheduleSync());
  }

  @HostListener('input')
  onInput() {
    const rawDigits = this.element.nativeElement.value.replace(/\D/g, '');

    if (!rawDigits) {
      this.writeValue(null, '');
      return;
    }

    const numericValue = Number(rawDigits) / 100;
    this.writeValue(numericValue, this.format(numericValue));
  }

  @HostListener('blur')
  onBlur() {
    this.scheduleSync();
  }

  private scheduleSync() {
    setTimeout(() => this.syncViewFromModel(), 0);
  }

  private syncViewFromModel() {
    const value = this.ngControl?.control?.value;
    if (value === null || value === undefined || value === '') {
      this.element.nativeElement.value = '';
      return;
    }

    const numericValue = typeof value === 'number' ? value : Number(value);
    this.element.nativeElement.value = Number.isFinite(numericValue) ? this.format(numericValue) : '';
  }

  private writeValue(modelValue: number | null, viewValue: string) {
    this.element.nativeElement.value = viewValue;
    this.ngControl?.control?.setValue(modelValue, { emitEvent: false, emitModelToViewChange: false });
  }

  private format(value: number) {
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  }
}
