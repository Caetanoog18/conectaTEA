export interface InstitutionRequest {
  name: string;
  taxId: string | null;
  email: string;
  phone: string | null;
  street: string | null;
  addressNumber: string | null;
  complement: string | null;
  district: string | null;
  city: string;
  state: string;
  postalCode: string | null;
}

export interface Institution extends InstitutionRequest {
  id: string;
  createdAt: string;
  updatedAt: string;
}
