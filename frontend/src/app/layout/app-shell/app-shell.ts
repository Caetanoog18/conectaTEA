import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';

import {AuthService} from '../../core/auth/auth';

@Component({
  selector: 'app-app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppShell {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly menuOpen = signal(false);
  readonly userEmail = this.authService.getCurrentUserEmail() ?? 'Usuário autenticado';
  readonly roleLabel = this.resolveRoleLabel();

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private resolveRoleLabel(): string {
    const systemRole = this.authService
      .getCurrentUserRoles()
      .find((role) => !role.startsWith('FACTOR_'));

    const labels: Record<string, string> = {
      ADMINISTRATOR: 'Administrador',
      PEDAGOGICAL_COORDINATOR:
        'Coordenador pedagógico',
      TEACHER: 'Professor',
      AEE_TEACHER: 'Professor de AEE',
      PSYCHOLOGIST: 'Psicólogo',
      PHYSICIAN: 'Médico',
      LEGAL_GUARDIAN: 'Responsável legal'
    };

    return systemRole ? labels[systemRole] ?? systemRole : 'Usuário';
  }
}
