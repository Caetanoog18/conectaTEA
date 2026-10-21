import {Routes} from '@angular/router';
import {authGuard} from './core/auth/auth.guard';
import {roleGuard} from './core/auth/role.guard';
import {ADMIN_ROLES, MANAGEMENT_ROLES} from './core/auth/roles';

const managementAccess = {
  roles: MANAGEMENT_ROLES
};

const administratorAccess = {
  roles: ADMIN_ROLES
};

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
    canActivateChild: [authGuard, roleGuard],
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
        path: 'access-denied',
        loadComponent: () =>
          import('./features/access/pages/access-denied/access-denied')
            .then(component => component.AccessDenied)
      },
      {
        path: 'students/new',
        data: managementAccess,
        loadComponent: () =>
          import('./features/students/pages/student-form/student-form')
            .then(component => component.StudentForm)
      },
      {
        path: 'students/:studentId/edit',
        data: managementAccess,
        loadComponent: () =>
          import('./features/students/pages/student-form/student-form')
            .then(component => component.StudentForm)
      },
      {
        path: 'students/:studentId/guardians/new',
        data: managementAccess,
        loadComponent: () =>
          import(
            './features/students/pages/student-guardian-form/student-guardian-form'
            ).then(component => component.StudentGuardianForm)
      },
      {
        path: 'students/:studentId/guardians/:guardianId/edit',
        data: managementAccess,
        loadComponent: () =>
          import(
            './features/students/pages/student-guardian-form/student-guardian-form'
            ).then(component => component.StudentGuardianForm)
      },
      {
        path: 'students/:studentId/care-team/new',
        data: managementAccess,
        loadComponent: () =>
          import(
            './features/careteam/pages/professional-link-form/professional-link-form'
            ).then(component => component.ProfessionalLinkForm)
      },
      {
        path: 'students/:studentId/guardian-links/:linkId/consents/new',
        data: managementAccess,
        loadComponent: () =>
          import(
            './features/consents/pages/consent-form/consent-form'
            ).then(component => component.ConsentForm)
      },
      {
        path: 'students/:studentId/guardian-links/:linkId/consents',
        data: managementAccess,
        loadComponent: () =>
          import(
            './features/consents/pages/consent-history/consent-history'
            ).then(component => component.ConsentHistory)
      },
      {
        path: 'students/:studentId',
        data: managementAccess,
        loadComponent: () =>
          import(
            './features/students/pages/student-detail/student-detail'
            ).then(component => component.StudentDetail)
      },
      {
        path: 'students',
        data: managementAccess,
        loadComponent: () =>
          import('./features/students/pages/student-list/student-list')
            .then(component => component.StudentList)
      },
      {
        path: 'guardians/new',
        data: managementAccess,
        loadComponent: () =>
          import('./features/guardians/pages/guardian-form/guardian-form')
            .then(component => component.GuardianForm)
      },
      {
        path: 'guardians/:guardianId/edit',
        data: managementAccess,
        loadComponent: () =>
          import('./features/guardians/pages/guardian-form/guardian-form')
            .then(component => component.GuardianForm)
      },
      {
        path: 'guardians',
        data: managementAccess,
        loadComponent: () =>
          import('./features/guardians/pages/guardian-list/guardian-list')
            .then(component => component.GuardianList)
      },
      {
        path: 'users/new',
        data: administratorAccess,
        loadComponent: () =>
          import('./features/users/pages/user-form/user-form')
            .then(component => component.UserForm)
      },
      {
        path: 'users',
        data: administratorAccess,
        loadComponent: () =>
          import('./features/users/pages/user-list/user-list')
            .then(component => component.UserList)
      },
      {
        path: 'institution',
        loadComponent: () =>
          import('./features/institution/pages/institution-page/institution-page')
            .then(component => component.InstitutionPage)
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
    redirectTo: 'dashboard'
  }
];
