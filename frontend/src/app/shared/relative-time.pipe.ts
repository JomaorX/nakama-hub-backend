import { Pipe, PipeTransform } from '@angular/core';

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 60 * 60 * 24 * 365],
  ['month', 60 * 60 * 24 * 30],
  ['week', 60 * 60 * 24 * 7],
  ['day', 60 * 60 * 24],
  ['hour', 60 * 60],
  ['minute', 60],
];

/**
 * "hace 3 horas" en lugar de una fecha completa.
 *
 * El backend serializa LocalDateTime sin zona horaria, así que se interpreta como
 * hora del servidor. Cuando haya usuarios fuera de España convendrá pasar a Instant
 * en el backend y quitar este apaño.
 */
@Pipe({ name: 'relativeTime' })
export class RelativeTimePipe implements PipeTransform {
  private readonly formatter = new Intl.RelativeTimeFormat('es', { numeric: 'auto' });

  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const elapsedSeconds = (date.getTime() - Date.now()) / 1000;
    const absolute = Math.abs(elapsedSeconds);

    if (absolute < 60) {
      return 'ahora mismo';
    }

    for (const [unit, secondsInUnit] of UNITS) {
      if (absolute >= secondsInUnit) {
        return this.formatter.format(Math.round(elapsedSeconds / secondsInUnit), unit);
      }
    }

    return 'ahora mismo';
  }
}
