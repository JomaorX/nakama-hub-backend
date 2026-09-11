import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { errorMessage } from '../../shared/api-error';
import { Spinner } from '../../shared/spinner';

@Component({
  selector: 'app-verify-email-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Spinner],
  template: `
    <section class="auth">
      <h1>Confirmar la cuenta</h1>

      @if (checking()) {
        <app-spinner label="Confirmando" />
      } @else if (verified()) {
        <p class="notice">Tu dirección queda confirmada. Ya puedes usar la cuenta con normalidad.</p>
        <p class="switch"><a routerLink="/">Ir a la comunidad</a></p>
      } @else {
        <p class="error" role="alert">{{ error() }}</p>
        <p class="switch">
          Puedes pedir un enlace nuevo desde <a routerLink="/ajustes">tus ajustes</a>.
        </p>
      }
    </section>
  `,
  styles: `
    .notice {
      background: var(--surface-3);
      border: 1px solid var(--border-strong);
      border-radius: 10px;
      padding: 0.8rem 1rem;
      margin: 0;
      line-height: 1.55;
    }
  `,
})
export class VerifyEmailPage {
  private readonly auth = inject(AuthService);
  private readonly seo = inject(SeoService);

  readonly token = input<string | undefined>('');

  protected readonly checking = signal(true);
  protected readonly verified = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.seo.noIndex();

    const token = this.token();
    if (!token) {
      this.checking.set(false);
      this.error.set('Este enlace no es válido.');
      return;
    }

    this.auth.verifyEmail(token).subscribe({
      next: () => {
        this.checking.set(false);
        this.verified.set(true);
      },
      error: (err: unknown) => {
        this.checking.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
