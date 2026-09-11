import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_ORIGIN } from '../api.config';

const API_PREFIXES = ['/api/', '/auth/'];

/**
 * Antepone el origen del backend a las rutas de la API.
 *
 * En el navegador API_ORIGIN está vacío y la petición sale relativa. En el render
 * de servidor lleva la URL absoluta, porque quien pide es Node y una ruta relativa
 * no significa nada ahí.
 */
export const apiUrlInterceptor: HttpInterceptorFn = (request, next) => {
  const origin = inject(API_ORIGIN);

  if (!origin || !API_PREFIXES.some((prefix) => request.url.startsWith(prefix))) {
    return next(request);
  }

  return next(request.clone({ url: `${origin}${request.url}` }));
};
