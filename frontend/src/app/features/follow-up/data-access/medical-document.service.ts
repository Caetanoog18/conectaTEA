import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';

import type {PagedResponse} from '../../../core/models/paged-response';
import type {MedicalDocument, UploadMedicalDocumentRequest} from './medical-document.models';

@Injectable({providedIn: 'root'})
export class MedicalDocumentService {
  private readonly http = inject(HttpClient);

  findAll(studentId: string, page = 0, size = 20): Observable<PagedResponse<MedicalDocument>> {
    return this.http.get<PagedResponse<MedicalDocument>>(
      `/api/me/students/${studentId}/medical-documents`,
      {
        params: new HttpParams()
          .set('page', page)
          .set('size', size)
      }
    );
  }

  upload(studentId: string, request: UploadMedicalDocumentRequest): Observable<MedicalDocument> {
    const body = new FormData();

    body.append('title', request.title);
    body.append('issuedOn', request.issuedOn);
    body.append('file', request.file);

    return this.http.post<MedicalDocument>(`/api/me/students/${studentId}/medical-documents`, body);
  }

  download(studentId: string, documentId: string): Observable<Blob> {
    return this.http.get(
      `/api/me/students/${studentId}/medical-documents/${documentId}/file`, {responseType: 'blob'});
  }
}
