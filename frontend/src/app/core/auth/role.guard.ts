import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';

import {AuthService} from './auth';
import {AccessService} from './access.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const accessService = inject(AccessService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login'], {
      queryParams: {
        returnUrl: state.url
      }
    });
  }

  const allowedRoles: unknown = route.data['roles'];

  if (allowedRoles === undefined) {
    return true;
  }

  if (
    Array.isArray(allowedRoles) &&
    allowedRoles.every(role => typeof role === 'string') &&
    accessService.hasAnyRole(allowedRoles)
  ) {
    return true;
  }

  return router.createUrlTree(['/access-denied']);
};
