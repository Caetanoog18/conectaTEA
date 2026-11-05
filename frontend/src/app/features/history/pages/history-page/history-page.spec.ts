import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {BehaviorSubject} from 'rxjs';
import {afterEach, describe, expect, it} from 'vitest';
import {HistoryPage} from './history-page';

describe('HistoryPage', () => {
  const firstId = '11111111-1111-4111-8111-111111111111';
  const secondId = '22222222-2222-4222-8222-222222222222';

  let fixture: ComponentFixture<HistoryPage>;
  let component: HistoryPage;
  let http: HttpTestingController;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const observation = {
    id: 'event-1',
    type: 'OBSERVATION',
    studentId: firstId,
    authorId: 'author-1',
    authorName: 'Profissional de teste',
    purpose: 'MULTIPROFESSIONAL_MONITORING',
    title: 'Observação de teste',
    content: 'Conteúdo da observação.',
    occurredAt: '2026-10-21T15:30:00Z',
    recordedAt: '2026-10-21T15:35:00Z'
  };

  function page<T>(content: T[], index = 0, pages = 1, count = content.length) {
    return {
      content,
      page: index,
      size: 20,
      totalElements: count,
      totalPages: pages
    };
  }

  async function setup(mode: 'timeline' | 'audit'): Promise<void> {
    params = new BehaviorSubject(convertToParamMap(
      mode === 'timeline' ? {studentId: firstId} : {}
    ));

    await TestBed.configureTestingModule({
      imports: [HistoryPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {data: {historyMode: mode}},
            paramMap: params.asObservable()
          }
        }
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(HistoryPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function timelineRequest(id = firstId) {
    return http.expectOne(request =>
      request.method === 'GET' &&
      request.url === `/api/me/students/${id}/timeline`
    );
  }

  function auditRequest() {
    return http.expectOne(request =>
      request.method === 'GET' && request.url === '/api/audit-events'
    );
  }

  afterEach(() => {
    http.verify();
  });

  it('should display authorized observations and use server pagination', async () => {
    await setup('timeline');

    const request = timelineRequest();

    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.has('purpose')).toBe(false);

    request.flush(page([observation], 0, 2, 21));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(observation.title);
    expect(fixture.nativeElement.textContent).toContain(observation.authorName);
    expect(fixture.nativeElement.textContent)
      .toContain('Acompanhamento multiprofissional');

    component.changePage(1);

    const next = timelineRequest();
    expect(next.request.params.get('page')).toBe('1');
    next.flush(page([], 1, 2, 21));
  });

  it('should cancel the old request when changing students', async () => {
    await setup('timeline');

    const oldRequest = timelineRequest();

    params.next(convertToParamMap({studentId: secondId}));

    expect(oldRequest.cancelled).toBe(true);
    timelineRequest(secondId).flush(page([], 0, 0));

    expect(component.studentId()).toBe(secondId);
    expect(component.timeline()).toEqual([]);
  });

  it('should clear previous observations when access is denied', async () => {
    await setup('timeline');
    timelineRequest().flush(page([observation]));

    component.retry();
    timelineRequest().flush(null, {status: 403, statusText: 'Forbidden'});
    fixture.detectChanges();

    expect(component.timeline()).toEqual([]);
    expect(fixture.nativeElement.textContent)
      .toContain('Você não tem acesso à linha do tempo');
    expect(fixture.nativeElement.textContent).not.toContain(observation.content);
  });

  it('should convert Brazilian local dates and keep the applied filters', async () => {
    await setup('audit');
    auditRequest().flush(page([], 0, 0));

    component.form.patchValue({
      from: '01/10/2026 08:30',
      to: '02/10/2026 18:00',
      studentId: firstId,
      action: 'MEDICAL_DOCUMENT_DOWNLOAD',
      outcome: 'SUCCESS'
    });

    component.applyFilters();

    const filtered = auditRequest();
    const query = filtered.request.params;

    expect(query.get('from')).toBe(new Date(2026, 9, 1, 8, 30).toISOString());
    expect(query.get('to')).toBe(new Date(2026, 9, 2, 18, 0).toISOString());
    expect(query.get('studentId')).toBe(firstId);
    expect(query.get('action')).toBe('MEDICAL_DOCUMENT_DOWNLOAD');
    expect(query.get('outcome')).toBe('SUCCESS');
    expect(query.has('actorUserId')).toBe(false);

    filtered.flush(page([], 0, 2, 21));

    component.form.controls.outcome.setValue('DENIED');
    component.changePage(1);

    const next = auditRequest();
    expect(next.request.params.get('outcome')).toBe('SUCCESS');
    expect(next.request.params.get('page')).toBe('1');
    next.flush(page([], 1, 2, 21));
  });

  it('should reject impossible dates, reversed periods and invalid UUIDs', async () => {
    await setup('audit');
    auditRequest().flush(page([], 0, 0));

    component.form.patchValue({from: '31/02/2026 09:00'});
    component.applyFilters();
    expect(component.filterError()).toContain('dd/mm/aaaa');

    component.form.patchValue({
      from: '02/10/2026 09:00',
      to: '01/10/2026 09:00'
    });
    component.applyFilters();
    expect(component.filterError()).toContain('anterior');

    component.form.patchValue({from: '', to: '', requestId: 'abc'});
    component.applyFilters();
    expect(component.filterError()).toContain('UUID');

    http.expectNone(request => request.url === '/api/audit-events');
  });

  it('should show the audit outcome and retry a failed consultation', async () => {
    await setup('audit');

    auditRequest().flush(null, {
      status: 503,
      statusText: 'Service Unavailable'
    });

    expect(component.error()).toBeTruthy();
    component.retry();

    auditRequest().flush(page([{
      id: 'event-2',
      actorUserId: null,
      action: 'MEDICAL_DOCUMENT_DOWNLOAD',
      outcome: 'DENIED',
      studentId: firstId,
      resourceId: null,
      requestId: secondId,
      occurredAt: '2026-10-21T15:30:00Z'
    }]));

    fixture.detectChanges();

    expect(component.error()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Download de laudo médico');
    expect(fixture.nativeElement.textContent).toContain('Acesso negado');
    expect(fixture.nativeElement.textContent).toContain(secondId);
  });
});
