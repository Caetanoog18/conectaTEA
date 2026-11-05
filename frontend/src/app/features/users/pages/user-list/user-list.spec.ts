import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {UserList} from './user-list';
import {UserService} from '../../data-access/user.service';
import {User} from '../../data-access/user.models';
import {AuthService} from '../../../../core/auth/auth';

describe('UserList', () => {
  let component: UserList;
  let fixture: ComponentFixture<UserList>;

  const user: User = {
    id: 'user-1',
    fullName: 'Maria Professora',
    email: 'maria@example.com',
    role: 'TEACHER',
    active: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const service = {
    findAll: vi.fn(),
    updateStatus: vi.fn()
  };

  beforeEach(async () => {
    vi.resetAllMocks();

    service.findAll.mockReturnValue(of({
      content: [user],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1
    }));

    await TestBed.configureTestingModule({
      imports: [UserList],
      providers: [
        provideRouter([]),
        {provide: UserService, useValue: service},
        {
          provide: AuthService,
          useValue: {
            getCurrentUserEmail: () => 'admin@example.com'
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load users and display their roles', () => {
    expect(service.findAll).toHaveBeenCalledWith(0, 20);
    expect(component.users()).toEqual([user]);

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Maria Professora');
    expect(element.textContent).toContain('Professor');
  });

  it('should cancel without changing the status', () => {
    component.requestStatusChange(user);
    component.cancelStatusChange();

    expect(component.selectedUser()).toBeNull();
    expect(service.updateStatus).not.toHaveBeenCalled();
  });

  it('should deactivate a user after confirmation', () => {
    service.updateStatus.mockReturnValue(of({
      ...user,
      active: false
    }));

    component.requestStatusChange(user);
    component.confirmStatusChange();

    expect(service.updateStatus).toHaveBeenCalledWith(user.id, false);
    expect(component.users()[0].active).toBe(false);
    expect(component.selectedUser()).toBeNull();
    expect(component.successMessage()).toBe('Usuário desativado com sucesso.');
  });

  it('should prevent self-deactivation', () => {
    component.requestStatusChange({
      ...user,
      email: 'ADMIN@example.com'
    });

    expect(component.selectedUser()).toBeNull();
    expect(service.updateStatus).not.toHaveBeenCalled();
  });

  it('should preserve the current status when updating fails', () => {
    service.updateStatus.mockReturnValue(throwError(() => new HttpErrorResponse({status: 403})));

    component.requestStatusChange(user);
    component.confirmStatusChange();

    expect(component.users()[0].active).toBe(true);
    expect(component.actionErrorMessage()).toBeTruthy();
    expect(component.updatingStatus()).toBe(false);
  });

  it('should prevent duplicate status submissions', () => {
    const response = new Subject<User>();

    service.updateStatus.mockReturnValue(response);

    component.requestStatusChange(user);
    component.confirmStatusChange();
    component.confirmStatusChange();

    expect(service.updateStatus).toHaveBeenCalledTimes(1);
    expect(component.updatingStatus()).toBe(true);

    response.next({...user, active: false});
    response.complete();

    expect(component.updatingStatus()).toBe(false);
  });

  it('should activate an inactive user', () => {
    const inactiveUser = {...user, active: false};

    service.updateStatus.mockReturnValue(of(user));

    component.requestStatusChange(inactiveUser);
    component.confirmStatusChange();

    expect(service.updateStatus).toHaveBeenCalledWith(user.id, true);
    expect(component.successMessage()).toBe('Usuário ativado com sucesso.');
  });
});
