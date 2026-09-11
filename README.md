# Nakama Hub Backend

API REST para una comunidad de anime, manga y series: publicaciones con hilos de
comentarios anidados, categorías, seguimiento entre usuarios y reputación.

## 📖 Documentación de la API

[![Postman Docs](https://img.shields.io/badge/Postman-API_Docs-orange)](https://documenter.getpostman.com/view/46853536/2sB3WqvgX9)

## 🚀 Tecnologías

- Java 25
- Spring Boot 3.5
- Spring Security con JWT
- JPA / Hibernate
- MySQL en ejecución, H2 en los tests
- Maven

## ⚙️ Puesta en marcha

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

## 📦 Funcionalidad

### 🔐 Autenticación
- Registro y login con contraseñas cifradas con BCrypt
- Token de acceso JWT de vida corta y token de refresco opaco de un solo uso
- Rotación en cada refresco con detección de reutilización: si reaparece un token
  ya gastado se revocan todas las sesiones del usuario
- Cierre de sesión y cambio de contraseña, que invalida las sesiones abiertas
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
- Listados paginados por post, por hilo y por autor
- Un comentario nunca revela el contenido de un post que el visitante no puede ver

### 🔎 Búsqueda
- `GET /api/search/posts` con filtros por texto, tipo de contenido, serie y categoría,
  respetando siempre la privacidad de quien consulta
- `GET /api/search/users` y `GET /api/search/series`

### 👤 Perfiles
- Perfil público y privado, seguimiento entre usuarios y puntos de reputación
- Edición de nombre de usuario, email, biografía, avatar y privacidad

### 🛡️ Moderación
- Suspensión de cuentas y borrado de posts y comentarios

### 🔒 Datos personales
- Borrado de cuenta lógico con anonimización: se eliminan los datos personales y
  las publicaciones quedan atribuidas a una cuenta sin identidad
- Exportación de los datos propios en `GET /api/users/me/export`
- Un cambio de nombre de usuario invalida los tokens emitidos antes

## 🧭 Pendiente
- Subida real de imágenes, ahora solo se guardan URLs
- Verificación por email y recuperación de contraseña
- Reporte y bloqueo de usuarios
- Purgado definitivo de las cuentas anonimizadas pasado un plazo de retención
- Migraciones con Flyway en lugar de `ddl-auto=update`
- Documentación viva con springdoc

## 🧠 Flujo de trabajo

El proyecto sigue GitFlow. Cada funcionalidad se desarrolla en su rama y se integra
mediante Pull Request.

- `feature/auth` → [PR #1](https://github.com/JomaorX/nakama-hub-backend/pull/1): autenticación y módulo de posts
- `feature/comment` → [PR #2](https://github.com/JomaorX/nakama-hub-backend/pull/2): módulo de comentarios
- `feature/post` → [PR #3](https://github.com/JomaorX/nakama-hub-backend/pull/3): mejoras del módulo de posts
- `feature/user` → [PR #4](https://github.com/JomaorX/nakama-hub-backend/pull/4): módulo de usuarios

## 📂 Estructura

```
src/
├── main/java/com/nakamahub/backend/
│   ├── config/         # seguridad, manejo de errores y datos iniciales
│   ├── controllers/
│   ├── dtos/
│   ├── models/
│   ├── repositories/
│   ├── security/       # filtros JWT y utilidades
│   └── services/
└── test/java/com/nakamahub/backend/
```

## 📬 Contacto

Hecho con ❤️ por José Miguel Martínez [LinkedIn](https://www.linkedin.com/in/martinez97pro) • [GitHub](https://github.com/JomaorX) • [Portfolio](https://jomaor.dev)
