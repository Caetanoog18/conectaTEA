import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';

import {AuthService} from '../../core/auth/auth';
import {AccessService} from '../../core/auth/access.service';

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

  readonly access = inject(AccessService);
  readonly menuOpen = signal(false);

  get userEmail(): string {
    return this.authService.getCurrentUserEmail() ??
      'Usuário autenticado';
  }

  get roleLabel(): string {
    return this.access.roleLabel;
  }

  toggleMenu(): void {
    this.menuOpen.update(open => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.closeMenu();
    this.authService.logout();

    void this.router.navigate(['/login']);
  }
}
