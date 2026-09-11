import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    title: 'Nakama Hub · Comunidad de anime y manga',
    loadComponent: () => import('./features/posts/explore.page').then((m) => m.ExplorePage),
  },
  {
    path: 'muro',
    title: 'Tu muro · Nakama Hub',
    canActivate: [authGuard],
    loadComponent: () => import('./features/posts/feed.page').then((m) => m.FeedPage),
  },
  {
    path: 'buscar',
    title: 'Buscar · Nakama Hub',
    loadComponent: () => import('./features/search/search.page').then((m) => m.SearchPage),
  },
  {
    path: 'publicar',
    title: 'Nueva publicación · Nakama Hub',
    canActivate: [authGuard],
    loadComponent: () => import('./features/posts/post-editor.page').then((m) => m.PostEditorPage),
  },
  {
    path: 'post/:id',
    loadComponent: () => import('./features/posts/post-detail.page').then((m) => m.PostDetailPage),
  },
  {
    path: 'post/:id/editar',
    title: 'Editar publicación · Nakama Hub',
    canActivate: [authGuard],
    loadComponent: () => import('./features/posts/post-editor.page').then((m) => m.PostEditorPage),
  },
  {
    path: 'ajustes',
    title: 'Ajustes · Nakama Hub',
    canActivate: [authGuard],
    loadComponent: () => import('./features/settings/settings.page').then((m) => m.SettingsPage),
  },
  {
    path: 'u/:username',
    loadComponent: () => import('./features/profile/profile.page').then((m) => m.ProfilePage),
  },
  {
    path: 'entrar',
    title: 'Entrar · Nakama Hub',
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'registro',
    title: 'Crear cuenta · Nakama Hub',
    loadComponent: () => import('./features/auth/signup.page').then((m) => m.SignupPage),
  },
  {
    path: '**',
    title: 'Página no encontrada · Nakama Hub',
    loadComponent: () => import('./features/not-found.page').then((m) => m.NotFoundPage),
  },
];
