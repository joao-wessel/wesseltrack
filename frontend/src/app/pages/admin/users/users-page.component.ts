import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { FinanceService } from '../../../core/finance.service';
import { ManagedUser } from '../../../core/models';
import { ToastService } from '../../../core/toast.service';

@Component({
  selector: 'app-users-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './users-page.component.html',
  styleUrl: './users-page.component.scss'
})
export class UsersPageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);

  readonly users = signal<ManagedUser[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly compactLayout = signal(this.isCompactViewport());
  readonly mobileFormOpen = signal(false);
  readonly showForm = computed(() => !this.compactLayout() || this.mobileFormOpen());
  readonly form = this.fb.nonNullable.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    username: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(4), Validators.maxLength(80)]),
    password: this.fb.nonNullable.control('', [Validators.minLength(8), Validators.maxLength(120)]),
    role: this.fb.nonNullable.control<'ADMIN' | 'USER'>('USER', Validators.required)
  });

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.handleViewportResize, { passive: true });
    }
    this.load();
  }

  ngOnDestroy() {
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.handleViewportResize);
    }
  }

  save() {
    const password = this.form.controls.password.value;
    if (!this.editingId() && !password) {
      this.form.controls.password.setErrors({ required: true });
    }

    if (this.form.invalid) {
      this.mobileFormOpen.set(true);
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos do usu\u00E1rio.');
      return;
    }

    const payload = {
      ...this.form.getRawValue(),
      password: password || null
    };
    const request = this.editingId()
      ? this.financeService.updateUser(this.editingId()!, payload)
      : this.financeService.createUser(payload as any);

    request.subscribe({
      next: () => {
        this.toastService.success(this.editingId() ? 'Usu\u00E1rio atualizado com sucesso.' : 'Usu\u00E1rio criado com sucesso.');
        this.cancel();
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar usu\u00E1rio.')
    });
  }

  edit(user: ManagedUser) {
    this.editingId.set(user.id);
    this.mobileFormOpen.set(true);
    this.form.reset({
      name: user.name,
      username: user.username,
      password: '',
      role: user.role
    });
  }

  async remove(user: ManagedUser) {
    const confirmed = await this.confirmDialog.open({
      title: 'Excluir usu\u00E1rio',
      message: `Confirma a exclus\u00E3o do usu\u00E1rio "${user.username}"? Esta a\u00E7\u00E3o n\u00E3o poder\u00E1 ser desfeita.`,
      confirmLabel: 'Excluir',
      cancelLabel: 'Cancelar',
      variant: 'danger'
    });

    if (!confirmed) {
      return;
    }

    this.financeService.deleteUser(user.id).subscribe({
      next: () => {
        this.toastService.success('Usu\u00E1rio exclu\u00EDdo com sucesso.');
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao excluir usu\u00E1rio.')
    });
  }

  cancel() {
    this.editingId.set(null);
    this.form.reset({ name: '', username: '', password: '', role: 'USER' });
    if (this.compactLayout()) {
      this.mobileFormOpen.set(false);
    }
  }

  toggleMobileForm() {
    this.mobileFormOpen.update((value) => !value);
  }

  private load() {
    this.financeService.getUsers().subscribe({
      next: (users) => this.users.set(users),
      error: () => this.toastService.error('N\u00E3o foi poss\u00EDvel carregar os usu\u00E1rios.')
    });
  }

  private readonly handleViewportResize = () => {
    this.compactLayout.set(this.isCompactViewport());
  };

  private isCompactViewport(): boolean {
    return typeof window !== 'undefined' && window.innerWidth <= 720;
  }
}
