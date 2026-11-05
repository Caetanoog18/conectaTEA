import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, forkJoin} from 'rxjs';
import {ConsentService} from '../../data-access/consent.service';
import {ConsentPurpose, CONSENT_PURPOSE_LABELS, CreateConsentRequest} from '../../data-access/consent.models';
import {StudentGuardianService} from '../../../students/data-access/student-guardian.service';
import {StudentGuardian} from '../../../students/data-access/student-guardian.models';

function parseGrantedAt(value: string): Date | null {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
    return null;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return null;
  }

  const pad = (part: number): string =>
    String(part).padStart(2, '0');

  const normalized =
    `${date.getFullYear()}-` +
    `${pad(date.getMonth() + 1)}-` +
    `${pad(date.getDate())}T` +
    `${pad(date.getHours())}:` +
    `${pad(date.getMinutes())}`;

  return normalized === value ? date : null;
}

function grantedAtValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string;

  if (!value) {
    return null;
  }

  const date = parseGrantedAt(value);

  if (!date) {
    return {invalidDate: true};
  }

  return date.getTime() > Date.now() ? {futureDate: true} : null;
}

function validityValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string;

  if (!value) {
    return null;
  }

  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return {invalidDate: true};
  }

  const date = new Date(`${value}T00:00:00Z`);

  if (Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== value) {
    return {invalidDate: true};
  }

  const today = new Date().toISOString().slice(0, 10);

  return value < today ? {pastDate: true} : null;
}

@Component({
  selector: 'app-consent-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './consent-form.html',
  styleUrl: './consent-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConsentForm implements OnInit {
  private readonly consentService = inject(ConsentService);
  private readonly linkService = inject(StudentGuardianService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly studentId = this.route.snapshot.paramMap.get('studentId');
  readonly linkId = this.route.snapshot.paramMap.get('linkId');
  readonly link = signal<StudentGuardian | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly loadFailed = signal(false);
  readonly hasActiveConsent = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly purposeOptions: {
    value: ConsentPurpose;
    label: string;
  }[] = [
    {
      value: 'EDUCATIONAL_SUPPORT',
      label: CONSENT_PURPOSE_LABELS.EDUCATIONAL_SUPPORT
    },
    {
      value: 'MULTIPROFESSIONAL_MONITORING',
      label: CONSENT_PURPOSE_LABELS.MULTIPROFESSIONAL_MONITORING
    },
    {
      value: 'INFORMATION_SHARING_WITH_CARE_TEAM',
      label: CONSENT_PURPOSE_LABELS.INFORMATION_SHARING_WITH_CARE_TEAM
    },
    {
      value: 'REPORT_GENERATION',
      label: CONSENT_PURPOSE_LABELS.REPORT_GENERATION
    },
    {
      value: 'IMAGE_AND_MEDIA_USE',
      label: CONSENT_PURPOSE_LABELS.IMAGE_AND_MEDIA_USE
    }
  ];

  readonly form = this.formBuilder.group({
    purposes: this.formBuilder.control<ConsentPurpose[]>(
      [],
      Validators.required
    ),
    termsVersion: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(20)
      ]
    ],
    grantedAt: [
      '',
      [
        Validators.required,
        grantedAtValidator
      ]
    ],
    validUntil: [
      '',
      validityValidator
    ],
    confirmed: [
      false,
      Validators.requiredTrue
    ]
  });

  get historyLink(): string[] {
    if (!this.studentId || !this.linkId) {
      return ['/students'];
    }

    return [
      '/students',
      this.studentId,
      'guardian-links',
      this.linkId,
      'consents'
    ];
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    if (this.loading() || this.saving()) {
      return;
    }

    this.errorMessage.set(null);
    this.loadFailed.set(false);
    this.hasActiveConsent.set(false);
    this.link.set(null);

    if (!this.studentId || !this.linkId) {
      this.loadFailed.set(true);
      this.errorMessage.set('Estudante ou vínculo não informado.');
      return;
    }

    this.loading.set(true);

    forkJoin({
      links: this.linkService.findByStudent(this.studentId),
      history: this.consentService.findHistory(this.linkId)
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: result => {
          const link = result.links.find(item =>
            item.id === this.linkId &&
            item.studentId === this.studentId
          );

          if (!link) {
            this.loadFailed.set(true);
            this.errorMessage.set(
              'Vínculo com responsável não encontrado neste estudante.'
            );
            return;
          }

          const belongsToCurrentLink = result.history.every(consent =>
            consent.studentId === this.studentId &&
            consent.studentGuardianId === this.linkId
          );

          if (!belongsToCurrentLink) {
            this.loadFailed.set(true);
            this.errorMessage.set(
              'O histórico recebido não corresponde ao vínculo informado.'
            );
            return;
          }

          this.link.set(link);
          this.hasActiveConsent.set(
            result.history.some(consent => consent.status === 'ACTIVE')
          );
        },
        error: (error: HttpErrorResponse) => {
          this.loadFailed.set(true);
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  togglePurpose(purpose: ConsentPurpose, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const control = this.form.controls.purposes;
    const selected = control.value;

    control.setValue(
      checked
        ? [...new Set([...selected, purpose])]
        : selected.filter(item => item !== purpose)
    );

    control.markAsTouched();
    control.markAsDirty();
  }

  save(): void {
    if (
      this.loading() ||
      this.saving() ||
      this.loadFailed() ||
      this.hasActiveConsent() ||
      !this.link()?.legalGuardian ||
      !this.linkId
    ) {
      return;
    }

    this.form.controls.grantedAt.updateValueAndValidity();
    this.form.controls.validUntil.updateValueAndValidity();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue();
    const grantedAt = parseGrantedAt(values.grantedAt);

    if (!grantedAt) {
      return;
    }

    const grantedDate = grantedAt.toISOString().slice(0, 10);

    if (values.validUntil && values.validUntil < grantedDate) {
      this.form.controls.validUntil.setErrors({
        beforeGrant: true
      });
      return;
    }

    const request: CreateConsentRequest = {
      purposes: values.purposes,
      termsVersion: values.termsVersion.trim(),
      grantedAt: grantedAt.toISOString(),
      validUntil: values.validUntil || null
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    this.consentService
      .create(this.linkId, request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false))
      )
      .subscribe({
        next: () => {
          void this.router.navigate(this.historyLink);
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 409) {
            this.hasActiveConsent.set(true);
          }

          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Seu perfil não pode registrar consentimentos.';
    }

    if (error.status === 404) {
      return 'Vínculo com responsável não encontrado.';
    }

    if (error.status === 409) {
      return 'O vínculo já possui consentimento ativo ou ocorreu um conflito. Consulte o histórico antes de continuar.';
    }

    if (error.status === 422) {
      return 'Confira se estudante e responsável estão ativos, se o vínculo é de responsável legal e se as datas são válidas.';
    }

    if (error.status === 400) {
      return 'Confira as finalidades, a versão do termo e as datas informadas.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível concluir a operação de consentimento.';
  }
}
