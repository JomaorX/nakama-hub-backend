import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { errorMessage } from '../../shared/api-error';

@Component({
  selector: 'app-reset-password-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth">
      <h1>Elegir contraseña nueva</h1>

      @if (!token()) {
        <p class="error" role="alert">
          Este enlace no es válido. Pide uno nuevo desde
          <a routerLink="/recuperar">recuperar la cuenta</a>.
        </p>
      } @else {
        <p class="muted">Al cambiarla se cerrarán todas tus sesiones abiertas.</p>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label>
            <span>Contraseña nueva</span>
            <input type="password" formControlName="newPassword" autocomplete="new-password" />
            <small>Entre 6 y 20 caracteres, con mayúsculas y minúsculas.</small>
          </label>

          @if (error()) {
            <p class="error" role="alert">{{ error() }}</p>
          }

          <button type="submit" [disabled]="form.invalid || sending()">
            {{ sending() ? 'Guardando…' : 'Guardar y entrar' }}
          </button>
        </form>
      }
    </section>
  `,
})
export class ResetPasswordPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly seo = inject(SeoService);

  /** Llega del parámetro ?token= del enlace del correo. */
  readonly token = input<string | undefined>('');

  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(20)]],
  });

  ngOnInit(): void {
    this.seo.noIndex();
  }

  protected submit(): void {
    const token = this.token();
    if (!token || this.form.invalid || this.sending()) {
      return;
    }

    this.sending.set(true);
    this.error.set(null);

    this.auth.resetPassword(token, this.form.getRawValue().newPassword).subscribe({
      next: () => void this.router.navigate(['/entrar'], { queryParams: { restablecida: true } }),
      error: (err: unknown) => {
        this.sending.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
