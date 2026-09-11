import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Comment } from '../../core/models/comment.model';
import { Post } from '../../core/models/post.model';
import { AuthService } from '../../core/services/auth.service';
import { CommentService } from '../../core/services/comment.service';
import { PostService } from '../../core/services/post.service';
import { errorMessage } from '../../shared/api-error';
import { Avatar } from '../../shared/avatar';
import { RelativeTimePipe } from '../../shared/relative-time.pipe';
import { Spinner } from '../../shared/spinner';

@Component({
  selector: 'app-post-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ReactiveFormsModule, Avatar, RelativeTimePipe, Spinner],
  template: `
    @if (loading()) {
      <app-spinner />
    } @else if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    } @else if (post(); as current) {
      <article class="stack">
        <header class="post-head">
          <h1>{{ current.title }}</h1>
          <div class="post-meta">
            <a class="post-meta__author" [routerLink]="['/u', current.authorUsername]">
              <app-avatar [username]="current.authorUsername" [size]="32" />
              <span>{{ current.authorUsername }}</span>
            </a>
            <time [attr.datetime]="current.createdAt">{{ current.createdAt | relativeTime }}</time>
            @if (current.edited) {
              <span class="badge">editado</span>
            }
          </div>
          <div class="tags">
            @if (current.serieName) {
              <span class="tag tag--serie">{{ current.serieName }}</span>
            }
            @for (category of current.categories; track category) {
              <span class="tag">{{ category }}</span>
            }
          </div>
        </header>

        <div class="prose">{{ current.content }}</div>

        @for (image of current.imageUrls; track image) {
          <img class="post-image" [src]="image" alt="" loading="lazy" />
        }

        <footer class="post-actions">
          <button
            class="button button--ghost"
            (click)="toggleLike()"
            [disabled]="liking() || !isLoggedIn()"
            [title]="isLoggedIn() ? 'Me gusta' : 'Entra para dar me gusta'"
          >
            &#9829; {{ current.likesCount }}
          </button>
          <span class="views">&#128065; {{ current.viewsCount }} visitas</span>

          @if (isAuthor()) {
            <button class="button button--danger" (click)="remove()">Borrar</button>
          }
        </footer>
      </article>

      <section class="stack comments">
        <h2>Comentarios ({{ comments().length }})</h2>

        @if (isLoggedIn()) {
          <form class="comment-form" [formGroup]="commentForm" (ngSubmit)="publishComment()" novalidate>
            <textarea
              formControlName="content"
              rows="3"
              placeholder="Escribe un comentario…"
              maxlength="1000"
            ></textarea>
            @if (commentError()) {
              <p class="error" role="alert">{{ commentError() }}</p>
            }
            <button type="submit" [disabled]="commentForm.invalid || posting()">
              {{ posting() ? 'Publicando…' : 'Comentar' }}
            </button>
          </form>
        } @else {
          <p class="muted"><a routerLink="/entrar">Entra</a> para participar en la conversación.</p>
        }

        @for (comment of comments(); track comment.id) {
          <article class="comment">
            <a class="comment__author" [routerLink]="['/u', comment.authorUsername]">
              <app-avatar [username]="comment.authorUsername" [size]="28" />
              <span>{{ comment.authorUsername }}</span>
            </a>
            <time>
              {{ comment.createdAt | relativeTime }}@if (comment.edited) {<span class="badge">editado</span>}
            </time>
            <p>{{ comment.content }}</p>
          </article>
        } @empty {
          <p class="muted">Todavía no hay comentarios.</p>
        }
      </section>
    }
  `,
  styles: `
    .post-head { display: grid; gap: 0.65rem; }
    .post-head h1 { margin: 0; font-size: 1.75rem; line-height: 1.25; }
    .post-meta { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; color: var(--text-dim); font-size: 0.88rem; }
    .post-meta__author { display: flex; align-items: center; gap: 0.5rem; color: inherit; text-decoration: none; }
    .post-meta__author:hover span { color: var(--accent); }
    .badge { background: var(--surface-3); border-radius: 999px; padding: 0.1rem 0.5rem; font-size: 0.75rem; }

    .prose { white-space: pre-wrap; line-height: 1.7; }
    .post-image { width: 100%; border-radius: 12px; border: 1px solid var(--border); }

    .post-actions { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; padding-top: 0.5rem; border-top: 1px solid var(--border); }
    .views { color: var(--text-dim); font-size: 0.85rem; }

    .comments { margin-top: 2rem; }
    .comments h2 { font-size: 1.15rem; margin: 0; }
    .comment-form { display: grid; gap: 0.6rem; }
    .comment-form button { justify-self: start; }

    .comment {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 0.25rem 0.75rem;
      padding: 0.85rem 0;
      border-top: 1px solid var(--border);
    }
    .comment__author { display: flex; align-items: center; gap: 0.5rem; font-weight: 600; font-size: 0.9rem; color: inherit; text-decoration: none; }
    .comment time { color: var(--text-dim); font-size: 0.8rem; align-self: center; }
    .comment p { grid-column: 1 / -1; margin: 0.25rem 0 0; line-height: 1.6; white-space: pre-wrap; }
  `,
})
export class PostDetailPage {
  private readonly postService = inject(PostService);
  private readonly commentService = inject(CommentService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Llega de la ruta gracias a withComponentInputBinding. */
  readonly id = input.required<string>();

  protected readonly post = signal<Post | null>(null);
  protected readonly comments = signal<Comment[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly commentError = signal<string | null>(null);
  protected readonly posting = signal(false);
  protected readonly liking = signal(false);

  protected readonly isLoggedIn = this.auth.isLoggedIn;

  protected readonly commentForm = inject(FormBuilder).nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(1000)]],
  });

  ngOnInit(): void {
    const postId = Number(this.id());

    this.postService.byId(postId).subscribe({
      next: (post) => {
        this.post.set(post);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
      },
    });

    this.commentService.byPost(postId).subscribe({
      next: (page) => this.comments.set(page.content),
      error: () => this.comments.set([]),
    });
  }

  protected isAuthor(): boolean {
    return this.post()?.authorUsername === this.auth.username();
  }

  protected toggleLike(): void {
    const current = this.post();
    if (!current || this.liking()) {
      return;
    }

    this.liking.set(true);
    this.postService.toggleLike(current.id).subscribe({
      next: (updated) => {
        this.post.set(updated);
        this.liking.set(false);
      },
      error: () => this.liking.set(false),
    });
  }

  protected publishComment(): void {
    const current = this.post();
    if (!current || this.commentForm.invalid || this.posting()) {
      return;
    }

    this.posting.set(true);
    this.commentError.set(null);

    this.commentService
      .create({ content: this.commentForm.getRawValue().content, postId: current.id })
      .subscribe({
        next: (comment) => {
          this.comments.update((list) => [...list, comment]);
          this.commentForm.reset();
          this.posting.set(false);
        },
        error: (err: unknown) => {
          this.posting.set(false);
          this.commentError.set(errorMessage(err));
        },
      });
  }

  protected remove(): void {
    const current = this.post();
    if (!current || !confirm('¿Seguro que quieres borrar este post?')) {
      return;
    }

    this.postService.remove(current.id).subscribe({
      next: () => void this.router.navigateByUrl('/'),
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }
}
