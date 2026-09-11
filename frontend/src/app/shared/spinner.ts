import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-spinner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="spinner" role="status" [attr.aria-label]="label()"></div>`,
  styles: `
    .spinner {
      width: 26px;
      height: 26px;
      margin: 2rem auto;
      border: 3px solid var(--border);
      border-top-color: var(--accent);
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    @media (prefers-reduced-motion: reduce) {
      .spinner { animation-duration: 2s; }
    }
  `,
})
export class Spinner {
  readonly label = input('Cargando');
}
