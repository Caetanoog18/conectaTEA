import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {RouterLink} from '@angular/router';
import {AccessService} from '../../../../core/auth/access.service';
import type {UserRole} from '../../../../core/auth/roles';

interface DashboardContent {
  title: string;
  description: string;
}

const CONTENT: Record<UserRole, DashboardContent> = {
  ADMINISTRATOR: {
    title: 'Painel administrativo',
    description:
      'Gerencie os cadastros, as contas e a rede de atendimento dos estudantes.'
  },
  PEDAGOGICAL_COORDINATOR: {
    title: 'Painel da coordenação pedagógica',
    description:
      'Organize os estudantes, os responsáveis, a equipe e os consentimentos.'
  },
  TEACHER: {
    title: 'Painel do professor',
    description:
      'Seu espaço de acompanhamento educacional dos estudantes.'
  },
  AEE_TEACHER: {
    title: 'Painel do professor de AEE',
    description:
      'Seu espaço de acompanhamento educacional especializado.'
  },
  PSYCHOLOGIST: {
    title: 'Painel do psicólogo',
    description:
      'Seu espaço de acompanhamento multiprofissional dos estudantes.'
  },
  PHYSICIAN: {
    title: 'Painel do médico',
    description:
      'Seu espaço de acompanhamento médico dos estudantes.'
  },
  LEGAL_GUARDIAN: {
    title: 'Painel do responsável legal',
    description:
      'Seu espaço de consulta das informações autorizadas dos estudantes sob sua responsabilidade.'
  }
};

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Dashboard {
  readonly access = inject(AccessService);

  get content(): DashboardContent {
    const role = this.access.primaryRole;

    return role
      ? CONTENT[role]
      : {
        title: 'Painel do ConectaTEA',
        description:
          'Seu perfil não foi reconhecido. Solicite a verificação da conta à administração.'
      };
  }

  get shortcuts() {
    return this.access.navigation.filter(
      item => item.path !== '/dashboard'
    );
  }

  get showProfessionalNotice(): boolean {
    return this.access.isProfessional() &&
      !this.access.canManageStudents();
  }
}
