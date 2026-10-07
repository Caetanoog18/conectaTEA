import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {StudentList} from './student-list';
import {StudentService} from '../../data-access/student.service';

describe('StudentList', () => {
  let component: StudentList;
  let fixture: ComponentFixture<StudentList>;

  const studentServiceStub = {
    findAll: () => of({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0
    })
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StudentList],
      providers: [
        {
          provide: StudentService,
          useValue: studentServiceStub
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
    expect(component.currentPage()).toBe(0);
    expect(component.students()).toEqual([]);
  });
});
