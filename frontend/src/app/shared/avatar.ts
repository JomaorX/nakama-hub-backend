import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Avatar del usuario, con inicial de respaldo mientras no haya subida de imágenes. */
@Component({
  selector: 'app-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (url()) {
      <img class="avatar" [src]="url()" [alt]="username()" [style.width.px]="size()" [style.height.px]="size()" />
    } @else {
      <span
        class="avatar avatar--initial"
        [style.width.px]="size()"
        [style.height.px]="size()"
        [style.font-size.px]="size() * 0.42"
        [attr.aria-label]="username()"
      >{{ initial() }}</span>
    }
  `,
  styles: `
    .avatar {
      border-radius: 50%;
      object-fit: cover;
      flex-shrink: 0;
      display: inline-grid;
      place-items: center;
      background: var(--surface-3);
      border: 1px solid var(--border);
    }
    .avatar--initial {
      font-weight: 700;
      color: var(--accent);
      text-transform: uppercase;
      user-select: none;
    }
  `,
})
export class Avatar {
  readonly username = input.required<string>();
  readonly url = input<string | null>(null);
  readonly size = input(40);

  protected readonly initial = computed(() => this.username().charAt(0));
}
