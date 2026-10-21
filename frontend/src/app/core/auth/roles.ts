export type UserRole =
  | 'ADMINISTRATOR'
  | 'PEDAGOGICAL_COORDINATOR'
  | 'TEACHER'
  | 'AEE_TEACHER'
  | 'PSYCHOLOGIST'
  | 'PHYSICIAN'
  | 'LEGAL_GUARDIAN';

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  ADMINISTRATOR: 'Administrador',
  PEDAGOGICAL_COORDINATOR: 'Coordenador pedagógico',
  TEACHER: 'Professor',
  AEE_TEACHER: 'Professor de AEE',
  PSYCHOLOGIST: 'Psicólogo',
  PHYSICIAN: 'Médico',
  LEGAL_GUARDIAN: 'Responsável legal'
};

export const ALL_ROLES: readonly UserRole[] = [
  'ADMINISTRATOR',
  'PEDAGOGICAL_COORDINATOR',
  'TEACHER',
  'AEE_TEACHER',
  'PSYCHOLOGIST',
  'PHYSICIAN',
  'LEGAL_GUARDIAN'
];

export const ADMIN_ROLES: readonly UserRole[] = [
  'ADMINISTRATOR'
];

export const MANAGEMENT_ROLES: readonly UserRole[] = [
  'ADMINISTRATOR',
  'PEDAGOGICAL_COORDINATOR'
];

export const PROFESSIONAL_ROLES: readonly UserRole[] = [
  'PEDAGOGICAL_COORDINATOR',
  'TEACHER',
  'AEE_TEACHER',
  'PSYCHOLOGIST',
  'PHYSICIAN'
];

export function isUserRole(value: unknown): value is UserRole {
  return typeof value === 'string' &&
    ALL_ROLES.some(role => role === value);
}
