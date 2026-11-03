import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';

import type {PagedResponse} from '../../../core/models/paged-response';
import type {
  CreateObservationRequest,
  GenerateReportRequest,
  Observation,
  StudentReport
} from './student-records.models';

@Injectable({
  providedIn: 'root'
})
export class StudentRecordsService {
  private readonly http = inject(HttpClient);

  findAll(studentId: string, page = 0, size = 20): Observable<PagedResponse<Observation>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<PagedResponse<Observation>>(
      `/api/me/students/${studentId}/observations`,
      {params}
    );
  }

  create(
    studentId: string,
    request: CreateObservationRequest
  ): Observable<Observation> {
    return this.http.post<Observation>(
      `/api/me/students/${studentId}/observations`,
      request
    );
  }

  generate(studentId: string, request: GenerateReportRequest): Observable<StudentReport> {
    return this.http.post<StudentReport>(`/api/me/students/${studentId}/reports`, request);
  }

  generatePdf(studentId: string, request: GenerateReportRequest): Observable<Blob> {
    return this.http.post(
      `/api/me/students/${studentId}/reports/pdf`, request, {responseType: 'blob'});
  }
}
