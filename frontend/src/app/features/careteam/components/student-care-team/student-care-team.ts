import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  Input,
  OnChanges,
  signal,
  SimpleChanges
} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, Subscription} from 'rxjs';
import {CareTeamService} from '../../data-access/care-team.service';
import {PROFESSIONAL_ROLE_LABELS, ProfessionalLink} from '../../data-access/care-team.models';

@Component({
  selector: 'app-student-care-team',
  imports: [DatePipe, RouterLink, ReactiveFormsModule],
  templateUrl: './student-care-team.html',
  styleUrl: './student-care-team.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentCareTeam implements OnChanges {
  @Input({required: true}) studentId = '';
  @Input() studentActive = true;

  private readonly careTeamService = inject(CareTeamService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  private loadRequest?: Subscription;
  private endRequest?: Subscription;

  readonly links = signal<ProfessionalLink[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedLink = signal<ProfessionalLink | null>(null);
  readonly ending = signal(false);
  readonly actionErrorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly endForm = this.formBuilder.group({
    reason: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(500)
      ]
    ]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['studentId']) {
      this.endRequest?.unsubscribe();
      this.load();
    }
  }

  load(): void {
    if (this.ending()) {
      return;
    }

    this.loadRequest?.unsubscribe();

    this.links.set([]);
    this.selectedLink.set(null);
    this.errorMessage.set(null);
    this.actionErrorMessage.set(null);
    this.successMessage.set(null);
    this.endForm.reset();

    if (!this.studentId) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);

    this.loadRequest = this.careTeamService
      .findByStudent(this.studentId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: links => {
          this.links.set(links);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  requestEnd(link: ProfessionalLink): void {
    if (
      this.loading() ||
      this.ending() ||
      !link.active ||
      link.studentId !== this.studentId
    ) {
      return;
    }

    this.selectedLink.set(link);
    this.endForm.reset();
    this.actionErrorMessage.set(null);
    this.successMessage.set(null);
  }

  cancelEnd(): void {
    if (this.ending()) {
      return;
    }

    this.selectedLink.set(null);
    this.endForm.reset();
    this.actionErrorMessage.set(null);
  }

  confirmEnd(): void {
    const link = this.selectedLink();

    if (
      !link ||
      !link.active ||
      link.studentId !== this.studentId ||
      this.ending()
    ) {
      return;
    }

    if (this.endForm.invalid) {
      this.endForm.markAllAsTouched();
      return;
    }

    const reason = this.endForm.getRawValue().reason.trim();

    this.ending.set(true);
    this.actionErrorMessage.set(null);

    this.endRequest = this.careTeamService
      .end(link.id, {reason})
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.ending.set(false)))
      .subscribe({
        next: updatedLink => {
          this.links.update(links =>
            links.map(current =>
              current.id === updatedLink.id
                ? updatedLink
                : current
            )
          );

          this.selectedLink.set(null);
          this.endForm.reset();
          this.successMessage.set(
            'Vínculo encerrado com sucesso. O histórico foi preservado.'
          );
        },
        error: (error: HttpErrorResponse) => {
          this.actionErrorMessage.set(
            this.resolveEndErrorMessage(error)
          );
        }
      });
  }

  roleLabel(role: string): string {
    return PROFESSIONAL_ROLE_LABELS[role] ?? role;
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Seu perfil não pode consultar a equipe deste estudante.';
    }

    if (error.status === 404) {
      return 'Estudante não encontrado.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível carregar a equipe multiprofissional.';
  }

  private resolveEndErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Seu perfil não pode encerrar este vínculo.';
    }

    if (error.status === 404) {
      return 'Vínculo não encontrado. Atualize a equipe.';
    }

    if (error.status === 409) {
      return 'O vínculo já foi encerrado ou houve um conflito. Atualize a equipe.';
    }

    if (error.status === 400) {
      return 'Informe um motivo válido com até 500 caracteres.';
    }

    if (error.status === 422) {
      return 'Não foi possível encerrar o vínculo devido às datas do acompanhamento.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível encerrar o vínculo. Tente novamente.';
  }
}
