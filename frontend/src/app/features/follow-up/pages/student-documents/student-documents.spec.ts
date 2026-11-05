import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {BehaviorSubject, of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {StudentDocuments} from './student-documents';
import {AccessService} from '../../../../core/auth/access.service';
import {FollowUpService} from '../../data-access/follow-up.service';
import {MedicalDocumentService} from '../../data-access/medical-document.service';
import {MedicalDocumentDownloadService} from '../../data-access/medical-document-download.service';
import type {Student} from '../../../students/data-access/student.models';
import type {MedicalDocument} from '../../data-access/medical-document.models';

describe('StudentDocuments', () => {
  let component: StudentDocuments;
  let fixture: ComponentFixture<StudentDocuments>;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const student: Student = {
    id: 'student-1',
    fullName: 'Ana Souza',
    preferredName: null,
    birthDate: '2015-05-10',
    enrollmentNumber: 'MAT-1',
    schoolYear: 2026,
    gradeLevel: '5º ano',
    className: 'B',
    cpf: '52998224725',
    active: true,
    createdAt: '2020-01-01T12:00:00Z',
    updatedAt: '2020-01-01T12:00:00Z'
  };

  const document: MedicalDocument = {
    id: 'document-1',
    studentId: student.id,
    title: 'Avaliação médica',
    issuedOn: '2020-01-02',
    uploadedByUserId: 'physician-1',
    uploadedByName: 'Médica de teste',
    sizeBytes: 1024,
    createdAt: '2020-01-02T12:00:00Z'
  };

  const page = {
    content: [document],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1
  };

  const service = {
    findAll: vi.fn(),
    upload: vi.fn(),
    download: vi.fn()
  };

  const followUp = {
    findById: vi.fn()
  };

  const download = {
    save: vi.fn()
  };

  const access = {
    hasAnyRole: vi.fn()
  };

  function fillForm(): void {
    component.form.setValue({
      title: ' Avaliação médica ',
      issuedOn: '02/01/2020'
    });

    component.file.set(new File(
      ['%PDF-1.4'],
      'laudo.pdf',
      {type: 'application/pdf'}
    ));
  }

  function selectFile(file: File): void {
    const input = {
      files: {
        item: () => file
      },
      value: 'laudo.pdf'
    };

    component.selectFile(
      {target: input} as unknown as Event
    );
  }

  beforeEach(async () => {
    vi.resetAllMocks();

    params = new BehaviorSubject(
      convertToParamMap({studentId: student.id})
    );

    access.hasAnyRole.mockReturnValue(true);
    followUp.findById.mockReturnValue(of(student));
    service.findAll.mockReturnValue(of(page));
    service.upload.mockReturnValue(of(document));
    service.download.mockReturnValue(of(new Blob(
      ['%PDF-1.4'],
      {type: 'application/pdf'}
    )));

    await TestBed.configureTestingModule({
      imports: [StudentDocuments],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {paramMap: params}
        },
        {
          provide: AccessService,
          useValue: access
        },
        {
          provide: FollowUpService,
          useValue: followUp
        },
        {
          provide: MedicalDocumentService,
          useValue: service
        },
        {
          provide: MedicalDocumentDownloadService,
          useValue: download
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentDocuments);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('should display authorized metadata and Brazilian dates', () => {
    const element: HTMLElement = fixture.nativeElement;

    expect(service.findAll).toHaveBeenCalledWith(student.id, 0, 20);
    expect(element.textContent).toContain(document.title);
    expect(element.textContent).toContain('02/01/2020');
    expect(element.textContent).toContain(document.uploadedByName);
  });

  it('should not expose the upload form to other professional roles', () => {
    fixture.destroy();

    access.hasAnyRole.mockReturnValue(false);

    fixture = TestBed.createComponent(StudentDocuments);
    component = fixture.componentInstance;

    fixture.detectChanges();

    fillForm();
    component.upload();

    expect(component.canUpload).toBe(false);
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(service.upload).not.toHaveBeenCalled();
  });

  it('should reject missing fields and missing file', () => {
    component.upload();

    expect(service.upload).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBe(true);
    expect(component.actionError()).toBeTruthy();
  });

  it('should reject invalid dates', () => {
    fillForm();

    component.form.controls.issuedOn.setValue('31/02/2020');
    component.upload();

    expect(service.upload).not.toHaveBeenCalled();
  });

  it('should trim the title and send an ISO date without a timezone conversion', () => {
    fillForm();
    component.upload();

    expect(service.upload).toHaveBeenCalledWith(student.id, {
      title: 'Avaliação médica',
      issuedOn: '2020-01-02',
      file: expect.any(File)
    });

    expect(component.file()).toBeNull();
    expect(component.form.controls.title.value).toBe('');
    expect(component.successMessage()).toBe('Laudo enviado com sucesso.');
    expect(service.findAll).toHaveBeenCalledTimes(2);
  });

  it('should preserve the form and file after a server failure', () => {
    service.upload.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 503}))
    );

    fillForm();
    component.upload();

    expect(component.form.controls.title.value)
      .toBe(' Avaliação médica ');

    expect(component.file()).not.toBeNull();
    expect(component.uploading()).toBe(false);
    expect(component.actionError()).toBeTruthy();
  });

  it('should prevent duplicate uploads while a request is pending', () => {
    const response = new Subject<MedicalDocument>();

    service.upload.mockReturnValue(response);

    fillForm();
    component.upload();
    component.upload();

    expect(service.upload).toHaveBeenCalledTimes(1);

    response.next(document);
    response.complete();

    expect(component.busy()).toBe(false);
  });

  it('should reject non-PDF files', () => {
    selectFile(new File(
      ['text'],
      'document.txt',
      {type: 'text/plain'}
    ));

    expect(component.file()).toBeNull();
    expect(component.actionError()).toContain('PDF');
  });

  it('should reject oversized or empty files', () => {
    selectFile(new File(
      [],
      'laudo.pdf',
      {type: 'application/pdf'}
    ));

    expect(component.file()).toBeNull();

    selectFile(new File(
      [new Uint8Array(10 * 1024 * 1024 + 1)],
      'laudo.pdf',
      {type: 'application/pdf'}
    ));

    expect(component.file()).toBeNull();
    expect(component.actionError()).toContain('10 MiB');
  });

  it('should download an authorized PDF', () => {
    component.downloadDocument(document);

    expect(service.download).toHaveBeenCalledWith(
      student.id,
      document.id
    );

    expect(download.save).toHaveBeenCalledWith(
      expect.any(Blob),
      document.id
    );

    expect(component.downloadingId()).toBeNull();
  });

  it('should reject a non-PDF download response', () => {
    service.download.mockReturnValue(of(new Blob(
      ['{}'],
      {type: 'application/json'}
    )));

    component.downloadDocument(document);

    expect(download.save).not.toHaveBeenCalled();
    expect(component.actionError()).toContain('PDF válido');
  });

  it('should clear sensitive data when a download is denied', () => {
    service.download.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 403}))
    );

    component.downloadDocument(document);

    expect(component.student()).toBeNull();
    expect(component.documents()).toEqual([]);
    expect(component.pageError()).toBeTruthy();
  });

  it('should clear prior data if loading another student fails', () => {
    followUp.findById.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 403}))
    );

    params.next(convertToParamMap({
      studentId: 'student-2'
    }));

    expect(component.student()).toBeNull();
    expect(component.documents()).toEqual([]);
    expect(component.pageError()).toBeTruthy();
  });

  it('should cancel a pending download when the route changes', () => {
    const response = new Subject<Blob>();

    service.download.mockReturnValue(response);
    component.downloadDocument(document);

    followUp.findById.mockReturnValue(of({
      ...student,
      id: 'student-2'
    }));

    service.findAll.mockReturnValue(of({
      ...page,
      content: []
    }));

    params.next(convertToParamMap({
      studentId: 'student-2'
    }));

    response.next(new Blob(
      ['%PDF-1.4'],
      {type: 'application/pdf'}
    ));
    response.complete();

    expect(download.save).not.toHaveBeenCalled();
    expect(component.student()?.id).toBe('student-2');
    expect(component.documents()).toEqual([]);
  });

  it('should reject metadata belonging to another student', () => {
    service.findAll.mockReturnValue(of({
      ...page,
      content: [{
        ...document,
        studentId: 'student-2'
      }]
    }));

    component.loadPage(0);

    expect(component.documents()).toEqual([]);
    expect(component.pageError()).toBeTruthy();
  });

  it('should not download a document belonging to another student', () => {
    component.downloadDocument({
      ...document,
      studentId: 'student-2'
    });

    expect(service.download).not.toHaveBeenCalled();
  });

  it('should not load a page outside the available range', () => {
    component.loadPage(1);

    expect(service.findAll).toHaveBeenCalledTimes(1);
  });
});
