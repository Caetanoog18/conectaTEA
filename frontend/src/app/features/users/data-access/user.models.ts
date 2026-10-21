import {ALL_ROLES, USER_ROLE_LABELS} from '../../../core/auth/roles';
import type {UserRole} from '../../../core/auth/roles';

export {USER_ROLE_LABELS} from '../../../core/auth/roles';
export type {UserRole} from '../../../core/auth/roles';

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

export const USER_ROLE_OPTIONS = ALL_ROLES.map(value => ({
  value,
  label: USER_ROLE_LABELS[value]
}));
