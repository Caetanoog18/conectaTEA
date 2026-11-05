export interface MedicalDocument {
  id: string;
  studentId: string;
  title: string;
  issuedOn: string;
  uploadedByUserId: string;
  uploadedByName: string;
  sizeBytes: number;
  createdAt: string;
}

export interface UploadMedicalDocumentRequest {
  title: string;
  issuedOn: string;
  file: File;
}
