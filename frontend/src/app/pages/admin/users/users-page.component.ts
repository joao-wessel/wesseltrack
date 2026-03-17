import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FinanceService } from '../../../core/finance.service';
import { ManagedUser } from '../../../core/models';
import { ToastService } from '../../../core/toast.service';

@Component({
  selector: 'app-users-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './users-page.component.html',
  styleUrl: './users-page.component.scss'
})
export class UsersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);

  readonly users = signal<ManagedUser[]>([]);
  readonly form = this.fb.nonNullable.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    username: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(4), Validators.maxLength(80)]),
    password: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(8), Validators.maxLength(120)]),
    role: this.fb.nonNullable.control<'ADMIN' | 'USER'>('USER', Validators.required)
  });

  constructor() {
    this.load();
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos do usuário.');
      return;
    }

    this.financeService.createUser(this.form.getRawValue()).subscribe({
      next: () => {
        this.toastService.success('Usuário criado com sucesso.');
        this.form.reset({ name: '', username: '', password: '', role: 'USER' });
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao criar usuário.')
    });
  }

  private load() {
    this.financeService.getUsers().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.toastService.error('Não foi possível carregar os usuários.')
    });
  }
}
