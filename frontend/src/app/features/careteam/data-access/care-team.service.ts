import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PagedResponse} from '../../../core/models/paged-response';
import {
  CreateProfessionalLinkRequest,
  EndProfessionalLinkRequest,
  ProfessionalLink,
  ProfessionalOption
} from './care-team.models';

@Injectable({
  providedIn: 'root'
})
export class CareTeamService {
  private readonly http = inject(HttpClient);

  findByStudent(studentId: string): Observable<ProfessionalLink[]> {
    return this.http.get<ProfessionalLink[]>(
      `/api/students/${studentId}/care-team`
    );
  }

  findProfessionals(page = 0, size = 20): Observable<PagedResponse<ProfessionalOption>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<ProfessionalOption>>(
      '/api/care-team/professionals',
      {params}
    );
  }

  create(studentId: string, request: CreateProfessionalLinkRequest): Observable<ProfessionalLink> {
    return this.http.post<ProfessionalLink>(
      `/api/students/${studentId}/care-team`,
      request
    );
  }

  end(linkId: string, request: EndProfessionalLinkRequest): Observable<ProfessionalLink> {
    return this.http.patch<ProfessionalLink>(
      `/api/care-team-links/${linkId}/end`,
      request
    );
  }
}
