import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GuardianForm} from './guardian-form';
import {GuardianService} from '../../data-access/guardian.service';

describe('GuardianForm', () => {
  let component: GuardianForm;
  let fixture: ComponentFixture<GuardianForm>;

  const guardian = {
    id: 'guardian-1',
    userId: 'user-1',
    fullName: 'Maria da Silva',
    cpf: '52998224725',
    email: null,
    phone: '47999999999',
    active: true,
    createdAt: '2026-10-20T12:00:00Z',
    updatedAt: '2026-10-20T12:00:00Z'
  };

  const service = {
    findById: vi.fn(),
    create: vi.fn(),
    update: vi.fn()
  };

  async function setup(guardianId: string | null = null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [GuardianForm],
      providers: [
        provideRouter([]),
        {provide: GuardianService, useValue: service},
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(guardianId ? {guardianId} : {})
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GuardianForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.resetAllMocks();
    service.findById.mockReturnValue(of(guardian));
    service.create.mockReturnValue(of(guardian));
    service.update.mockReturnValue(of(guardian));
  });

  it('should create', async () => {
    await setup();
    expect(component).toBeTruthy();
    expect(component.editMode).toBe(false);
  });

  it('should reject an incomplete form', async () => {
    await setup();

    component.save();

    expect(service.create).not.toHaveBeenCalled();
    expect(component.form.controls.fullName.touched).toBe(true);
  });

  it('should send empty optional fields as null', async () => {
    await setup();

    service.create.mockReturnValue(throwError(() => ({status: 409})));

    component.form.setValue({
      fullName: ' Maria da Silva ',
      cpf: '529.982.247-25',
      email: '',
      phone: '47999999999'
    });

    component.save();

    expect(service.create).toHaveBeenCalledWith({
      fullName: 'Maria da Silva',
      cpf: '52998224725',
      email: null,
      phone: '47999999999',
      userId: null
    });
    expect(component.saving()).toBe(false);
    expect(component.errorMessage()).toBeTruthy();
  });

  it('should preserve the associated user when editing', async () => {
    await setup(guardian.id);

    service.update.mockReturnValue(throwError(() => ({status: 409})));

    expect(component.form.controls.cpf.value).toBe('52998224725');
    expect(component.form.valid).toBe(true);

    component.save();

    expect(service.update).toHaveBeenCalledWith(
      guardian.id,
      expect.objectContaining({
        cpf: '52998224725',
        userId: 'user-1'
      })
    );
  });

  it('should prevent saving after loading fails', async () => {
    service.findById.mockReturnValue(throwError(() => ({status: 404})));

    await setup(guardian.id);
    component.save();

    expect(component.loadFailed()).toBe(true);
    expect(service.update).not.toHaveBeenCalled();
  });

  it('should require a valid CPF', async () => {
    await setup();

    const cpf = component.form.controls.cpf;

    cpf.setValue('');
    expect(cpf.hasError('required')).toBe(true);

    cpf.setValue('11111111111');
    expect(cpf.hasError('cpf')).toBe(true);

    cpf.setValue('529.982.247-25');
    expect(cpf.valid).toBe(true);
  });
});
