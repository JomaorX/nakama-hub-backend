import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserProfile } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';
import { SeoService } from '../../core/services/seo.service';
import { UserService } from '../../core/services/user.service';
import { errorMessage } from '../../shared/api-error';
import { Spinner } from '../../shared/spinner';

@Component({
  selector: 'app-settings-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Spinner],
  template: `
    <section class="stack narrow">
      <header class="page-head">
        <h1>Ajustes de la cuenta</h1>
        <p class="page-head__sub">Tu perfil, tu contraseña y tus datos</p>
      </header>

      @if (loading()) {
        <app-spinner />
      } @else if (profile(); as me) {
        <section class="panel stack">
          <h2>Perfil</h2>
          <form class="stack" [formGroup]="profileForm" (ngSubmit)="saveProfile()" novalidate>
            <label>
              <span>Biografía</span>
              <textarea formControlName="bio" rows="3" maxlength="120"></textarea>
              <small>Entre 4 y 120 caracteres.</small>
            </label>

            <label>
              <span>URL del avatar</span>
              <input formControlName="avatarUrl" placeholder="https://…" />
              <small>Por ahora solo enlaces. La subida de imágenes llegará más adelante.</small>
            </label>

            <button type="submit" class="button--small" [disabled]="savingProfile()">
              {{ savingProfile() ? 'Guardando…' : 'Guardar perfil' }}
            </button>
          </form>
          @if (profileMessage()) {
            <p class="ok" role="status">{{ profileMessage() }}</p>
          }
          @if (profileError()) {
            <p class="error" role="alert">{{ profileError() }}</p>
          }
        </section>

        <section class="panel stack">
          <h2>Contraseña</h2>
          <form class="stack" [formGroup]="passwordForm" (ngSubmit)="changePassword()" novalidate>
            <label>
              <span>Contraseña actual</span>
              <input type="password" formControlName="currentPassword" autocomplete="current-password" />
            </label>
            <label>
              <span>Contraseña nueva</span>
              <input type="password" formControlName="newPassword" autocomplete="new-password" />
              <small>Entre 6 y 20 caracteres, con mayúsculas y minúsculas.</small>
            </label>

            <p class="muted">Cambiarla cierra la sesión en todos tus dispositivos.</p>

            <button type="submit" class="button--small" [disabled]="passwordForm.invalid || savingPassword()">
              {{ savingPassword() ? 'Cambiando…' : 'Cambiar contraseña' }}
            </button>
          </form>
          @if (passwordError()) {
            <p class="error" role="alert">{{ passwordError() }}</p>
          }
        </section>

        <section class="panel stack">
          <h2>Tus datos</h2>
          <p class="muted">
            Puedes descargar todo lo que guardamos sobre ti: perfil, publicaciones, comentarios,
            seguidores y me gusta.
          </p>
          <button type="button" class="button button--ghost button--small" (click)="exportData()">
            Descargar mis datos
          </button>
        </section>

        <section class="panel panel--danger stack">
          <h2>Eliminar la cuenta</h2>
          <p class="muted">
            Se borran tus datos personales y tu cuenta deja de existir. Tus publicaciones y
            comentarios permanecen sin autor identificable, para no romper las conversaciones
            de otras personas. No se puede deshacer.
          </p>
          <button type="button" class="button button--danger button--small" (click)="deleteAccount()">
            Eliminar mi cuenta
          </button>
          @if (deleteError()) {
            <p class="error" role="alert">{{ deleteError() }}</p>
          }
        </section>
      } @else if (loadError()) {
        <p class="error" role="alert">{{ loadError() }}</p>
      }
    </section>
  `,
  styles: `
    .narrow { max-width: 40rem; }
    .panel {
      background: var(--surface-2);
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 1.2rem 1.3rem;
    }
    .panel--danger { border-color: color-mix(in srgb, var(--danger) 40%, var(--border)); }
    .panel h2 { margin: 0; font-size: 1.05rem; }
    .panel button[type='submit'], .panel .button { justify-self: start; }
    .ok { color: var(--text-dim); font-size: 0.88rem; margin: 0; }
  `,
})
export class SettingsPage {
  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly seo = inject(SeoService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly profile = signal<UserProfile | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  protected readonly savingProfile = signal(false);
  protected readonly profileError = signal<string | null>(null);
  protected readonly profileMessage = signal<string | null>(null);

  protected readonly savingPassword = signal(false);
  protected readonly passwordError = signal<string | null>(null);

  protected readonly deleteError = signal<string | null>(null);

  protected readonly profileForm = this.formBuilder.nonNullable.group({
    bio: ['', [Validators.minLength(4), Validators.maxLength(120)]],
    avatarUrl: [''],
  });

  protected readonly passwordForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(20)]],
  });

  ngOnInit(): void {
    this.seo.noIndex();

    this.userService.me().subscribe({
      next: (me) => {
        this.profile.set(me);
        this.profileForm.setValue({ bio: me.bio ?? '', avatarUrl: me.avatarUrl ?? '' });
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.loadError.set(errorMessage(err));
      },
    });
  }

  protected saveProfile(): void {
    if (this.savingProfile()) {
      return;
    }

    this.savingProfile.set(true);
    this.profileError.set(null);
    this.profileMessage.set(null);

    const { bio, avatarUrl } = this.profileForm.getRawValue();

    // Son dos endpoints distintos en la API, así que se encadenan.
    this.userService.updateBio(bio).subscribe({
      next: () => {
        if (!avatarUrl) {
          this.finishProfileSave();
          return;
        }
        this.userService.updateAvatar(avatarUrl).subscribe({
          next: () => this.finishProfileSave(),
          error: (err: unknown) => this.failProfileSave(err),
        });
      },
      error: (err: unknown) => this.failProfileSave(err),
    });
  }

  private finishProfileSave(): void {
    this.savingProfile.set(false);
    this.profileMessage.set('Perfil actualizado.');
  }

  private failProfileSave(err: unknown): void {
    this.savingProfile.set(false);
    this.profileError.set(errorMessage(err));
  }

  protected changePassword(): void {
    if (this.passwordForm.invalid || this.savingPassword()) {
      return;
    }

    this.savingPassword.set(true);
    this.passwordError.set(null);

    const { currentPassword, newPassword } = this.passwordForm.getRawValue();

    this.userService.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        // El backend revoca todas las sesiones, así que la actual ya no sirve.
        this.auth.endSession();
        void this.router.navigate(['/entrar'], { queryParams: { expirada: true } });
      },
      error: (err: unknown) => {
        this.savingPassword.set(false);
        this.passwordError.set(errorMessage(err));
      },
    });
  }

  protected exportData(): void {
    this.userService.exportMyData().subscribe({
      next: (data) => downloadJson(data, 'nakamahub-mis-datos.json'),
      error: (err: unknown) => this.profileError.set(errorMessage(err)),
    });
  }

  protected deleteAccount(): void {
    const confirmation = prompt(
      'Esta acción no se puede deshacer. Escribe tu nombre de usuario para confirmar:',
    );

    if (confirmation !== this.profile()?.username) {
      return;
    }

    this.userService.deleteAccount().subscribe({
      next: () => {
        this.auth.endSession();
        void this.router.navigateByUrl('/');
      },
      error: (err: unknown) => this.deleteError.set(errorMessage(err)),
    });
  }
}

function downloadJson(data: unknown, filename: string): void {
  const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
