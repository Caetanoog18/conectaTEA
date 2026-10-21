import {ChangeDetectionStrategy, Component, DestroyRef, inject, signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {UserService} from '../../data-access/user.service';
import {USER_ROLE_OPTIONS, UserRole} from '../../data-access/user.models';
import {cpfValidator, normalizeCpf} from '../../../../shared/validators/cpf.validator';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  return control.get('password')?.value ===
  control.get('confirmPassword')?.value ? null : {passwordMismatch: true};
}

@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-form.html',
  styleUrl: '../../../../shared/styles/management.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserForm {
  private readonly userService = inject(UserService);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly roleOptions = USER_ROLE_OPTIONS;
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    fullName: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(120)
      ]
    ],
    cpf: ['', [Validators.required, cpfValidator]],
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.maxLength(254)
      ]
    ],
    role: this.formBuilder.control<UserRole | ''>(
      '',
      Validators.required
    ),
    password: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.minLength(12),
        Validators.maxLength(64)
      ]
    ],
    confirmPassword: ['', Validators.required]
  }, {
    validators: passwordsMatch
  });

  save(): void {
    if (this.saving()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue();

    if (!values.role) {
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);

    this.userService
      .create({
        fullName: values.fullName.trim(),
        cpf: normalizeCpf(values.cpf),
        email: values.email.trim(),
        role: values.role,
        password: values.password
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false))
      )
      .subscribe({
        next: () => {
          this.form.controls.password.reset();
          this.form.controls.confirmPassword.reset();

          void this.router.navigate(['/users']);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveError(error));
        }
      });
  }

  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Somente administradores podem cadastrar usuários.';
    }

    if (error.status === 409) {
      return 'O e-mail ou CPF já está cadastrado, ou ocorreu um conflito de dados.';
    }

    if (error.status === 400) {
      return 'Confira os dados e informe uma senha entre 12 e 64 caracteres.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível cadastrar o usuário.';
  }
}
