import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { SITE_ORIGIN } from '../api.config';

export interface PageSeo {
  title: string;
  description: string;
  /** Ruta del recurso, sin el origen. */
  path: string;
  image?: string | null;
  type?: 'website' | 'article';
  publishedAt?: string | null;
  author?: string | null;
}

const SITE_NAME = 'Nakama Hub';
const MAX_DESCRIPTION = 155;

/**
 * Título, descripción, canónica y Open Graph de cada página.
 *
 * Esto es lo que da sentido al renderizado en servidor: sin etiquetas propias por
 * publicación, todas las páginas comparten el mismo título y Google no tiene con
 * qué distinguirlas. Se ejecuta también en el servidor, así que las etiquetas ya
 * vienen en el HTML que recibe el buscador, sin depender de que ejecute el
 * JavaScript.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly document = inject(DOCUMENT);
  private readonly origin = inject(SITE_ORIGIN);

  apply(seo: PageSeo): void {
    const fullTitle = `${seo.title} · ${SITE_NAME}`;
    const description = truncate(seo.description, MAX_DESCRIPTION);
    const url = `${this.origin}${seo.path}`;

    this.title.setTitle(fullTitle);

    this.setTags({
      description,
      'og:title': fullTitle,
      'og:description': description,
      'og:url': url,
      'og:type': seo.type ?? 'website',
      'og:site_name': SITE_NAME,
      'og:image': seo.image ?? null,
      'twitter:card': seo.image ? 'summary_large_image' : 'summary',
      'article:published_time': seo.publishedAt ?? null,
      'article:author': seo.author ?? null,
    });

    this.setCanonical(url);
  }

  /** Evita que una página de error o privada acabe indexada. */
  noIndex(): void {
    this.meta.updateTag({ name: 'robots', content: 'noindex' });
  }

  private setTags(tags: Record<string, string | null>): void {
    for (const [key, value] of Object.entries(tags)) {
      const selector = key.includes(':') ? `property="${key}"` : `name="${key}"`;

      if (value === null) {
        this.meta.removeTag(selector);
        continue;
      }

      this.meta.updateTag(
        key.includes(':') ? { property: key, content: value } : { name: key, content: value },
        selector,
      );
    }

    this.meta.removeTag('name="robots"');
  }

  private setCanonical(url: string): void {
    let link = this.document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');

    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }

    link.setAttribute('href', url);
  }
}

/** Corta por la última palabra entera para no dejar la descripción a medias. */
function truncate(text: string, max: number): string {
  const clean = text.replace(/\s+/g, ' ').trim();
  if (clean.length <= max) {
    return clean;
  }
  const cut = clean.slice(0, max);
  return `${cut.slice(0, cut.lastIndexOf(' '))}…`;
}
