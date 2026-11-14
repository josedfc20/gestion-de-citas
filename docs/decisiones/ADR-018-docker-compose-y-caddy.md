# ADR-018 · Producción con Docker Compose y Caddy delante

- **Estado:** aceptada el 14/11/2026
- **Origen:** 7.0 ([plan de despliegue](../propuestas/7.0-plan-de-despliegue.md),
  decisiones 1 y 2) y 7a ([imágenes y Compose](../propuestas/7.1-7a-produccion-docker.md))

## Contexto

En desarrollo la web la sirve Vite, que además reenvía `/api` al backend
para que el navegador vea una sola dirección (la cookie del refresh viaja
sin CORS, ADR-007 y ADR-012). En producción hace falta alguien que haga lo
mismo, que ponga el HTTPS (RNF-004) y las cabeceras de seguridad, y la
aplicación tiene que poder arrancar en un servidor gratuito y pequeño (Oracle
Cloud *Always Free*, que es ARM) sin instalar Java ni Node en él.

## Decisión

- **Dos imágenes de Docker**: la del backend (JRE 21 de Temurin, el `.jar`
  en capas y un usuario sin privilegios) y la de la web (la web compilada
  dentro de **Caddy**). La CI las construye en cada PR y, con una etiqueta
  `v*`, las publica para Intel y ARM en GitHub Container Registry.
- **Caddy** sirve la web, reenvía `/api/*` al backend y consigue y renueva
  solo el certificado de Let's Encrypt.
- **`docker-compose.prod.yml`** junta Caddy, el backend y PostgreSQL. Solo
  Caddy abre puertos (80 y 443); el backend y la base de datos solo se ven en
  la red interna de Docker.
- **Perfil `prod`**: sin Swagger y con la IP real del usuario tomada de
  `X-Forwarded-For` solo cuando la petición llega de la red interna (D-9).

## Alternativas descartadas

- **Nginx + Certbot:** el proxy más extendido, pero el certificado se pide y
  se renueva con otro programa que hay que montar y vigilar.
- **Que Spring Boot sirva también la web** (un solo contenedor): mezcla la
  web con el backend y el HTTPS seguiría necesitando un proxy delante.
- **Una plataforma gestionada (Render, Railway, Fly.io):** menos trabajo de
  servidor, pero sus planes gratuitos se duermen o borran la base de datos.
  Queda como plan B: las mismas imágenes sirven allí.
- **Compilar en el servidor** (`git pull` y `--build`): no depende de GitHub,
  pero el servidor gratuito tendría que compilar Java y la web.

## Consecuencias

- El mismo Compose arranca en el PC (con `DOMINIO=localhost` y un
  certificado propio de Caddy) y en cualquier servidor con Docker.
- La versión desplegada es siempre una etiqueta concreta (`VERSION` en
  `.env.prod`), y volver a la anterior es cambiar ese número.
- Un servicio nuevo detrás del proxy debe leer la IP real con cuidado:
  `getRemoteAddr()` solo es fiable con el perfil `prod` (D-9).
