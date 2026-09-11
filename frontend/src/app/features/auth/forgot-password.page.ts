import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { errorMessage } from '../../shared/api-error';

@Component({
  selector: 'app-forgot-password-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth">
      <h1>Recuperar la cuenta</h1>

      @if (sent()) {
        <p class="notice">
          Si esa dirección está registrada, te hemos enviado un enlace para elegir una
          contraseña nueva. Caduca en una hora.
        </p>
        <p class="switch"><a routerLink="/entrar">Volver a entrar</a></p>
      } @else {
        <p class="muted">
          Escribe tu dirección de correo y te enviaremos un enlace para volver a entrar.
        </p>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label>
            <span>Email</span>
            <input type="email" formControlName="email" autocomplete="email" />
          </label>

          @if (error()) {
            <p class="error" role="alert">{{ error() }}</p>
          }

          <button type="submit" [disabled]="form.invalid || sending()">
            {{ sending() ? 'Enviando…' : 'Enviar enlace' }}
          </button>
        </form>

        <p class="switch"><a routerLink="/entrar">Me acabo de acordar, quiero entrar</a></p>
      }
    </section>
  `,
  styles: `
    .notice {
      background: var(--surface-3);
      border: 1px solid var(--border-strong);
      border-radius: 10px;
      padding: 0.8rem 1rem;
      font-size: 0.92rem;
      margin: 0;
      line-height: 1.55;
    }
  `,
})
export class ForgotPasswordPage {
  private readonly auth = inject(AuthService);
  private readonly seo = inject(SeoService);

  protected readonly sending = signal(false);
  protected readonly sent = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    this.seo.noIndex();
  }

  protected submit(): void {
    if (this.form.invalid || this.sending()) {
      return;
    }

    this.sending.set(true);
    this.error.set(null);

    this.auth.requestPasswordReset(this.form.getRawValue().email).subscribe({
      // La API responde igual exista o no la cuenta, así que el mensaje también.
      next: () => this.sent.set(true),
      error: (err: unknown) => {
        this.sending.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
