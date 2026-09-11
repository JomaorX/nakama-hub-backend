import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PublicProfile } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { UserService } from '../../core/services/user.service';
import { errorMessage } from '../../shared/api-error';
import { Avatar } from '../../shared/avatar';
import { PostCard } from '../../shared/post-card';
import { ReportButton } from '../../shared/report-button';
import { Spinner } from '../../shared/spinner';

@Component({
  selector: 'app-profile-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, PostCard, Spinner, RouterLink, ReportButton],
  template: `
    @if (loading()) {
      <app-spinner />
    } @else if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    } @else if (profile(); as user) {
      <section class="stack">
        <header class="profile">
          <app-avatar [username]="user.username" [url]="user.avatarUrl" [size]="84" />
          <div class="profile__info">
            <h1>{{ user.username }}</h1>
            @if (user.bio) {
              <p class="profile__bio">{{ user.bio }}</p>
            }
            <dl class="profile__stats">
              <div><dt>Seguidores</dt><dd>{{ user.followersCount }}</dd></div>
              <div><dt>Siguiendo</dt><dd>{{ user.followingCount }}</dd></div>
              <div><dt>Posts</dt><dd>{{ user.postsCount }}</dd></div>
            </dl>
          </div>

          <div class="profile__actions">
            @if (user.own) {
              <a class="button button--ghost" routerLink="/ajustes">Ajustes</a>
              <a class="button" routerLink="/publicar">Publicar algo</a>
            } @else if (isLoggedIn()) {
              <button
                class="button"
                [class.button--ghost]="user.followedByMe"
                (click)="toggleFollow()"
                [disabled]="following()"
              >
                {{ following() ? 'Guardando…' : user.followedByMe ? 'Dejar de seguir' : 'Seguir' }}
              </button>
              <app-report-button targetType="USER" [targetId]="user.id" compact />
            }
          </div>
        </header>

        @if (user.posts === null) {
          <p class="muted">Este perfil es privado. Solo sus seguidores ven lo que publica.</p>
        } @else {
          <div class="stack">
            @for (post of user.posts; track post.id) {
              <app-post-card [post]="post" />
            } @empty {
              <p class="muted">Todavía no ha publicado nada.</p>
            }
          </div>
        }
      </section>
    }
  `,
  styles: `
    .profile {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 1rem 1.25rem;
      align-items: start;
      padding-bottom: 1.25rem;
      border-bottom: 1px solid var(--border);
    }
    .profile__info h1 { margin: 0 0 0.35rem; font-size: 1.5rem; }
    .profile__bio { margin: 0 0 0.75rem; color: var(--text-dim); line-height: 1.55; }
    .profile__stats { display: flex; gap: 1.5rem; margin: 0; }
    .profile__stats div { display: grid; gap: 0.1rem; }
    .profile__stats dt { color: var(--text-dim); font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.04em; }
    .profile__stats dd { margin: 0; font-weight: 700; font-size: 1.05rem; }
    .profile__actions {
      grid-column: 1 / -1;
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    @media (min-width: 640px) {
      .profile__actions { grid-column: auto; justify-content: flex-end; align-self: center; }
      .profile { grid-template-columns: auto 1fr auto; }
    }
  `,
})
export class ProfilePage {
  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly seo = inject(SeoService);

  readonly username = input.required<string>();

  protected readonly profile = signal<PublicProfile | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly following = signal(false);

  protected readonly isLoggedIn = this.auth.isLoggedIn;

  ngOnInit(): void {
    this.userService.profile(this.username()).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.loading.set(false);
        this.seo.apply({
          title: profile.username,
          description:
            profile.bio ??
            `Publicaciones de ${profile.username} en Nakama Hub, comunidad de anime, manga y series.`,
          path: `/u/${profile.username}`,
          image: profile.avatarUrl,
        });
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
        this.seo.noIndex();
      },
    });
  }

  protected toggleFollow(): void {
    if (this.following()) {
      return;
    }

    this.following.set(true);
    this.userService.toggleFollow(this.username()).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.following.set(false);
      },
      error: (err: unknown) => {
        this.following.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
