import type {GuardianRelationship} from '../../students/data-access/student-guardian.models';

export interface AccessibleStudent {
  id: string;
  fullName: string;
  preferredName: string | null;
  enrollmentNumber: string;
  schoolYear: number;
  gradeLevel: string;
  className: string;
}

export interface LegalGuardianContact {
  guardianId: string;
  fullName: string;
  relationship: GuardianRelationship;
  primaryContact: boolean;
  email: string | null;
  phone: string;
}
