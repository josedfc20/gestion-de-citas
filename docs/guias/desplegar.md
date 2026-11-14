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

**Comprobar que está bien protegida** (cabeceras, HTTPS, CSP y sesión): con
todo arrancado, pasa las pruebas de producción, como explica
[`pruebas-e2e.md`, apartado 10](pruebas-e2e.md#10-pruebas-de-producción-paso-7b).

## 3. Las cabeceras de seguridad (paso 7b)

Caddy las pone en todas las respuestas (`frontend/Caddyfile`):

| Cabecera | Qué le dice al navegador |
|---|---|
| `Strict-Transport-Security: max-age=31536000` | Durante un año, esta dirección solo por HTTPS |
| `Content-Security-Policy` | Solo ejecutar y cargar lo que venga de esta dirección; nada en línea |
| `X-Content-Type-Options: nosniff` | No adivinar el tipo de los archivos |
| `Referrer-Policy: strict-origin-when-cross-origin` | Al salir a otra web, solo el dominio |
| `Permissions-Policy` | Sin cámara, micrófono, ubicación, pagos ni USB |
| `Cross-Origin-Opener-Policy: same-origin` | Otras pestañas no tocan la nuestra |

Además, `/assets/*` se guarda un año en la caché del navegador (sus nombres
cambian con cada versión) e `index.html` nunca, para que una versión nueva se
vea al momento.

## 4. Correo (paso 7d)

El único correo que envía la aplicación es el de **recuperar la contraseña**.
En desarrollo va a Mailpit; en producción sale por **Brevo** (plan gratuito)
por SMTP, siempre cifrado (STARTTLS obligatorio en el perfil `prod`) y con
tiempos máximos de 5 s para conectar y 10 s para enviar.

1. **Remitente.** Una cuenta de Gmail creada solo para la aplicación
   (la nuestra es `gestiondecitas.app@gmail.com`). No
   uses una de iCloud ni de Yahoo: sus reglas antifraude (DMARC) piden
   retener los correos que dicen venir de ellos y salen de otro servidor,
   así que irían a spam.
2. **Cuenta en Brevo.** Date de alta en [brevo.com](https://www.brevo.com)
   con el plan *Free*.
3. **Verificar el remitente.** En Brevo, *Senders, domains & dedicated IPs*
   → *Senders* → *Add a sender*, con la dirección de Gmail del paso 1. Brevo
   manda un correo a esa dirección: abre el enlace para verificarla.
4. **Clave SMTP.** En *SMTP & API* → pestaña *SMTP* → *Generate a new SMTP
   key*. Copia la clave en ese momento (no se vuelve a ver) y el *Login* que
   aparece en la misma página.
5. **`.env.prod`.** Rellena `MAIL_USERNAME` (el *Login*), `MAIL_PASSWORD`
   (la clave SMTP) y `MAIL_FROM` (el remitente). `MAIL_HOST` y `MAIL_PORT`
   ya vienen puestos. La clave es un secreto: nunca va a Git ni a un chat.
6. **Probarlo.** Con tu correo personal como `ADMIN_EMAIL`, arranca el
   Compose de producción, entra en https://localhost, pulsa "¿Has olvidado
   tu contraseña?" y pon ese correo. Debe llegar en un minuto (mira también
   en spam la primera vez) y el enlace debe abrir la página para elegir una
   contraseña nueva.

**Si no llega:** `docker compose -f docker-compose.prod.yml --env-file .env.prod logs backend`
muestra "No se pudo enviar el correo de recuperación de contraseña" con el
motivo: `Authentication failed` (el *Login* o la clave SMTP están mal),
`sender not verified` o parecido (falta el paso 3) o un tiempo agotado (el
servidor no llega a Brevo: revisa la red o el cortafuegos).

## 5. En un servidor

Se completa en el paso 7e: crear el servidor gratuito de Oracle Cloud, el
subdominio de DuckDNS, el cortafuegos, las copias de
seguridad, el restablecimiento nocturno de la demo y cómo actualizar a una
versión nueva.
