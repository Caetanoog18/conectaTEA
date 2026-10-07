import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {RouterLink} from '@angular/router';
import {finalize} from 'rxjs';
import {Student} from '../../data-access/student.models';
import {StudentService} from '../../data-access/student.service';

@Component({
  selector: 'app-student-list',
  imports: [
    DatePipe,
    RouterLink
  ],
  templateUrl: './student-list.html',
  styleUrl: './student-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentList implements OnInit {
  private readonly studentService =
    inject(StudentService);

  readonly students = signal<Student[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionErrorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly updatingStudentId = signal<string | null>(null);
  readonly currentPage = signal(0);
  readonly pageSize = 20;
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    if (page < 0 || this.loading()) {
      return;
    }

    if (this.totalPages() > 0 && page >= this.totalPages()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.studentService
      .findAll(page, this.pageSize)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.students.set(response.content);
          this.currentPage.set(response.page);
          this.totalElements.set(response.totalElements);
          this.totalPages.set(response.totalPages);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  changeStatus(student: Student): void {
    if (this.updatingStudentId() !== null) {
      return;
    }

    const newStatus = !student.active;
    const action = newStatus ? 'ativar' : 'desativar';
    const confirmed = window.confirm(`Deseja realmente ${action} o estudante ` + `"${student.fullName}"?`);

    if (!confirmed) {
      return;
    }

    this.updatingStudentId.set(student.id);
    this.actionErrorMessage.set(null);
    this.successMessage.set(null);

    this.studentService
      .updateStatus(student.id, newStatus)
      .pipe(finalize(() => this.updatingStudentId.set(null)))
      .subscribe({
        next: updatedStudent => {
          this.students.update(students =>
            students.map(currentStudent =>
              currentStudent.id === updatedStudent.id ? updatedStudent : currentStudent));

          this.successMessage.set(
            newStatus
              ? 'Estudante ativado com sucesso.'
              : 'Estudante desativado com sucesso.'
          );
        },
        error: (error: HttpErrorResponse) => {
          this.actionErrorMessage.set(this.resolveStatusErrorMessage(error));
        }
      });
  }

  previousPage(): void {
    this.loadPage(this.currentPage() - 1);
  }

  nextPage(): void {
    this.loadPage(this.currentPage() + 1);
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Seu perfil não pode consultar estudantes.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    const detail = error.error?.detail;

    if (typeof detail === 'string' && detail) {
      return detail;
    }

    return 'Não foi possível carregar os estudantes.';
  }

  private resolveStatusErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Seu perfil não pode alterar estudantes.';
    }

    if (error.status === 404) {
      return 'Estudante não encontrado.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    const detail = error.error?.detail;

    if (typeof detail === 'string' && detail) {
      return detail;
    }

    return 'Não foi possível alterar o status do estudante.';
  }
}
