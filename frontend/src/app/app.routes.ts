import {Routes} from '@angular/router';
import {authGuard} from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login').then(component => component.Login)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/app-shell/app-shell').then(component => component.AppShell),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard/dashboard').then(component => component.Dashboard)
      },
      {
        path: 'students/new',
        loadComponent: () =>
          import('./features/students/pages/student-form/student-form').then(component => component.StudentForm)
      },
      {
        path: 'students/:studentId/edit',
        loadComponent: () =>
          import('./features/students/pages/student-form/student-form').then(component => component.StudentForm)
      },
      {
        path: 'students/:studentId/guardians/new',
        loadComponent: () =>
          import(
            './features/students/pages/student-guardian-form/student-guardian-form'
            ).then(component => component.StudentGuardianForm)
      },
      {
        path: 'students/:studentId/guardians/:guardianId/edit',
        loadComponent: () =>
          import(
            './features/students/pages/student-guardian-form/student-guardian-form'
            ).then(component => component.StudentGuardianForm)
      },
      {
        path: 'students/:studentId',
        loadComponent: () =>
          import('./features/students/pages/student-detail/student-detail')
            .then(component => component.StudentDetail)
      },
      {
        path: 'students',
        loadComponent: () =>
          import('./features/students/pages/student-list/student-list').then(component => component.StudentList)
      },
      {
        path: 'guardians',
        loadComponent: () =>
          import('./features/guardians/pages/guardian-list/guardian-list').then(component => component.GuardianList)
      },
      {
        path: 'guardians/new',
        loadComponent: () =>
          import('./features/guardians/pages/guardian-form/guardian-form')
            .then(component => component.GuardianForm)
      },
      {
        path: 'guardians/:guardianId/edit',
        loadComponent: () =>
          import('./features/guardians/pages/guardian-form/guardian-form')
            .then(component => component.GuardianForm)
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
