import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {InstitutionPage} from './institution-page';
import {InstitutionService} from '../../data-access/institution.service';
import {Institution} from '../../data-access/institution.models';
import {AuthService} from '../../../../core/auth/auth';

describe('InstitutionPage', () => {
  let component: InstitutionPage;
  let fixture: ComponentFixture<InstitutionPage>;

  const institution: Institution = {
    id: 'institution-1',
    name: 'Escola ConectaTEA',
    taxId: null,
    email: 'escola@example.com',
    phone: null,
    street: null,
    addressNumber: null,
    complement: null,
    district: null,
    city: 'Blumenau',
    state: 'SC',
    postalCode: null,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const service = {
    find: vi.fn(),
    create: vi.fn(),
    update: vi.fn()
  };

  const auth = {
    getCurrentUserRoles: vi.fn()
  };

  async function setup(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [InstitutionPage],
      providers: [
        {provide: InstitutionService, useValue: service},
        {provide: AuthService, useValue: auth}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InstitutionPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.resetAllMocks();

    auth.getCurrentUserRoles.mockReturnValue(['ADMINISTRATOR']);
    service.find.mockReturnValue(of(institution));
    service.create.mockReturnValue(of(institution));
    service.update.mockReturnValue(of(institution));
  });

  it('should load the existing institution', async () => {
    await setup();

    expect(component.institution()).toEqual(institution);
    expect(component.form.controls.name.value).toBe(institution.name);
    expect(component.ready()).toBe(true);
  });

  it('should update an existing institution', async () => {
    await setup();

    component.form.controls.name.setValue(' Escola Atualizada ');
    component.save();

    expect(service.update).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Escola Atualizada',
        taxId: null,
        phone: null
      })
    );
    expect(service.create).not.toHaveBeenCalled();
    expect(component.successMessage()).toBeTruthy();
  });

  it('should create only when the initial request returns 404', async () => {
    service.find.mockReturnValue(throwError(() => new HttpErrorResponse({status: 404})));

    await setup();

    component.form.patchValue({
      name: 'Nova Escola',
      email: 'nova@example.com',
      city: 'Blumenau',
      state: 'sc'
    });

    component.save();

    expect(service.create).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Nova Escola',
        state: 'SC',
        taxId: null,
        postalCode: null
      })
    );
    expect(service.update).not.toHaveBeenCalled();
  });

  it('should prevent saving after a loading failure', async () => {
    service.find.mockReturnValue(throwError(() => new HttpErrorResponse({status: 500})));

    await setup();

    component.save();

    expect(component.ready()).toBe(false);
    expect(service.create).not.toHaveBeenCalled();
    expect(service.update).not.toHaveBeenCalled();
  });

  it('should allow consultation without administrative editing', async () => {
    auth.getCurrentUserRoles.mockReturnValue(['TEACHER']);

    await setup();

    component.save();

    expect(component.institution()).toEqual(institution);
    expect(component.isAdmin).toBe(false);
    expect(service.create).not.toHaveBeenCalled();
    expect(service.update).not.toHaveBeenCalled();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('button[type="submit"]')).toBeNull();
    expect(element.querySelector('fieldset')?.disabled).toBe(true);
  });

  it('should reject invalid mandatory fields', async () => {
    await setup();

    component.form.controls.state.setValue('Santa Catarina');
    component.save();

    expect(component.form.controls.state.invalid).toBe(true);
    expect(service.update).not.toHaveBeenCalled();
  });

  it('should preserve values when saving fails', async () => {
    service.update.mockReturnValue(throwError(() => new HttpErrorResponse({status: 400})));

    await setup();

    component.form.controls.name.setValue('Nome alterado');
    component.save();

    expect(component.form.controls.name.value).toBe('Nome alterado');
    expect(component.errorMessage()).toBeTruthy();
    expect(component.saving()).toBe(false);
  });

  it('should prevent duplicate submissions', async () => {
    const response = new Subject<Institution>();

    service.update.mockReturnValue(response);

    await setup();

    component.save();
    component.save();

    expect(service.update).toHaveBeenCalledTimes(1);
    expect(component.saving()).toBe(true);

    response.next(institution);
    response.complete();

    expect(component.saving()).toBe(false);
  });
});
