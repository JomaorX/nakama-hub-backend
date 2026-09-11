import { ApplicationConfig, mergeApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { API_ORIGIN, SITE_ORIGIN } from './core/api.config';
import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    {
      // En el servidor no hay origen relativo: quien pide es Node, no un navegador.
      provide: API_ORIGIN,
      useValue: process.env['API_ORIGIN'] ?? 'http://localhost:8080',
    },
    {
      provide: SITE_ORIGIN,
      useValue: process.env['SITE_ORIGIN'] ?? 'http://localhost:4200',
    },
  ],
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
