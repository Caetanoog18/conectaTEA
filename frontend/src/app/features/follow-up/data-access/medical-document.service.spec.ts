import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {MedicalDocumentService} from './medical-document.service';

describe('MedicalDocumentService', () => {
  let service: MedicalDocumentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(MedicalDocumentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should request a page of metadata', () => {
    service.findAll('student-1', 1, 20).subscribe();

    const req = http.expectOne(request => request.url === '/api/me/students/student-1/medical-documents');

    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('20');

    req.flush({
      content: [],
      page: 1,
      size: 20,
      totalElements: 0,
      totalPages: 0
    });
  });

  it('should send multipart with an ISO date and no client author', () => {
    const file = new File(['%PDF-1.4'], 'laudo.pdf', {type: 'application/pdf'});

    service.upload('student-1', {
      title: 'Laudo',
      issuedOn: '2020-01-02',
      file
    }).subscribe();

    const req = http.expectOne('/api/me/students/student-1/medical-documents');

    const body = req.request.body as FormData;

    expect(req.request.method).toBe('POST');
    expect(body.get('title')).toBe('Laudo');
    expect(body.get('issuedOn')).toBe('2020-01-02');
    expect(body.get('file')).toBe(file);
    expect(body.has('uploadedByUserId')).toBe(false);
    expect(req.request.headers.has('Content-Type')).toBe(false);

    req.flush({});
  });

  it('should request the file through HttpClient as a blob', () => {
    service.download('student-1', 'document-1').subscribe();

    const req = http.expectOne('/api/me/students/student-1/medical-documents/document-1/file');

    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');

    req.flush(new Blob(['%PDF-1.4'], {type: 'application/pdf'}));
  });
});
