import type {AccessibleStudent} from './follow-up.models';

export type ObservationPurpose = | 'EDUCATIONAL_SUPPORT' | 'MULTIPROFESSIONAL_MONITORING';

export interface Observation {
  id: string;
  studentId: string;
  authorId: string;
  authorName: string;
  purpose: ObservationPurpose;
  title: string;
  content: string;
  occurredAt: string;
  createdAt: string;
}

export interface CreateObservationRequest {
  title: string;
  content: string;
  occurredAt: string;
}

export interface GenerateReportRequest {
  from: string;
  to: string;
}

export interface StudentReport {
  reportId: string;
  generatedAt: string;
  student: AccessibleStudent;
  generatedBy: {
    id: string;
    fullName: string;
  };
  purpose: ObservationPurpose;
  from: string;
  to: string;
  totalObservations: number;
  observations: Observation[];
}
