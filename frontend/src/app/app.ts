import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { Avatar } from './shared/avatar';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Avatar],
  template: `
    <a class="skip-link" href="#contenido">Saltar al contenido</a>

    <header class="topbar">
      <div class="topbar__inner">
        <a class="brand" routerLink="/">
          <span class="brand__mark">NH</span>
          <span class="brand__name">Nakama Hub</span>
        </a>

        <nav class="nav" aria-label="Principal">
          <a routerLink="/" routerLinkActive="nav__link--active" [routerLinkActiveOptions]="{ exact: true }">
            Explorar
          </a>
          @if (isLoggedIn()) {
            <a routerLink="/muro" routerLinkActive="nav__link--active">Mi muro</a>
          }
          <a routerLink="/buscar" routerLinkActive="nav__link--active">Buscar</a>
        </nav>

        <div class="topbar__actions">
          @if (isLoggedIn()) {
            <a class="button button--small" routerLink="/publicar">Publicar</a>
            <button
              class="account"
              (click)="menuOpen.set(!menuOpen())"
              [attr.aria-expanded]="menuOpen()"
              aria-haspopup="menu"
            >
              <app-avatar [username]="username()!" [size]="32" />
            </button>

            @if (menuOpen()) {
              <div class="menu" role="menu">
                <a role="menuitem" [routerLink]="['/u', username()]" (click)="menuOpen.set(false)">Mi perfil</a>
                <button role="menuitem" (click)="logout()">Cerrar sesión</button>
              </div>
            }
          } @else {
            <a class="button button--ghost button--small" routerLink="/entrar">Entrar</a>
            <a class="button button--small" routerLink="/registro">Crear cuenta</a>
          }
        </div>
      </div>
    </header>

    <main id="contenido" class="container">
      <router-outlet />
    </main>

    <footer class="footer">
      <p>Nakama Hub · comunidad de anime, manga y series</p>
    </footer>
  `,
  styles: `
    .skip-link {
      position: absolute;
      left: -9999px;
      top: 0;
      background: var(--accent);
      color: var(--accent-contrast);
      padding: 0.6rem 1rem;
      z-index: 100;
    }
    .skip-link:focus { left: 0; }

    .topbar {
      position: sticky;
      top: 0;
      z-index: 20;
      background: color-mix(in srgb, var(--surface-1) 88%, transparent);
      backdrop-filter: blur(10px);
      border-bottom: 1px solid var(--border);
    }
    .topbar__inner {
      max-width: 60rem;
      margin: 0 auto;
      padding: 0.65rem 1rem;
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .topbar__actions { margin-left: auto; display: flex; align-items: center; gap: 0.5rem; position: relative; }

    .brand { display: flex; align-items: center; gap: 0.5rem; text-decoration: none; color: inherit; }
    .brand__mark {
      display: grid;
      place-items: center;
      width: 32px;
      height: 32px;
      border-radius: 9px;
      background: var(--accent);
      color: var(--accent-contrast);
      font-weight: 800;
      font-size: 0.8rem;
    }
    .brand__name { font-weight: 700; letter-spacing: -0.01em; }

    .nav { display: flex; gap: 0.25rem; }
    .nav a {
      color: var(--text-dim);
      text-decoration: none;
      font-size: 0.92rem;
      font-weight: 600;
      padding: 0.4rem 0.65rem;
      border-radius: 8px;
    }
    .nav a:hover { color: var(--text); background: var(--surface-3); }
    .nav__link--active { color: var(--accent) !important; }

    .account { background: none; border: none; padding: 0; cursor: pointer; display: flex; }

    .menu {
      position: absolute;
      top: calc(100% + 0.5rem);
      right: 0;
      min-width: 11rem;
      background: var(--surface-2);
      border: 1px solid var(--border-strong);
      border-radius: 10px;
      padding: 0.3rem;
      display: grid;
      box-shadow: 0 12px 32px rgb(0 0 0 / 0.25);
    }
    .menu a, .menu button {
      background: none;
      border: none;
      text-align: left;
      font: inherit;
      color: var(--text);
      text-decoration: none;
      padding: 0.55rem 0.7rem;
      border-radius: 7px;
      cursor: pointer;
    }
    .menu a:hover, .menu button:hover { background: var(--surface-3); }

    .container { max-width: 60rem; margin: 0 auto; padding: 1.75rem 1rem 4rem; }

    .footer {
      border-top: 1px solid var(--border);
      padding: 1.5rem 1rem;
      text-align: center;
      color: var(--text-dim);
      font-size: 0.85rem;
    }
    .footer p { margin: 0; }

    @media (max-width: 560px) {
      .brand__name { display: none; }
      .nav a { padding: 0.4rem 0.45rem; font-size: 0.85rem; }
    }
  `,
})
export class App {
  private readonly auth = inject(AuthService);

  protected readonly isLoggedIn = this.auth.isLoggedIn;
  protected readonly username = this.auth.username;
  protected readonly menuOpen = signal(false);

  protected logout(): void {
    this.menuOpen.set(false);
    this.auth.logout();
  }
}
