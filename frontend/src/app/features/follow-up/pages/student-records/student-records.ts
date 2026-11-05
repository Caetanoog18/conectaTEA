import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, forkJoin, Subscription} from 'rxjs';

import {FollowUpService} from '../../data-access/follow-up.service';
import {StudentRecordsService} from '../../data-access/student-records.service';
import {ReportDownloadService} from '../../data-access/report-download.service';
import {
  parseLocalDateTime,
  pastDateTimeValidator,
  reportPeriodValidator
} from '../../data-access/record-date.validators';
import type {Student} from '../../../students/data-access/student.models';
import type {
  GenerateReportRequest,
  Observation,
  ObservationPurpose,
  StudentReport
} from '../../data-access/student-records.models';

@Component({
  selector: 'app-student-records',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './student-records.html',
  styleUrls: [
    '../../../../shared/styles/management.scss',
    './student-records.scss'
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentRecords implements OnInit {
  private readonly service = inject(StudentRecordsService);
  private readonly followUpService = inject(FollowUpService);
  private readonly downloadService = inject(ReportDownloadService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  private requests = new Subscription();

  readonly studentId = signal<string | null>(null);
  readonly student = signal<Student | null>(null);
  readonly observations = signal<Observation[]>([]);
  readonly report = signal<StudentReport | null>(null);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly generating = signal(false);

  readonly pageError = signal<string | null>(null);
  readonly createError = signal<string | null>(null);
  readonly reportError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;

  readonly busy = computed(() =>
    this.loading() || this.saving() || this.generating()
  );

  readonly observationForm = this.formBuilder.group({
    title: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(120)
      ]
    ],
    content: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(5000)
      ]
    ],
    occurredAt: [
      '',
      [
        Validators.required,
        pastDateTimeValidator
      ]
    ]
  });

  readonly reportForm = this.formBuilder.group({
    from: ['', Validators.required],
    to: ['', Validators.required]
  }, {
    validators: reportPeriodValidator
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.requests.unsubscribe();
    });
  }

  ngOnInit(): void {
    this.reportForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.report.set(null);
      });

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.requests.unsubscribe();
        this.requests = new Subscription();

        this.clearData();
        this.pageError.set(null);
        this.createError.set(null);
        this.reportError.set(null);
        this.successMessage.set(null);

        this.observationForm.reset();
        this.reportForm.reset();

        const id = params.get('studentId');

        this.studentId.set(id);

        if (!id) {
          this.pageError.set('Estudante não informado.');
          return;
        }

        this.fetchPage(0);
      });
  }

  loadPage(page: number): void {
    if (
      this.busy() ||
      page < 0 ||
      (this.totalPages() > 0 && page >= this.totalPages())
    ) {
      return;
    }

    this.fetchPage(page);
  }

  createObservation(): void {
    const id = this.studentId();

    if (!id || !this.student() || this.busy()) {
      return;
    }

    this.observationForm.controls.occurredAt
      .updateValueAndValidity();

    if (this.observationForm.invalid) {
      this.observationForm.markAllAsTouched();
      return;
    }

    const values = this.observationForm.getRawValue();
    const occurredAt = parseLocalDateTime(values.occurredAt);

    if (!occurredAt) {
      return;
    }

    this.saving.set(true);
    this.createError.set(null);
    this.successMessage.set(null);

    this.requests.add(
      this.service.create(id, {
        title: values.title.trim(),
        content: values.content.trim(),
        occurredAt: occurredAt.toISOString()
      })
        .pipe(finalize(() => this.saving.set(false)))
        .subscribe({
          next: () => {
            this.observationForm.reset();
            this.successMessage.set(
              'Observação registrada com sucesso.'
            );

            this.fetchPage(0);
          },
          error: (error: HttpErrorResponse) => {
            if (error.status === 401 || error.status === 403) {
              this.clearData();
              this.pageError.set(this.resolveError(error));
              return;
            }

            this.createError.set(this.resolveError(error));
          }
        })
    );
  }

  generateReport(pdf = false): void {
    const id = this.studentId();

    if (!id || !this.student() || this.busy()) {
      return;
    }

    this.reportForm.updateValueAndValidity();

    if (this.reportForm.invalid) {
      this.reportForm.markAllAsTouched();
      return;
    }

    const values = this.reportForm.getRawValue();
    const from = parseLocalDateTime(values.from);
    const to = parseLocalDateTime(values.to);

    if (!from || !to) {
      return;
    }

    const request: GenerateReportRequest = {
      from: from.toISOString(),
      to: to.toISOString()
    };

    this.generating.set(true);
    this.report.set(null);
    this.reportError.set(null);

    if (pdf) {
      this.requests.add(
        this.service.generatePdf(id, request)
          .pipe(finalize(() => this.generating.set(false)))
          .subscribe({
            next: blob => {
              if (
                blob.size === 0 ||
                blob.type.split(';')[0].toLowerCase() !==
                'application/pdf'
              ) {
                this.reportError.set(
                  'O servidor não retornou um arquivo PDF válido.'
                );
                return;
              }

              this.downloadService.save(blob);
            },
            error: (error: HttpErrorResponse) => {
              this.handleReportError(error);
            }
          })
      );

      return;
    }

    this.requests.add(
      this.service.generate(id, request)
        .pipe(finalize(() => this.generating.set(false)))
        .subscribe({
          next: report => {
            if (
              report.student.id !== id ||
              report.observations.some(item => item.studentId !== id)
            ) {
              this.reportError.set(
                'O relatório recebido não corresponde ao estudante solicitado.'
              );
              return;
            }

            this.report.set(report);
          },
          error: (error: HttpErrorResponse) => {
            this.handleReportError(error);
          }
        })
    );
  }

  purposeLabel(purpose: ObservationPurpose): string {
    return purpose === 'EDUCATIONAL_SUPPORT'
      ? 'Acompanhamento educacional'
      : 'Acompanhamento multiprofissional';
  }

  private fetchPage(page: number, preserveReportError = false): void {
    const id = this.studentId();

    if (!id) {
      return;
    }

    this.loading.set(true);
    this.pageError.set(null);
    this.createError.set(null);
    this.clearData();

    if (!preserveReportError) {
      this.reportError.set(null);
    }

    this.requests.add(
      forkJoin({
        student: this.followUpService.findById(id),
        observations: this.service.findAll(id, page, this.pageSize)
      })
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: result => {
            if (
              result.student.id !== id ||
              result.observations.content.some(item =>
                item.studentId !== id
              )
            ) {
              this.pageError.set(
                'Os registros recebidos não correspondem ao estudante solicitado.'
              );
              return;
            }

            this.student.set(result.student);
            this.observations.set(result.observations.content);
            this.currentPage.set(result.observations.page);
            this.totalPages.set(result.observations.totalPages);
            this.totalElements.set(result.observations.totalElements);
          },
          error: (error: HttpErrorResponse) => {
            this.pageError.set(this.resolveError(error));
          }
        })
    );
  }

  private handleReportError(error: HttpErrorResponse): void {
    this.report.set(null);

    if (error.status === 403) {
      this.reportError.set(
        'A geração exige vínculo vigente e consentimento válido que inclua também a geração de relatórios.'
      );

      this.fetchPage(this.currentPage(), true);
      return;
    }

    if (error.status === 401) {
      this.clearData();
      this.pageError.set(this.resolveError(error));
      return;
    }

    if (error.status === 422) {
      this.reportError.set(
        'Reduza o período: o limite é de 500 observações. Na exportação em PDF, o erro também pode indicar caracteres não suportados.'
      );
      return;
    }

    this.reportError.set(this.resolveError(error));
  }

  private clearData(): void {
    this.student.set(null);
    this.observations.set([]);
    this.report.set(null);
    this.currentPage.set(0);
    this.totalPages.set(0);
    this.totalElements.set(0);
  }

  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return 'Sua sessão expirou. Entre novamente.';
    }

    if (error.status === 403 || error.status === 404) {
      return 'O acompanhamento não está disponível para sua conta. Verifique o vínculo e os consentimentos com a coordenação.';
    }

    if (error.status === 400) {
      return 'Confira os campos e as datas. O relatório aceita um período de até 366 dias.';
    }

    if (error.status === 503) {
      return 'Não foi possível concluir a operação. Atualize os registros antes de tentar novamente.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor. Atualize os registros antes de reenviar.';
    }

    return 'Não foi possível concluir a operação. Atualize os registros antes de tentar novamente.';
  }
}
