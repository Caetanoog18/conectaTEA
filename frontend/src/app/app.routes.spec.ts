import {describe, expect, it} from 'vitest';

import {routes} from './app.routes';
import {authGuard} from './core/auth/auth.guard';
import {roleGuard} from './core/auth/role.guard';
import {ADMIN_ROLES, MANAGEMENT_ROLES} from './core/auth/roles';

describe('Application route permissions', () => {
  const shell = routes.find(route => route.path === '');
  const children = shell?.children ?? [];

  it('should check authentication on child navigation', () => {
    expect(shell?.canActivateChild).toContain(authGuard);
    expect(shell?.canActivateChild).toContain(roleGuard);
  });

  it('should protect every student and guardian management route', () => {
    const managementRoutes = children.filter(route =>
      route.path?.startsWith('students') ||
      route.path?.startsWith('guardians')
    );

    expect(managementRoutes.length).toBeGreaterThan(0);

    for (const route of managementRoutes) {
      expect(route.data?.['roles']).toEqual(MANAGEMENT_ROLES);
    }
  });

  it('should protect every user management route', () => {
    const userRoutes = children.filter(route =>
      route.path?.startsWith('users')
    );

    expect(userRoutes).toHaveLength(2);

    for (const route of userRoutes) {
      expect(route.data?.['roles']).toEqual(ADMIN_ROLES);
    }
  });

  it('should keep the denied page reachable by authenticated users', () => {
    const deniedRoute = children.find(route =>
      route.path === 'access-denied');

    expect(deniedRoute).toBeDefined();
    expect(deniedRoute?.data?.['roles']).toBeUndefined();
  });
});
