import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Comment } from '../../core/models/comment.model';
import { Post } from '../../core/models/post.model';
import { AuthService } from '../../core/services/auth.service';
import { CommentService } from '../../core/services/comment.service';
import { PostService } from '../../core/services/post.service';
import { SeoService } from '../../core/services/seo.service';
import { errorMessage } from '../../shared/api-error';
import { Avatar } from '../../shared/avatar';
import { RelativeTimePipe } from '../../shared/relative-time.pipe';
import { ReportButton } from '../../shared/report-button';
import { Spinner } from '../../shared/spinner';

@Component({
  selector: 'app-post-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ReactiveFormsModule, Avatar, RelativeTimePipe, Spinner, ReportButton],
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
            class="button button--ghost like"
            [class.like--on]="current.likedByMe"
            (click)="toggleLike()"
            [disabled]="liking() || !isLoggedIn()"
            [title]="isLoggedIn() ? (current.likedByMe ? 'Quitar me gusta' : 'Me gusta') : 'Entra para dar me gusta'"
          >
            &#9829; {{ current.likesCount }}
          </button>
          <span class="views">&#128065; {{ current.viewsCount }} visitas</span>

          @if (current.own) {
            <a class="button button--ghost" [routerLink]="['/post', current.id, 'editar']">Editar</a>
            <button class="button button--danger" (click)="remove()">Borrar</button>
          } @else if (isLoggedIn()) {
            <app-report-button targetType="POST" [targetId]="current.id" />
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

            @if (editingId() === comment.id) {
              <form class="comment__edit" [formGroup]="editForm" (ngSubmit)="saveComment(comment.id)" novalidate>
                <textarea formControlName="content" rows="3" maxlength="1000"></textarea>
                <div class="comment__edit-actions">
                  <button type="submit" class="button button--small" [disabled]="editForm.invalid">Guardar</button>
                  <button type="button" class="button button--ghost button--small" (click)="cancelEdit()">
                    Cancelar
                  </button>
                </div>
              </form>
            } @else {
              <p>{{ comment.content }}</p>
              <div class="comment__actions">
                @if (comment.authorUsername === username()) {
                  <button type="button" class="link-button" (click)="startEdit(comment)">Editar</button>
                  <button type="button" class="link-button link-button--danger" (click)="removeComment(comment.id)">
                    Borrar
                  </button>
                } @else if (isLoggedIn()) {
                  <app-report-button targetType="COMMENT" [targetId]="comment.id" compact />
                }
              </div>
            }
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
    .comment__actions { grid-column: 1 / -1; display: flex; gap: 0.75rem; margin-top: 0.35rem; }
    .comment__edit { grid-column: 1 / -1; display: grid; gap: 0.5rem; margin-top: 0.4rem; }
    .comment__edit-actions { display: flex; gap: 0.5rem; }

    .link-button {
      background: none;
      border: none;
      padding: 0;
      font: inherit;
      font-size: 0.82rem;
      color: var(--text-dim);
      cursor: pointer;
      text-decoration: underline;
    }
    .link-button:hover { color: var(--text); }
    .link-button--danger:hover { color: var(--danger); }

    .like--on { color: var(--accent); border-color: var(--accent); }
  `,
})
export class PostDetailPage {
  private readonly postService = inject(PostService);
  private readonly commentService = inject(CommentService);
  private readonly auth = inject(AuthService);
  private readonly seo = inject(SeoService);
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
  protected readonly username = this.auth.username;

  /** Identificador del comentario que se está editando, o null. */
  protected readonly editingId = signal<number | null>(null);

  private readonly formBuilder = inject(FormBuilder);

  protected readonly commentForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(1000)]],
  });

  protected readonly editForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(1000)]],
  });

  ngOnInit(): void {
    const postId = Number(this.id());

    this.postService.byId(postId).subscribe({
      next: (post) => {
        this.post.set(post);
        this.loading.set(false);
        this.seo.apply({
          title: post.title,
          description: post.content,
          path: `/post/${post.id}`,
          image: post.imageUrls[0] ?? null,
          type: 'article',
          publishedAt: post.createdAt,
          author: post.authorUsername,
        });
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
        this.seo.noIndex();
      },
    });

    this.commentService.byPost(postId).subscribe({
      next: (page) => this.comments.set(page.content),
      error: () => this.comments.set([]),
    });
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

  protected startEdit(comment: Comment): void {
    this.editingId.set(comment.id);
    this.editForm.setValue({ content: comment.content });
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
  }

  protected saveComment(id: number): void {
    if (this.editForm.invalid) {
      return;
    }

    this.commentService.update(id, this.editForm.getRawValue().content).subscribe({
      next: (updated) => {
        this.comments.update((list) => list.map((item) => (item.id === id ? updated : item)));
        this.editingId.set(null);
      },
      error: (err: unknown) => this.commentError.set(errorMessage(err)),
    });
  }

  protected removeComment(id: number): void {
    if (!confirm('¿Seguro que quieres borrar este comentario?')) {
      return;
    }

    this.commentService.remove(id).subscribe({
      next: () => this.comments.update((list) => list.filter((item) => item.id !== id)),
      error: (err: unknown) => this.commentError.set(errorMessage(err)),
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
