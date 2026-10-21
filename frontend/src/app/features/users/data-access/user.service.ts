import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PagedResponse} from '../../../core/models/paged-response';
import {CreateUserRequest, User} from './user.models';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);

  findAll(page = 0, size = 20): Observable<PagedResponse<User>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<User>>('/api/users', {params});
  }

  create(request: CreateUserRequest): Observable<User> {
    return this.http.post<User>('/api/users', request);
  }

  updateStatus(userId: string, active: boolean): Observable<User> {
    return this.http.patch<User>(
      `/api/users/${userId}/status`,
      {active}
    );
  }
}
