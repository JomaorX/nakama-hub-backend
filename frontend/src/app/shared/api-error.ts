import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../core/models/auth.model';

const FALLBACK = 'Algo ha fallado. Vuelve a intentarlo en un momento.';

/** Texto que enseñar al usuario a partir del error unificado que devuelve la API. */
export function errorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return FALLBACK;
  }

  if (error.status === 0) {
    return 'No se ha podido conectar con el servidor.';
  }

  const body = error.error as ApiError | null;

  if (body?.fieldErrors) {
    const first = Object.values(body.fieldErrors)[0];
    if (first) {
      return first;
    }
  }

  return body?.message ?? FALLBACK;
}

/** Errores por campo, para pintarlos junto a cada input del formulario. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof HttpErrorResponse)) {
    return {};
  }
  return (error.error as ApiError | null)?.fieldErrors ?? {};
}
