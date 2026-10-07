import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {StudentList} from './student-list';
import {StudentService} from '../../data-access/student.service';

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
    findAll: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    studentServiceMock.findAll.mockReturnValue(
      of(emptyPage)
    );

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
});
