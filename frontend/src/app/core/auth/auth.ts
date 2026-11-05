import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, tap} from 'rxjs';

import {
  JwtPayload,
  LoginRequest,
  TokenResponse
} from './auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly tokenKey = 'conectatea.access-token';

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/api/auth/login', request)
      .pipe(
        tap((response) => {
          sessionStorage.setItem(
            this.tokenKey,
            response.accessToken
          );
        })
      );
  }

  logout(): void {
    sessionStorage.removeItem(this.tokenKey);
  }

  getAccessToken(): string | null {
    const token = sessionStorage.getItem(this.tokenKey);

    if (!token) {
      return null;
    }

    if (this.isExpired(token)) {
      this.logout();
      return null;
    }

    return token;
  }

  isAuthenticated(): boolean {
    return this.getAccessToken() !== null;
  }

  getCurrentUserEmail(): string | null {
    return this.getPayload()?.sub ?? null;
  }

  getCurrentUserRoles(): string[] {
    return this.getPayload()?.roles ?? [];
  }

  private getPayload(): JwtPayload | null {
    const token = sessionStorage.getItem(this.tokenKey);

    if (!token) {
      return null;
    }

    try {
      const encodedPayload = token.split('.')[1];

      if (!encodedPayload) {
        return null;
      }

      const normalizedPayload = encodedPayload
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(
          Math.ceil(encodedPayload.length / 4) * 4,
          '='
        );

      return JSON.parse(
        atob(normalizedPayload)
      ) as JwtPayload;
    } catch {
      return null;
    }
  }

  private isExpired(token: string): boolean {
    try {
      const payload = this.decodePayload(token);

      if (!payload.exp) {
        return true;
      }

      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }

  private decodePayload(token: string): JwtPayload {
    const encodedPayload = token.split('.')[1];

    if (!encodedPayload) {
      throw new Error('Invalid JWT');
    }

    const normalizedPayload = encodedPayload
      .replace(/-/g, '+')
      .replace(/_/g, '/')
      .padEnd(
        Math.ceil(encodedPayload.length / 4) * 4,
        '='
      );

    return JSON.parse(
      atob(normalizedPayload)
    ) as JwtPayload;
  }
}
