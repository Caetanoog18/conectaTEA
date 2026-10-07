import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PagedResponse} from '../../../core/models/paged-response';
import {Student} from './student.models';

@Injectable({
  providedIn: 'root'
})
export class StudentService {
  private readonly http = inject(HttpClient);

  findAll(page: number, size: number): Observable<PagedResponse<Student>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<PagedResponse<Student>>('/api/students', {params});
  }
}
