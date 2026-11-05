export type ConsentStatus =
  | 'ACTIVE'
  | 'REVOKED'
  | 'EXPIRED';

export type ConsentPurpose =
  | 'EDUCATIONAL_SUPPORT'
  | 'MULTIPROFESSIONAL_MONITORING'
  | 'INFORMATION_SHARING_WITH_CARE_TEAM'
  | 'REPORT_GENERATION'
  | 'IMAGE_AND_MEDIA_USE';

export interface Consent {
  id: string;
  studentGuardianId: string;
  studentId: string;
  studentName: string;
  guardianId: string;
  guardianName: string;
  status: ConsentStatus;
  purposes: ConsentPurpose[];
  termsVersion: string;
  grantedAt: string;
  validUntil: string | null;
  recordedByUserId: string;
  revokedAt: string | null;
  revokedByUserId: string | null;
  revocationReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export const CONSENT_STATUS_LABELS: Record<ConsentStatus, string> = {
  ACTIVE: 'Ativo',
  REVOKED: 'Revogado',
  EXPIRED: 'Expirado'
};

export const CONSENT_PURPOSE_LABELS: Record<ConsentPurpose, string> = {
  EDUCATIONAL_SUPPORT: 'Apoio educacional',
  MULTIPROFESSIONAL_MONITORING: 'Acompanhamento multiprofissional',
  INFORMATION_SHARING_WITH_CARE_TEAM: 'Compartilhamento com a equipe',
  REPORT_GENERATION: 'Geração de relatórios',
  IMAGE_AND_MEDIA_USE: 'Uso de imagem e mídia'
};

export interface CreateConsentRequest {
  purposes: ConsentPurpose[];
  termsVersion: string;
  grantedAt: string;
  validUntil: string | null;
}

export interface RevokeConsentRequest {
  reason: string;
}
