import {Routes} from '@angular/router';
import {authGuard} from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login')
        .then(component => component.Login)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/app-shell/app-shell')
        .then(component => component.AppShell),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard/dashboard')
            .then(component => component.Dashboard)
      },
      {
        path: 'students',
        loadComponent: () =>
          import('./features/students/pages/student-list/student-list')
            .then(component => component.StudentList)
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
