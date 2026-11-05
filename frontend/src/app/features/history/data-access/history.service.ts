import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';

import type {PagedResponse} from '../../../core/models/paged-response';
import type {
  AuditEvent,
  AuditFilters,
  PeriodFilters,
  TimelineEvent
} from './history.models';

@Injectable({
  providedIn: 'root'
})
export class HistoryService {
  private readonly http = inject(HttpClient);

  findTimeline(studentId: string, filters: PeriodFilters, page = 0) {
    const params = this.buildParams({
      from: filters.from,
      to: filters.to
    }, page);

    return this.http.get<PagedResponse<TimelineEvent>>(
      `/api/me/students/${encodeURIComponent(studentId)}/timeline`,
      {params}
    );
  }

  findAudit(filters: AuditFilters, page = 0) {
    const params = this.buildParams(filters, page);

    return this.http.get<PagedResponse<AuditEvent>>(
      '/api/audit-events',
      {params}
    );
  }

  private buildParams(filters: AuditFilters, page: number): HttpParams {
    let params = new HttpParams()
      .set('page', page)
      .set('size', 20);

    for (const [key, value] of Object.entries(filters)) {
      if (typeof value === 'string' && value !== '') {
        params = params.set(key, value);
      }
    }

    return params;
  }
}
