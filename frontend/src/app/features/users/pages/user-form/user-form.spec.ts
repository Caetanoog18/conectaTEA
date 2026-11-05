import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {UserForm} from './user-form';
import {UserService} from '../../data-access/user.service';
import {User} from '../../data-access/user.models';

describe('UserForm', () => {
  let component: UserForm;
  let fixture: ComponentFixture<UserForm>;

  const user: User = {
    id: 'user-1',
    fullName: 'Maria da Silva',
    email: 'maria@example.com',
    role: 'TEACHER',
    active: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const service = {
    create: vi.fn()
  };

  function fillForm(): void {
    component.form.setValue({
      fullName: ' Maria da Silva ',
      cpf: '529.982.247-25',
      email: 'maria@example.com',
      role: 'TEACHER',
      password: 'StrongPassword123!',
      confirmPassword: 'StrongPassword123!'
    });
  }

  beforeEach(async () => {
    vi.resetAllMocks();

    service.create.mockReturnValue(of(user));

    await TestBed.configureTestingModule({
      imports: [UserForm],
      providers: [
        provideRouter([]),
        {provide: UserService, useValue: service}
      ]
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(UserForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should reject an incomplete form', () => {
    component.save();

    expect(service.create).not.toHaveBeenCalled();
    expect(component.form.controls.fullName.touched).toBe(true);
  });

  it('should require a valid CPF', () => {
    fillForm();

    component.form.controls.cpf.setValue('11111111111');
    component.save();

    expect(component.form.controls.cpf.hasError('cpf')).toBe(true);
    expect(service.create).not.toHaveBeenCalled();
  });

  it('should reject different passwords', () => {
    fillForm();

    component.form.controls.confirmPassword.setValue('AnotherPassword123!');
    component.save();

    expect(component.form.hasError('passwordMismatch')).toBe(true);
    expect(service.create).not.toHaveBeenCalled();
  });

  it('should create a user with normalized CPF', () => {
    fillForm();
    component.save();

    expect(service.create).toHaveBeenCalledWith({
      fullName: 'Maria da Silva',
      cpf: '52998224725',
      email: 'maria@example.com',
      role: 'TEACHER',
      password: 'StrongPassword123!'
    });

    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/users']);
    expect(component.form.controls.password.value).toBe('');
    expect(component.form.controls.confirmPassword.value).toBe('');
  });

  it('should preserve registration data after a conflict', () => {
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({status: 409})));

    fillForm();
    component.save();

    expect(component.form.controls.email.value).toBe('maria@example.com');
    expect(component.errorMessage()).toBeTruthy();
    expect(component.saving()).toBe(false);
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  });

  it('should prevent duplicate submissions', () => {
    const response = new Subject<User>();

    service.create.mockReturnValue(response);

    fillForm();
    component.save();
    component.save();

    expect(service.create).toHaveBeenCalledTimes(1);

    response.next(user);
    response.complete();

    expect(component.saving()).toBe(false);
  });
});
