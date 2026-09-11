# Nakama Hub · frontend

Cliente en Angular con renderizado en servidor de la API de Nakama Hub.

## Puesta en marcha

```bash
npm install
npm start          # http://localhost:4200
```

`ng serve` reenvía `/api` y `/auth` a `http://localhost:8080` mediante
`proxy.conf.json`, así que no hay CORS ni URLs que cambiar entre entornos. El
backend se levanta aparte, y para desarrollo puede ir sin MySQL:

```bash
cd .. && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
npm run build      # compila cliente y servidor
node dist/frontend/server/server.mjs
```

## Decisiones

**Renderizado en servidor.** Es el motivo de haber elegido Angular con SSR y no
una SPA suelta: el contenido público se sirve ya renderizado para que Google lo
indexe, que es el único canal de crecimiento orgánico de una comunidad de nicho.
`app.routes.server.ts` decide el modo de cada ruta: servidor para el contenido
público, prerenderizado para las pantallas estáticas y cliente para las privadas,
donde renderizar en servidor solo produciría un parpadeo de contenido anónimo.

**Origen de la API.** En el navegador las peticiones salen relativas. En el
render de servidor quien pide es Node, donde una ruta relativa no significa nada,
así que ahí se inyecta la URL absoluta mediante `API_ORIGIN`.

**Sesión.** El token de acceso dura una hora y el de refresco es de un solo uso.
El interceptor renueva la sesión al recibir un 401 y comparte un único refresco
entre todas las peticiones en vuelo: el backend trata la reutilización de un
token de refresco como robo y revocaría la sesión entera si se lanzasen varios
refrescos en paralelo.

**Estilo de Angular.** Componentes independientes sin NgModules, señales para el
estado, `inject()` y el control de flujo `@if` / `@for`.

## Estructura

```
src/app/
├── core/          # modelos, servicios de API, interceptores y guardas
├── features/      # una carpeta por pantalla, todas cargadas de forma diferida
└── shared/        # componentes y utilidades reutilizables
```
