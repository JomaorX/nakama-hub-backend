import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { LoginPayload, Session, SignupPayload } from '../models/auth.model';
import { TokenStorage } from './token-storage';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(TokenStorage);

  /**
   * Nombre del usuario con sesión iniciada, o null.
   *
   * Durante el render de servidor siempre es null, porque el token vive en el
   * navegador. La página se sirve en su versión anónima y la cabecera se completa
   * al hidratar. Para el contenido público, que es lo que interesa indexar, el
   * HTML que ve Google es el correcto.
   */
  private readonly currentUsername = signal<string | null>(this.storage.username);

  readonly username = this.currentUsername.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUsername() !== null);

  login(payload: LoginPayload): Observable<Session> {
    return this.http
      .post<Session>('/auth/login', payload)
      .pipe(tap((session) => this.startSession(session)));
  }

  signup(payload: SignupPayload): Observable<unknown> {
    return this.http.post('/auth/signup', payload);
  }

  /** Cierra la sesión en el servidor para que el token de refresco quede revocado. */
  logout(): void {
    const refreshToken = this.storage.refreshToken;
    this.endSession();

    if (refreshToken) {
      this.http.post('/auth/logout', { refreshToken }).subscribe({
        error: () => {
          // La sesión local ya está cerrada; que falle la revocación no cambia nada aquí.
        },
      });
    }
  }

  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>('/auth/password/forgot', { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/auth/password/reset', { token, newPassword });
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>('/auth/verify', { token });
  }

  resendVerification(): Observable<void> {
    return this.http.post<void>('/api/users/me/verify/resend', {});
  }

  startSession(session: Session): void {
    this.storage.save(session);
    this.currentUsername.set(session.username);
  }

  endSession(): void {
    this.storage.clear();
    this.currentUsername.set(null);
  }
}
