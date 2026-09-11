import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PostListPage } from './post-list.page';

@Component({
  selector: 'app-explore-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PostListPage],
  template: `
    <app-post-list-page
      source="explore"
      heading="Explorar"
      subtitle="Lo último que publica la comunidad"
      emptyMessage="Todavía no hay nada publicado. Sé el primero."
    />
  `,
})
export class ExplorePage {}
