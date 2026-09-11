import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type ReportTargetType = 'POST' | 'COMMENT' | 'USER';

export const REPORT_REASONS = [
  { value: 'SPAM', label: 'Spam o publicidad' },
  { value: 'ACOSO', label: 'Acoso a otra persona' },
  { value: 'DISCURSO_DE_ODIO', label: 'Discurso de odio' },
  { value: 'CONTENIDO_SEXUAL', label: 'Contenido sexual' },
  { value: 'VIOLENCIA', label: 'Violencia' },
  { value: 'SPOILER_SIN_AVISO', label: 'Spoiler sin avisar' },
  { value: 'SUPLANTACION', label: 'Suplantación de identidad' },
  { value: 'OTRO', label: 'Otro motivo' },
] as const;

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);

  create(targetType: ReportTargetType, targetId: number, reason: string, details: string) {
    return this.http.post('/api/reports', { targetType, targetId, reason, details }) as Observable<unknown>;
  }
}
