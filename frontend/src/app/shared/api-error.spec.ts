import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage, fieldErrors } from './api-error';

const respuesta = (status: number, body: unknown) =>
  new HttpErrorResponse({ status, error: body, url: '/api/posts' });

describe('errorMessage', () => {
  it('prefiere el primer error de campo cuando la validación falla', () => {
    const error = respuesta(400, {
      status: 400,
      message: 'Hay campos con valores no válidos',
      fieldErrors: { title: 'El título es obligatorio' },
    });

    expect(errorMessage(error)).toBe('El título es obligatorio');
  });

  it('usa el mensaje de la API cuando no hay errores de campo', () => {
    expect(errorMessage(respuesta(409, { message: 'Ya posees un Post con ese título' })))
      .toBe('Ya posees un Post con ese título');
  });

  it('distingue el servidor caído de un error de la API', () => {
    expect(errorMessage(respuesta(0, null))).toContain('conectar');
  });

  it('devuelve un mensaje genérico ante algo que no es una respuesta HTTP', () => {
    expect(errorMessage(new Error('vaya'))).toContain('Algo ha fallado');
  });
});

describe('fieldErrors', () => {
  it('devuelve los errores por campo para pintarlos junto a cada input', () => {
    const error = respuesta(400, { fieldErrors: { email: 'El email debe ser válido' } });
    expect(fieldErrors(error)).toEqual({ email: 'El email debe ser válido' });
  });

  it('devuelve un objeto vacío cuando no los hay', () => {
    expect(fieldErrors(respuesta(500, {}))).toEqual({});
  });
});
