import {inject, Injectable} from '@angular/core';
import {AuthService} from './auth';
import {
  ADMIN_ROLES,
  ALL_ROLES,
  isUserRole,
  MANAGEMENT_ROLES,
  PROFESSIONAL_ROLES,
  USER_ROLE_LABELS,
  UserRole
} from './roles';

export interface NavigationItem {
  path: string;
  label: string;
  description: string;
  symbol: string;
}

@Injectable({
  providedIn: 'root'
})
export class AccessService {
  private readonly authService = inject(AuthService);

  get roles(): UserRole[] {
    const roles: unknown = this.authService.getCurrentUserRoles();

    return Array.isArray(roles)
      ? roles.filter(isUserRole)
      : [];
  }

  get primaryRole(): UserRole | null {
    return ALL_ROLES.find(role => this.roles.includes(role)) ?? null;
  }

  get roleLabel(): string {
    const role = this.primaryRole;

    return role ? USER_ROLE_LABELS[role] : 'Perfil não reconhecido';
  }

  hasAnyRole(allowedRoles: readonly string[]): boolean {
    return this.roles.some(role => allowedRoles.includes(role));
  }

  canManageUsers(): boolean {
    return this.hasAnyRole(ADMIN_ROLES);
  }

  canManageStudents(): boolean {
    return this.hasAnyRole(MANAGEMENT_ROLES);
  }

  isProfessional(): boolean {
    return this.hasAnyRole(PROFESSIONAL_ROLES);
  }

  get navigation(): NavigationItem[] {
    const items: NavigationItem[] = [
      {
        path: '/dashboard',
        label: 'Painel',
        description: 'Acesse a visão inicial do seu perfil.',
        symbol: '⌂'
      }
    ];

    if (this.canManageStudents()) {
      items.push(
        {
          path: '/students',
          label: 'Estudantes',
          description:
            'Gerencie cadastros, responsáveis, equipe e consentimentos.',
          symbol: '♙'
        },
        {
          path: '/guardians',
          label: 'Responsáveis',
          description:
            'Consulte e gerencie os cadastros dos responsáveis.',
          symbol: '♧'
        }
      );
    }

    if (this.canManageUsers()) {
      items.push({
        path: '/users',
        label: 'Usuários',
        description:
          'Cadastre contas e gerencie sua ativação.',
        symbol: '♙'
      });
    }

    items.push({
      path: '/institution',
      label: 'Instituição',
      description: this.canManageUsers()
        ? 'Consulte e atualize os dados da instituição.'
        : 'Consulte os dados da instituição.',
      symbol: '⌂'
    });

    return items;
  }
}
