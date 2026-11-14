# Desplegar GestionDeCitas

> Fase 7 (Despliegue). Esta guía empieza en el paso 7a con "Probarlo en tu
> PC" y se completa en el 7e con el servidor, el dominio, el correo, las
> copias de seguridad y las actualizaciones.
> Decisiones: [ADR-018](../decisiones/ADR-018-docker-compose-y-caddy.md) y
> el [plan de la fase 7](../propuestas/7.0-plan-de-despliegue.md).

## 1. Cómo queda montada

```
Navegador ──HTTPS──▶ web (Caddy) ──┬── /          → la web compilada
                                   └── /api/...   → backend (Spring Boot, perfil prod)
                                                       └── postgres (PostgreSQL 16)
```

Todo está en [`docker-compose.prod.yml`](../../docker-compose.prod.yml):

| Servicio | Imagen | Qué hace | Puertos abiertos |
|---|---|---|---|
| `web` | `ghcr.io/josedfc20/gestion-de-citas-web` | Caddy: HTTPS automático, la web y `/api` al backend | 80 (solo redirige) y 443 |
| `backend` | `ghcr.io/josedfc20/gestion-de-citas-backend` | La API con los perfiles `prod` y `demo` | ninguno (red interna) |
| `postgres` | `postgres:16-alpine` | La base de datos, en el volumen `datos-postgres` | ninguno (red interna) |

Las imágenes las construye y publica la CI al crear una etiqueta `v*`; en el
PC se construyen con `--build`. Swagger está apagado en producción: la API
está en [`docs/api/openapi.json`](../api/openapi.json).

## 2. Probarlo en tu PC

Necesitas Docker Desktop arrancado (el mismo de siempre). No hace falta
parar el `docker-compose.yml` de desarrollo: usan otros nombres y no
comparten base de datos.

1. En la raíz del repositorio, copia la plantilla de variables:

   ```powershell
   copy .env.prod.example .env.prod
   ```

2. Abre `.env.prod` y cambia las contraseñas y el `JWT_SECRET` (cualquier
   texto largo sirve para probar). Deja `DOMINIO=localhost` y
   `VERSION=local`.

3. Construye las imágenes y arranca todo (la primera vez tarda unos minutos,
   porque compila el backend y la web dentro de Docker):

   ```powershell
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build --wait
   ```

4. Abre **https://localhost**. Como el certificado es de Caddy y no de una
   autoridad conocida, el navegador avisa una vez: "Configuración avanzada" →
   "Continuar a localhost". En el servidor real el certificado será válido y
   no saldrá el aviso.

5. Entra con una cuenta de demo (`cliente@demo.invalid`, `luis@demo.invalid`…)
   y la contraseña de `DEMO_PASSWORD`, o con el administrador de
   `ADMIN_EMAIL`.

6. Cuando acabes, páralo todo y libera la memoria:

   ```powershell
   docker compose -f docker-compose.prod.yml --env-file .env.prod down
   ```

   Los datos se quedan en los volúmenes para la próxima vez. Para empezar de
   cero, añade `-v` (borra la base de datos de esta prueba, no la de
   desarrollo).

**Si los puertos 80 o 443 están ocupados** (por ejemplo, por IIS u otro
programa), cambia `PUERTO_HTTP` y `PUERTO_HTTPS` en `.env.prod` (por ejemplo
8080 y 8443) y abre `https://localhost:8443`.

**Ver qué pasa por dentro:**

```powershell
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f backend
```

## 3. En un servidor

Se completa en el paso 7e: crear el servidor gratuito de Oracle Cloud, el
subdominio de DuckDNS, el correo con Brevo, el cortafuegos, las copias de
seguridad, el restablecimiento nocturno de la demo y cómo actualizar a una
versión nueva.
