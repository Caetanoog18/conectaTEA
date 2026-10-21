import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Consent, CreateConsentRequest, RevokeConsentRequest} from './consent.models';

@Injectable({
  providedIn: 'root'
})
export class ConsentService {
  private readonly http = inject(HttpClient);

  findHistory(linkId: string): Observable<Consent[]> {
    return this.http.get<Consent[]>(
      `/api/student-guardian-links/${linkId}/consents`
    );
  }

  create(linkId: string, request: CreateConsentRequest): Observable<Consent> {
    return this.http.post<Consent>(
      `/api/student-guardian-links/${linkId}/consents`,
      request
    );
  }

  revoke(consentId: string, request: RevokeConsentRequest): Observable<Consent> {
    return this.http.patch<Consent>(
      `/api/consents/${consentId}/revoke`,
      request
    );
  }
}
