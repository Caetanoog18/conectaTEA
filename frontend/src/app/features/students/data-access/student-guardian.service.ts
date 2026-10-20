import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {StudentGuardian} from './student-guardian.models';

@Injectable({
  providedIn: 'root'
})
export class StudentGuardianService {
  private readonly http = inject(HttpClient);

  findByStudent(studentId: string): Observable<StudentGuardian[]> {
    return this.http.get<StudentGuardian[]>(`/api/students/${studentId}/guardians`);
  }
}
