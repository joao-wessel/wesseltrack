import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { adminGuard } from './core/admin.guard';
import { LoginPageComponent } from './pages/auth/login-page.component';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';
import { ShellPageComponent } from './pages/shell/shell-page.component';
import { IncomesPageComponent } from './pages/incomes/incomes-page.component';
import { ExpensesPageComponent } from './pages/expenses/expenses-page.component';
import { GoalsPageComponent } from './pages/goals/goals-page.component';
import { CategoriesPageComponent } from './pages/admin/categories/categories-page.component';
import { UsersPageComponent } from './pages/admin/users/users-page.component';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  {
    path: '',
    component: ShellPageComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardPageComponent },
      { path: 'incomes', component: IncomesPageComponent },
      { path: 'expenses', component: ExpensesPageComponent },
      { path: 'goals', component: GoalsPageComponent },
      { path: 'categories', component: CategoriesPageComponent, canActivate: [adminGuard] },
      { path: 'users', component: UsersPageComponent, canActivate: [adminGuard] }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
