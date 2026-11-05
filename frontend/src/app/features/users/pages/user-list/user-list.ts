import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs';
import {AuthService} from '../../../../core/auth/auth';
import {UserService} from '../../data-access/user.service';
import {User, USER_ROLE_LABELS, UserRole} from '../../data-access/user.models';

@Component({
  selector: 'app-user-list',
  imports: [RouterLink],
  templateUrl: './user-list.html',
  styleUrl: '../../../../shared/styles/management.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserList implements OnInit {
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly users = signal<User[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly actionErrorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;
  readonly selectedUser = signal<User | null>(null);
  readonly updatingStatus = signal(false);

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    if (
      page < 0 ||
      this.loading() ||
      this.updatingStatus() ||
      (this.totalPages() > 0 && page >= this.totalPages())
    ) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.selectedUser.set(null);
    this.actionErrorMessage.set(null);

    this.userService
      .findAll(page, this.pageSize)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: response => {
          this.users.set(response.content);
          this.currentPage.set(response.page);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.resolveError(error));
        }
      });
  }

  isCurrentUser(user: User): boolean {
    const email = this.authService.getCurrentUserEmail();

    return email !== null &&
      user.email.toLowerCase() === email.toLowerCase();
  }

  requestStatusChange(user: User): void {
    if (
      this.loading() ||
      this.updatingStatus() ||
      (user.active && this.isCurrentUser(user))
    ) {
      return;
    }

    this.selectedUser.set(user);
    this.actionErrorMessage.set(null);
    this.successMessage.set(null);
  }

  cancelStatusChange(): void {
    if (this.updatingStatus()) {
      return;
    }

    this.selectedUser.set(null);
    this.actionErrorMessage.set(null);
  }

  confirmStatusChange(): void {
    const user = this.selectedUser();

    if (
      !user ||
      this.updatingStatus() ||
      (user.active && this.isCurrentUser(user))
    ) {
      return;
    }

    this.updatingStatus.set(true);
    this.actionErrorMessage.set(null);

    this.userService
      .updateStatus(user.id, !user.active)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.updatingStatus.set(false))
      )
      .subscribe({
        next: updatedUser => {
          this.users.update(users =>
            users.map(item =>
              item.id === updatedUser.id ? updatedUser : item));

          this.selectedUser.set(null);
          this.successMessage.set(
            updatedUser.active
              ? 'Usuário ativado com sucesso.'
              : 'Usuário desativado com sucesso.'
          );
        },
        error: (error: HttpErrorResponse) => {
          this.actionErrorMessage.set(this.resolveError(error));
        }
      });
  }

  roleLabel(role: UserRole): string {
    return USER_ROLE_LABELS[role] ?? role;
  }

  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 403) {
      return 'Somente administradores podem gerenciar usuários.';
    }

    if (error.status === 404) {
      return 'Usuário não encontrado. Atualize a listagem.';
    }

    if (error.status === 409) {
      return 'Não foi possível alterar o status. Você não pode desativar a própria conta.';
    }

    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }

    return 'Não foi possível concluir a operação de usuários.';
  }
}
