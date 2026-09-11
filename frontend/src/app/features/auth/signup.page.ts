import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage, fieldErrors } from '../../shared/api-error';

@Component({
  selector: 'app-signup-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth">
      <h1>Crear cuenta</h1>

      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <label>
          <span>Nombre de usuario</span>
          <input formControlName="username" autocomplete="username" autocapitalize="none" />
          <small>Entre 4 y 22 caracteres. Letras, números y guiones bajos.</small>
          @if (errors()['username']) {
            <small class="error">{{ errors()['username'] }}</small>
          }
        </label>

        <label>
          <span>Email</span>
          <input type="email" formControlName="email" autocomplete="email" />
          @if (errors()['email']) {
            <small class="error">{{ errors()['email'] }}</small>
          }
        </label>

        <label>
          <span>Contraseña</span>
          <input type="password" formControlName="password" autocomplete="new-password" />
          <small>Entre 6 y 20 caracteres, con mayúsculas y minúsculas.</small>
          @if (errors()['password']) {
            <small class="error">{{ errors()['password'] }}</small>
          }
        </label>

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <button type="submit" [disabled]="submitting() || form.invalid">
          {{ submitting() ? 'Creando…' : 'Crear cuenta' }}
        </button>
      </form>

      <p class="switch">¿Ya tienes cuenta? <a routerLink="/entrar">Entra aquí</a></p>
    </section>
  `,
})
export class SignupPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errors = signal<Record<string, string>>({});

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(22)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(20)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.errors.set({});

    const credentials = this.form.getRawValue();

    this.auth.signup(credentials).subscribe({
      // El registro no devuelve sesión, así que se entra a continuación para que
      // el usuario no tenga que escribir sus datos dos veces seguidas.
      next: () =>
        this.auth
          .login({ identifier: credentials.username, password: credentials.password })
          .subscribe({
            next: () => void this.router.navigateByUrl('/muro'),
            error: () => void this.router.navigateByUrl('/entrar'),
          }),
      error: (err: unknown) => {
        this.submitting.set(false);
        this.errors.set(fieldErrors(err));
        this.error.set(Object.keys(fieldErrors(err)).length ? null : errorMessage(err));
      },
    });
  }
}
