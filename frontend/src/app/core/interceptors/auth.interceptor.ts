import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { RefreshGateway } from '../services/refresh-gateway';
import { TokenStorage } from '../services/token-storage';

/** Rutas que no llevan token y cuyo 401 no debe disparar un refresco. */
const PUBLIC_AUTH_ROUTES = ['/auth/login', '/auth/signup', '/auth/refresh', '/auth/logout'];

/**
 * Un único refresco en curso, compartido por todas las peticiones.
 *
 * Sin esto, varias peticiones que caduquen a la vez lanzarían refrescos en
 * paralelo. Como el backend rota el token en cada uso y trata la reutilización
 * como robo, eso revocaría la sesión entera. Así que la primera petición que ve
 * el 401 refresca y las demás esperan al token resultante.
 */
let refreshInProgress = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const storage = inject(TokenStorage);
  const auth = inject(AuthService);
  const refresher = inject(RefreshGateway);
  const router = inject(Router);

  const isPublicAuthRoute = PUBLIC_AUTH_ROUTES.some((route) => request.url.includes(route));
  const accessToken = storage.accessToken;
  const outgoing = accessToken && !isPublicAuthRoute ? withToken(request, accessToken) : request;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      const expired =
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !isPublicAuthRoute &&
        storage.refreshToken !== null;

      if (!expired) {
        return throwError(() => error);
      }

      if (refreshInProgress) {
        return waitForRefresh(request, next);
      }

      refreshInProgress = true;
      refreshedToken$.next(null);

      return refresher.refresh(storage.refreshToken!).pipe(
        switchMap((session) => {
          refreshInProgress = false;
          refreshedToken$.next(session.accessToken);
          return next(withToken(request, session.accessToken));
        }),
        catchError((refreshError: unknown) => {
          refreshInProgress = false;
          auth.endSession();
          void router.navigate(['/entrar'], { queryParams: { expirada: true } });
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};

function waitForRefresh(request: HttpRequest<unknown>, next: HttpHandlerFn): Observable<any> {
  return refreshedToken$.pipe(
    filter((token): token is string => token !== null),
    take(1),
    switchMap((token) => next(withToken(request, token))),
  );
}

function withToken(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
