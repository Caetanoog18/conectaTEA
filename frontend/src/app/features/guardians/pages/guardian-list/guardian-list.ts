import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { Guardian } from '../../data-access/guardian.models';
import { GuardianService } from '../../data-access/guardian.service';

@Component({
  selector: 'app-guardian-list',
  imports: [],
  templateUrl: './guardian-list.html',
  styleUrl: './guardian-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GuardianList implements OnInit {
  private readonly guardianService =
    inject(GuardianService);

  readonly guardians = signal<Guardian[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly currentPage = signal(0);
  readonly pageSize = 20;
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    if (page < 0 || this.loading()) {
      return;
    }

    if (this.totalPages() > 0 && page >= this.totalPages()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.guardianService
      .findAll(page, this.pageSize)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.guardians.set(response.content);
          this.currentPage.set(response.page);
          this.totalElements.set(response.totalElements);
          this.totalPages.set(response.totalPages);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveErrorMessage(error));
        }
      });
  }

  previousPage(): void {
    this.loadPage(this.currentPage() - 1);
  }

  nextPage(): void {
    this.loadPage(this.currentPage() + 1);
  }

  formatCpf(cpf: string | null): string {
    if (!cpf) {
      return 'Não informado';
    }

    const digits = cpf.replace(/\D/g, '');

    if (digits.length !== 11) {
      return cpf;
    }

    return digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Seu perfil não pode consultar responsáveis';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor';
    }

    const detail = error.error?.detail;

    if (typeof detail === 'string' && detail) {
      return detail;
    }

    return 'Não foi possível carregar os responsáveis';
  }
}
