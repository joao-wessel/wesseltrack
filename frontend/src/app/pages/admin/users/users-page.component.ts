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
  readonly editingId = signal<number | null>(null);
  readonly form = this.fb.nonNullable.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    username: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(4), Validators.maxLength(80)]),
    password: this.fb.nonNullable.control('', [Validators.minLength(8), Validators.maxLength(120)]),
    role: this.fb.nonNullable.control<'ADMIN' | 'USER'>('USER', Validators.required)
  });

  constructor() {
    this.requirePassword();
    this.load();
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos do usuário.');
      return;
    }

    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name,
      username: raw.username,
      role: raw.role,
      ...(raw.password ? { password: raw.password } : {})
    };

    const request = this.editingId()
      ? this.financeService.updateUser(this.editingId()!, payload)
      : this.financeService.createUser({ ...payload, password: raw.password });

    request.subscribe({
      next: () => {
        this.toastService.success(this.editingId() ? 'Usuário atualizado com sucesso.' : 'Usuário criado com sucesso.');
        this.cancel();
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar usuário.')
    });
  }

  edit(user: ManagedUser) {
    this.editingId.set(user.id);
    this.form.controls.password.setValidators([Validators.minLength(8), Validators.maxLength(120)]);
    this.form.controls.password.updateValueAndValidity({ emitEvent: false });
    this.form.reset({
      name: user.name,
      username: user.username,
      password: '',
      role: user.role
    });
  }

  remove(user: ManagedUser) {
    this.financeService.deleteUser(user.id).subscribe({
      next: () => {
        this.toastService.success('Usuário excluído com sucesso.');
        if (this.editingId() === user.id) {
          this.cancel();
        }
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao excluir usuário.')
    });
  }

  cancel() {
    this.editingId.set(null);
    this.requirePassword();
    this.form.reset({ name: '', username: '', password: '', role: 'USER' });
  }

  private requirePassword() {
    this.form.controls.password.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(120)]);
    this.form.controls.password.updateValueAndValidity({ emitEvent: false });
  }

  private load() {
    this.financeService.getUsers().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.toastService.error('Não foi possível carregar os usuários.')
    });
  }
}
