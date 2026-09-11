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

/**
 * Origen público del sitio, usado para construir las URL canónicas y las etiquetas
 * de Open Graph. En el navegador se deduce de la propia página; en el render de
 * servidor hay que decírselo, porque ahí no existe `location`.
 */
export const SITE_ORIGIN = new InjectionToken<string>('SITE_ORIGIN', {
  providedIn: 'root',
  factory: () => (typeof location === 'undefined' ? '' : location.origin),
});
