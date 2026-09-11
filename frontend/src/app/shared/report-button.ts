import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { REPORT_REASONS, ReportService, ReportTargetType } from '../core/services/report.service';
import { errorMessage } from './api-error';

/**
 * Canal de denuncia de contenido.
 *
 * El Reglamento de Servicios Digitales obliga a ofrecerlo, y el backend ya lo
 * soportaba, pero sin un botón en la interfaz no existe para quien lo necesita.
 */
@Component({
  selector: 'app-report-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  template: `
    @if (sent()) {
      <span class="sent">Gracias, lo revisaremos.</span>
    } @else if (!open()) {
      <button
        type="button"
        [class]="compact() ? 'link-button' : 'button button--ghost'"
        (click)="open.set(true)"
      >Reportar</button>
    } @else {
      <form class="report" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <label>
          <span>Motivo</span>
          <select formControlName="reason">
            @for (reason of reasons; track reason.value) {
              <option [value]="reason.value">{{ reason.label }}</option>
            }
          </select>
        </label>

        <label>
          <span>Explicación (opcional)</span>
          <textarea formControlName="details" rows="2" maxlength="1000"></textarea>
        </label>

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <div class="report__actions">
          <button type="submit" class="button button--small" [disabled]="sending()">
            {{ sending() ? 'Enviando…' : 'Enviar reporte' }}
          </button>
          <button type="button" class="button button--ghost button--small" (click)="open.set(false)">
            Cancelar
          </button>
        </div>
      </form>
    }
  `,
  styles: `
    .report {
      display: grid;
      gap: 0.6rem;
      padding: 0.9rem 1rem;
      background: var(--surface-3);
      border: 1px solid var(--border-strong);
      border-radius: 12px;
      width: 100%;
      max-width: 28rem;
    }
    .report__actions { display: flex; gap: 0.5rem; }
    .sent { color: var(--text-dim); font-size: 0.85rem; }
    .link-button {
      background: none;
      border: none;
      padding: 0;
      font: inherit;
      font-size: 0.82rem;
      color: var(--text-dim);
      cursor: pointer;
      text-decoration: underline;
    }
    .link-button:hover { color: var(--danger); }
  `,
})
export class ReportButton {
  private readonly reportService = inject(ReportService);

  readonly targetType = input.required<ReportTargetType>();
  readonly targetId = input.required<number>();
  readonly compact = input(false, { transform: (value: unknown) => value !== false });

  protected readonly reasons = REPORT_REASONS;
  protected readonly open = signal(false);
  protected readonly sent = signal(false);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    reason: ['SPAM', Validators.required],
    details: [''],
  });

  protected submit(): void {
    if (this.sending()) {
      return;
    }

    this.sending.set(true);
    this.error.set(null);

    const { reason, details } = this.form.getRawValue();

    this.reportService.create(this.targetType(), this.targetId(), reason, details).subscribe({
      next: () => {
        this.sending.set(false);
        this.open.set(false);
        this.sent.set(true);
      },
      error: (err: unknown) => {
        this.sending.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
