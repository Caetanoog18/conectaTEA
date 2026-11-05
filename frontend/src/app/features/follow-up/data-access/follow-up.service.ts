import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';

import type {PagedResponse} from '../../../core/models/paged-response';
import type {Student} from '../../students/data-access/student.models';
import type {AccessibleStudent, LegalGuardianContact} from './follow-up.models';

@Injectable({
  providedIn: 'root'
})
export class FollowUpService {
  private readonly http = inject(HttpClient);

  findAll(page = 0, size = 20): Observable<PagedResponse<AccessibleStudent>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<AccessibleStudent>>('/api/me/students', {params});
  }

  findById(studentId: string): Observable<Student> {
    return this.http.get<Student>(`/api/me/students/${studentId}`);
  }

  findLegalGuardians(studentId: string): Observable<LegalGuardianContact[]> {
    return this.http.get<LegalGuardianContact[]>(`/api/me/students/${studentId}/legal-guardians`);
  }
}
