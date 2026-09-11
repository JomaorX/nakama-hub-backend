import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * Cómo se rinde cada ruta en el servidor.
 *
 * El contenido público se renderiza en servidor para que Google lo indexe, que es
 * el motivo de haber montado SSR. Las pantallas privadas se dejan en cliente: el
 * token vive en el navegador, así que renderizarlas en servidor solo produciría un
 * parpadeo de contenido anónimo antes de hidratar.
 */
export const serverRoutes: ServerRoute[] = [
  { path: '', renderMode: RenderMode.Server },
  { path: 'post/:id', renderMode: RenderMode.Server },
  { path: 'u/:username', renderMode: RenderMode.Server },
  { path: 'buscar', renderMode: RenderMode.Server },

  { path: 'entrar', renderMode: RenderMode.Prerender },
  { path: 'registro', renderMode: RenderMode.Prerender },
  { path: 'recuperar', renderMode: RenderMode.Prerender },

  // Llevan el token en la URL, así que se resuelven en cliente y no se prerenderizan.
  { path: 'restablecer', renderMode: RenderMode.Client },
  { path: 'verificar', renderMode: RenderMode.Client },

  { path: 'muro', renderMode: RenderMode.Client },
  { path: 'publicar', renderMode: RenderMode.Client },
  { path: 'post/:id/editar', renderMode: RenderMode.Client },
  { path: 'ajustes', renderMode: RenderMode.Client },

  { path: '**', renderMode: RenderMode.Server },
];
