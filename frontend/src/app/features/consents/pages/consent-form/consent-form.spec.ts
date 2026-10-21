import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ConsentForm} from './consent-form';
import {ConsentService} from '../../data-access/consent.service';
import {Consent} from '../../data-access/consent.models';
import {StudentGuardianService} from '../../../students/data-access/student-guardian.service';
import {StudentGuardian} from '../../../students/data-access/student-guardian.models';

describe('ConsentForm', () => {
  let component: ConsentForm;
  let fixture: ComponentFixture<ConsentForm>;

  const link: StudentGuardian = {
    id: 'link-1',
    studentId: 'student-1',
    studentName: 'João da Silva',
    guardianId: 'guardian-1',
    guardianName: 'Maria da Silva',
    relationship: 'MOTHER',
    legalGuardian: true,
    primaryContact: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const consent: Consent = {
    id: 'consent-1',
    studentGuardianId: link.id,
    studentId: link.studentId,
    studentName: link.studentName,
    guardianId: link.guardianId,
    guardianName: link.guardianName,
    status: 'ACTIVE',
    purposes: ['EDUCATIONAL_SUPPORT'],
    termsVersion: '1.0',
    grantedAt: '2026-10-20T12:00:00Z',
    validUntil: null,
    recordedByUserId: 'admin-1',
    revokedAt: null,
    revokedByUserId: null,
    revocationReason: null,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const consentService = {
    findHistory: vi.fn(),
    create: vi.fn()
  };

  const linkService = {
    findByStudent: vi.fn()
  };

  function localDateTime(date: Date): string {
    const pad = (value: number): string =>
      String(value).padStart(2, '0');

    return (
      `${date.getFullYear()}-` +
      `${pad(date.getMonth() + 1)}-` +
      `${pad(date.getDate())}T` +
      `${pad(date.getHours())}:` +
      `${pad(date.getMinutes())}`
    );
  }

  function fillValidForm(): string {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    yesterday.setHours(12, 0, 0, 0);

    const grantedAt = localDateTime(yesterday);

    component.form.setValue({
      purposes: ['EDUCATIONAL_SUPPORT'],
      termsVersion: ' 1.0 ',
      grantedAt,
      validUntil: '',
      confirmed: true
    });

    return grantedAt;
  }

  beforeEach(async () => {
    vi.resetAllMocks();

    consentService.findHistory.mockReturnValue(of([]));
    consentService.create.mockReturnValue(of(consent));
    linkService.findByStudent.mockReturnValue(of([link]));

    await TestBed.configureTestingModule({
      imports: [ConsentForm],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                studentId: 'student-1',
                linkId: 'link-1'
              })
            }
          }
        },
        {
          provide: ConsentService,
          useValue: consentService
        },
        {
          provide: StudentGuardianService,
          useValue: linkService
        }
      ]
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate')
      .mockResolvedValue(true);

    fixture = TestBed.createComponent(ConsentForm);
    component = fixture.componentInstance;
  });

  it('should load the guardian link and consent history', () => {
    fixture.detectChanges();

    expect(linkService.findByStudent).toHaveBeenCalledWith('student-1');
    expect(consentService.findHistory).toHaveBeenCalledWith('link-1');
    expect(component.link()).toEqual(link);
    expect(component.loading()).toBe(false);
  });

  it('should reject an incomplete form', () => {
    fixture.detectChanges();

    component.save();

    expect(consentService.create).not.toHaveBeenCalled();
    expect(component.form.controls.purposes.touched).toBe(true);
    expect(component.form.controls.confirmed.invalid).toBe(true);
  });

  it('should register consent and return to its history', () => {
    fixture.detectChanges();

    const grantedAt = fillValidForm();

    component.save();

    expect(consentService.create).toHaveBeenCalledWith(
      'link-1',
      {
        purposes: ['EDUCATIONAL_SUPPORT'],
        termsVersion: '1.0',
        grantedAt: new Date(grantedAt).toISOString(),
        validUntil: null
      }
    );

    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith([
      '/students',
      'student-1',
      'guardian-links',
      'link-1',
      'consents'
    ]);
  });

  it('should reject a future grant date', () => {
    fixture.detectChanges();

    fillValidForm();

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    component.form.controls.grantedAt.setValue(localDateTime(tomorrow));

    component.save();

    expect(component.form.controls.grantedAt.hasError('futureDate')).toBe(true);

    expect(consentService.create).not.toHaveBeenCalled();
  });

  it('should prevent registration when an active consent exists', () => {
    consentService.findHistory.mockReturnValue(of([consent]));

    fixture.detectChanges();

    fillValidForm();
    component.save();

    expect(component.hasActiveConsent()).toBe(true);
    expect(consentService.create).not.toHaveBeenCalled();
  });

  it('should prevent registration for a non-legal guardian', () => {
    linkService.findByStudent.mockReturnValue(of([
      {
        ...link,
        legalGuardian: false
      }
    ]));

    fixture.detectChanges();

    fillValidForm();
    component.save();

    expect(consentService.create).not.toHaveBeenCalled();
  });

  it('should preserve form values when registration fails', () => {
    consentService.create.mockReturnValue(throwError(() => new HttpErrorResponse({status: 422})));

    fixture.detectChanges();

    const grantedAt = fillValidForm();

    component.save();

    expect(component.saving()).toBe(false);
    expect(component.errorMessage()).toBeTruthy();
    expect(component.form.controls.grantedAt.value).toBe(grantedAt);
    expect(component.form.controls.purposes.value).toEqual(['EDUCATIONAL_SUPPORT']);
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  });

  it('should prevent duplicate submissions while saving', () => {
    const response = new Subject<Consent>();

    consentService.create.mockReturnValue(response);

    fixture.detectChanges();

    fillValidForm();

    component.save();
    component.save();

    expect(component.saving()).toBe(true);
    expect(consentService.create).toHaveBeenCalledTimes(1);

    response.next(consent);
    response.complete();

    expect(component.saving()).toBe(false);
  });

  it('should prevent saving after loading fails', () => {
    linkService.findByStudent.mockReturnValue(throwError(() => new HttpErrorResponse({status: 500})));

    fixture.detectChanges();

    fillValidForm();
    component.save();

    expect(component.loadFailed()).toBe(true);
    expect(consentService.create).not.toHaveBeenCalled();
  });
});
