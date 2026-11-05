import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ConsentHistory} from './consent-history';
import {ConsentService} from '../../data-access/consent.service';
import {Consent} from '../../data-access/consent.models';

describe('ConsentHistory', () => {
  let component: ConsentHistory;
  let fixture: ComponentFixture<ConsentHistory>;

  const consent: Consent = {
    id: 'consent-1',
    studentGuardianId: 'link-1',
    studentId: 'student-1',
    studentName: 'João da Silva',
    guardianId: 'guardian-1',
    guardianName: 'Maria da Silva',
    status: 'ACTIVE',
    purposes: [
      'EDUCATIONAL_SUPPORT',
      'INFORMATION_SHARING_WITH_CARE_TEAM'
    ],
    termsVersion: '1.0',
    grantedAt: '2026-10-20T12:00:00Z',
    validUntil: '2026-11-20',
    recordedByUserId: 'admin-1',
    revokedAt: null,
    revokedByUserId: null,
    revocationReason: null,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const service = {
    findHistory: vi.fn(),
    revoke: vi.fn()
  };

  beforeEach(async () => {
    vi.resetAllMocks();

    service.findHistory.mockReturnValue(of([consent]));

    await TestBed.configureTestingModule({
      imports: [ConsentHistory],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(
              convertToParamMap({
                studentId: 'student-1',
                linkId: 'link-1'
              })
            )
          }
        },
        {
          provide: ConsentService,
          useValue: service
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConsentHistory);
    component = fixture.componentInstance;
  });

  it('should display consent details and authorized purposes', () => {
    fixture.detectChanges();

    expect(service.findHistory).toHaveBeenCalledWith('link-1');
    expect(component.consents()).toEqual([consent]);

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Maria da Silva');
    expect(element.textContent).toContain('João da Silva');
    expect(element.textContent).toContain('Ativo');
    expect(element.textContent).toContain('Apoio educacional');
    expect(element.textContent).toContain('Compartilhamento com a equipe');
  });

  it('should display an empty history', () => {
    service.findHistory.mockReturnValue(of([]));

    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Nenhum consentimento registrado para este vínculo.');
    expect(component.errorMessage()).toBeNull();
  });

  it('should display revoked consent details', () => {
    service.findHistory.mockReturnValue(of([
      {
        ...consent,
        status: 'REVOKED',
        revokedAt: '2026-10-21T12:00:00Z',
        revokedByUserId: 'admin-1',
        revocationReason: 'Solicitação do responsável legal.'
      }
    ]));

    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Revogado');
    expect(element.textContent).toContain('Solicitação do responsável legal.');
  });

  it('should display the expired status returned by the API', () => {
    service.findHistory.mockReturnValue(of([
      {
        ...consent,
        status: 'EXPIRED'
      }
    ]));

    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Expirado');
  });

  it('should show an error and allow retrying', () => {
    service.findHistory.mockReturnValue(throwError(() => new HttpErrorResponse({status: 500})));

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Não foi possível carregar o histórico de consentimentos.');

    service.findHistory.mockReturnValue(of([consent]));

    const element: HTMLElement = fixture.nativeElement;
    element.querySelector<HTMLButtonElement>('button')!.click();

    fixture.detectChanges();

    expect(service.findHistory).toHaveBeenCalledTimes(2);
    expect(component.errorMessage()).toBeNull();
    expect(element.textContent).toContain('Maria da Silva');
  });

  it('should reject consent data from another link', () => {
    service.findHistory.mockReturnValue(of([
      {
        ...consent,
        studentGuardianId: 'another-link'
      }
    ]));

    fixture.detectChanges();

    expect(component.consents()).toEqual([]);
    expect(component.errorMessage()).toBe('Os consentimentos recebidos não correspondem ao vínculo informado.');
  });

  it('should cancel revocation without calling the API', () => {
    fixture.detectChanges();

    component.requestRevocation(consent);
    component.revokeForm.controls.reason.setValue('Solicitação do responsável.');

    component.cancelRevocation();

    expect(component.selectedConsent()).toBeNull();
    expect(component.revokeForm.controls.reason.value).toBe('');
    expect(service.revoke).not.toHaveBeenCalled();
  });

  it('should reject a revocation reason containing only spaces', () => {
    fixture.detectChanges();

    component.requestRevocation(consent);
    component.revokeForm.controls.reason.setValue('   ');

    component.confirmRevocation();

    expect(component.revokeForm.controls.reason.invalid).toBe(true);
    expect(component.revokeForm.controls.reason.touched).toBe(true);
    expect(service.revoke).not.toHaveBeenCalled();
  });

  it('should reject a revocation reason longer than 500 characters', () => {
    fixture.detectChanges();

    component.requestRevocation(consent);
    component.revokeForm.controls.reason.setValue('a'.repeat(501));

    component.confirmRevocation();

    expect(component.revokeForm.controls.reason.hasError('maxlength')).toBe(true);

    expect(service.revoke).not.toHaveBeenCalled();
  });

  it('should revoke consent and preserve its history', () => {
    const revokedConsent: Consent = {
      ...consent,
      status: 'REVOKED',
      revokedAt: '2026-10-21T12:00:00Z',
      revokedByUserId: 'admin-1',
      revocationReason: 'Solicitação do responsável legal.'
    };

    service.revoke.mockReturnValue(of(revokedConsent));

    fixture.detectChanges();

    component.requestRevocation(consent);
    component.revokeForm.controls.reason.setValue(' Solicitação do responsável legal. ');

    component.confirmRevocation();
    fixture.detectChanges();

    expect(service.revoke).toHaveBeenCalledWith(
      consent.id,
      {
        reason: 'Solicitação do responsável legal.'
      }
    );

    expect(component.consents()).toEqual([revokedConsent]);
    expect(component.selectedConsent()).toBeNull();
    expect(component.revoking()).toBe(false);
    expect(component.successMessage()).toBe('Consentimento revogado com sucesso. O histórico foi preservado.');

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Revogado');
    expect(element.textContent).toContain('Solicitação do responsável legal.');
    expect(element.querySelector('.consent-actions')).toBeNull();
  });

  it('should not allow selecting expired or revoked consent', () => {
    fixture.detectChanges();

    component.requestRevocation({
      ...consent,
      status: 'EXPIRED'
    });

    expect(component.selectedConsent()).toBeNull();

    component.requestRevocation({
      ...consent,
      status: 'REVOKED'
    });

    expect(component.selectedConsent()).toBeNull();
    expect(service.revoke).not.toHaveBeenCalled();
  });

  it('should preserve consent and reason when revocation fails', () => {
    service.revoke.mockReturnValue(throwError(() => new HttpErrorResponse({status: 403})));

    fixture.detectChanges();

    component.requestRevocation(consent);
    component.revokeForm.controls.reason.setValue('Solicitação do responsável legal.');

    component.confirmRevocation();

    expect(component.consents()).toEqual([consent]);
    expect(component.selectedConsent()).toEqual(consent);
    expect(component.revokeForm.controls.reason.value).toBe('Solicitação do responsável legal.');
    expect(component.revoking()).toBe(false);
    expect(component.successMessage()).toBeNull();
    expect(component.actionErrorMessage()).toBe('Seu perfil não pode revogar este consentimento.');
  });

  it('should prevent duplicate submissions while revoking', () => {
    const response = new Subject<Consent>();

    service.revoke.mockReturnValue(response);

    fixture.detectChanges();

    component.requestRevocation(consent);
    component.revokeForm.controls.reason.setValue('Solicitação do responsável legal.');

    component.confirmRevocation();
    component.confirmRevocation();

    expect(component.revoking()).toBe(true);
    expect(service.revoke).toHaveBeenCalledTimes(1);

    response.next({
      ...consent,
      status: 'REVOKED',
      revokedAt: '2026-10-21T12:00:00Z',
      revokedByUserId: 'admin-1',
      revocationReason: 'Solicitação do responsável legal.'
    });
    response.complete();

    expect(component.revoking()).toBe(false);
    expect(component.selectedConsent()).toBeNull();
  });

  it('should allow refreshing history after a revocation conflict', () => {
    service.revoke.mockReturnValue(throwError(() => new HttpErrorResponse({status: 409})));

    fixture.detectChanges();

    component.requestRevocation(consent);
    component.revokeForm.controls.reason.setValue('Solicitação do responsável legal.');

    component.confirmRevocation();

    expect(component.actionErrorMessage()).toBe(
      'O consentimento pode estar expirado ou já revogado. Atualize o histórico.'
    );

    service.findHistory.mockReturnValue(of([
      {
        ...consent,
        status: 'EXPIRED'
      }
    ]));

    component.load();

    expect(component.consents()[0].status).toBe('EXPIRED');
    expect(component.selectedConsent()).toBeNull();
    expect(component.actionErrorMessage()).toBeNull();
  });

  it('should display a loading state while waiting for the API', () => {
    const response = new Subject<Consent[]>();

    service.findHistory.mockReturnValue(response);

    fixture.detectChanges();

    expect(component.loading()).toBe(true);

    response.next([consent]);
    response.complete();

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.consents()).toEqual([consent]);
  });
});
