import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import { finalize } from 'rxjs';

import {AuthService} from '../../../../core/auth/auth';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.email
      ]
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
    })
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login(this.form.getRawValue()).pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          const returnUrl =
            this.route.snapshot.queryParamMap.get(
              'returnUrl') ?? '/dashboard';

          void this.router.navigateByUrl(returnUrl);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return 'E-mail ou senha inválidos.';
    }

    if (error.status === 429) {
      return 'Muitas tentativas. Aguarde e tente novamente.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível realizar o login.';
  }
}
