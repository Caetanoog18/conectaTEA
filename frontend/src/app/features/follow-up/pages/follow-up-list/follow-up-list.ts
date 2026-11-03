import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';

import {FollowUpService} from '../../data-access/follow-up.service';
import type {AccessibleStudent} from '../../data-access/follow-up.models';

@Component({
  selector: 'app-follow-up-list',
  imports: [RouterLink],
  templateUrl: './follow-up-list.html',
  styleUrls: [
    '../../../../shared/styles/management.scss',
    './follow-up-list.scss'
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FollowUpList implements OnInit {
  private readonly service = inject(FollowUpService);
  private readonly destroyRef = inject(DestroyRef);

  readonly students = signal<AccessibleStudent[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    if (
      page < 0 ||
      this.loading() ||
      (this.totalPages() > 0 && page >= this.totalPages())
    ) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.students.set([]);

    this.service
      .findAll(page, this.pageSize)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: response => {
          this.students.set(response.content);
          this.currentPage.set(response.page);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
        },
        error: (error: HttpErrorResponse) => {
          this.totalPages.set(0);
          this.totalElements.set(0);
          this.errorMessage.set(this.resolveError(error));
        }
      });
  }

  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return 'Sua sessão expirou. Entre novamente.';
    }

    if (error.status === 403) {
      return 'Sua conta não possui acesso ao acompanhamento profissional.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível carregar os estudantes acompanhados.';
  }
}
