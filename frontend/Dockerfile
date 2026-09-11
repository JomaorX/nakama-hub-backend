# Compilación
FROM node:22-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Ejecución: servidor de render en servidor de Angular
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production

COPY --from=build /build/dist/frontend ./dist/frontend
COPY --from=build /build/package.json ./

USER node

EXPOSE 4000
CMD ["node", "dist/frontend/server/server.mjs"]
