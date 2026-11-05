import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {StudentCareTeam} from './student-care-team';
import {CareTeamService} from '../../data-access/care-team.service';
import {ProfessionalLink} from '../../data-access/care-team.models';
import {provideRouter} from '@angular/router';


describe('StudentCareTeam', () => {
  let component: StudentCareTeam;
  let fixture: ComponentFixture<StudentCareTeam>;

  const activeLink: ProfessionalLink = {
    id: 'link-1',
    studentId: 'student-1',
    studentName: 'João da Silva',
    professionalId: 'professional-1',
    professionalName: 'Maria Professora',
    professionalRole: 'TEACHER',
    startedOn: '2026-10-01',
    endedOn: null,
    active: true,
    createdByUserId: 'admin-1',
    endedByUserId: null,
    endReason: null,
    createdAt: '2026-10-01T12:00:00Z',
    updatedAt: '2026-10-01T12:00:00Z'
  };

  const endedLink: ProfessionalLink = {
    ...activeLink,
    id: 'link-2',
    professionalId: 'professional-2',
    professionalName: 'Carlos Médico',
    professionalRole: 'PHYSICIAN',
    active: false,
    endedOn: '2026-10-15',
    endedByUserId: 'admin-1',
    endReason: 'Encerramento do acompanhamento'
  };

  const serviceMock = {
    findByStudent: vi.fn(),
    end: vi.fn()
  };

  beforeEach(async () => {
    vi.resetAllMocks();

    serviceMock.findByStudent.mockReturnValue(of([activeLink, endedLink]));

    await TestBed.configureTestingModule({
      imports: [StudentCareTeam],
      providers: [
        provideRouter([]),
        {
          provide: CareTeamService,
          useValue: serviceMock,
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentCareTeam);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('studentId', 'student-1');
  });

  it('should display active and ended links', () => {
    fixture.detectChanges();

    expect(serviceMock.findByStudent).toHaveBeenCalledWith('student-1');

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Maria Professora');
    expect(element.textContent).toContain('Carlos Médico');
    expect(element.textContent).toContain('Professor');
    expect(element.textContent).toContain('Médico');
    expect(element.textContent).toContain('Encerrado');
    expect(element.textContent).toContain('Encerramento do acompanhamento');
  });

  it('should display an empty state when no links exist', () => {
    serviceMock.findByStudent.mockReturnValue(of([]));

    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Nenhum profissional vinculado a este estudante.');
  });

  it('should show an error and allow retrying', () => {
    serviceMock.findByStudent.mockReturnValue(throwError(() => new HttpErrorResponse({status: 500})));

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Não foi possível carregar a equipe multiprofissional.');

    serviceMock.findByStudent.mockReturnValue(of([activeLink]));

    const element: HTMLElement = fixture.nativeElement;
    element.querySelector<HTMLButtonElement>('button')!.click();

    fixture.detectChanges();

    expect(component.errorMessage()).toBeNull();
    expect(element.textContent).toContain('Maria Professora');
    expect(serviceMock.findByStudent).toHaveBeenCalledTimes(2);
  });

  it('should load the team again when the student changes', () => {
    fixture.detectChanges();

    serviceMock.findByStudent.mockReturnValue(of([]));

    fixture.componentRef.setInput('studentId', 'student-2');
    fixture.detectChanges();

    expect(serviceMock.findByStudent).toHaveBeenLastCalledWith('student-2');
    expect(component.links()).toEqual([]);
  });

  it('should cancel without ending the link', () => {
    fixture.detectChanges();

    component.requestEnd(activeLink);
    component.endForm.controls.reason.setValue('Motivo de teste');

    component.cancelEnd();

    expect(component.selectedLink()).toBeNull();
    expect(component.endForm.controls.reason.value).toBe('');
    expect(serviceMock.end).not.toHaveBeenCalled();
  });

  it('should require a nonblank reason', () => {
    fixture.detectChanges();

    component.requestEnd(activeLink);
    component.endForm.controls.reason.setValue('   ');

    component.confirmEnd();

    expect(component.endForm.invalid).toBe(true);
    expect(component.endForm.controls.reason.touched).toBe(true);
    expect(serviceMock.end).not.toHaveBeenCalled();
  });

  it('should reject reasons longer than 500 characters', () => {
    fixture.detectChanges();

    component.requestEnd(activeLink);
    component.endForm.controls.reason.setValue('a'.repeat(501));

    component.confirmEnd();

    expect(component.endForm.controls.reason.hasError('maxlength'))
      .toBe(true);

    expect(serviceMock.end).not.toHaveBeenCalled();
  });

  it('should end the link and preserve the history', () => {
    const updatedLink: ProfessionalLink = {
      ...activeLink,
      active: false,
      endedOn: '2026-10-20',
      endedByUserId: 'admin-1',
      endReason: 'Fim do acompanhamento'
    };

    serviceMock.end.mockReturnValue(of(updatedLink));

    fixture.detectChanges();

    component.requestEnd(activeLink);
    component.endForm.controls.reason.setValue(
      '  Fim do acompanhamento  '
    );

    component.confirmEnd();
    fixture.detectChanges();

    expect(serviceMock.end).toHaveBeenCalledWith(
      activeLink.id,
      {reason: 'Fim do acompanhamento'}
    );

    expect(component.links()).toEqual([updatedLink, endedLink]);
    expect(component.selectedLink()).toBeNull();
    expect(component.ending()).toBe(false);

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent)
      .toContain('Fim do acompanhamento');

    expect(element.textContent)
      .toContain('Vínculo encerrado com sucesso.');

    expect(element.querySelectorAll('tbody button').length).toBe(0);
  });

  it('should not select an already ended link', () => {
    fixture.detectChanges();

    component.requestEnd(endedLink);

    expect(component.selectedLink()).toBeNull();
    expect(serviceMock.end).not.toHaveBeenCalled();
  });

  it('should preserve the reason and current status when ending fails', () => {
    serviceMock.end.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 403}))
    );

    fixture.detectChanges();

    component.requestEnd(activeLink);
    component.endForm.controls.reason.setValue(
      'Fim do acompanhamento'
    );

    component.confirmEnd();

    expect(component.links()[0].active).toBe(true);
    expect(component.selectedLink()).toEqual(activeLink);
    expect(component.endForm.controls.reason.value)
      .toBe('Fim do acompanhamento');

    expect(component.ending()).toBe(false);
    expect(component.actionErrorMessage())
      .toBe('Seu perfil não pode encerrar este vínculo.');
  });

  it('should prevent duplicate submissions while ending', () => {
    const response = new Subject<ProfessionalLink>();

    serviceMock.end.mockReturnValue(response);

    fixture.detectChanges();

    component.requestEnd(activeLink);
    component.endForm.controls.reason.setValue(
      'Fim do acompanhamento'
    );

    component.confirmEnd();
    component.confirmEnd();

    expect(serviceMock.end).toHaveBeenCalledTimes(1);
    expect(component.ending()).toBe(true);

    response.next({
      ...activeLink,
      active: false,
      endedOn: '2026-10-20',
      endedByUserId: 'admin-1',
      endReason: 'Fim do acompanhamento'
    });
    response.complete();

    expect(component.ending()).toBe(false);
  });

  it('should clear the confirmation when the student changes', () => {
    fixture.detectChanges();

    component.requestEnd(activeLink);
    component.endForm.controls.reason.setValue('Motivo anterior');

    serviceMock.findByStudent.mockReturnValue(of([]));

    fixture.componentRef.setInput('studentId', 'student-2');
    fixture.detectChanges();

    expect(component.selectedLink()).toBeNull();
    expect(component.endForm.controls.reason.value).toBe('');
    expect(component.links()).toEqual([]);
  });
});
