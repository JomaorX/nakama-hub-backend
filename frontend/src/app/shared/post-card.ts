import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Post } from '../core/models/post.model';
import { Avatar } from './avatar';
import { RelativeTimePipe } from './relative-time.pipe';

@Component({
  selector: 'app-post-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Avatar, RelativeTimePipe],
  template: `
    <article class="card">
      <header class="card__head">
        <a class="card__author" [routerLink]="['/u', post().authorUsername]">
          <app-avatar [username]="post().authorUsername" [size]="34" />
          <span class="card__author-name">{{ post().authorUsername }}</span>
        </a>
        <time class="card__time" [attr.datetime]="post().createdAt">
          {{ post().createdAt | relativeTime }}
        </time>
      </header>

      <a class="card__body" [routerLink]="['/post', post().id]">
        <h2 class="card__title">{{ post().title }}</h2>
        <p class="card__excerpt">{{ post().content }}</p>
      </a>

      <footer class="card__foot">
        <div class="tags">
          @if (post().serieName) {
            <span class="tag tag--serie">{{ post().serieName }}</span>
          }
          @for (category of post().categories; track category) {
            <span class="tag">{{ category }}</span>
          }
          @if (post().status !== 'PUBLISHED') {
            <span class="tag tag--draft">{{ post().status === 'DRAFT' ? 'Borrador' : 'Archivado' }}</span>
          }
          @if (post().privacy !== 'PUBLIC') {
            <span class="tag tag--private">
              {{ post().privacy === 'PRIVATE' ? 'Privado' : 'Solo seguidores' }}
            </span>
          }
        </div>
        <div class="stats">
          <span title="Me gusta">&#9829; {{ post().likesCount }}</span>
          <span title="Visitas">&#128065; {{ post().viewsCount }}</span>
        </div>
      </footer>
    </article>
  `,
  styles: `
    .card {
      background: var(--surface-2);
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 1rem 1.15rem;
      display: grid;
      gap: 0.75rem;
      transition: border-color 0.15s ease, transform 0.15s ease;
    }
    .card:hover { border-color: var(--border-strong); }

    .card__head { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; }
    .card__author { display: flex; align-items: center; gap: 0.55rem; text-decoration: none; color: inherit; }
    .card__author-name { font-weight: 600; font-size: 0.92rem; }
    .card__author:hover .card__author-name { color: var(--accent); }
    .card__time { color: var(--text-dim); font-size: 0.8rem; white-space: nowrap; }

    .card__body { text-decoration: none; color: inherit; display: grid; gap: 0.4rem; }
    .card__title { margin: 0; font-size: 1.12rem; line-height: 1.3; }
    .card__body:hover .card__title { color: var(--accent); }
    .card__excerpt {
      margin: 0;
      color: var(--text-dim);
      font-size: 0.92rem;
      line-height: 1.55;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card__foot { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; flex-wrap: wrap; }
    .tags { display: flex; gap: 0.35rem; flex-wrap: wrap; }
    .stats { display: flex; gap: 0.75rem; color: var(--text-dim); font-size: 0.85rem; white-space: nowrap; }
  `,
})
export class PostCard {
  readonly post = input.required<Post>();
}
