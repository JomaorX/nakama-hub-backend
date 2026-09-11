import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Page } from '../../core/models/page.model';
import { Post } from '../../core/models/post.model';
import { PostService } from '../../core/services/post.service';
import { SeoService } from '../../core/services/seo.service';
import { errorMessage } from '../../shared/api-error';
import { PostCard } from '../../shared/post-card';
import { Spinner } from '../../shared/spinner';

type Source = 'explore' | 'feed';

/**
 * Lista paginada de posts, compartida por el descubrimiento y el timeline.
 * Solo cambia de dónde vienen los datos y qué se dice cuando no hay ninguno.
 */
@Component({
  selector: 'app-post-list-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PostCard, Spinner, RouterLink],
  template: `
    <section class="stack">
      <header class="page-head">
        <h1>{{ heading() }}</h1>
        <p class="page-head__sub">{{ subtitle() }}</p>
      </header>

      @if (loading() && !posts().length) {
        <app-spinner />
      } @else if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      } @else if (!posts().length) {
        <div class="empty">
          <p>{{ emptyMessage() }}</p>
          @if (source() === 'feed') {
            <a class="button" routerLink="/buscar">Buscar gente a la que seguir</a>
          }
        </div>
      } @else {
        <div class="stack">
          @for (post of posts(); track post.id) {
            <app-post-card [post]="post" />
          }
        </div>

        @if (!lastPage()) {
          <button class="button button--ghost" (click)="loadMore()" [disabled]="loading()">
            {{ loading() ? 'Cargando…' : 'Ver más' }}
          </button>
        }
      }
    </section>
  `,
})
export class PostListPage {
  private readonly postService = inject(PostService);
  private readonly seo = inject(SeoService);

  readonly source = input.required<Source>();
  readonly heading = input.required<string>();
  readonly subtitle = input.required<string>();
  readonly emptyMessage = input.required<string>();

  protected readonly posts = signal<Post[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly lastPage = signal(true);

  private page = 0;

  ngOnInit(): void {
    if (this.source() === 'feed') {
      // El muro es personal: no tiene sentido que lo indexe nadie.
      this.seo.noIndex();
    } else {
      this.seo.apply({
        title: this.heading(),
        description: 'Teorías, reseñas y debates sobre anime, manga y series publicados por la comunidad.',
        path: '/',
      });
    }

    this.load();
  }

  protected loadMore(): void {
    this.page += 1;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.request().subscribe({
      next: (result) => {
        this.posts.update((current) => [...current, ...result.content]);
        this.lastPage.set(result.last);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  private request(): Observable<Page<Post>> {
    return this.source() === 'feed'
      ? this.postService.feed(this.page)
      : this.postService.explore(this.page);
  }
}
