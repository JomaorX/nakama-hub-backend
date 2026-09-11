import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../shared/api-error';

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth">
      <h1>Entrar</h1>

      @if (sessionExpired()) {
        <p class="notice">Tu sesión ha caducado. Vuelve a entrar para continuar.</p>
      }
      @if (passwordReset()) {
        <p class="notice">Contraseña cambiada. Entra con la nueva.</p>
      }

      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <label>
          <span>Usuario o email</span>
          <input formControlName="identifier" autocomplete="username" autocapitalize="none" />
        </label>

        <label>
          <span>Contraseña</span>
          <input type="password" formControlName="password" autocomplete="current-password" />
        </label>

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <button type="submit" [disabled]="submitting() || form.invalid">
          {{ submitting() ? 'Entrando…' : 'Entrar' }}
        </button>
      </form>

      <p class="switch"><a routerLink="/recuperar">He olvidado mi contraseña</a></p>
      <p class="switch">¿Todavía no tienes cuenta? <a routerLink="/registro">Créala aquí</a></p>
    </section>
  `,
  styles: `
    .notice {
      background: var(--surface-3);
      border: 1px solid var(--border-strong);
      border-radius: 10px;
      padding: 0.7rem 0.9rem;
      font-size: 0.9rem;
      margin: 0 0 1rem;
    }
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sessionExpired = signal(
    this.route.snapshot.queryParamMap.get('expirada') === 'true',
  );
  protected readonly passwordReset = signal(
    this.route.snapshot.queryParamMap.get('restablecida') === 'true',
  );

  protected readonly form = inject(FormBuilder).nonNullable.group({
    identifier: ['', Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.error.set(null);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const target = this.route.snapshot.queryParamMap.get('volverA') ?? '/muro';
        void this.router.navigateByUrl(target);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
