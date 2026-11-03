import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter
} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {StudentRecords} from './student-records';
import {FollowUpService} from '../../data-access/follow-up.service';
import {StudentRecordsService} from '../../data-access/student-records.service';
import {ReportDownloadService} from '../../data-access/report-download.service';
import type {Student} from '../../../students/data-access/student.models';
import type {Observation, StudentReport} from '../../data-access/student-records.models';

describe('StudentRecords', () => {
  let component: StudentRecords;
  let fixture: ComponentFixture<StudentRecords>;

  const student: Student = {
    id: 'student-1',
    fullName: 'João da Silva',
    preferredName: null,
    birthDate: '2015-05-10',
    enrollmentNumber: 'MAT-001',
    schoolYear: 2026,
    gradeLevel: '5º ano',
    className: 'Turma B',
    cpf: '52998224725',
    active: true,
    createdAt: '2026-01-01T12:00:00Z',
    updatedAt: '2026-01-01T12:00:00Z'
  };

  const observation: Observation = {
    id: 'observation-1',
    studentId: student.id,
    authorId: 'professional-1',
    authorName: 'Maria Professora',
    purpose: 'EDUCATIONAL_SUPPORT',
    title: 'Participação',
    content: 'Participou da atividade.',
    occurredAt: '2026-01-01T12:00:00Z',
    createdAt: '2026-01-01T12:00:00Z'
  };

  const report: StudentReport = {
    reportId: 'report-1',
    generatedAt: '2026-01-02T12:00:00Z',
    student: {
      id: student.id,
      fullName: student.fullName,
      preferredName: null,
      enrollmentNumber: student.enrollmentNumber,
      schoolYear: student.schoolYear,
      gradeLevel: student.gradeLevel,
      className: student.className
    },
    generatedBy: {
      id: 'professional-1',
      fullName: 'Maria Professora'
    },
    purpose: 'EDUCATIONAL_SUPPORT',
    from: '2026-01-01T00:00:00Z',
    to: '2026-01-02T00:00:00Z',
    totalObservations: 1,
    observations: [observation]
  };

  const service = {
    findAll: vi.fn(),
    create: vi.fn(),
    generate: vi.fn(),
    generatePdf: vi.fn()
  };

  const followUp = {
    findById: vi.fn()
  };

  const download = {
    save: vi.fn()
  };

  function fillObservation(): void {
    component.observationForm.setValue({
      title: ' Participação ',
      content: ' Participou da atividade. ',
      occurredAt: '01/01/2020 12:00'
    });
  }

  function fillPeriod(): void {
    component.reportForm.setValue({
      from: '01/01/2020 00:00',
      to: '01/02/2020 00:00'
    });
  }

  beforeEach(async () => {
    vi.resetAllMocks();

    followUp.findById.mockReturnValue(of(student));

    service.findAll.mockReturnValue(of({
      content: [observation],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1
    }));

    service.create.mockReturnValue(of(observation));
    service.generate.mockReturnValue(of(report));

    service.generatePdf.mockReturnValue(of(
      new Blob(['%PDF-1.4'], {type: 'application/pdf'})
    ));

    await TestBed.configureTestingModule({
      imports: [StudentRecords],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(
              convertToParamMap({studentId: student.id})
            )
          }
        },
        {provide: FollowUpService, useValue: followUp},
        {provide: StudentRecordsService, useValue: service},
        {provide: ReportDownloadService, useValue: download}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentRecords);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should display the student and authorized observations', () => {
    expect(service.findAll).toHaveBeenCalledWith(student.id, 0, 20);

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain(student.fullName);
    expect(element.textContent).toContain(observation.title);
    expect(element.textContent).toContain(observation.authorName);
  });

  it('should reject an incomplete observation', () => {
    component.createObservation();

    expect(service.create).not.toHaveBeenCalled();
    expect(component.observationForm.controls.title.touched).toBe(true);
  });

  it('should register observation with trimmed text and UTC date', () => {
    fillObservation();

    component.createObservation();

    expect(service.create).toHaveBeenCalledWith(student.id, {
      title: 'Participação',
      content: 'Participou da atividade.',
      occurredAt: new Date('2020-01-01T12:00').toISOString()
    });

    expect(component.observationForm.controls.content.value).toBe('');
    expect(component.successMessage()).toBe('Observação registrada com sucesso.');
  });

  it('should preserve observation values after a server failure', () => {
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({status: 503})));

    fillObservation();
    component.createObservation();

    expect(component.observationForm.controls.title.value).toBe(' Participação ');
    expect(component.createError()).toBeTruthy();
    expect(component.saving()).toBe(false);
  });

  it('should prevent duplicate observation submissions', () => {
    const response = new Subject<Observation>();

    service.create.mockReturnValue(response);

    fillObservation();

    component.createObservation();
    component.createObservation();

    expect(service.create).toHaveBeenCalledTimes(1);

    response.next(observation);
    response.complete();

    expect(component.saving()).toBe(false);
  });

  it('should reject reversed report dates', () => {
    component.reportForm.setValue({
      from: '01/02/2020 00:00',
      to: '01/01/2020 00:00'
    });

    component.generateReport();

    expect(component.reportForm.hasError('invalidPeriod')).toBe(true);
    expect(service.generate).not.toHaveBeenCalled();
  });

  it('should reject a report period longer than 366 days', () => {
    component.reportForm.setValue({
      from: '01/01/2020 00:00',
      to: '01/01/2022 00:00'
    });

    component.generateReport();

    expect(component.reportForm.hasError('periodTooLong')).toBe(true);
    expect(service.generate).not.toHaveBeenCalled();
  });

  it('should generate and display a report', () => {
    fillPeriod();
    component.generateReport();

    expect(service.generate).toHaveBeenCalledWith(student.id, {
      from: new Date('2020-01-01T00:00').toISOString(),
      to: new Date('2020-02-01T00:00').toISOString()
    });

    expect(component.report()).toEqual(report);
  });

  it('should clear the report when the period changes', () => {
    fillPeriod();
    component.generateReport();

    expect(component.report()).toEqual(report);

    component.reportForm.controls.to.setValue('01/03/2020 00:00');

    expect(component.report()).toBeNull();
  });

  it('should download PDF returned by the API', () => {
    fillPeriod();
    component.generateReport(true);

    expect(service.generatePdf).toHaveBeenCalledTimes(1);
    expect(download.save).toHaveBeenCalledWith(expect.any(Blob));
  });

  it('should reject a non-PDF download response', () => {
    service.generatePdf.mockReturnValue(of(
      new Blob(['{"error":"invalid"}'], {
        type: 'application/json'
      })
    ));

    fillPeriod();
    component.generateReport(true);

    expect(download.save).not.toHaveBeenCalled();
    expect(component.reportError()).toBe('O servidor não retornou um arquivo PDF válido.');
  });

  it('should revalidate access when report generation is forbidden', () => {
    service.generate.mockReturnValue(throwError(() => new HttpErrorResponse({status: 403})));

    followUp.findById.mockReturnValue(throwError(() => new HttpErrorResponse({status: 403})));

    fillPeriod();
    component.generateReport();

    expect(component.report()).toBeNull();
    expect(component.student()).toBeNull();
    expect(component.observations()).toEqual([]);
    expect(component.pageError()).toBeTruthy();
  });

  it('should preserve observation access when only report permission is missing', () => {
    service.generate.mockReturnValue(throwError(() => new HttpErrorResponse({status: 403})));

    fillPeriod();
    component.generateReport();

    expect(component.student()).toEqual(student);
    expect(component.observations()).toEqual([observation]);
    expect(component.reportError()).toContain('geração de relatórios');
  });

  it('should prevent overlapping report requests', () => {
    const response = new Subject<StudentReport>();

    service.generate.mockReturnValue(response);

    fillPeriod();

    component.generateReport();
    component.generateReport();
    component.generateReport(true);

    expect(service.generate).toHaveBeenCalledTimes(1);
    expect(service.generatePdf).not.toHaveBeenCalled();

    response.next(report);
    response.complete();

    expect(component.generating()).toBe(false);
  });
});
