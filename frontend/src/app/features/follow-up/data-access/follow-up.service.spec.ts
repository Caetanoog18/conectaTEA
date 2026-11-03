import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {FollowUpService} from './follow-up.service';

describe('FollowUpService', () => {
  let service: FollowUpService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(FollowUpService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should list students through the professional endpoint', () => {
    service.findAll(1, 20).subscribe();

    const request = http.expectOne(req =>
      req.url === '/api/me/students' &&
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

  it('should load the authorized student profile', () => {
    service.findById('student-1').subscribe();

    const request = http.expectOne('/api/me/students/student-1');

    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('should load authorized legal guardian contacts', () => {
    service.findLegalGuardians('student-1').subscribe();

    const request = http.expectOne('/api/me/students/student-1/legal-guardians');

    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
