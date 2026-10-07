import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {finalize} from 'rxjs';
import {StudentService} from '../../data-access/student.service';
import {StudentRequest} from '../../data-access/student.models';

@Component({
  selector: 'app-student-form',
  imports: [
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './student-form.html',
  styleUrl: './student-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentForm {
  private readonly formBuilder =
    inject(NonNullableFormBuilder);

  private readonly studentService =
    inject(StudentService);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly studentId =
    this.route.snapshot.paramMap.get('studentId');

  readonly editMode = this.studentId !== null;

  readonly loading = signal(this.editMode);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    fullName: [
      '',
      [
        Validators.required,
        Validators.maxLength(150)
      ]
    ],
    preferredName: [
      '',
      Validators.maxLength(100)
    ],
    birthDate: [
      '',
      Validators.required
    ],
    enrollmentNumber: [
      '',
      [
        Validators.required,
        Validators.maxLength(50)
      ]
    ],
    schoolYear: [
      new Date().getFullYear(),
      [
        Validators.required,
        Validators.min(2000),
        Validators.max(2100)
      ]
    ],
    gradeLevel: [
      '',
      [
        Validators.required,
        Validators.maxLength(50)
      ]
    ],
    className: [
      '',
      [
        Validators.required,
        Validators.maxLength(50)
      ]
    ]
  });

  constructor() {
    if (this.studentId) {
      this.loadStudent(this.studentId);
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    const request: StudentRequest = {
      fullName: value.fullName.trim(),
      preferredName: value.preferredName.trim() || null,
      birthDate: value.birthDate,
      enrollmentNumber: value.enrollmentNumber.trim(),
      schoolYear: value.schoolYear,
      gradeLevel: value.gradeLevel.trim(),
      className: value.className.trim()
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    const operation = this.studentId
      ? this.studentService.update(this.studentId, request)
      : this.studentService.create(request);

    operation
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigate(['/students']);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(
            this.resolveErrorMessage(error)
          );
        }
      });
  }

  hasError(
    fieldName: keyof typeof this.form.controls,
    errorName: string
  ): boolean {
    const control = this.form.controls[fieldName];

    return control.touched && control.hasError(errorName);
  }

  private loadStudent(studentId: string): void {
    this.studentService
      .findById(studentId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: student => {
          this.form.patchValue({
            fullName: student.fullName,
            preferredName:
              student.preferredName ?? '',
            birthDate: student.birthDate,
            enrollmentNumber:
            student.enrollmentNumber,
            schoolYear: student.schoolYear,
            gradeLevel: student.gradeLevel,
            className: student.className
          });
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(
            this.resolveErrorMessage(error)
          );
        }
      });
  }

  private resolveErrorMessage(
    error: HttpErrorResponse
  ): string {
    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    if (error.status === 404) {
      return 'Estudante não encontrado.';
    }

    if (error.status === 409) {
      return 'Esta matrícula já está sendo utilizada.';
    }

    if (error.status === 400) {
      return error.error?.detail ??
        'Verifique os dados informados.';
    }

    return error.error?.detail ??
      'Não foi possível salvar o estudante.';
  }
}
