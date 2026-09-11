import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PublicProfile, UserProfile } from '../models/user.model';

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

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>('/api/users/me/password', { currentPassword, newPassword });
  }
}
