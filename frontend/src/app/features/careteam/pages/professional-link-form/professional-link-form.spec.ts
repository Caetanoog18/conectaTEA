import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter,
  Router
} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProfessionalLinkForm} from './professional-link-form';
import {CareTeamService} from '../../data-access/care-team.service';
import {ProfessionalLink, ProfessionalOption} from '../../data-access/care-team.models';
import {StudentService} from '../../../students/data-access/student.service';
import {Student} from '../../../students/data-access/student.models';

describe('ProfessionalLinkForm', () => {
  let component: ProfessionalLinkForm;
  let fixture: ComponentFixture<ProfessionalLinkForm>;

  const student: Student = {
    id: 'student-1',
    fullName: 'João da Silva',
    cpf: '52998224725',
    preferredName: null,
    birthDate: '2015-03-12',
    enrollmentNumber: 'MAT-001',
    schoolYear: 2026,
    gradeLevel: '5º ano',
    className: 'Turma A',
    active: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const professional: ProfessionalOption = {
    id: 'professional-1',
    fullName: 'Maria Professora',
    email: 'teacher@example.com',
    role: 'TEACHER'
  };

  const link: ProfessionalLink = {
    id: 'link-1',
    studentId: student.id,
    studentName: student.fullName,
    professionalId: professional.id,
    professionalName: professional.fullName,
    professionalRole: professional.role,
    startedOn: '2026-01-01',
    endedOn: null,
    active: true,
    createdByUserId: 'admin-1',
    endedByUserId: null,
    endReason: null,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const studentServiceMock = {
    findById: vi.fn()
  };

  const careTeamServiceMock = {
    findByStudent: vi.fn(),
    findProfessionals: vi.fn(),
    create: vi.fn()
  };

  beforeEach(async () => {
    vi.resetAllMocks();

    studentServiceMock.findById.mockReturnValue(of(student));
    careTeamServiceMock.findByStudent.mockReturnValue(of([]));
    careTeamServiceMock.findProfessionals.mockReturnValue(of({
      content: [professional],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1
    }));
    careTeamServiceMock.create.mockReturnValue(of(link));

    await TestBed.configureTestingModule({
      imports: [ProfessionalLinkForm],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({studentId: student.id})
            }
          }
        },
        {
          provide: StudentService,
          useValue: studentServiceMock
        },
        {
          provide: CareTeamService,
          useValue: careTeamServiceMock
        }
      ]
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(ProfessionalLinkForm);
    component = fixture.componentInstance;
  });

  it('should require a professional before saving', () => {
    fixture.detectChanges();

    component.save();

    expect(careTeamServiceMock.create).not.toHaveBeenCalled();
    expect(component.errorMessage()).toBe('Selecione um profissional.');
  });

  it('should create a link and return to the profile', () => {
    fixture.detectChanges();

    component.selectProfessional(professional);
    component.form.controls.startedOn.setValue('2026-01-01');
    component.save();

    expect(careTeamServiceMock.create).toHaveBeenCalledWith(
      student.id,
      {
        professionalId: professional.id,
        startedOn: '2026-01-01'
      }
    );

    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/students', student.id]);
  });

  it('should reject a future start date', () => {
    fixture.detectChanges();

    const future = new Date();
    future.setUTCDate(future.getUTCDate() + 1);

    component.selectProfessional(professional);
    component.form.controls.startedOn.setValue(future.toISOString().slice(0, 10));

    component.save();

    expect(component.form.controls.startedOn.hasError('futureDate')).toBe(true);

    expect(careTeamServiceMock.create).not.toHaveBeenCalled();
  });

  it('should block a professional who already has an active link', () => {
    careTeamServiceMock.findByStudent.mockReturnValue(of([link]));

    fixture.detectChanges();

    component.selectProfessional(professional);

    expect(component.selectedProfessional()).toBeNull();
    expect(component.isAlreadyLinked(professional.id)).toBe(true);
  });

  it('should preserve the selection when creation fails', () => {
    careTeamServiceMock.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 409}))
    );

    fixture.detectChanges();

    component.selectProfessional(professional);
    component.save();

    expect(component.selectedProfessional()).toEqual(professional);
    expect(component.saving()).toBe(false);
    expect(component.errorMessage()).toContain('vínculo ativo');
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  });

  it('should prevent duplicate submissions', () => {
    const response = new Subject<ProfessionalLink>();
    careTeamServiceMock.create.mockReturnValue(response);

    fixture.detectChanges();

    component.selectProfessional(professional);
    component.save();
    component.save();

    expect(careTeamServiceMock.create).toHaveBeenCalledTimes(1);
    expect(component.saving()).toBe(true);

    response.next(link);
    response.complete();

    expect(component.saving()).toBe(false);
  });
});
