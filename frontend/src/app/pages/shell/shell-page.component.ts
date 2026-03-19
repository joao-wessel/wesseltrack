import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-shell-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell-page.component.html',
  styleUrl: './shell-page.component.scss'
})
export class ShellPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);
  readonly authService = inject(AuthService);
  readonly eyebrow = computed(() => new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric'
  }).format(new Date()));
  readonly isAdmin = computed(() => this.authService.user()?.role === 'ADMIN');
  readonly passwordModalOpen = signal(false);
  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: this.fb.nonNullable.control('', [Validators.required]),
    newPassword: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(8)]),
    confirmPassword: this.fb.nonNullable.control('', [Validators.required])
  });

  logout() {
    this.authService.logout();
  }

  openPasswordModal() {
    this.passwordModalOpen.set(true);
  }

  closePasswordModal() {
    this.passwordModalOpen.set(false);
    this.passwordForm.reset({
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
  }

  submitPasswordChange() {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos da nova senha.');
      return;
    }

    const { currentPassword, newPassword, confirmPassword } = this.passwordForm.getRawValue();
    if (newPassword !== confirmPassword) {
      this.toastService.error('A confirmação da nova senha não confere.');
      return;
    }

    this.authService.changePassword({ currentPassword, newPassword, confirmPassword }).subscribe({
      next: () => {
        this.toastService.success('Senha alterada com sucesso.');
        this.closePasswordModal();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao alterar a senha.')
    });
  }
}
