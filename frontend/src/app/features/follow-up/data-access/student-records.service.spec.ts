import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {StudentRecordsService} from './student-records.service';

describe('StudentRecordsService', () => {
  let service: StudentRecordsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(StudentRecordsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should request a page of observations', () => {
    service.findAll('student-1', 1, 20).subscribe();

    const request = http.expectOne(req =>
      req.url === '/api/me/students/student-1/observations' &&
      req.params.get('page') === '1' &&
      req.params.get('size') === '20'
    );

    expect(request.request.method).toBe('GET');

    request.flush({
      content: [],
      page: 1,
      size: 20,
      totalElements: 0,
      totalPages: 0
    });
  });

  it('should send only observation request fields', () => {
    const body = {
      title: 'Participação',
      content: 'Participou da atividade.',
      occurredAt: '2026-01-01T12:00:00Z'
    };

    service.create('student-1', body).subscribe();

    const request = http.expectOne('/api/me/students/student-1/observations');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.body.authorId).toBeUndefined();
    expect(request.request.body.purpose).toBeUndefined();

    request.flush({});
  });

  it('should generate a report for the requested period', () => {
    const body = {
      from: '2026-01-01T00:00:00Z',
      to: '2026-02-01T00:00:00Z'
    };

    service.generate('student-1', body).subscribe();

    const request = http.expectOne('/api/me/students/student-1/reports');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);

    request.flush({});
  });

  it('should request PDF as a blob', () => {
    service.generatePdf('student-1', {
      from: '2026-01-01T00:00:00Z',
      to: '2026-02-01T00:00:00Z'
    }).subscribe();

    const request = http.expectOne('/api/me/students/student-1/reports/pdf');

    expect(request.request.method).toBe('POST');
    expect(request.request.responseType).toBe('blob');

    request.flush(new Blob(['%PDF-1.4'], {
      type: 'application/pdf'
    }));
  });
});
