import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, Subscription} from 'rxjs';
import {ConsentService} from '../../data-access/consent.service';
import {Consent, ConsentPurpose, ConsentStatus, CONSENT_PURPOSE_LABELS, CONSENT_STATUS_LABELS} from '../../data-access/consent.models';

@Component({
  selector: 'app-consent-history',
  imports: [DatePipe, RouterLink, ReactiveFormsModule],
  templateUrl: './consent-history.html',
  styleUrl: './consent-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConsentHistory implements OnInit {
  private readonly consentService = inject(ConsentService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  private loadRequest?: Subscription;
  private revokeRequest?: Subscription;
  private linkId: string | null = null;

  readonly studentId = signal<string | null>(null);
  readonly consents = signal<Consent[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedConsent = signal<Consent | null>(null);
  readonly revoking = signal(false);
  readonly actionErrorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly revokeForm = this.formBuilder.group({
    reason: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(500)
      ]
    ]
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.revokeRequest?.unsubscribe();

        this.studentId.set(params.get('studentId'));
        this.linkId = params.get('linkId');

        this.load();
      });
  }

  load(): void {
    if (this.revoking()) {
      return;
    }

    this.loadRequest?.unsubscribe();

    this.consents.set([]);
    this.selectedConsent.set(null);
    this.errorMessage.set(null);
    this.actionErrorMessage.set(null);
    this.successMessage.set(null);
    this.revokeForm.reset();
    this.loading.set(false);

    const studentId = this.studentId();
    const linkId = this.linkId;

    if (!studentId || !linkId) {
      this.errorMessage.set('Estudante ou vínculo não informado.');
      return;
    }

    this.loading.set(true);

    this.loadRequest = this.consentService
      .findHistory(linkId)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false)))
      .subscribe({
        next: consents => {
          const belongsToCurrentLink = consents.every(consent =>
            consent.studentId === studentId &&
            consent.studentGuardianId === linkId
          );

          if (!belongsToCurrentLink) {
            this.errorMessage.set(
              'Os consentimentos recebidos não correspondem ao vínculo informado.'
            );
            return;
          }

          this.consents.set(consents);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveLoadError(error));
        }
      });
  }

  requestRevocation(consent: Consent): void {
    if (
      this.loading() ||
      this.revoking() ||
      consent.status !== 'ACTIVE' ||
      consent.studentId !== this.studentId() ||
      consent.studentGuardianId !== this.linkId
    ) {
      return;
    }

    this.selectedConsent.set(consent);
    this.revokeForm.reset();
    this.actionErrorMessage.set(null);
    this.successMessage.set(null);
  }

  cancelRevocation(): void {
    if (this.revoking()) {
      return;
    }

    this.selectedConsent.set(null);
    this.revokeForm.reset();
    this.actionErrorMessage.set(null);
  }

  confirmRevocation(): void {
    const consent = this.selectedConsent();

    if (
      !consent ||
      this.loading() ||
      this.revoking() ||
      consent.status !== 'ACTIVE' ||
      consent.studentId !== this.studentId() ||
      consent.studentGuardianId !== this.linkId
    ) {
      return;
    }

    if (this.revokeForm.invalid) {
      this.revokeForm.markAllAsTouched();
      return;
    }

    const reason = this.revokeForm.controls.reason.value.trim();

    this.revoking.set(true);
    this.actionErrorMessage.set(null);
    this.successMessage.set(null);

    this.revokeRequest = this.consentService
      .revoke(consent.id, {reason})
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.revoking.set(false)))
      .subscribe({
        next: updatedConsent => {
          this.consents.update(consents =>
            consents.map(item => item.id === updatedConsent.id ? updatedConsent : item));

          this.selectedConsent.set(null);
          this.revokeForm.reset();
          this.successMessage.set('Consentimento revogado com sucesso. O histórico foi preservado.');
        },
        error: (error: HttpErrorResponse) => {
          this.actionErrorMessage.set(this.resolveRevocationError(error));
        }
      });
  }

  statusLabel(status: ConsentStatus): string {
    return CONSENT_STATUS_LABELS[status] ?? status;
  }

  purposeLabel(purpose: ConsentPurpose): string {
    return CONSENT_PURPOSE_LABELS[purpose] ?? purpose;
  }

  private resolveLoadError(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Seu perfil não pode consultar consentimentos.';
    }

    if (error.status === 404 || error.status === 422) {
      return 'Vínculo com responsável não encontrado.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível carregar o histórico de consentimentos.';
  }

  private resolveRevocationError(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Seu perfil não pode revogar este consentimento.';
    }

    if (error.status === 404) {
      return 'Consentimento não encontrado. Atualize o histórico.';
    }

    if (error.status === 409) {
      return 'O consentimento pode estar expirado ou já revogado. Atualize o histórico.';
    }

    if (error.status === 400) {
      return 'Informe um motivo válido com até 500 caracteres.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor. Confira o histórico antes de tentar novamente.';
    }

    return 'Não foi possível revogar o consentimento. Confira o histórico antes de tentar novamente.';
  }
}
