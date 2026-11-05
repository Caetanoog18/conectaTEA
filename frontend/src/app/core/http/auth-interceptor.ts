import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';

import {AuthService} from '../auth/auth';

export const authInterceptor: HttpInterceptorFn = (
  request,
  next
) => {
  const authService = inject(AuthService);
  const accessToken = authService.getAccessToken();

  const isLoginRequest =
    request.url.endsWith('/api/auth/login');

  if (!accessToken || isLoginRequest) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      }
    })
  );
};
