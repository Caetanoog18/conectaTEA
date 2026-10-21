import {TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AccessService} from './access.service';
import {AuthService} from './auth';
import {UserRole} from './roles';

describe('AccessService', () => {
  const auth = {
    getCurrentUserRoles: vi.fn()
  };

  beforeEach(() => {
    vi.resetAllMocks();

    TestBed.configureTestingModule({
      providers: [
        {provide: AuthService, useValue: auth}
      ]
    });
  });

  function getAccess(role: UserRole): AccessService {
    auth.getCurrentUserRoles.mockReturnValue([role]);

    return TestBed.inject(AccessService);
  }

  it('should expose administrative navigation to administrators', () => {
    const access = getAccess('ADMINISTRATOR');

    expect(access.navigation.map(item => item.path)).toEqual([
      '/dashboard',
      '/students',
      '/guardians',
      '/users',
      '/institution'
    ]);
  });

  it('should hide user administration from coordinators', () => {
    const access = getAccess('PEDAGOGICAL_COORDINATOR');

    expect(access.canManageStudents()).toBe(true);
    expect(access.canManageUsers()).toBe(false);
    expect(access.navigation.some(item => item.path === '/users'))
      .toBe(false);
  });

  it.each<UserRole>([
    'TEACHER',
    'AEE_TEACHER',
    'PSYCHOLOGIST',
    'PHYSICIAN',
    'LEGAL_GUARDIAN'
  ])('should hide management navigation from %s', role => {
    const access = getAccess(role);

    expect(access.navigation.map(item => item.path)).toEqual([
      '/dashboard',
      '/institution'
    ]);
  });

  it('should ignore unrecognized roles', () => {
    auth.getCurrentUserRoles.mockReturnValue([
      'FACTOR_PASSWORD',
      'UNKNOWN_ROLE'
    ]);

    const access = TestBed.inject(AccessService);

    expect(access.primaryRole).toBeNull();
    expect(access.canManageStudents()).toBe(false);
    expect(access.canManageUsers()).toBe(false);
  });

  it('should handle an invalid roles value', () => {
    auth.getCurrentUserRoles.mockReturnValue(null);

    const access = TestBed.inject(AccessService);

    expect(access.roles).toEqual([]);
    expect(access.canManageUsers()).toBe(false);
  });
});
