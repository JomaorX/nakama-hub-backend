import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../models/page.model';
import { ContentType, Post } from '../models/post.model';
import { Serie, UserSearchResult } from '../models/user.model';

export interface PostSearchFilters {
  q?: string;
  contentType?: ContentType | null;
  serie?: string | null;
  category?: string | null;
}

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly http = inject(HttpClient);

  posts(filters: PostSearchFilters, page = 0, size = 10): Observable<Page<Post>> {
    let params = new HttpParams().set('page', page).set('size', size);

    for (const [key, value] of Object.entries(filters)) {
      if (value) {
        params = params.set(key, value);
      }
    }

    return this.http.get<Page<Post>>('/api/search/posts', { params });
  }

  users(q: string, page = 0, size = 10): Observable<Page<UserSearchResult>> {
    return this.http.get<Page<UserSearchResult>>('/api/search/users', {
      params: new HttpParams().set('q', q).set('page', page).set('size', size),
    });
  }

  series(q: string, page = 0, size = 10): Observable<Page<Serie>> {
    return this.http.get<Page<Serie>>('/api/search/series', {
      params: new HttpParams().set('q', q).set('page', page).set('size', size),
    });
  }
}
