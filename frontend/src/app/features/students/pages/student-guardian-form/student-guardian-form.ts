import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, forkJoin} from 'rxjs';
import {Guardian} from '../../../guardians/data-access/guardian.models';
import {GuardianService} from '../../../guardians/data-access/guardian.service';
import {Student} from '../../data-access/student.models';
import {StudentService} from '../../data-access/student.service';
import {
  GUARDIAN_RELATIONSHIP_LABELS,
  GuardianRelationship,
  UpdateStudentGuardianRequest} from '../../data-access/student-guardian.models';
import {StudentGuardianService} from '../../data-access/student-guardian.service';

@Component({
  selector: 'app-student-guardian-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './student-guardian-form.html',
  styleUrl: './student-guardian-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentGuardianForm implements OnInit {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly studentService = inject(StudentService);
  private readonly guardianService = inject(GuardianService);
  private readonly linkService = inject(StudentGuardianService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly studentId = this.route.snapshot.paramMap.get('studentId');
  readonly guardianId = this.route.snapshot.paramMap.get('guardianId');
  readonly editMode = this.guardianId !== null;

  readonly student = signal<Student | null>(null);
  readonly guardianName = signal<string | null>(null);
  readonly selectedGuardian = signal<Guardian | null>(null);

  readonly loading = signal(true);
  readonly loadFailed = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly candidates = signal<Guardian[]>([]);
  readonly candidatesLoading = signal(false);
  readonly candidatesError = signal<string | null>(null);
  readonly candidatesPage = signal(0);
  readonly candidatesTotalPages = signal(0);

  readonly relationships = Object.entries(GUARDIAN_RELATIONSHIP_LABELS)
    .map(([value, label]) => ({value, label}));

  private linkedGuardianIds = new Set<string>();

  readonly form = this.formBuilder.group({
    relationship: [
      'OTHER' as GuardianRelationship,
      Validators.required
    ],
    legalGuardian: [false],
    primaryContact: [false]
  });

  ngOnInit(): void {
    const studentId = this.studentId;

    if (!studentId) {
      this.loading.set(false);
      this.loadFailed.set(true);
      this.errorMessage.set('Estudante não informado.');
      return;
    }

    forkJoin({
      student: this.studentService.findById(studentId),
      links: this.linkService.findByStudent(studentId)
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({student, links}) => {
          this.student.set(student);
          this.linkedGuardianIds = new Set(
            links.map(link => link.guardianId)
          );

          if (this.guardianId) {
            const link = links.find(
              current => current.guardianId === this.guardianId
            );

            if (!link) {
              this.loadFailed.set(true);
              this.errorMessage.set('Vínculo não encontrado.');
              return;
            }

            this.guardianName.set(link.guardianName);

            this.form.patchValue({
              relationship: link.relationship,
              legalGuardian: link.legalGuardian,
              primaryContact: link.primaryContact
            });
          } else if (student.active) {
            this.loadCandidates(0);
          }
        },
        error: (error: HttpErrorResponse) => {
          this.loadFailed.set(true);
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  loadCandidates(page: number): void {
    if (
      page < 0 ||
      this.editMode ||
      this.candidatesLoading() ||
      this.saving()
    ) {
      return;
    }

    this.candidatesLoading.set(true);
    this.candidatesError.set(null);

    this.guardianService
      .findAll(page, 20)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.candidatesLoading.set(false))
      )
      .subscribe({
        next: response => {
          this.candidates.set(response.content);
          this.candidatesPage.set(response.page);
          this.candidatesTotalPages.set(response.totalPages);
        },
        error: () => {
          this.candidatesError.set(
            'Não foi possível carregar os responsáveis.'
          );
        }
      });
  }

  canSelect(guardian: Guardian): boolean {
    return guardian.active &&
      !this.linkedGuardianIds.has(guardian.id);
  }

  isAlreadyLinked(guardian: Guardian): boolean {
    return this.linkedGuardianIds.has(guardian.id);
  }

  selectGuardian(guardian: Guardian): void {
    if (this.editMode || this.saving() || !this.canSelect(guardian)) {
      return;
    }

    this.selectedGuardian.set(guardian);
    this.errorMessage.set(null);
  }

  save(): void {
    const studentId = this.studentId;
    const student = this.student();

    if (
      !studentId ||
      !student ||
      this.loading() ||
      this.loadFailed() ||
      this.saving()
    ) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const selectedGuardian = this.selectedGuardian();

    if (!this.editMode && !student.active) {
      this.errorMessage.set(
        'Ative o estudante antes de criar um vínculo.'
      );
      return;
    }

    if (!this.editMode && !selectedGuardian) {
      this.errorMessage.set('Selecione um responsável.');
      return;
    }

    const request: UpdateStudentGuardianRequest =
      this.form.getRawValue();

    const operation = this.guardianId
      ? this.linkService.update(
        studentId,
        this.guardianId,
        request
      )
      : this.linkService.create(studentId, {
        ...request,
        guardianId: selectedGuardian!.id
      });

    this.saving.set(true);
    this.errorMessage.set(null);

    operation
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false))
      )
      .subscribe({
        next: () => {
          void this.router.navigate(['/students', studentId]);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    if (error.status === 403) {
      return 'Seu perfil não pode gerenciar estes vínculos.';
    }

    if (error.status === 404) {
      return 'Estudante, responsável ou vínculo não encontrado.';
    }

    if (error.status === 409) {
      return 'O vínculo já existe ou outro responsável já é o contato principal.';
    }

    if (error.status === 422) {
      return 'Verifique se o estudante e o responsável estão ativos.';
    }

    if (error.status === 400) {
      return 'Verifique os dados informados.';
    }

    return 'Não foi possível concluir a operação.';
  }
}
