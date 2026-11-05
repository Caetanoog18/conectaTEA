import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Institution, InstitutionRequest} from './institution.models';

@Injectable({
  providedIn: 'root'
})
export class InstitutionService {
  private readonly http = inject(HttpClient);

  find(): Observable<Institution> {
    return this.http.get<Institution>('/api/institution');
  }

  create(request: InstitutionRequest): Observable<Institution> {
    return this.http.post<Institution>('/api/institution', request);
  }

  update(request: InstitutionRequest): Observable<Institution> {
    return this.http.put<Institution>('/api/institution', request);
  }
}
