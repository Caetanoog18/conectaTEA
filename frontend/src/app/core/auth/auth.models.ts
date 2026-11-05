export interface LoginRequest {email: string;password: string;}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface JwtPayload {
  sub?: string;
  exp?: number;
  iat?: number;
  roles?: string[];
}
