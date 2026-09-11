# Nakama Hub

Comunidad de anime, manga y series: publicaciones con hilos de comentarios
anidados, categorías, seguimiento entre usuarios y reputación.

El repositorio contiene las dos mitades del proyecto:

| Carpeta | Qué es |
|---|---|
| raíz | API REST en Java con Spring Boot |
| [`frontend/`](frontend) | Cliente en Angular con renderizado en servidor |

## 📖 Documentación de la API

Con la aplicación levantada, la documentación se genera del propio código:

- Swagger UI en `http://localhost:8080/swagger-ui.html`
- Contrato OpenAPI en `http://localhost:8080/v3/api-docs`, del que se puede
  generar directamente el cliente HTTP del frontend

[![Postman Docs](https://img.shields.io/badge/Postman-API_Docs-orange)](https://documenter.getpostman.com/view/46853536/2sB3WqvgX9)

## 🚀 Tecnologías

- Java 25
- Spring Boot 3.5
- Spring Security con JWT
- JPA / Hibernate
- MySQL en ejecución, H2 en los tests
- Maven

## ⚙️ Puesta en marcha

Para desarrollo, sin necesidad de instalar MySQL:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # API en :8080
cd frontend && npm install && npm start                 # web en :4200
```

El perfil `dev` usa una base de datos en memoria que se vacía al parar el proceso.
Sin credenciales SMTP los correos se escriben en el log en lugar de enviarse, así
que el enlace de verificación o de recuperación se copia desde la consola.
Contra MySQL de verdad:

```bash
cp .env.example .env          # y rellena los valores
openssl rand -base64 48       # genera el JWT_SECRET
./mvnw spring-boot:run
```

El fichero `.env` lo carga Spring al arrancar y **nunca debe subirse al repositorio**.
La aplicación no arranca si `JWT_SECRET` falta o tiene menos de 32 bytes.

```bash
./mvnw verify                 # compila y ejecuta la suite, sin necesidad de MySQL
```

## 🚢 Despliegue

Todo se levanta en un único servidor con `docker compose up -d --build`: MySQL,
la API, el render en servidor de Angular y Caddy como proxy, que se encarga solo
del certificado HTTPS. Un VPS pequeño sirve de sobra para empezar.

## 📦 Funcionalidad

### 🔐 Autenticación
- Registro y login con contraseñas cifradas con BCrypt
- Token de acceso JWT de vida corta y token de refresco opaco de un solo uso
- Rotación en cada refresco con detección de reutilización: si reaparece un token
  ya gastado se revocan todas las sesiones del usuario
- Cierre de sesión y cambio de contraseña, que invalida las sesiones abiertas
- Verificación de la dirección de correo y recuperación de contraseña por enlace
- Límite de intentos por cuenta y por dirección, con `Retry-After` en la respuesta
- Roles: `USER`, `MODERATOR`, `ADMIN`
- Las cuentas suspendidas quedan bloqueadas en el filtro de seguridad

### 📝 Posts
- Publicación y edición con tipo de contenido `ANIME`, `MANGA`, `SERIE` o `GENERAL`
- Estados `DRAFT`, `PUBLISHED` y `ARCHIVED`
- Visibilidad `PUBLIC`, `FOLLOWERS_ONLY` y `PRIVATE`, aplicada tanto en el feed
  como al pedir un post por su identificador
- Categorías, asociación a serie, likes y contador de visitas
- Timeline personal en `GET /api/posts/feed` con lo que publican las cuentas
  seguidas, más lo propio

### 💬 Comentarios
- Hilos con respuestas anidadas, con edición por parte del autor
- Cada comentario informa de cuántas respuestas tiene, que se cargan solo al abrirlas
- Listados paginados por post, por hilo y por autor
- Un comentario nunca revela el contenido de un post que el visitante no puede ver

### 🔍 Indexación
- `GET /sitemap.xml` con la portada, los posts públicos y los perfiles no privados,
  para que los buscadores no dependan de ir siguiendo enlaces desde la portada
- Nunca delata borradores, posts privados ni perfiles privados

### 🔎 Búsqueda
- `GET /api/search/posts` con filtros por texto, tipo de contenido, serie y categoría,
  respetando siempre la privacidad de quien consulta
- `GET /api/search/users` y `GET /api/search/series`

### 🚫 Bloqueo
- Bloqueo entre usuarios, con efecto simétrico: ninguno ve las publicaciones ni
  los comentarios del otro, y no pueden seguirse
- Bloquear deshace el seguimiento en ambos sentidos
- Lista de cuentas bloqueadas en los ajustes

### 👤 Perfiles
- Perfil público y privado, seguimiento entre usuarios y puntos de reputación
- Edición de nombre de usuario, email, biografía, avatar y privacidad

### 🛡️ Moderación
- Suspensión de cuentas y borrado de posts y comentarios
- Reporte de posts, comentarios y cuentas por parte de cualquier usuario,
  con copia del contenido denunciado para que editarlo o borrarlo no destruya
  la prueba
- Cola de moderación en `GET /api/reports`, con cierre y nota del moderador

### 🔒 Datos personales
- Borrado de cuenta lógico con anonimización: se eliminan los datos personales y
  las publicaciones quedan atribuidas a una cuenta sin identidad
- Exportación de los datos propios en `GET /api/users/me/export`
- Un cambio de nombre de usuario invalida los tokens emitidos antes

## 🧭 Pendiente
- Subida real de imágenes, ahora solo se guardan URLs
- Purgado definitivo de las cuentas anonimizadas pasado un plazo de retención
- Migraciones con Flyway en lugar de `ddl-auto=update`

## 🧠 Flujo de trabajo

El proyecto sigue GitFlow. Cada funcionalidad se desarrolla en su rama y se integra
mediante Pull Request.

- `feature/auth` → [PR #1](https://github.com/JomaorX/nakama-hub-backend/pull/1): autenticación y módulo de posts
- `feature/comment` → [PR #2](https://github.com/JomaorX/nakama-hub-backend/pull/2): módulo de comentarios
- `feature/post` → [PR #3](https://github.com/JomaorX/nakama-hub-backend/pull/3): mejoras del módulo de posts
- `feature/user` → [PR #4](https://github.com/JomaorX/nakama-hub-backend/pull/4): módulo de usuarios

## 📂 Estructura

```
├── src/main/java/com/nakamahub/backend/
│   ├── config/         # seguridad, manejo de errores y datos iniciales
│   ├── controllers/
│   ├── dtos/
│   ├── models/
│   ├── repositories/
│   ├── security/       # filtros JWT y utilidades
│   └── services/
├── src/test/java/      # 85 tests de integración sobre H2
└── frontend/           # cliente Angular, ver su propio README
```

## 📬 Contacto

Hecho con ❤️ por José Miguel Martínez [LinkedIn](https://www.linkedin.com/in/martinez97pro) • [GitHub](https://github.com/JomaorX) • [Portfolio](https://jomaor.dev)
