import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {StudentList} from './student-list';
import {StudentService} from '../../data-access/student.service';
import {Student} from '../../data-access/student.models';

describe('StudentList', () => {
  let component: StudentList;
  let fixture: ComponentFixture<StudentList>;

  const emptyPage = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0
  };

  const studentServiceMock = {
    findAll: vi.fn(),
    updateStatus: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    studentServiceMock.findAll.mockReturnValue(of(emptyPage));

    await TestBed.configureTestingModule({
      imports: [
        StudentList
      ],
      providers: [
        provideRouter([]),
        {
          provide: StudentService,
          useValue: studentServiceMock
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the first page', () => {
    expect(studentServiceMock.findAll).toHaveBeenCalledWith(0, 20);
    expect(component.currentPage()).toBe(0);
    expect(component.totalElements()).toBe(0);
    expect(component.totalPages()).toBe(0);
    expect(component.students()).toEqual([]);
  });

  it('should deactivate a student after confirmation', () => {
    const student: Student = {
      id: 'student-1',
      fullName: 'Ana Beatriz Souza',
      preferredName: 'Ana',
      birthDate: '2016-04-15',
      enrollmentNumber: 'MAT-2026-TESTE-001',
      schoolYear: 2026,
      gradeLevel: '4º ano',
      className: 'Turma B',
      active: true,
      createdAt: '2026-10-07T00:00:00Z',
      updatedAt: '2026-10-07T00:00:00Z'
    };

    const updatedStudent: Student = {
      ...student,
      active: false,
      updatedAt: '2026-10-07T00:10:00Z'
    };

    studentServiceMock.updateStatus.mockReturnValue(of(updatedStudent));

    vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.students.set([student]);
    component.changeStatus(student);

    expect(window.confirm).toHaveBeenCalled();
    expect(studentServiceMock.updateStatus).toHaveBeenCalledWith(student.id, false);
    expect(component.students()[0].active).toBeFalsy();
    expect(component.successMessage()).toBe('Estudante desativado com sucesso.');
    expect(component.actionErrorMessage()).toBeNull();
  });

  it('should not change status when confirmation is cancelled', () => {
    const student: Student = {
      id: 'student-1',
      fullName: 'Ana Beatriz Souza',
      preferredName: 'Ana',
      birthDate: '2016-04-15',
      enrollmentNumber: 'MAT-2026-TESTE-001',
      schoolYear: 2026,
      gradeLevel: '4º ano',
      className: 'Turma B',
      active: true,
      createdAt: '2026-10-07T00:00:00Z',
      updatedAt: '2026-10-07T00:00:00Z'
    };

    vi.spyOn(window, 'confirm').mockReturnValue(false);

    component.students.set([student]);
    component.changeStatus(student);

    expect(studentServiceMock.updateStatus).not.toHaveBeenCalled();
    expect(component.students()[0].active).toBeTruthy();
  });
});
