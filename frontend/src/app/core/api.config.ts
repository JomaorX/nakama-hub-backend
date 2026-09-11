import { InjectionToken } from '@angular/core';

/**
 * Origen del backend.
 *
 * En el navegador queda vacío: las peticiones salen relativas y las resuelve el
 * proxy de `ng serve` en desarrollo y nginx en producción, de modo que no hay
 * CORS ni una URL que cambiar al desplegar.
 *
 * En el render de servidor no existe un origen relativo, porque quien pide es un
 * proceso de Node y no un navegador, así que ahí sí hace falta la URL absoluta.
 */
export const API_ORIGIN = new InjectionToken<string>('API_ORIGIN', {
  providedIn: 'root',
  factory: () => '',
});
