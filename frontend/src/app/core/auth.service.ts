import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { AuthResponse, UserProfile } from './models';
import { RUNTIME_CONFIG } from './runtime-config';

interface LoginPayload { username: string; password: string; }
interface ChangePasswordPayload { currentPassword: string; newPassword: string; confirmPassword: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly runtimeConfig = inject(RUNTIME_CONFIG);
  private readonly apiUrl = this.runtimeConfig.apiBaseUrl;
  private readonly tokenKey = 'wesseltrack.token';
  private readonly userKey = 'wesseltrack.user';

  readonly token = signal<string | null>(sessionStorage.getItem(this.tokenKey));
  readonly user = signal<UserProfile | null>(this.readUser());
  readonly isAuthenticated = computed(() => !!this.token());

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  login(payload: LoginPayload) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, payload).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  changePassword(payload: ChangePasswordPayload) {
    return this.http.post<void>(`${this.apiUrl}/account/change-password`, payload);
  }

  logout() {
    sessionStorage.removeItem(this.tokenKey);
    sessionStorage.removeItem(this.userKey);
    this.token.set(null);
    this.user.set(null);
    this.router.navigate(['/login']);
  }

  private storeSession(response: AuthResponse) {
    sessionStorage.setItem(this.tokenKey, response.token);
    sessionStorage.setItem(this.userKey, JSON.stringify(response.user));
    this.token.set(response.token);
    this.user.set(response.user);
  }

  private readUser(): UserProfile | null {
    const raw = sessionStorage.getItem(this.userKey);
    return raw ? JSON.parse(raw) as UserProfile : null;
  }
}
