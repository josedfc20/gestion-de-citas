# syntax=docker/dockerfile:1
# Imagen de producción de la web (paso 7a): la web compilada dentro de Caddy, que
# la sirve, reenvía /api al backend y consigue solo el certificado HTTPS.

# Etapa 1: compilar la web. Los archivos que salen son los mismos para Intel y
# ARM, así que corre en la arquitectura de quien construye (BUILDPLATFORM).
FROM --platform=$BUILDPLATFORM node:24.22.3-alpine AS compilar
WORKDIR /fuente
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# Etapa 2: Caddy con la web en /srv y su configuración.
FROM caddy:2.11.9-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=compilar /fuente/dist/ /srv/
