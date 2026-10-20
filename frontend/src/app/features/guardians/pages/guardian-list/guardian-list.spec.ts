import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GuardianList} from './guardian-list';
import {GuardianService} from '../../data-access/guardian.service';
import {Guardian} from '../../data-access/guardian.models';

describe('GuardianList', () => {
  let component: GuardianList;
  let fixture: ComponentFixture<GuardianList>;

  const guardianServiceMock = {
    findAll: vi.fn(),
    updateStatus: vi.fn()
  };

  const emptyPage = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0
  };

  const guardian: Guardian = {
    id: 'guardian-1',
    userId: null,
    fullName: 'Maria da Silva',
    cpf: '52998224725',
    email: null,
    phone: '47999999999',
    active: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  beforeEach(async () => {
    vi.resetAllMocks();

    guardianServiceMock.findAll.mockReturnValue(of(emptyPage));

    await TestBed.configureTestingModule({
      imports: [GuardianList],
      providers: [
        provideRouter([]),
        {
          provide: GuardianService,
          useValue: guardianServiceMock
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GuardianList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the first page', () => {
    expect(guardianServiceMock.findAll).toHaveBeenCalledWith(0, 20);
    expect(component.guardians()).toEqual([]);
    expect(component.currentPage()).toBe(0);
    expect(component.totalElements()).toBe(0);
    expect(component.totalPages()).toBe(0);
  });

  it('should cancel without changing the status', () => {
    component.requestStatusChange(guardian);

    expect(component.selectedGuardian()).toEqual(guardian);

    component.cancelStatusChange();

    expect(component.selectedGuardian()).toBeNull();
    expect(guardianServiceMock.updateStatus).not.toHaveBeenCalled();
  });

  it('should deactivate a guardian after confirmation', () => {
    const updatedGuardian = {
      ...guardian,
      active: false
    };

    component.guardians.set([guardian]);

    guardianServiceMock.updateStatus.mockReturnValue(of(updatedGuardian));

    component.requestStatusChange(guardian);
    component.confirmStatusChange();

    expect(guardianServiceMock.updateStatus).toHaveBeenCalledWith(guardian.id, false);
    expect(component.guardians()).toEqual([updatedGuardian]);
    expect(component.selectedGuardian()).toBeNull();
    expect(component.updatingStatus()).toBe(false);
    expect(component.successMessage()).toBe('Responsável desativado com sucesso.');
  });

  it('should activate an inactive guardian', () => {
    const inactiveGuardian = {
      ...guardian,
      active: false
    };

    component.guardians.set([inactiveGuardian]);
    guardianServiceMock.updateStatus.mockReturnValue(of(guardian));

    component.requestStatusChange(inactiveGuardian);
    component.confirmStatusChange();

    expect(guardianServiceMock.updateStatus).toHaveBeenCalledWith(guardian.id, true);
    expect(component.guardians()).toEqual([guardian]);
    expect(component.selectedGuardian()).toBeNull();
    expect(component.successMessage()).toBe('Responsável ativado com sucesso.');
  });

  it('should preserve the current status when the request fails', () => {
    component.guardians.set([guardian]);

    guardianServiceMock.updateStatus.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 403}))
    );

    component.requestStatusChange(guardian);
    component.confirmStatusChange();

    expect(component.guardians()).toEqual([guardian]);
    expect(component.selectedGuardian()).toEqual(guardian);
    expect(component.updatingStatus()).toBe(false);
    expect(component.successMessage()).toBeNull();
    expect(component.statusErrorMessage()).toBe('Seu perfil não pode alterar o status de responsáveis');
  });

  it('should prevent duplicate submissions while updating', () => {
    const response = new Subject<Guardian>();

    guardianServiceMock.updateStatus.mockReturnValue(response);
    component.guardians.set([guardian]);

    component.requestStatusChange(guardian);
    component.confirmStatusChange();
    component.confirmStatusChange();

    expect(guardianServiceMock.updateStatus).toHaveBeenCalledTimes(1);
    expect(component.updatingStatus()).toBe(true);

    response.next({...guardian, active: false});
    response.complete();

    expect(component.updatingStatus()).toBe(false);
    expect(component.guardians()[0].active).toBe(false);
  });
});
