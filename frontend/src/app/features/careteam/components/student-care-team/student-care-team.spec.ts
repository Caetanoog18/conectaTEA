import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {of, throwError} from 'rxjs';
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
    findByStudent: vi.fn()
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
});
