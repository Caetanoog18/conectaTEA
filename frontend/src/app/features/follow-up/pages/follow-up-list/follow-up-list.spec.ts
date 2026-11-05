import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {FollowUpList} from './follow-up-list';
import {FollowUpService} from '../../data-access/follow-up.service';
import type {AccessibleStudent} from '../../data-access/follow-up.models';
import type {PagedResponse} from '../../../../core/models/paged-response';

describe('FollowUpList', () => {
  let component: FollowUpList;
  let fixture: ComponentFixture<FollowUpList>;

  const student: AccessibleStudent = {
    id: 'student-1',
    fullName: 'João da Silva',
    preferredName: 'João',
    enrollmentNumber: 'MAT-001',
    schoolYear: 2026,
    gradeLevel: '5º ano',
    className: 'Turma B'
  };

  const page: PagedResponse<AccessibleStudent> = {
    content: [student],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1
  };

  const service = {
    findAll: vi.fn()
  };

  beforeEach(async () => {
    vi.resetAllMocks();

    service.findAll.mockReturnValue(of(page));

    await TestBed.configureTestingModule({
      imports: [FollowUpList],
      providers: [
        provideRouter([]),
        {provide: FollowUpService, useValue: service}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FollowUpList);
    component = fixture.componentInstance;
  });

  it('should load authorized students and link to follow-up', () => {
    fixture.detectChanges();

    expect(service.findAll).toHaveBeenCalledWith(0, 20);
    expect(component.students()).toEqual([student]);

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain(student.fullName);
    expect(element.querySelector('a[href="/my-students/student-1"]')).not.toBeNull();

    expect(element.querySelector('a[href="/students/student-1"]')).toBeNull();
  });

  it('should display an empty state', () => {
    service.findAll.mockReturnValue(of({
      ...page,
      content: [],
      totalElements: 0,
      totalPages: 0
    }));

    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Nenhum estudante disponível nesta página.');
  });

  it('should clear previous student data when access is denied', () => {
    fixture.detectChanges();

    service.findAll.mockReturnValue(throwError(() => new HttpErrorResponse({status: 403})));

    component.loadPage(0);

    expect(component.students()).toEqual([]);
    expect(component.totalElements()).toBe(0);
    expect(component.errorMessage()).toBeTruthy();
  });

  it('should load another page', () => {
    service.findAll.mockReturnValue(of({
      ...page,
      totalElements: 21,
      totalPages: 2
    }));

    fixture.detectChanges();

    service.findAll.mockReturnValue(of({
      ...page,
      page: 1,
      totalElements: 21,
      totalPages: 2
    }));

    component.loadPage(1);

    expect(service.findAll).toHaveBeenLastCalledWith(1, 20);
    expect(component.currentPage()).toBe(1);
  });

  it('should prevent overlapping requests', () => {
    const response = new Subject<PagedResponse<AccessibleStudent>>();

    service.findAll.mockReturnValue(response);

    fixture.detectChanges();
    component.loadPage(0);

    expect(service.findAll).toHaveBeenCalledTimes(1);
    expect(component.loading()).toBe(true);

    response.next(page);
    response.complete();

    expect(component.loading()).toBe(false);
  });
});
