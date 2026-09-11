import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ContentType, PostPayload, PostStatus, PrivacyLevel } from '../../core/models/post.model';
import { PostService } from '../../core/services/post.service';
import { SearchService } from '../../core/services/search.service';
import { errorMessage, fieldErrors } from '../../shared/api-error';

/** Categorías que precarga el backend en CategoryInitializer. */
const CATEGORIES = [
  'Acción', 'Comedia', 'Seinen', 'Shonen', 'Romance', 'Ecchi', 'Aventura',
  'Debate', 'Opinión', 'Cosplay', 'Noticias', 'Eventos', 'Curiosidades',
  'Recomendaciones', 'Crítica', 'General',
];

@Component({
  selector: 'app-post-editor-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  template: `
    <section class="stack narrow">
      <header class="page-head">
        <h1>Nueva publicación</h1>
        <p class="page-head__sub">Comparte una teoría, una reseña o lo que se te ocurra</p>
      </header>

      <form class="stack" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <label>
          <span>Título</span>
          <input formControlName="title" maxlength="150" />
          @if (errors()['title']) {
            <small class="error">{{ errors()['title'] }}</small>
          }
        </label>

        <label>
          <span>Contenido</span>
          <textarea formControlName="content" rows="12" maxlength="20000"></textarea>
          @if (errors()['content']) {
            <small class="error">{{ errors()['content'] }}</small>
          }
        </label>

        <label>
          <span>Serie</span>
          <select formControlName="serieName" (change)="syncContentType()">
            <option value="">Sin serie, tema general</option>
            @for (serie of series(); track serie) {
              <option [value]="serie">{{ serie }}</option>
            }
          </select>
          <small>Si eliges serie, el tipo de contenido no puede ser general.</small>
        </label>

        <label>
          <span>Tipo de contenido</span>
          <select formControlName="contentType">
            @for (type of contentTypes(); track type) {
              <option [value]="type">{{ type }}</option>
            }
          </select>
        </label>

        <fieldset>
          <legend>Categorías</legend>
          <div class="chips">
            @for (category of categories; track category) {
              <button
                type="button"
                class="chip"
                [class.chip--on]="selected().includes(category)"
                (click)="toggleCategory(category)"
              >{{ category }}</button>
            }
          </div>
          <small>Elige entre una y cinco.</small>
          @if (errors()['categories']) {
            <small class="error">{{ errors()['categories'] }}</small>
          }
        </fieldset>

        <div class="row">
          <label>
            <span>Estado</span>
            <select formControlName="status">
              <option value="PUBLISHED">Publicar ya</option>
              <option value="DRAFT">Guardar como borrador</option>
            </select>
          </label>

          <label>
            <span>Quién puede verlo</span>
            <select formControlName="privacy">
              <option value="PUBLIC">Todo el mundo</option>
              <option value="FOLLOWERS_ONLY">Solo mis seguidores</option>
              <option value="PRIVATE">Solo yo</option>
            </select>
          </label>
        </div>

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <button type="submit" [disabled]="submitting() || form.invalid || !selected().length">
          {{ submitting() ? 'Guardando…' : 'Publicar' }}
        </button>
      </form>
    </section>
  `,
  styles: `
    .narrow { max-width: 46rem; }
    fieldset { border: 1px solid var(--border); border-radius: 12px; padding: 0.9rem 1rem; margin: 0; }
    legend { padding: 0 0.4rem; font-size: 0.85rem; color: var(--text-dim); }
    .chips { display: flex; flex-wrap: wrap; gap: 0.4rem; }
    .chip {
      background: var(--surface-3);
      border: 1px solid var(--border);
      color: var(--text-dim);
      border-radius: 999px;
      padding: 0.3rem 0.75rem;
      font: inherit;
      font-size: 0.85rem;
      cursor: pointer;
    }
    .chip--on { background: var(--accent); border-color: var(--accent); color: var(--accent-contrast); }
    .row { display: grid; gap: 1rem; }
    @media (min-width: 560px) { .row { grid-template-columns: 1fr 1fr; } }
  `,
})
export class PostEditorPage {
  private readonly postService = inject(PostService);
  private readonly searchService = inject(SearchService);
  private readonly router = inject(Router);

  protected readonly categories = CATEGORIES;
  protected readonly series = signal<string[]>([]);
  protected readonly selected = signal<string[]>([]);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errors = signal<Record<string, string>>({});

  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(150)]],
    content: ['', [Validators.required, Validators.minLength(10)]],
    serieName: [''],
    contentType: ['GENERAL' as ContentType, Validators.required],
    status: ['PUBLISHED' as PostStatus],
    privacy: ['PUBLIC' as PrivacyLevel],
  });

  ngOnInit(): void {
    this.searchService.series('', 0, 50).subscribe({
      next: (page) => this.series.set(page.content.map((serie) => serie.name)),
      error: () => this.series.set([]),
    });
  }

  /** El backend exige tipo GENERAL sin serie, y distinto de GENERAL con ella. */
  protected contentTypes(): ContentType[] {
    return this.form.getRawValue().serieName ? ['ANIME', 'MANGA', 'SERIE'] : ['GENERAL'];
  }

  protected syncContentType(): void {
    const allowed = this.contentTypes();
    if (!allowed.includes(this.form.getRawValue().contentType)) {
      this.form.controls.contentType.setValue(allowed[0]);
    }
  }

  protected toggleCategory(category: string): void {
    this.selected.update((current) =>
      current.includes(category)
        ? current.filter((item) => item !== category)
        : current.length >= 5
          ? current
          : [...current, category],
    );
  }

  protected submit(): void {
    if (this.form.invalid || !this.selected().length || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.errors.set({});

    const value = this.form.getRawValue();
    const payload: PostPayload = {
      title: value.title,
      content: value.content,
      contentType: value.contentType,
      status: value.status,
      privacy: value.privacy,
      serieName: value.serieName || null,
      categories: this.selected(),
    };

    this.postService.create(payload).subscribe({
      next: (post) => void this.router.navigate(['/post', post.id]),
      error: (err: unknown) => {
        this.submitting.set(false);
        this.errors.set(fieldErrors(err));
        this.error.set(Object.keys(fieldErrors(err)).length ? null : errorMessage(err));
      },
    });
  }
}
