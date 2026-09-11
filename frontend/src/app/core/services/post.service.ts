import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../models/page.model';
import { Post, PostPayload } from '../models/post.model';

@Injectable({ providedIn: 'root' })
export class PostService {
  private readonly http = inject(HttpClient);

  /** Descubrimiento: todo lo visible para quien consulta, con o sin sesión. */
  explore(page = 0, size = 10): Observable<Page<Post>> {
    return this.http.get<Page<Post>>('/api/posts', { params: pagination(page, size) });
  }

  /** Timeline: lo que publican las cuentas seguidas, más lo propio. Requiere sesión. */
  feed(page = 0, size = 10): Observable<Page<Post>> {
    return this.http.get<Page<Post>>('/api/posts/feed', { params: pagination(page, size) });
  }

  byId(id: number): Observable<Post> {
    return this.http.get<Post>(`/api/posts/${id}`);
  }

  create(payload: PostPayload): Observable<Post> {
    return this.http.post<Post>('/api/posts', payload);
  }

  update(id: number, payload: PostPayload): Observable<Post> {
    return this.http.put<Post>(`/api/posts/${id}`, payload);
  }

  toggleLike(id: number): Observable<Post> {
    return this.http.post<Post>(`/api/posts/${id}/like`, {});
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/posts/${id}`);
  }
}

export function pagination(page: number, size: number): HttpParams {
  return new HttpParams().set('page', page).set('size', size);
}
