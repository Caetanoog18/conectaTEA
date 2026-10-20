import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  Input,
  OnChanges,
  signal
} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, Subscription} from 'rxjs';
import {CareTeamService} from '../../data-access/care-team.service';
import {PROFESSIONAL_ROLE_LABELS, ProfessionalLink} from '../../data-access/care-team.models';

@Component({
  selector: 'app-student-care-team',
  imports: [DatePipe],
  templateUrl: './student-care-team.html',
  styleUrl: './student-care-team.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentCareTeam implements OnChanges {
  @Input({required: true}) studentId = '';

  private readonly careTeamService = inject(CareTeamService);
  private readonly destroyRef = inject(DestroyRef);

  private request?: Subscription;

  readonly links = signal<ProfessionalLink[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnChanges(): void {
    this.load();
  }

  load(): void {
    this.request?.unsubscribe();

    this.links.set([]);
    this.errorMessage.set(null);

    if (!this.studentId) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);

    this.request = this.careTeamService
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
}
