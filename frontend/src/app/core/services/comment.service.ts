import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Comment, CreateCommentPayload } from '../models/comment.model';
import { Page } from '../models/page.model';
import { pagination } from './post.service';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private readonly http = inject(HttpClient);

  byPost(postId: number, page = 0, size = 20): Observable<Page<Comment>> {
    return this.http.get<Page<Comment>>(`/api/comments/post/${postId}`, {
      params: pagination(page, size),
    });
  }

  replies(commentId: number, page = 0, size = 20): Observable<Page<Comment>> {
    return this.http.get<Page<Comment>>(`/api/comments/${commentId}/replies`, {
      params: pagination(page, size),
    });
  }

  create(payload: CreateCommentPayload): Observable<Comment> {
    return this.http.post<Comment>('/api/comments', payload);
  }

  update(id: number, content: string): Observable<Comment> {
    return this.http.put<Comment>(`/api/comments/${id}`, { content });
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/comments/${id}`);
  }
}
