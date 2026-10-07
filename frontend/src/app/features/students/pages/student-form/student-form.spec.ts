import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {StudentForm} from './student-form';
import {StudentService} from '../../data-access/student.service';

describe('StudentForm', () => {
  let component: StudentForm;
  let fixture: ComponentFixture<StudentForm>;

  const studentServiceMock = {
    findById: vi.fn(),
    create: vi.fn(),
    update: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [
        StudentForm
      ],
      providers: [
        provideRouter([]),
        {
          provide: StudentService,
          useValue: studentServiceMock
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start in create mode without student id', () => {
    expect(component.editMode).toBeFalsy();
    expect(component.studentId).toBeNull();
  });

  it('should keep the form invalid when required fields are empty', () => {
    component.form.patchValue({
      fullName: '',
      birthDate: '',
      enrollmentNumber: '',
      gradeLevel: '',
      className: ''
    });

    expect(component.form.invalid).toBeTruthy();
  });
});
