import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';

import {AuthService} from '../../../../core/auth/auth';
import {InstitutionService} from '../../data-access/institution.service';
import {Institution, InstitutionRequest} from '../../data-access/institution.models';

interface InstitutionField {
  key: keyof InstitutionRequest;
  label: string;
  type: 'text' | 'email';
  required: boolean;
  maxLength: number;
}

@Component({
  selector: 'app-institution-page',
  imports: [ReactiveFormsModule],
  templateUrl: './institution-page.html',
  styleUrl: '../../../../shared/styles/management.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InstitutionPage implements OnInit {
  private readonly service = inject(InstitutionService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly isAdmin = this.authService.getCurrentUserRoles().includes('ADMINISTRATOR');
  readonly institution = signal<Institution | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly ready = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly fields: InstitutionField[] = [
    {
      key: 'name',
      label: 'Nome da instituição',
      type: 'text',
      required: true,
      maxLength: 160
    },
    {
      key: 'taxId',
      label: 'CNPJ — opcional',
      type: 'text',
      required: false,
      maxLength: 18
    },
    {
      key: 'email',
      label: 'E-mail',
      type: 'email',
      required: true,
      maxLength: 254
    },
    {
      key: 'phone',
      label: 'Telefone — opcional',
      type: 'text',
      required: false,
      maxLength: 20
    },
    {
      key: 'street',
      label: 'Rua — opcional',
      type: 'text',
      required: false,
      maxLength: 160
    },
    {
      key: 'addressNumber',
      label: 'Número — opcional',
      type: 'text',
      required: false,
      maxLength: 20
    },
    {
      key: 'complement',
      label: 'Complemento — opcional',
      type: 'text',
      required: false,
      maxLength: 100
    },
    {
      key: 'district',
      label: 'Bairro — opcional',
      type: 'text',
      required: false,
      maxLength: 100
    },
    {
      key: 'city',
      label: 'Cidade',
      type: 'text',
      required: true,
      maxLength: 100
    },
    {
      key: 'state',
      label: 'UF',
      type: 'text',
      required: true,
      maxLength: 2
    },
    {
      key: 'postalCode',
      label: 'CEP — opcional',
      type: 'text',
      required: false,
      maxLength: 9
    }
  ];

  readonly form = this.formBuilder.group({
    name: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(160)
      ]
    ],
    taxId: [
      '',
      Validators.pattern(
        /^(?:\d{14}|\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2})$/
      )
    ],
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.maxLength(254)
      ]
    ],
    phone: ['', Validators.maxLength(20)],
    street: ['', Validators.maxLength(160)],
    addressNumber: ['', Validators.maxLength(20)],
    complement: ['', Validators.maxLength(100)],
    district: ['', Validators.maxLength(100)],
    city: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(100)
      ]
    ],
    state: [
      '',
      [
        Validators.required,
        Validators.pattern(/^[A-Za-z]{2}$/)
      ]
    ],
    postalCode: [
      '',
      Validators.pattern(/^(?:\d{8}|\d{5}-\d{3})$/)
    ]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    if (this.loading() || this.saving()) {
      return;
    }

    this.loading.set(true);
    this.ready.set(false);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.service
      .find()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: institution => {
          this.applyInstitution(institution);
          this.ready.set(true);
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 404) {
            this.institution.set(null);
            this.form.reset();
            this.ready.set(true);
            return;
          }

          this.errorMessage.set(this.resolveError(error));
        }
      });
  }

  save(): void {
    if (
      !this.isAdmin ||
      !this.ready() ||
      this.loading() ||
      this.saving()
    ) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue();
    const optional = (value: string): string | null =>
      value.trim() || null;

    const request: InstitutionRequest = {
      name: values.name.trim(),
      taxId: values.taxId ? optional(values.taxId.replace(/\D/g, '')) : null,
      email: values.email.trim(),
      phone: optional(values.phone),
      street: optional(values.street),
      addressNumber: optional(values.addressNumber),
      complement: optional(values.complement),
      district: optional(values.district),
      city: values.city.trim(),
      state: values.state.toUpperCase(),
      postalCode: values.postalCode ? optional(values.postalCode.replace(/\D/g, '')) : null
    };

    const operation = this.institution()
      ? this.service.update(request)
      : this.service.create(request);

    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    operation
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)))
      .subscribe({
        next: institution => {
          this.applyInstitution(institution);
          this.successMessage.set('Dados da instituição salvos com sucesso.');
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveError(error));
        }
      });
  }

  private applyInstitution(institution: Institution): void {
    this.institution.set(institution);

    this.form.reset({
      name: institution.name,
      taxId: institution.taxId ?? '',
      email: institution.email,
      phone: institution.phone ?? '',
      street: institution.street ?? '',
      addressNumber: institution.addressNumber ?? '',
      complement: institution.complement ?? '',
      district: institution.district ?? '',
      city: institution.city,
      state: institution.state,
      postalCode: institution.postalCode ?? ''
    });
  }

  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Somente administradores podem alterar a instituição.';
    }

    if (error.status === 409) {
      return 'Já existe uma instituição cadastrada. Use Recarregar dados para consultar o cadastro atual.';
    }

    if (error.status === 404) {
      return 'Instituição não encontrada. Recarregue os dados.';
    }

    if (error.status === 400) {
      return 'Confira os campos obrigatórios, o CNPJ, a UF e o CEP.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível concluir a operação da instituição.';
  }
}
