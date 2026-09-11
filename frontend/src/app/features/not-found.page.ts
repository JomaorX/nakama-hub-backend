import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <section class="empty">
      <h1>Aquí no hay nada</h1>
      <p>La página que buscas no existe o ya no está disponible.</p>
      <a class="button" routerLink="/">Volver a explorar</a>
    </section>
  `,
})
export class NotFoundPage {}
