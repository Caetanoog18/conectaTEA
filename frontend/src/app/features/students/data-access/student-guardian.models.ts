export type GuardianRelationship =
  | 'MOTHER'
  | 'FATHER'
  | 'STEPMOTHER'
  | 'STEPFATHER'
  | 'GRANDMOTHER'
  | 'GRANDFATHER'
  | 'SIBLING'
  | 'LEGAL_GUARDIAN'
  | 'OTHER';

export interface StudentGuardian {
  id: string;
  studentId: string;
  studentName: string;
  guardianId: string;
  guardianName: string;
  relationship: GuardianRelationship;
  legalGuardian: boolean;
  primaryContact: boolean;
  createdAt: string;
  updatedAt: string;
}

export const GUARDIAN_RELATIONSHIP_LABELS:
  Record<GuardianRelationship, string> = {
  MOTHER: 'Mãe',
  FATHER: 'Pai',
  STEPMOTHER: 'Madrasta',
  STEPFATHER: 'Padrasto',
  GRANDMOTHER: 'Avó',
  GRANDFATHER: 'Avô',
  SIBLING: 'Irmão ou irmã',
  LEGAL_GUARDIAN: 'Responsável legal',
  OTHER: 'Outro'
};

export interface UpdateStudentGuardianRequest {
  relationship: GuardianRelationship;
  legalGuardian: boolean;
  primaryContact: boolean;
}

export interface CreateStudentGuardianRequest
  extends UpdateStudentGuardianRequest {
  guardianId: string;
}
