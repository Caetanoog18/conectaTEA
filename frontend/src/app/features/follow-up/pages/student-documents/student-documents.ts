import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal
} from '@angular/core';
import {DatePipe, DecimalPipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, forkJoin, Subscription} from 'rxjs';

import {AccessService} from '../../../../core/auth/access.service';
import {FollowUpService} from '../../data-access/follow-up.service';
import {MedicalDocumentService} from '../../data-access/medical-document.service';
import {MedicalDocumentDownloadService} from '../../data-access/medical-document-download.service';
import {
  brazilianDateToIso,
  documentDateValidator
} from '../../data-access/document-date.validator';
import type {Student} from '../../../students/data-access/student.models';
import type {MedicalDocument} from '../../data-access/medical-document.models';

@Component({
  selector: 'app-student-documents',
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './student-documents.html',
  styleUrl: './student-documents.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentDocuments implements OnInit {
  private readonly service = inject(MedicalDocumentService);
  private readonly followUp = inject(FollowUpService);
  private readonly download = inject(MedicalDocumentDownloadService);
  private readonly access = inject(AccessService);
  private readonly builder = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  private requests = new Subscription();

  @ViewChild('fileInput')
  private fileInput?: ElementRef<HTMLInputElement>;

  readonly studentId = signal<string | null>(null);
  readonly student = signal<Student | null>(null);
  readonly documents = signal<MedicalDocument[]>([]);
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly downloadingId = signal<string | null>(null);
  readonly pageError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly file = signal<File | null>(null);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;

  readonly busy = computed(() =>
    this.loading() ||
    this.uploading() ||
    this.downloadingId() !== null
  );

  readonly form = this.builder.group({
    title: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(120)
      ]
    ],
    issuedOn: [
      '',
      [
        Validators.required,
        documentDateValidator
      ]
    ]
  });

  get canUpload(): boolean {
    return this.access.hasAnyRole(['PHYSICIAN']);
  }

  constructor() {
    this.destroyRef.onDestroy(() => this.requests.unsubscribe());
  }

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.requests.unsubscribe();
        this.requests = new Subscription();

        this.clearData();
        this.form.reset();
        this.resetFile();

        this.actionError.set(null);
        this.successMessage.set(null);
        this.studentId.set(params.get('studentId'));

        if (!this.studentId()) {
          this.pageError.set('Estudante não informado.');
          return;
        }

        this.loadPage(0);
      });
  }

  selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const selected = input.files?.item(0) ?? null;

    this.file.set(null);
    this.actionError.set(null);

    if (!selected) {
      return;
    }

    if (!/\.pdf$/i.test(selected.name) || (selected.type && selected.type !== 'application/pdf')) {
      this.actionError.set('Selecione um arquivo PDF.');
      input.value = '';
      return;
    }

    if (selected.size === 0 || selected.size > 10 * 1024 * 1024) {
      this.actionError.set('Envie um PDF não vazio de até 10 MiB.');
      input.value = '';
      return;
    }

    this.file.set(selected);
  }

  loadPage(page: number): void {
    const id = this.studentId();

    if (
      !id ||
      this.busy() ||
      page < 0 ||
      (this.totalPages() > 0 && page >= this.totalPages())
    ) {
      return;
    }

    this.loading.set(true);
    this.pageError.set(null);
    this.clearData();

    this.requests.add(
      forkJoin({
        student: this.followUp.findById(id),
        page: this.service.findAll(id, page, this.pageSize)
      })
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: result => {
            if (
              result.student.id !== id ||
              result.page.content.some(item => item.studentId !== id)
            ) {
              this.pageError.set(
                'Não foi possível validar os laudos recebidos.'
              );
              return;
            }

            this.student.set(result.student);
            this.documents.set(result.page.content);
            this.currentPage.set(result.page.page);
            this.totalPages.set(result.page.totalPages);
            this.totalElements.set(result.page.totalElements);
          },
          error: (error: HttpErrorResponse) => {
            if (error.status === 401 || error.status === 403) {
              this.handleActionError(error);
            } else {
              this.pageError.set(this.errorText(error));
            }
          }
        })
    );
  }

  upload(): void {
    const id = this.studentId();
    const file = this.file();

    if (!id || !this.student() || this.busy() || !this.canUpload) {
      return;
    }

    this.form.controls.issuedOn.updateValueAndValidity();
    this.actionError.set(null);
    this.successMessage.set(null);

    if (this.form.invalid || !file) {
      this.form.markAllAsTouched();
      this.actionError.set('Preencha o título, a data de emissão e selecione um PDF.');
      return;
    }

    const issuedOn = brazilianDateToIso(this.form.controls.issuedOn.value);

    if (!issuedOn) {
      return;
    }

    this.uploading.set(true);

    this.requests.add(
      this.service.upload(id, {
        title: this.form.controls.title.value.trim(),
        issuedOn,
        file
      })
        .pipe(finalize(() => this.uploading.set(false)))
        .subscribe({
          next: () => {
            this.form.reset();
            this.resetFile();
            this.successMessage.set('Laudo enviado com sucesso.');
          },
          error: (error: HttpErrorResponse) =>
            this.handleActionError(error),
          complete: () => {
            this.uploading.set(false);
            this.loadPage(0);
          }
        })
    );
  }

  downloadDocument(document: MedicalDocument): void {
    const id = this.studentId();

    if (!id || !this.student() || this.busy() || document.studentId !== id) {
      return;
    }

    this.downloadingId.set(document.id);
    this.actionError.set(null);

    this.requests.add(
      this.service.download(id, document.id)
        .pipe(finalize(() => this.downloadingId.set(null)))
        .subscribe({
          next: blob => {
            if (
              !blob.size ||
              blob.type.split(';')[0].toLowerCase() !== 'application/pdf'
            ) {
              this.actionError.set('O servidor não retornou um arquivo PDF válido.');
              return;
            }

            this.download.save(blob, document.id);
          },
          error: (error: HttpErrorResponse) =>
            this.handleActionError(error)
        })
    );
  }

  private handleActionError(error: HttpErrorResponse): void {
    if (error.status === 401 || error.status === 403) {
      this.clearData();
      this.form.reset();
      this.resetFile();
      this.successMessage.set(null);
      this.pageError.set(this.errorText(error));
      return;
    }

    this.actionError.set(this.errorText(error));
  }

  private errorText(error: HttpErrorResponse): string {
    if (error.status === 401 || error.status === 403) {
      return 'Acesso aos laudos não autorizado. Verifique sua sessão, o vínculo e os consentimentos.';
    }

    if (error.status === 413) {
      return 'O arquivo excede o limite permitido de 10 MiB.';
    }

    if (error.status === 404) {
      return 'Laudo não encontrado.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    if (error.status === 400) {
      return typeof error.error?.detail === 'string'
        ? error.error.detail
        : 'Confira o título, a data de emissão e se o PDF é válido e não possui senha.';
    }

    return 'Não foi possível processar os laudos. Tente novamente.';
  }

  private resetFile(): void {
    this.file.set(null);

    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  private clearData(): void {
    this.student.set(null);
    this.documents.set([]);
    this.currentPage.set(0);
    this.totalPages.set(0);
    this.totalElements.set(0);
  }
}
