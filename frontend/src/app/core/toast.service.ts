import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error';
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly messages = signal<ToastMessage[]>([]);
  private nextId = 1;

  success(text: string) {
    this.push('success', text);
  }

  error(text: string) {
    this.push('error', text);
  }

  remove(id: number) {
    this.messages.update((messages) => messages.filter((item) => item.id !== id));
  }

  private push(type: 'success' | 'error', text: string) {
    const id = this.nextId++;
    this.messages.update((messages) => [...messages, { id, type, text }]);
    setTimeout(() => this.remove(id), 4000);
  }
}
