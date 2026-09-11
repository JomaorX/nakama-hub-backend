import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Session } from '../models/auth.model';
import { AuthService } from './auth.service';

/**
 * Canje del token de refresco, aparte de AuthService para que el interceptor no
 * dependa de él y no se forme un ciclo entre ambos.
 */
@Injectable({ providedIn: 'root' })
export class RefreshGateway {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  refresh(refreshToken: string): Observable<Session> {
    return this.http
      .post<Session>('/auth/refresh', { refreshToken })
      .pipe(tap((session) => this.auth.startSession(session)));
  }
}
