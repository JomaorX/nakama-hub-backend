import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Post } from '../../core/models/post.model';
import { UserSearchResult } from '../../core/models/user.model';
import { SearchService } from '../../core/services/search.service';
import { errorMessage } from '../../shared/api-error';
import { Avatar } from '../../shared/avatar';
import { PostCard } from '../../shared/post-card';
import { Spinner } from '../../shared/spinner';

type Tab = 'posts' | 'usuarios';

@Component({
  selector: 'app-search-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, PostCard, Avatar, Spinner],
  template: `
    <section class="stack">
      <header class="page-head">
        <h1>Buscar</h1>
        <p class="page-head__sub">Encuentra publicaciones y gente de la comunidad</p>
      </header>

      <form class="search-bar" [formGroup]="form" (ngSubmit)="search()" novalidate>
        <input formControlName="q" placeholder="Busca por título, contenido o nombre…" autocapitalize="none" />
        <button type="submit" [disabled]="loading()">Buscar</button>
      </form>

      <nav class="tabs">
        <button [class.tabs__item--active]="tab() === 'posts'" (click)="switchTo('posts')">Publicaciones</button>
        <button [class.tabs__item--active]="tab() === 'usuarios'" (click)="switchTo('usuarios')">Usuarios</button>
      </nav>

      @if (loading()) {
        <app-spinner />
      } @else if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      } @else if (tab() === 'posts') {
        <div class="stack">
          @for (post of posts(); track post.id) {
            <app-post-card [post]="post" />
          } @empty {
            <p class="muted">Sin resultados.</p>
          }
        </div>
      } @else {
        <div class="stack">
          @for (user of users(); track user.id) {
            <a class="user-row" [routerLink]="['/u', user.username]">
              <app-avatar [username]="user.username" [url]="user.avatarUrl" [size]="42" />
              <div>
                <strong>{{ user.username }}</strong>
                @if (user.bio) {
                  <p>{{ user.bio }}</p>
                }
              </div>
              <span class="user-row__stat">{{ user.followersCount }} seguidores</span>
            </a>
          } @empty {
            <p class="muted">Sin resultados.</p>
          }
        </div>
      }
    </section>
  `,
  styles: `
    .search-bar { display: flex; gap: 0.5rem; }
    .search-bar input { flex: 1; }

    .tabs { display: flex; gap: 0.35rem; border-bottom: 1px solid var(--border); }
    .tabs button {
      background: none;
      border: none;
      border-bottom: 2px solid transparent;
      color: var(--text-dim);
      padding: 0.55rem 0.9rem;
      cursor: pointer;
      font: inherit;
      font-weight: 600;
    }
    .tabs button:hover { color: var(--text); }
    .tabs__item--active { color: var(--accent) !important; border-bottom-color: var(--accent); }

    .user-row {
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 0.85rem;
      padding: 0.85rem 1rem;
      background: var(--surface-2);
      border: 1px solid var(--border);
      border-radius: 12px;
      text-decoration: none;
      color: inherit;
    }
    .user-row:hover { border-color: var(--border-strong); }
    .user-row p { margin: 0.15rem 0 0; color: var(--text-dim); font-size: 0.88rem; }
    .user-row__stat { color: var(--text-dim); font-size: 0.82rem; white-space: nowrap; }
  `,
})
export class SearchPage {
  private readonly searchService = inject(SearchService);
  private readonly router = inject(Router);

  /**
   * Llega del parámetro ?q= de la URL gracias a withComponentInputBinding.
   *
   * Cuando el parámetro no está en la URL, el enlace de inputs del router entrega
   * undefined en lugar de respetar el valor por defecto del input, así que hay que
   * normalizarlo antes de usarlo.
   */
  readonly q = input<string | undefined>('');

  protected readonly tab = signal<Tab>('posts');
  protected readonly posts = signal<Post[]>([]);
  protected readonly users = signal<UserSearchResult[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({ q: [''] });

  ngOnInit(): void {
    this.form.controls.q.setValue(this.q() ?? '');
    this.search();
  }

  protected switchTo(tab: Tab): void {
    if (this.tab() === tab) {
      return;
    }
    this.tab.set(tab);
    this.search();
  }

  protected search(): void {
    const term = (this.form.getRawValue().q ?? '').trim();

    this.loading.set(true);
    this.error.set(null);

    // La URL refleja la búsqueda para que el resultado se pueda compartir.
    void this.router.navigate([], { queryParams: term ? { q: term } : {}, replaceUrl: true });

    if (this.tab() === 'usuarios') {
      this.searchService.users(term).subscribe({
        next: (page) => {
          this.users.set(page.content);
          this.loading.set(false);
        },
        error: (err: unknown) => this.fail(err),
      });
      return;
    }

    this.searchService.posts({ q: term }).subscribe({
      next: (page) => {
        this.posts.set(page.content);
        this.loading.set(false);
      },
      error: (err: unknown) => this.fail(err),
    });
  }

  private fail(err: unknown): void {
    this.loading.set(false);
    this.error.set(errorMessage(err));
  }
}
