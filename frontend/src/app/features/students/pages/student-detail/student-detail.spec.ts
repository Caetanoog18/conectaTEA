import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {StudentDetail} from './student-detail';
import {StudentService} from '../../data-access/student.service';
import {StudentGuardianService} from '../../data-access/student-guardian.service';
import {Student} from '../../data-access/student.models';
import {StudentGuardian} from '../../data-access/student-guardian.models';
import {CareTeamService} from '../../../careteam/data-access/care-team.service';

describe('StudentDetail', () => {
  let component: StudentDetail;
  let fixture: ComponentFixture<StudentDetail>;

  const student: Student = {
    id: 'student-1',
    fullName: 'João da Silva',
    preferredName: null,
    birthDate: '2015-05-10',
    enrollmentNumber: 'MAT-001',
    schoolYear: 2026,
    gradeLevel: '5º ano',
    className: 'Turma B',
    active: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const link: StudentGuardian = {
    id: 'link-1',
    studentId: student.id,
    studentName: student.fullName,
    guardianId: 'guardian-1',
    guardianName: 'Maria da Silva',
    relationship: 'MOTHER',
    legalGuardian: true,
    primaryContact: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const studentServiceMock = {
    findById: vi.fn()
  };

  const linkServiceMock = {
    findByStudent: vi.fn()
  };

  const careTeamServiceMock = {
    findByStudent: vi.fn()
  };

  beforeEach(async () => {
    vi.resetAllMocks();

    studentServiceMock.findById.mockReturnValue(of(student));
    linkServiceMock.findByStudent.mockReturnValue(of([link]));
    careTeamServiceMock.findByStudent.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [StudentDetail],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(
              convertToParamMap({studentId: student.id})
            )
          }
        },
        {
          provide: StudentService,
          useValue: studentServiceMock
        },
        {
          provide: StudentGuardianService,
          useValue: linkServiceMock
        },
        {
          provide: CareTeamService,
          useValue: careTeamServiceMock
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentDetail);
    component = fixture.componentInstance;
  });

  it('should display the student and linked guardians', () => {
    fixture.detectChanges();

    expect(studentServiceMock.findById).toHaveBeenCalledWith(student.id);
    expect(linkServiceMock.findByStudent).toHaveBeenCalledWith(student.id);
    expect(careTeamServiceMock.findByStudent).toHaveBeenCalledWith(student.id);
    expect(component.student()).toEqual(student);
    expect(component.guardians()).toEqual([link]);

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain(student.fullName);
    expect(element.textContent).toContain(link.guardianName);
    expect(element.textContent).toContain('Mãe');
    expect(element.textContent).toContain('Equipe multiprofissional');
  });

  it('should preserve student details when guardian loading fails', () => {
    linkServiceMock.findByStudent.mockReturnValue(throwError(() => new HttpErrorResponse({status: 500})));

    fixture.detectChanges();

    expect(component.student()).toEqual(student);
    expect(component.guardiansErrorMessage()).toBeTruthy();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain(student.fullName);
    expect(element.textContent).toContain('Não foi possível carregar os responsáveis vinculados.');
    expect(element.textContent).not.toContain('Nenhum responsável vinculado');
  });

  it('should display a message when the student does not exist', () => {
    studentServiceMock.findById.mockReturnValue(throwError(() => new HttpErrorResponse({status: 404})));

    fixture.detectChanges();

    expect(component.student()).toBeNull();
    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBe('Estudante não encontrado.');
    expect(careTeamServiceMock.findByStudent).not.toHaveBeenCalled();
  });
});
