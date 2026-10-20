import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal
} from '@angular/core';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {GuardianService} from '../../data-access/guardian.service';
import {GuardianRequest} from '../../data-access/guardian.models';

@Component({
  selector: 'app-guardian-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './guardian-form.html',
  styleUrl: './guardian-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GuardianForm {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly guardianService = inject(GuardianService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly guardianId = this.route.snapshot.paramMap.get('guardianId');
  readonly editMode = this.guardianId !== null;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly loadFailed = signal(false);
  readonly errorMessage = signal<string | null>(null);

  private associatedUserId: string | null = null;

  readonly form = this.formBuilder.group({
    fullName: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(120)
      ]
    ],
    cpf: ['', Validators.pattern(/^(?:\d{11}|\d{3}\.\d{3}\.\d{3}-\d{2})$/)],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    phone: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(20)
      ]
    ]
  });

  constructor() {
    if (this.guardianId) {
      this.loadGuardian(this.guardianId);
    }
  }

  save(): void {
    if (this.loading() || this.saving() || this.loadFailed()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    const request: GuardianRequest = {
      fullName: value.fullName.trim(),
      cpf: value.cpf.trim() || null,
      email: value.email.trim() || null,
      phone: value.phone.trim(),
      userId: this.associatedUserId
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    const operation = this.guardianId
      ? this.guardianService.update(this.guardianId, request)
      : this.guardianService.create(request);

    operation
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false))
      )
      .subscribe({
        next: () => {
          void this.router.navigate(['/guardians']);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  hasError(field: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[field];
    return control.touched && control.invalid;
  }

  private loadGuardian(guardianId: string): void {
    this.loading.set(true);

    this.guardianService
      .findById(guardianId)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false)))
      .subscribe({
        next: guardian => {
          this.associatedUserId = guardian.userId;

          this.form.patchValue({
            fullName: guardian.fullName,
            cpf: guardian.cpf ?? '',
            email: guardian.email ?? '',
            phone: guardian.phone
          });
        },
        error: (error: HttpErrorResponse) => {
          this.loadFailed.set(true);
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor';
    }

    if (error.status === 403) {
      return 'Seu perfil não pode gerenciar responsáveis';
    }

    if (error.status === 404) {
      return 'Responsável não encontrado';
    }

    if (error.status === 409) {
      return 'Existe um conflito com outro cadastro. Verifique os dados';
    }

    if (error.status === 400) {
      return 'Verifique os dados informados, incluindo a validade do CPF';
    }

    return 'Não foi possível concluir a operação. Tente novamente';
  }
}
