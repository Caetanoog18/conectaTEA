import type {ConsentPurpose} from '../../consents/data-access/consent.models';

export interface TimelineEvent {
  id: string;
  type: string;
  studentId: string;
  authorId: string;
  authorName: string;
  purpose: ConsentPurpose;
  title: string;
  content: string;
  occurredAt: string;
  recordedAt: string;
}

export const AUDIT_ACTION_LABELS = {
  OBSERVATION_CREATE: 'Criação de observação',
  OBSERVATION_LIST: 'Listagem de observações',
  OBSERVATION_READ: 'Consulta de observação',
  TIMELINE_READ: 'Consulta da linha do tempo',
  AUDIT_EVENTS_LIST: 'Consulta da auditoria',
  REPORT_GENERATE: 'Geração de relatório',
  REPORT_PDF_EXPORT: 'Exportação de relatório em PDF',
  USER_CREATE: 'Criação de usuário',
  USER_STATUS_UPDATE: 'Alteração de status de usuário',
  CONSENT_CREATE: 'Registro de consentimento',
  CONSENT_REVOKE: 'Revogação de consentimento',
  STUDENT_CREATE: 'Criação de estudante',
  STUDENT_UPDATE: 'Edição de estudante',
  STUDENT_STATUS_UPDATE: 'Alteração de status de estudante',
  GUARDIAN_CREATE: 'Criação de responsável',
  GUARDIAN_UPDATE: 'Edição de responsável',
  GUARDIAN_STATUS_UPDATE: 'Alteração de status de responsável',
  STUDENT_GUARDIAN_LINK_CREATE: 'Criação de vínculo com responsável',
  STUDENT_GUARDIAN_LINK_UPDATE: 'Edição de vínculo com responsável',
  STUDENT_GUARDIAN_LINK_DELETE: 'Remoção de vínculo com responsável',
  CARE_TEAM_LINK_CREATE: 'Vínculo de profissional',
  CARE_TEAM_LINK_END: 'Encerramento de vínculo profissional',
  MEDICAL_DOCUMENT_UPLOAD: 'Envio de laudo médico',
  MEDICAL_DOCUMENT_LIST: 'Listagem de laudos médicos',
  MEDICAL_DOCUMENT_DOWNLOAD: 'Download de laudo médico'
} as const;

export const AUDIT_OUTCOME_LABELS = {
  SUCCESS: 'Sucesso',
  DENIED: 'Acesso negado',
  FAILURE: 'Falha'
} as const;

export type AuditAction = keyof typeof AUDIT_ACTION_LABELS;
export type AuditOutcome = keyof typeof AUDIT_OUTCOME_LABELS;

export interface AuditEvent {
  id: string;
  actorUserId: string | null;
  action: AuditAction;
  outcome: AuditOutcome;
  studentId: string | null;
  resourceId: string | null;
  requestId: string;
  occurredAt: string;
}

export interface PeriodFilters {
  from?: string;
  to?: string;
}

export interface AuditFilters extends PeriodFilters {
  studentId?: string;
  actorUserId?: string;
  requestId?: string;
  action?: AuditAction;
  outcome?: AuditOutcome;
}
