import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CreateStudentGuardianRequest, StudentGuardian, UpdateStudentGuardianRequest} from './student-guardian.models';

@Injectable({
  providedIn: 'root'
})
export class StudentGuardianService {
  private readonly http = inject(HttpClient);

  findByStudent(studentId: string): Observable<StudentGuardian[]> {
    return this.http.get<StudentGuardian[]>(
      `/api/students/${studentId}/guardians`
    );
  }

  create(studentId: string, request: CreateStudentGuardianRequest): Observable<StudentGuardian> {
    return this.http.post<StudentGuardian>(
      `/api/students/${studentId}/guardians`,
      request
    );
  }

  update(studentId: string, guardianId: string, request: UpdateStudentGuardianRequest): Observable<StudentGuardian> {
    return this.http.put<StudentGuardian>(
      `/api/students/${studentId}/guardians/${guardianId}`,
      request
    );
  }
}
