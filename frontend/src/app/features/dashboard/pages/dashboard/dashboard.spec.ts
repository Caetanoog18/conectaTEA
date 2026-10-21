import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Dashboard} from './dashboard';
import {AuthService} from '../../../../core/auth/auth';

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;

  const auth = {
    getCurrentUserRoles: vi.fn()
  };

  async function setup(role: string): Promise<HTMLElement> {
    auth.getCurrentUserRoles.mockReturnValue([role]);

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: auth}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    vi.resetAllMocks();
  });

  it.each([
    ['ADMINISTRATOR', 'Painel administrativo'],
    [
      'PEDAGOGICAL_COORDINATOR',
      'Painel da coordenação pedagógica'
    ],
    ['TEACHER', 'Painel do professor'],
    ['AEE_TEACHER', 'Painel do professor de AEE'],
    ['PSYCHOLOGIST', 'Painel do psicólogo'],
    ['PHYSICIAN', 'Painel do médico'],
    ['LEGAL_GUARDIAN', 'Painel do responsável legal']
  ])('should display the dashboard for %s', async (role, title) => {
    const element = await setup(role);

    expect(element.querySelector('h1')?.textContent).toContain(title);
    expect(element.textContent).not.toContain('Próximas funcionalidades');
    expect(element.textContent).not.toContain('Em desenvolvimento');
    expect(element.textContent).not.toMatch(/\betapa\s+\d+/i);
  });

  it('should expose user management to administrators', async () => {
    const element = await setup('ADMINISTRATOR');

    expect(element.querySelector('a[href="/users"]')).not.toBeNull();
  });

  it('should hide user management from coordinators', async () => {
    const element = await setup('PEDAGOGICAL_COORDINATOR');

    expect(element.querySelector('a[href="/users"]')).toBeNull();
    expect(element.querySelector('a[href="/students"]')).not.toBeNull();
  });

  it('should not expose administrative student links to physicians', async () => {
    const element = await setup('PHYSICIAN');

    expect(element.querySelector('a[href="/students"]')).toBeNull();
    expect(element.querySelector('a[href="/users"]')).toBeNull();
    expect(element.textContent).toContain('Painel do médico');
  });

  it('should explain professional access requirements', async () => {
    const element = await setup('TEACHER');

    expect(element.textContent).toContain('profissional vigente e consentimento válido');
  });

  it('should show a restricted dashboard for an unknown role', async () => {
    const element = await setup('UNKNOWN_ROLE');

    expect(element.textContent).toContain('Seu perfil não foi reconhecido');
    expect(element.querySelector('a[href="/users"]')).toBeNull();
    expect(element.querySelector('a[href="/students"]')).toBeNull();
  });
});
