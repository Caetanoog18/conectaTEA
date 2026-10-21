import {TestBed} from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree
} from '@angular/router';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {roleGuard} from './role.guard';
import {AuthService} from './auth';
import {ADMIN_ROLES, ALL_ROLES, MANAGEMENT_ROLES, UserRole} from './roles';

describe('roleGuard', () => {
  const auth = {
    isAuthenticated: vi.fn(),
    getCurrentUserRoles: vi.fn()
  };

  beforeEach(() => {
    vi.resetAllMocks();

    auth.isAuthenticated.mockReturnValue(true);
    auth.getCurrentUserRoles.mockReturnValue(['ADMINISTRATOR']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: auth}
      ]
    });
  });

  function execute(roles?: readonly UserRole[]) {
    return TestBed.runInInjectionContext(() =>
      roleGuard(
        {
          data: roles ? {roles} : {}
        } as ActivatedRouteSnapshot,
        {
          url: '/students'
        } as RouterStateSnapshot
      )
    );
  }

  it.each([...ALL_ROLES])(
    'should enforce management permissions for %s',
    role => {
      auth.getCurrentUserRoles.mockReturnValue([role]);

      const result = execute(MANAGEMENT_ROLES);

      if (role === 'ADMINISTRATOR' || role === 'PEDAGOGICAL_COORDINATOR') {
        expect(result).toBe(true);
      } else {
        expect(result).toBeInstanceOf(UrlTree);
        expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/access-denied');
      }
    }
  );

  it.each([...ALL_ROLES])(
    'should restrict user administration for %s',
    role => {
      auth.getCurrentUserRoles.mockReturnValue([role]);

      const result = execute(ADMIN_ROLES);

      if (role === 'ADMINISTRATOR') {
        expect(result).toBe(true);
      } else {
        expect(result).toBeInstanceOf(UrlTree);
        expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/access-denied');
      }
    }
  );

  it('should allow authenticated access to unrestricted pages', () => {
    auth.getCurrentUserRoles.mockReturnValue(['LEGAL_GUARDIAN']);

    expect(execute()).toBe(true);
  });

  it('should redirect unauthenticated users to login', () => {
    auth.isAuthenticated.mockReturnValue(false);

    const result = execute(MANAGEMENT_ROLES);

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fstudents');
  });
});
