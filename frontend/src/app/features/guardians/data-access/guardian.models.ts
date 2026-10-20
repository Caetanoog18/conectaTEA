export interface Guardian {
  id: string;
  userId: string | null;
  fullName: string;
  cpf: string | null;
  email: string | null;
  phone: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface GuardianRequest {
  fullName: string;
  cpf: string | null;
  email: string | null;
  phone: string;
  userId: string | null;
}
