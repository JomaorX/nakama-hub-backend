import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Session } from '../models/auth.model';

const ACCESS_TOKEN_KEY = 'nakamahub.accessToken';
const REFRESH_TOKEN_KEY = 'nakamahub.refreshToken';
const USERNAME_KEY = 'nakamahub.username';

/**
 * Guarda la sesión en localStorage.
 *
 * Todo pasa por isPlatformBrowser porque durante el render de servidor no existe
 * localStorage y cualquier acceso directo rompería la página antes de enviarla.
 * Los try/catch cubren el modo privado y los navegadores con el almacenamiento
 * bloqueado, donde el acceso lanza en lugar de devolver vacío.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorage {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  get accessToken(): string | null {
    return this.read(ACCESS_TOKEN_KEY);
  }

  get refreshToken(): string | null {
    return this.read(REFRESH_TOKEN_KEY);
  }

  get username(): string | null {
    return this.read(USERNAME_KEY);
  }

  save(session: Session): void {
    this.write(ACCESS_TOKEN_KEY, session.accessToken);
    this.write(REFRESH_TOKEN_KEY, session.refreshToken);
    this.write(USERNAME_KEY, session.username);
  }

  clear(): void {
    for (const key of [ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, USERNAME_KEY]) {
      this.remove(key);
    }
  }

  private read(key: string): string | null {
    if (!this.isBrowser) {
      return null;
    }
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private write(key: string, value: string): void {
    if (!this.isBrowser) {
      return;
    }
    try {
      localStorage.setItem(key, value);
    } catch {
      // Sin almacenamiento la sesión dura lo que la pestaña. Preferible a romper.
    }
  }

  private remove(key: string): void {
    if (!this.isBrowser) {
      return;
    }
    try {
      localStorage.removeItem(key);
    } catch {
      // Nada que hacer.
    }
  }
}
