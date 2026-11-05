import { TestBed } from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import { AuthService } from './auth';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    sessionStorage.removeItem('conectatea.access-token');

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AuthService);
  });

  afterEach(() => {
    sessionStorage.removeItem('conectatea.access-token');
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start unauthenticated without a token', () => {
    expect(service.isAuthenticated()).toBe(false);
  });
});
