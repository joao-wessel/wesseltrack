import { Injectable, signal } from '@angular/core';

export interface ConfirmDialogOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'default';
}

interface ConfirmDialogState extends Required<ConfirmDialogOptions> {
  open: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  readonly state = signal<ConfirmDialogState>({
    open: false,
    title: '',
    message: '',
    confirmLabel: 'Confirmar',
    cancelLabel: 'Cancelar',
    variant: 'default'
  });

  private resolver?: (value: boolean) => void;

  open(options: ConfirmDialogOptions) {
    this.resolver?.(false);
    this.state.set({
      open: true,
      title: options.title,
      message: options.message,
      confirmLabel: options.confirmLabel ?? 'Confirmar',
      cancelLabel: options.cancelLabel ?? 'Cancelar',
      variant: options.variant ?? 'default'
    });

    return new Promise<boolean>((resolve) => {
      this.resolver = resolve;
    });
  }

  confirm() {
    this.finish(true);
  }

  cancel() {
    this.finish(false);
  }

  private finish(result: boolean) {
    this.state.update((current) => ({ ...current, open: false }));
    this.resolver?.(result);
    this.resolver = undefined;
  }
}
