import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot, UrlTree} from '@angular/router';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {adminGuard} from './admin.guard';
import {AuthService} from './auth';

describe('adminGuard', () => {
  const auth = {
    isAuthenticated: vi.fn(),
    getCurrentUserRoles: vi.fn()
  };

  function executeGuard() {
    return TestBed.runInInjectionContext(() =>
      adminGuard(
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot
      )
    );
  }

  beforeEach(() => {
    vi.resetAllMocks();

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: auth}
      ]
    });

    auth.isAuthenticated.mockReturnValue(true);
    auth.getCurrentUserRoles.mockReturnValue(['ADMINISTRATOR']);
  });

  it('should allow an administrator', () => {
    expect(executeGuard()).toBe(true);
  });

  it('should redirect another role to the dashboard', () => {
    auth.getCurrentUserRoles.mockReturnValue(['TEACHER']);

    const result = executeGuard();

    expect(result instanceof UrlTree).toBe(true);
    expect(result.toString()).toBe('/dashboard');
  });

  it('should redirect an unauthenticated user to login', () => {
    auth.isAuthenticated.mockReturnValue(false);

    const result = executeGuard();

    expect(result instanceof UrlTree).toBe(true);
    expect(result.toString()).toBe('/login');
  });
});
