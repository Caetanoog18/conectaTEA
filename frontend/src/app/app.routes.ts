import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login').then(
        (component) => component.Login
      )
  },
  {
    path: '',
    loadComponent: () =>
      import('./layout/app-shell/app-shell').then(
        (component) => component.AppShell
      ),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard/dashboard').then(
            (component) => component.Dashboard
          )
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
