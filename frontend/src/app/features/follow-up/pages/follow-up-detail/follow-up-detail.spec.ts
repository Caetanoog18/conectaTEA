import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {BehaviorSubject, of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {FollowUpDetail} from './follow-up-detail';
import {FollowUpService} from '../../data-access/follow-up.service';
import type {Student} from '../../../students/data-access/student.models';
import type {LegalGuardianContact} from '../../data-access/follow-up.models';

describe('FollowUpDetail', () => {
  let component: FollowUpDetail;
  let fixture: ComponentFixture<FollowUpDetail>;

  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const student: Student = {
    id: 'student-1',
    fullName: 'João da Silva',
    preferredName: null,
    birthDate: '2015-05-10',
    enrollmentNumber: 'MAT-001',
    schoolYear: 2026,
    gradeLevel: '5º ano',
    className: 'Turma B',
    cpf: '52998224725',
    active: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const guardian: LegalGuardianContact = {
    guardianId: 'guardian-1',
    fullName: 'Maria da Silva',
    relationship: 'MOTHER',
    primaryContact: true,
    email: 'maria@example.com',
    phone: '47999999999'
  };

  const service = {
    findById: vi.fn(),
    findLegalGuardians: vi.fn()
  };

  beforeEach(async () => {
    vi.resetAllMocks();

    params = new BehaviorSubject(convertToParamMap({studentId: student.id}));

    service.findById.mockReturnValue(of(student));
    service.findLegalGuardians.mockReturnValue(of([guardian]));

    await TestBed.configureTestingModule({
      imports: [FollowUpDetail],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: params.asObservable()
          }
        },
        {provide: FollowUpService, useValue: service}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FollowUpDetail);
    component = fixture.componentInstance;
  });

  it('should display the authorized student and legal guardian', () => {
    fixture.detectChanges();

    expect(service.findById).toHaveBeenCalledWith(student.id);
    expect(service.findLegalGuardians).toHaveBeenCalledWith(student.id);

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain(student.fullName);
    expect(element.textContent).toContain(guardian.fullName);
    expect(element.textContent).toContain(guardian.phone);
    expect(element.textContent).toContain('Mãe');
    expect(element.textContent).toContain('Contato principal');

    expect(element.textContent).not.toContain(student.cpf);
    expect(element.textContent).not.toContain('Editar cadastro');
  });

  it('should display an empty contact state', () => {
    service.findLegalGuardians.mockReturnValue(of([]));

    fixture.detectChanges();

    expect(component.student()).toEqual(student);
    expect(component.guardians()).toEqual([]);

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Nenhum contato de responsável legal disponível');
  });

  it('should remove displayed data when access is revoked', () => {
    fixture.detectChanges();

    service.findById.mockReturnValue(throwError(() => new HttpErrorResponse({status: 403})));

    component.refresh();
    fixture.detectChanges();

    expect(component.student()).toBeNull();
    expect(component.guardians()).toEqual([]);
    expect(component.errorMessage()).toBeTruthy();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).not.toContain(student.fullName);
    expect(element.textContent).not.toContain(guardian.fullName);
  });

  it('should not display partial data when contact loading fails', () => {
    service.findLegalGuardians.mockReturnValue(throwError(() => new HttpErrorResponse({status: 500})));

    fixture.detectChanges();

    expect(component.student()).toBeNull();
    expect(component.guardians()).toEqual([]);
    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBeTruthy();
  });

  it('should allow retrying after a loading failure', () => {
    service.findById.mockReturnValue(throwError(() => new HttpErrorResponse({status: 500})));

    fixture.detectChanges();

    service.findById.mockReturnValue(of(student));

    component.refresh();

    expect(component.errorMessage()).toBeNull();
    expect(component.student()).toEqual(student);
    expect(component.guardians()).toEqual([guardian]);
  });

  it('should cancel the previous profile when the route changes', () => {
    const previousResponse = new Subject<Student>();

    service.findById.mockReturnValue(previousResponse);

    fixture.detectChanges();

    const anotherStudent: Student = {
      ...student,
      id: 'student-2',
      fullName: 'Ana da Silva'
    };

    service.findById.mockReturnValue(of(anotherStudent));
    service.findLegalGuardians.mockReturnValue(of([]));

    params.next(convertToParamMap({studentId: anotherStudent.id}));

    previousResponse.next(student);
    previousResponse.complete();

    expect(component.student()).toEqual(anotherStudent);
    expect(component.guardians()).toEqual([]);
    expect(service.findById).toHaveBeenLastCalledWith('student-2');
  });

  it('should reject a profile belonging to another student', () => {
    service.findById.mockReturnValue(of({
      ...student,
      id: 'another-student'
    }));

    fixture.detectChanges();

    expect(component.student()).toBeNull();
    expect(component.guardians()).toEqual([]);
    expect(component.errorMessage()).toBe('Os dados recebidos não correspondem ao estudante solicitado.');
  });
});
