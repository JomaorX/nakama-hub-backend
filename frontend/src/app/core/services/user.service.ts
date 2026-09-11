import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PublicProfile, UserProfile, UserSearchResult } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/users/me');
  }

  profile(username: string): Observable<PublicProfile> {
    return this.http.get<PublicProfile>(`/api/users/${username}`);
  }

  toggleFollow(username: string): Observable<PublicProfile> {
    return this.http.put<PublicProfile>(`/api/users/${username}/follow`, {});
  }

  updateBio(bio: string): Observable<UserProfile> {
    return this.http.put<UserProfile>('/api/users/me/bio', { bio });
  }

  updateAvatar(avatarUrl: string): Observable<UserProfile> {
    return this.http.put<UserProfile>('/api/users/me/avatar', { avatarUrl });
  }

  /**
   * Bloquea o desbloquea. Al bloquear, el perfil deja de estar disponible para el
   * bloqueado, pero quien bloquea sigue viéndolo para poder deshacerlo.
   */
  toggleBlock(username: string): Observable<PublicProfile> {
    return this.http.put<PublicProfile>(`/api/users/${username}/block`, {});
  }

  blockedUsers(): Observable<UserSearchResult[]> {
    return this.http.get<UserSearchResult[]>('/api/users/me/blocked');
  }

  updatePrivacy(privacy: 'PUBLIC' | 'PRIVATE'): Observable<UserProfile> {
    return this.http.put<UserProfile>('/api/users/me/privacy', { privacy });
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>('/api/users/me/password', { currentPassword, newPassword });
  }

  /** Volcado de datos personales que exige el derecho de acceso del RGPD. */
  exportMyData(): Observable<unknown> {
    return this.http.get('/api/users/me/export');
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>('/api/users/me');
  }
}
