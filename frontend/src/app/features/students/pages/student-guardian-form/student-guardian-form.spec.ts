import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {StudentGuardianForm} from './student-guardian-form';
import {StudentService} from '../../data-access/student.service';
import {StudentGuardianService} from '../../data-access/student-guardian.service';
import {GuardianService} from '../../../guardians/data-access/guardian.service';
import {Student} from '../../data-access/student.models';
import {Guardian} from '../../../guardians/data-access/guardian.models';
import {StudentGuardian} from '../../data-access/student-guardian.models';

describe('StudentGuardianForm', () => {
  let component: StudentGuardianForm;
  let fixture: ComponentFixture<StudentGuardianForm>;

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

  const guardian: Guardian = {
    id: 'guardian-1',
    userId: null,
    fullName: 'Maria da Silva',
    cpf: null,
    email: null,
    phone: '47999999999',
    active: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const link: StudentGuardian = {
    id: 'link-1',
    studentId: student.id,
    studentName: student.fullName,
    guardianId: guardian.id,
    guardianName: guardian.fullName,
    relationship: 'MOTHER',
    legalGuardian: true,
    primaryContact: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const studentServiceMock = {
    findById: vi.fn()
  };

  const guardianServiceMock = {
    findAll: vi.fn()
  };

  const linkServiceMock = {
    findByStudent: vi.fn(),
    create: vi.fn(),
    update: vi.fn()
  };

  async function setup(editMode = false): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [StudentGuardianForm],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(
                editMode
                  ? {
                    studentId: student.id,
                    guardianId: guardian.id
                  }
                  : {studentId: student.id}
              )
            }
          }
        },
        {
          provide: StudentService,
          useValue: studentServiceMock
        },
        {
          provide: GuardianService,
          useValue: guardianServiceMock
        },
        {
          provide: StudentGuardianService,
          useValue: linkServiceMock
        }
      ]
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate')
      .mockResolvedValue(true);

    fixture = TestBed.createComponent(StudentGuardianForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.resetAllMocks();

    studentServiceMock.findById.mockReturnValue(of(student));
    guardianServiceMock.findAll.mockReturnValue(of({
      content: [guardian],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1
    }));

    linkServiceMock.findByStudent.mockReturnValue(of([]));
    linkServiceMock.create.mockReturnValue(of(link));
    linkServiceMock.update.mockReturnValue(of(link));
  });

  it('should require a selected guardian before creating a link', async () => {
    await setup();

    component.save();

    expect(linkServiceMock.create).not.toHaveBeenCalled();
    expect(component.errorMessage()).toBe('Selecione um responsável.');
  });

  it('should create a link and return to the student profile', async () => {
    await setup();

    component.selectGuardian(guardian);
    component.form.setValue({
      relationship: 'MOTHER',
      legalGuardian: true,
      primaryContact: true
    });

    component.save();

    expect(linkServiceMock.create).toHaveBeenCalledWith(
      student.id,
      {
        guardianId: guardian.id,
        relationship: 'MOTHER',
        legalGuardian: true,
        primaryContact: true
      }
    );

    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/students', student.id]);
  });

  it('should prevent selecting inactive or already linked guardians', async () => {
    linkServiceMock.findByStudent.mockReturnValue(of([link]));

    await setup();

    expect(component.canSelect(guardian)).toBe(false);

    component.selectGuardian({
      ...guardian,
      id: 'inactive-guardian',
      active: false
    });

    expect(component.selectedGuardian()).toBeNull();
  });

  it('should load and update an existing link', async () => {
    linkServiceMock.findByStudent.mockReturnValue(of([link]));

    await setup(true);

    expect(component.form.getRawValue()).toEqual({
      relationship: 'MOTHER',
      legalGuardian: true,
      primaryContact: true
    });

    component.form.controls.primaryContact.setValue(false);
    component.save();

    expect(linkServiceMock.update).toHaveBeenCalledWith(
      student.id,
      guardian.id,
      {
        relationship: 'MOTHER',
        legalGuardian: true,
        primaryContact: false
      }
    );

    expect(linkServiceMock.create).not.toHaveBeenCalled();
  });

  it('should preserve the form when the backend rejects a conflict', async () => {
    linkServiceMock.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 409}))
    );

    await setup();

    component.selectGuardian(guardian);
    component.save();

    expect(component.saving()).toBe(false);
    expect(component.selectedGuardian()).toEqual(guardian);
    expect(component.errorMessage()).toContain('contato principal');
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  });
});
