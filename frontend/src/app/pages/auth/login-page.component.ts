import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  readonly errorMessage = signal('');
  readonly loading = signal(false);

  readonly loginForm = this.fb.nonNullable.group({
    username: this.fb.nonNullable.control('', [Validators.required]),
    password: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(8)])
  });

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  hasError(controlName: 'username' | 'password') {
    const control = this.loginForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  getErrorMessage(controlName: 'username' | 'password') {
    const control = this.loginForm.controls[controlName];

    if (control.hasError('required')) {
      return controlName === 'username' ? 'Informe o usu\u00E1rio.' : 'Informe a senha.';
    }

    if (controlName === 'password' && control.hasError('minlength')) {
      return 'A senha deve ter pelo menos 8 caracteres.';
    }

    return '';
  }

  submitLogin() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(error?.error?.error ?? 'Falha ao autenticar.');
      }
    });
  }
}
