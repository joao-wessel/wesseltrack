import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-shell-page',
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell-page.component.html',
  styleUrl: './shell-page.component.scss'
})
export class ShellPageComponent {
  readonly authService = inject(AuthService);
  readonly eyebrow = computed(() => new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric'
  }).format(new Date()));
  readonly isAdmin = computed(() => this.authService.user()?.role === 'ADMIN');

  logout() {
    this.authService.logout();
  }
}
