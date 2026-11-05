import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal
} from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, forkJoin} from 'rxjs';
import {Student} from '../../../students/data-access/student.models';
import {StudentService} from '../../../students/data-access/student.service';
import {CareTeamService} from '../../data-access/care-team.service';
import {PROFESSIONAL_ROLE_LABELS, ProfessionalOption} from '../../data-access/care-team.models';

const validStartDate: ValidatorFn = control => {
  const value: unknown = control.value;

  if (!value) {
    return null;
  }

  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return {invalidDate: true};
  }

  const parsed = new Date(`${value}T00:00:00Z`);

  if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== value) {
    return {invalidDate: true};
  }

  return value > new Date().toISOString().slice(0, 10)
    ? {futureDate: true}
    : null;
};

@Component({
  selector: 'app-professional-link-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './professional-link-form.html',
  styleUrl: './professional-link-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfessionalLinkForm implements OnInit {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly studentService = inject(StudentService);
  private readonly careTeamService = inject(CareTeamService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly studentId = this.route.snapshot.paramMap.get('studentId');
  readonly student = signal<Student | null>(null);
  readonly loading = signal(true);
  readonly loadFailed = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly professionals = signal<ProfessionalOption[]>([]);
  readonly professionalsLoading = signal(false);
  readonly professionalsError = signal<string | null>(null);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);

  readonly selectedProfessional = signal<ProfessionalOption | null>(null);

  private activeProfessionalIds = new Set<string>();

  readonly form = this.formBuilder.group({
    startedOn: [
      new Date().toISOString().slice(0, 10),
      [Validators.required, validStartDate]
    ]
  });

  get todayUtc(): string {
    return new Date().toISOString().slice(0, 10);
  }

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
      links: this.careTeamService.findByStudent(studentId)
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({student, links}) => {
          this.student.set(student);

          this.activeProfessionalIds = new Set(
            links
              .filter(link => link.active)
              .map(link => link.professionalId)
          );

          if (student.active) {
            this.loadProfessionals(0);
          }
        },
        error: (error: HttpErrorResponse) => {
          this.loadFailed.set(true);
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  loadProfessionals(page: number): void {
    if (page < 0 || this.professionalsLoading() || this.saving()) {
      return;
    }

    this.currentPage.set(page);
    this.professionalsLoading.set(true);
    this.professionalsError.set(null);
    this.professionals.set([]);

    this.careTeamService
      .findProfessionals(page, 20)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.professionalsLoading.set(false))
      )
      .subscribe({
        next: response => {
          this.professionals.set(response.content);
          this.currentPage.set(response.page);
          this.totalPages.set(response.totalPages);
        },
        error: (error: HttpErrorResponse) => {
          this.professionalsError.set(this.resolveErrorMessage(error));
        }
      });
  }

  isAlreadyLinked(professionalId: string): boolean {
    return this.activeProfessionalIds.has(professionalId);
  }

  selectProfessional(professional: ProfessionalOption): void {
    if (this.saving() || this.isAlreadyLinked(professional.id)) {
      return;
    }

    this.selectedProfessional.set(professional);
    this.errorMessage.set(null);
  }

  roleLabel(role: string): string {
    return PROFESSIONAL_ROLE_LABELS[role] ?? role;
  }

  save(): void {
    const studentId = this.studentId;
    const professional = this.selectedProfessional();

    if (
      !studentId ||
      this.loading() ||
      this.loadFailed() ||
      this.saving()
    ) {
      return;
    }

    if (!this.student()?.active) {
      this.errorMessage.set('O estudante precisa estar ativo.');
      return;
    }

    this.form.controls.startedOn.updateValueAndValidity();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!professional) {
      this.errorMessage.set('Selecione um profissional.');
      return;
    }

    if (this.isAlreadyLinked(professional.id)) {
      this.errorMessage.set('Este profissional já possui vínculo ativo.');
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);

    this.careTeamService
      .create(studentId, {
        professionalId: professional.id,
        startedOn: this.form.getRawValue().startedOn
      })
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
      return 'Seu perfil não pode gerenciar a equipe deste estudante.';
    }

    if (error.status === 404) {
      return 'Estudante ou profissional não encontrado.';
    }

    if (error.status === 409) {
      return 'Já existe um vínculo ativo ou ocorreu um conflito de dados.';
    }

    if (error.status === 422) {
      return 'Verifique os cadastros ativos, o perfil profissional e a data de início.';
    }

    return 'Não foi possível concluir a operação.';
  }
}
