import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PostListPage } from './post-list.page';

@Component({
  selector: 'app-feed-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PostListPage],
  template: `
    <app-post-list-page
      source="feed"
      heading="Tu muro"
      subtitle="Lo que publican las cuentas que sigues, y lo tuyo"
      emptyMessage="Tu muro está vacío. Sigue a alguien o publica algo para empezar."
    />
  `,
})
export class FeedPage {}
