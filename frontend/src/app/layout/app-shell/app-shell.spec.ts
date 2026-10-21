import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from '@angular/router';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AppShell} from './app-shell';
import {AuthService} from '../../core/auth/auth';

describe('AppShell', () => {
  let component: AppShell;
  let fixture: ComponentFixture<AppShell>;

  const auth = {
    getCurrentUserEmail: vi.fn(),
    getCurrentUserRoles: vi.fn(),
    logout: vi.fn()
  };

  async function setup(role = 'ADMINISTRATOR'): Promise<void> {
    auth.getCurrentUserEmail.mockReturnValue('user@example.com');
    auth.getCurrentUserRoles.mockReturnValue([role]);

    await TestBed.configureTestingModule({
      imports: [AppShell],
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: auth}
      ]
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate')
      .mockResolvedValue(true);

    fixture = TestBed.createComponent(AppShell);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('should start with the mobile menu closed', async () => {
    await setup();

    expect(component.menuOpen()).toBe(false);
  });

  it('should toggle and close the mobile menu', async () => {
    await setup();

    component.toggleMenu();
    expect(component.menuOpen()).toBe(true);

    component.closeMenu();
    expect(component.menuOpen()).toBe(false);
  });

  it('should display administrative links for administrators', async () => {
    await setup();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('a[href="/students"]')).not.toBeNull();
    expect(element.querySelector('a[href="/guardians"]')).not.toBeNull();
    expect(element.querySelector('a[href="/users"]')).not.toBeNull();
  });

  it('should hide user management from coordinators', async () => {
    await setup('PEDAGOGICAL_COORDINATOR');

    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('a[href="/students"]')).not.toBeNull();
    expect(element.querySelector('a[href="/users"]')).toBeNull();
  });

  it('should hide administrative links from teachers', async () => {
    await setup('TEACHER');

    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('a[href="/students"]')).toBeNull();
    expect(element.querySelector('a[href="/guardians"]')).toBeNull();
    expect(element.querySelector('a[href="/users"]')).toBeNull();
    expect(element.querySelector('a[href="/institution"]')).not.toBeNull();
  });

  it('should clear the session and return to login on logout', async () => {
    await setup();

    component.toggleMenu();
    component.logout();

    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(component.menuOpen()).toBe(false);
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login']);
  });
});
