import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {
  catchError,
  combineLatest,
  finalize,
  forkJoin,
  of,
  startWith,
  Subject,
  switchMap
} from 'rxjs';

import {FollowUpService} from '../../data-access/follow-up.service';
import type {LegalGuardianContact} from '../../data-access/follow-up.models';
import type {Student} from '../../../students/data-access/student.models';
import {GUARDIAN_RELATIONSHIP_LABELS} from '../../../students/data-access/student-guardian.models';
import type {
  GuardianRelationship
} from '../../../students/data-access/student-guardian.models';

@Component({
  selector: 'app-follow-up-detail',
  imports: [DatePipe, RouterLink],
  templateUrl: './follow-up-detail.html',
  styleUrls: [
    '../../../../shared/styles/management.scss',
    './follow-up-detail.scss'
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FollowUpDetail implements OnInit {
  private readonly service = inject(FollowUpService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();

  readonly student = signal<Student | null>(null);
  readonly guardians = signal<LegalGuardianContact[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    combineLatest([
      this.route.paramMap,
      this.reload.pipe(startWith(undefined))
    ])
      .pipe(
        switchMap(([params]) => {
          this.student.set(null);
          this.guardians.set([]);
          this.errorMessage.set(null);

          const studentId = params.get('studentId');

          if (!studentId) {
            this.loading.set(false);
            this.errorMessage.set('Estudante não informado.');
            return of(null);
          }

          this.loading.set(true);

          return forkJoin({
            student: this.service.findById(studentId),
            guardians: this.service.findLegalGuardians(studentId)
          }).pipe(
            switchMap(result => {
              if (result.student.id !== studentId) {
                this.errorMessage.set(
                  'Os dados recebidos não correspondem ao estudante solicitado.'
                );
                return of(null);
              }

              return of(result);
            }),
            catchError((error: HttpErrorResponse) => {
              this.errorMessage.set(this.resolveError(error));
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
        this.guardians.set(result.guardians);
      });
  }

  refresh(): void {
    if (!this.loading()) {
      this.reload.next();
    }
  }

  relationshipLabel(relationship: GuardianRelationship): string {
    return GUARDIAN_RELATIONSHIP_LABELS[relationship] ?? relationship;
  }

  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return 'Sua sessão expirou. Entre novamente.';
    }

    if (error.status === 403 || error.status === 404) {
      return 'O acompanhamento deste estudante não está disponível para sua conta. Verifique com a coordenação seu vínculo e os consentimentos.';
    }

    if (error.status === 400) {
      return 'O endereço do estudante é inválido.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível carregar o acompanhamento do estudante.';
  }
}
