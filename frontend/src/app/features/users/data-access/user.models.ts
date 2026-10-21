export type UserRole =
  | 'ADMINISTRATOR'
  | 'PEDAGOGICAL_COORDINATOR'
  | 'TEACHER'
  | 'AEE_TEACHER'
  | 'PSYCHOLOGIST'
  | 'PHYSICIAN'
  | 'LEGAL_GUARDIAN';

export interface User {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  fullName: string;
  email: string;
  password: string;
  role: UserRole;
  cpf: string;
}

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  ADMINISTRATOR: 'Administrador',
  PEDAGOGICAL_COORDINATOR: 'Coordenador pedagógico',
  TEACHER: 'Professor',
  AEE_TEACHER: 'Professor de AEE',
  PSYCHOLOGIST: 'Psicólogo',
  PHYSICIAN: 'Médico',
  LEGAL_GUARDIAN: 'Responsável legal'
};

export const USER_ROLE_OPTIONS = (Object.keys(USER_ROLE_LABELS) as UserRole[])
  .map(value => ({
  value,
  label: USER_ROLE_LABELS[value]
}));
