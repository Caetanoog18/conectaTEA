import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ProfessionalLink} from './care-team.models';

@Injectable({
  providedIn: 'root'
})
export class CareTeamService {
  private readonly http = inject(HttpClient);

  findByStudent(studentId: string): Observable<ProfessionalLink[]> {
    return this.http.get<ProfessionalLink[]>(`/api/students/${studentId}/care-team`);
  }
}
