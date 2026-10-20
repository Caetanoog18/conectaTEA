import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {catchError, finalize, forkJoin, of, switchMap} from 'rxjs';
import {Student} from '../../data-access/student.models';
import {StudentService} from '../../data-access/student.service';
import {GUARDIAN_RELATIONSHIP_LABELS, GuardianRelationship, StudentGuardian} from '../../data-access/student-guardian.models';
import {StudentGuardianService} from '../../data-access/student-guardian.service';

@Component({
  selector: 'app-student-detail',
  imports: [DatePipe, RouterLink],
  templateUrl: './student-detail.html',
  styleUrl: './student-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentDetail implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly linkService = inject(StudentGuardianService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly student = signal<Student | null>(null);
  readonly guardians = signal<StudentGuardian[]>([]);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly guardiansErrorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap(params => {
          const studentId = params.get('studentId');

          this.student.set(null);
          this.guardians.set([]);
          this.errorMessage.set(null);
          this.guardiansErrorMessage.set(null);

          if (!studentId) {
            this.errorMessage.set('Estudante não informado.');
            return of(null);
          }

          this.loading.set(true);

          return forkJoin({
            student: this.studentService.findById(studentId),
            guardians: this.linkService.findByStudent(studentId).pipe(
              catchError(() => {
                this.guardiansErrorMessage.set('Não foi possível carregar os responsáveis vinculados.');

                return of(null);
              })
            )
          }).pipe(
            catchError((error: HttpErrorResponse) => {
              this.errorMessage.set(this.resolveErrorMessage(error));
              return of(null);
            }),
            finalize(() => this.loading.set(false))
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(result => {
        if (!result) {
          return;
        }

        this.student.set(result.student);
        this.guardians.set(result.guardians ?? []);
      });
  }

  relationshipLabel(relationship: GuardianRelationship): string {
    return GUARDIAN_RELATIONSHIP_LABELS[relationship] ?? relationship;
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 404) {
      return 'Estudante não encontrado.';
    }

    if (error.status === 403) {
      return 'Seu perfil não pode acessar este cadastro';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor';
    }

    return 'Não foi possível carregar o estudante';
  }
}
