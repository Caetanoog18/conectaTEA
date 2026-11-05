import { inject, Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import { PagedResponse } from '../../../core/models/paged-response';
import {Guardian, GuardianRequest} from './guardian.models';

@Injectable({
  providedIn: 'root'
})
export class GuardianService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/guardians';

  findAll(page = 0, size = 20): Observable<PagedResponse<Guardian>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<Guardian>>(this.apiUrl, { params });
  }

  findById(guardianId: string): Observable<Guardian> {
    return this.http.get<Guardian>(`${this.apiUrl}/${guardianId}`);
  }

  create(request: GuardianRequest): Observable<Guardian> {
    return this.http.post<Guardian>(this.apiUrl, request);
  }

  update(guardianId: string, request: GuardianRequest): Observable<Guardian> {
    return this.http.put<Guardian>(`${this.apiUrl}/${guardianId}`, request);
  }

  updateStatus(guardianId: string, active: boolean): Observable<Guardian> {
    return this.http.patch<Guardian>(`${this.apiUrl}/${guardianId}/status`, { active });
  }
}
