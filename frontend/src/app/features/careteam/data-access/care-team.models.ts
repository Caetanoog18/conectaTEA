export interface ProfessionalLink {
  id: string;
  studentId: string;
  studentName: string;
  professionalId: string;
  professionalName: string;
  professionalRole: string;
  startedOn: string;
  endedOn: string | null;
  active: boolean;
  createdByUserId: string;
  endedByUserId: string | null;
  endReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export const PROFESSIONAL_ROLE_LABELS: Record<string, string> = {
  ADMINISTRATOR: 'Administrador',
  PEDAGOGICAL_COORDINATOR: 'Coordenador pedagógico',
  TEACHER: 'Professor',
  AEE_TEACHER: 'Professor AEE',
  PSYCHOLOGIST: 'Psicólogo',
  PHYSICIAN: 'Médico',
  LEGAL_GUARDIAN: 'Responsável legal'
};

export interface ProfessionalOption {
  id: string;
  fullName: string;
  email: string;
  role: string;
}

export interface CreateProfessionalLinkRequest {
  professionalId: string;
  startedOn: string;
}
