import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {distinctUntilChanged, map, Subscription} from 'rxjs';

import {CONSENT_PURPOSE_LABELS} from '../../../consents/data-access/consent.models';
import {parseLocalDateTime} from '../../../follow-up/data-access/record-date.validators';
import {HistoryService} from '../../data-access/history.service';
import {AUDIT_ACTION_LABELS, AUDIT_OUTCOME_LABELS} from '../../data-access/history.models';
import type {
  AuditAction,
  AuditEvent,
  AuditFilters,
  AuditOutcome,
  TimelineEvent
} from '../../data-access/history.models';

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'app-history-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './history-page.html',
  styleUrl: './history-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HistoryPage {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(HistoryService);
  private readonly builder = inject(FormBuilder).nonNullable;
  private readonly destroyRef = inject(DestroyRef);

  private request?: Subscription;
  private applied: AuditFilters = {};

  readonly isAudit = this.route.snapshot.data['historyMode'] === 'audit';
  readonly studentId = signal<string | null>(null);
  readonly timeline = signal<TimelineEvent[]>([]);
  readonly audit = signal<AuditEvent[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly filterError = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly actions = Object.entries(AUDIT_ACTION_LABELS);
  readonly outcomes = Object.entries(AUDIT_OUTCOME_LABELS);
  readonly actionLabels = AUDIT_ACTION_LABELS;
  readonly outcomeLabels = AUDIT_OUTCOME_LABELS;
  readonly purposeLabels = CONSENT_PURPOSE_LABELS;

  readonly form = this.builder.group({
    from: '',
    to: '',
    studentId: '',
    actorUserId: '',
    requestId: '',
    action: this.builder.control<AuditAction | ''>(''),
    outcome: this.builder.control<AuditOutcome | ''>('')
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.request?.unsubscribe());

    if (this.isAudit) {
      this.load(0);
      return;
    }

    this.route.paramMap.pipe(
      map(params => params.get('studentId')),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(id => {
      this.studentId.set(id);
      this.resetFilters();
    });
  }

  applyFilters(): void {
    const values = this.form.getRawValue();
    const fromText = values.from.trim();
    const toText = values.to.trim();
    const from = parseLocalDateTime(fromText);
    const to = parseLocalDateTime(toText);

    if ((fromText && !from) || (toText && !to)) {
      this.filterError.set('Informe as datas no formato dd/mm/aaaa hh:mm.');
      return;
    }

    if (from && to && from >= to) {
      this.filterError.set('O início deve ser anterior ao fim do período.');
      return;
    }

    const filters: AuditFilters = {
      from: from?.toISOString(),
      to: to?.toISOString()
    };

    if (this.isAudit) {
      for (const key of ['studentId', 'actorUserId', 'requestId'] as const) {
        const value = values[key].trim();

        if (value && !UUID_PATTERN.test(value)) {
          this.filterError.set('Confira os identificadores: use o UUID completo.');
          return;
        }

        if (value) {
          filters[key] = value;
        }
      }

      filters.action = values.action || undefined;
      filters.outcome = values.outcome || undefined;
    }

    this.applied = filters;
    this.filterError.set(null);
    this.load(0);
  }

  resetFilters(): void {
    this.form.reset();
    this.applied = {};
    this.filterError.set(null);
    this.load(0);
  }

  changePage(page: number): void {
    if (!this.loading() && page >= 0 && page < this.totalPages()) {
      this.load(page);
    }
  }

  retry(): void {
    this.load(this.page());
  }

  private load(page: number): void {
    this.request?.unsubscribe();
    this.timeline.set([]);
    this.audit.set([]);
    this.error.set(null);
    this.totalElements.set(0);
    this.totalPages.set(0);
    this.page.set(page);
    this.loading.set(true);

    if (this.isAudit) {
      this.request = this.service.findAudit(this.applied, page).subscribe({
        next: result => {
          this.audit.set(result.content);
          this.finish(result.page, result.totalPages, result.totalElements);
        },
        error: error => this.fail(error)
      });
      return;
    }

    const id = this.studentId();

    if (!id) {
      this.loading.set(false);
      this.error.set('Estudante não identificado.');
      return;
    }

    this.request = this.service.findTimeline(id, this.applied, page).subscribe({
      next: result => {
        this.timeline.set(result.content);
        this.finish(result.page, result.totalPages, result.totalElements);
      },
      error: error => this.fail(error)
    });
  }

  private finish(page: number, totalPages: number, totalElements: number): void {
    this.page.set(page);
    this.totalPages.set(totalPages);
    this.totalElements.set(totalElements);
    this.loading.set(false);
  }

  private fail(error: unknown): void {
    this.loading.set(false);

    const status = error instanceof HttpErrorResponse ? error.status : 0;

    if (status === 401) {
      this.error.set('Sua sessão expirou. Entre novamente.');
    } else if (status === 403) {
      this.error.set(
        this.isAudit
          ? 'Sua conta não tem acesso à auditoria.'
          : 'Você não tem acesso à linha do tempo deste estudante.'
      );
    } else if (status === 400) {
      this.error.set('Os filtros foram recusados. Confira os valores informados.');
    } else {
      this.error.set('Não foi possível carregar os eventos. Tente novamente.');
    }
  }
}
