import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PagedResponse} from '../../../core/models/paged-response';
import {Student, StudentRequest} from './student.models';

@Injectable({
  providedIn: 'root'
})
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/students';

  findAll(page = 0, size = 20): Observable<PagedResponse<Student>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<Student>>(
      this.apiUrl,
      {params}
    );
  }

  findById(studentId: string): Observable<Student> {
    return this.http.get<Student>(
      `${this.apiUrl}/${studentId}`
    );
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(
      this.apiUrl,
      request
    );
  }

  update(
    studentId: string,
    request: StudentRequest
  ): Observable<Student> {
    return this.http.put<Student>(
      `${this.apiUrl}/${studentId}`,
      request
    );
  }

  updateStatus(
    studentId: string,
    active: boolean
  ): Observable<Student> {
    return this.http.patch<Student>(
      `${this.apiUrl}/${studentId}/status`,
      {active}
    );
  }
}
