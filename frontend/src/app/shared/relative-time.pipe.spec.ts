import { RelativeTimePipe } from './relative-time.pipe';

describe('RelativeTimePipe', () => {
  const pipe = new RelativeTimePipe();
  const hace = (segundos: number) => new Date(Date.now() - segundos * 1000).toISOString();

  it('llama ahora mismo a lo de hace menos de un minuto', () => {
    expect(pipe.transform(hace(20))).toBe('ahora mismo');
  });

  it('usa minutos, horas y días según la distancia', () => {
    expect(pipe.transform(hace(60 * 5))).toContain('minuto');
    expect(pipe.transform(hace(60 * 60 * 3))).toContain('hora');
    // A dos días el formato natural dice "anteayer", así que se comprueba con cuatro.
    expect(pipe.transform(hace(60 * 60 * 24 * 4))).toContain('días');
  });

  it('usa el lenguaje natural para ayer y anteayer', () => {
    expect(pipe.transform(hace(60 * 60 * 24))).toBe('ayer');
    expect(pipe.transform(hace(60 * 60 * 24 * 2))).toBe('anteayer');
  });

  it('no revienta con valores ausentes o inválidos', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
    expect(pipe.transform('no es una fecha')).toBe('');
  });
});
