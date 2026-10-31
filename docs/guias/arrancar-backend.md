# Guía · Arrancar y probar el backend en tu PC

> Para Windows, con el entorno ya instalado (`instalacion-entorno.md`).
> Todos los comandos se escriben en **PowerShell** (no hace falta administrador):
> menú Inicio → escribe "PowerShell" → Enter. Copia cada comando, pégalo con
> clic derecho y pulsa Enter.

Al terminar habrás comprobado que la base de datos y el correo de pruebas
arrancan en Docker, que el backend crea las tablas y responde, que puedes
registrarte e iniciar sesión (bloque B2), que puedes recuperar la contraseña,
exportar tus datos y eliminar la cuenta (bloque B3), que puedes gestionar el
catálogo y los empleados (B4), que puedes configurar los horarios y consultar
las horas libres (B5) y que las pruebas pasan.

## 1. Traer el código a tu PC

Ve a la carpeta del repositorio:

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones\gestion-de-citas
```

- **Si el PR aún no está fusionado** (para probarlo antes de aprobarlo):

  ```powershell
  git fetch origin
  git switch feature/horarios-disponibilidad
  ```

- **Si ya está fusionado en `main`:**

  ```powershell
  git switch main
  git pull
  ```

Comprueba que tienes las carpetas nuevas: `dir` debe mostrar `backend` y el
archivo `docker-compose.yml`.

## 2. Abrir Docker Desktop

Abre **Docker Desktop** desde el menú Inicio y espera a que abajo a la izquierda
diga **Engine running** (tarda unos segundos). Sin esto no funcionan ni la base
de datos ni las pruebas.

## 3. Crear o completar tu archivo `.env`

El `.env` guarda las contraseñas de tu PC. **Nunca se sube a GitHub** (está en
`.gitignore`); en el repositorio solo está la plantilla `.env.example`.

**Si aún no tienes `.env`** (primera vez):

```powershell
Copy-Item .env.example .env
```

**Si ya lo tienes de un bloque anterior**, no lo borres (tiene la contraseña
con la que se creó tu base de datos). Solo hay que añadirle las líneas nuevas:
las de `JWT_SECRET` y `ADMIN_*` (B2) y las del correo (B3).

### 3.1 Generar la clave `JWT_SECRET`

Es la clave con la que el backend firma los tokens de sesión. Genera una
aleatoria con este comando (cópialo entero, es una sola línea):

```powershell
$b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString('x2') })
```

Sale una línea de 96 letras y números, por ejemplo `3f9a0c...e71b`. Cópiala
(selecciónala con el ratón y clic derecho).

### 3.2 Editar el `.env`

```powershell
notepad .env
```

Deja el archivo así (con **tus** valores):

```
POSTGRES_DB=gestion_de_citas
POSTGRES_USER=gestion_de_citas
POSTGRES_PASSWORD=tu-clave-de-la-base-de-datos

DB_URL=jdbc:postgresql://localhost:5433/gestion_de_citas
JWT_SECRET=pega-aquí-la-clave-del-paso-3.1
CORS_ALLOWED_ORIGINS=http://localhost:5173
COOKIE_SECURE=true

ADMIN_EMAIL=tu-correo@ejemplo.com
ADMIN_PASSWORD=una-clave-para-el-administrador
ADMIN_NOMBRE=Jose

MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-responder@gestiondecitas.local
APP_WEB_URL=http://localhost:5173

DEMO_PASSWORD=una-clave-para-las-cuentas-de-demo
```

- `POSTGRES_PASSWORD`: si ya tenías `.env`, **no la cambies**. Si es nuevo,
  pon una tuya.
- `ADMIN_EMAIL` y `ADMIN_PASSWORD`: con ellos se crea el **administrador** la
  primera vez que arranca el backend. La contraseña necesita **10 caracteres
  o más** y no puede ser muy común (`1234567890`, `contraseña123`...).
- `MAIL_*` y `APP_WEB_URL`: déjalos tal cual. En local los correos van a
  **Mailpit**, un servidor de correo de pruebas que no envía nada a Internet.
- `DEMO_PASSWORD` (B5): solo se usa si arrancas con los datos de
  demostración (apartado 7.10). Es la contraseña de todas las cuentas de demo
  y sigue las mismas reglas que la del administrador.
- Usa **solo letras, números y guiones** en las contraseñas (sin espacios,
  `\`, `#`, comillas ni tildes) para evitar problemas.

Guarda (Ctrl+S) y cierra.

## 4. Arrancar la base de datos y el correo

```powershell
docker compose up -d
```

La primera vez descarga las imágenes de PostgreSQL 16 y de Mailpit (un
minuto). Comprueba que están en marcha:

```powershell
docker ps
```

Deben aparecer `gestion-de-citas-db` con el estado **Up ... (healthy)** y
`gestion-de-citas-correo` con **Up**. Si la base de datos dice
`(health: starting)`, espera unos segundos y repite.

Los dos se quedan funcionando en segundo plano; no hace falta tener esta
ventana abierta.

## 5. Arrancar el backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

- El `.\` delante es obligatorio en PowerShell.
- **La primera vez tarda varios minutos**: `mvnw` descarga Maven y todas las
  librerías. Las siguientes veces arranca en unos 10 segundos.
- Está listo cuando ves una línea parecida a
  `Started GestionDeCitasApplication in 5.3 seconds`.
- Un poco más arriba verás Flyway aplicando las migraciones:
  - Si tu base de datos venía del B4: `Successfully applied 1 migration ... now at version v6`
    (la V6 añade los ajustes de horario del B5).
  - Si es nueva: `Successfully applied 6 migrations ... now at version v6`.
  - Las siguientes veces dirá que el esquema está al día (`is up to date`).
- Justo después de `Started` verás
  `Administrador inicial creado (id 1).` Solo sale la primera vez; las
  siguientes dirá `El administrador inicial ya existe; no se modifica.`
- Ya **no** debe aparecer `Using generated security password` (era el aviso
  pendiente del B1).

**No cierres esta ventana**: el backend funciona mientras está abierta.

## 6. Comprobarlo en el navegador

Abre estas cuatro direcciones:

| Dirección | Qué debes ver | Qué significa |
|---|---|---|
| http://localhost:8080/actuator/health | `{"groups":["liveness","readiness"],"status":"UP"}` | El backend está vivo y conectado a la base de datos. |
| http://localhost:8080/swagger-ui.html | La página de Swagger "GestionDeCitas API" con los grupos **Autenticación** y **Mi perfil** | La documentación de la API, que además sirve para probarla (sección 7). |
| http://localhost:8080/api/v1/me | `{"detail":"Hay que iniciar sesión.", ... "status":401, ...}` | Correcto: `/me` necesita sesión, y el error sale en formato Problem Details. |
| http://localhost:8025 | La bandeja de **Mailpit**, vacía | Aquí llegan los correos que envía el backend (sección 7.6). |

## 7. Probar el registro y el login con Swagger

Abre http://localhost:8080/swagger-ui.html. Cada endpoint se despliega con un
clic; dentro, el botón **Try it out** permite editar los datos y **Execute**
envía la petición. La respuesta aparece debajo, en **Server response**.

### 7.1 Iniciar sesión como administrador

1. Despliega **POST /api/v1/auth/login** → **Try it out**.
2. Cambia el cuerpo por tu correo y contraseña del `.env`:

   ```json
   { "email": "tu-correo@ejemplo.com", "password": "una-clave-para-el-administrador" }
   ```

3. **Execute**. Debe salir **Code 200** y un cuerpo parecido a:

   ```json
   {
     "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJk...",
     "tokenType": "Bearer",
     "expiraEnSegundos": 900,
     "usuario": { "id": 1, "nombre": "Jose", "rol": "ADMIN", "debeCambiarPassword": false }
   }
   ```

   En **Response headers** verás también `set-cookie: refresh_token=...;
   HttpOnly; SameSite=Strict`: es el token de refresco, que el navegador
   guarda y JavaScript no puede leer.
4. Copia el valor de `accessToken` (lo que va entre comillas, sin ellas).

### 7.2 Usar el token

1. Arriba a la derecha pulsa **Authorize** 🔓, pega el token (sin escribir
   `Bearer`) → **Authorize** → **Close**. El candado queda cerrado 🔒.
2. Despliega **GET /api/v1/me** → **Try it out** → **Execute**.
   Debe salir **200** con tus datos (`"rol": "ADMIN"`).
3. El token caduca a los **15 minutos**. Cuando pase, `/me` dará **401**:
   ejecuta **POST /api/v1/auth/refresh** (sin cuerpo; usa la cookie) y te dará
   un `accessToken` nuevo para repetir el paso 1.

### 7.3 Registrar un cliente

1. Despliega **POST /api/v1/auth/registro** → **Try it out** y pon, por ejemplo:

   ```json
   {
     "nombre": "Ana",
     "apellidos": "García López",
     "email": "ana@ejemplo.com",
     "telefono": "600123123",
     "fechaNacimiento": "1990-05-20",
     "password": "Mi-clave-larga-2026",
     "confirmacionPassword": "Mi-clave-larga-2026",
     "aceptaPrivacidad": true
   }
   ```

2. **Execute** → **201** con `"rol": "CLIENTE"`.
3. Prueba los errores, que son parte de lo que hace el B2:

   | Cambia esto y vuelve a ejecutar | Respuesta |
   |---|---|
   | El mismo correo otra vez | **409** "Ya existe una cuenta con ese correo." |
   | `"fechaNacimiento": "2015-01-01"` (y otro correo) | **422** "Debes tener al menos 14 años..." |
   | `"aceptaPrivacidad": false` | **400** con el campo `aceptaPrivacidad` |
   | `"password"` y `"confirmacionPassword"`: `"1234567890"` | **400** "Es una contraseña demasiado común; elige otra." |

4. Inicia sesión con Ana (7.1) y pulsa **Authorize** con su token. Con
   **GET /api/v1/me** verás sus datos; si pruebas una ruta de administrador,
   el resultado es **403** "No tienes permiso para esta operación".

### 7.4 Otras pruebas rápidas

- **Contraseña mal** en el login: **401** "Correo o contraseña no válidos."
  (el mismo mensaje si el correo no existe, para no dar pistas).
- **Seis intentos de login fallidos seguidos** en menos de un minuto:
  el sexto da **429** "Demasiados intentos...". Espera un minuto.
- **POST /api/v1/auth/logout** → **204**. Después, `/auth/refresh` da **401**:
  la sesión se ha cerrado de verdad.
- **PUT /api/v1/me/password** para cambiar tu contraseña (pide la actual).

### 7.5 Lo mismo desde PowerShell (opcional)

Con una **segunda** ventana de PowerShell (la del backend sigue abierta).
Cambia el correo y la contraseña por los tuyos:

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType "application/json" -Body '{"email":"tu-correo@ejemplo.com","password":"una-clave-para-el-administrador"}'
$login.usuario
Invoke-RestMethod -Uri http://localhost:8080/api/v1/me -Headers @{ Authorization = "Bearer $($login.accessToken)" }
```

El primer comando no muestra nada; el segundo muestra `id`, `nombre`, `rol`
y `debeCambiarPassword`; el tercero, todos tus datos.

### 7.6 Recuperar la contraseña (B3)

Con el cliente Ana de la sección 7.3:

1. Despliega **POST /api/v1/auth/recuperar-password** → **Try it out**:

   ```json
   { "email": "ana@ejemplo.com" }
   ```

   **Execute** → **202** "Si el correo está registrado, recibirás un enlace en
   unos minutos." Con un correo que no existe la respuesta es **exactamente la
   misma**: así nadie puede averiguar qué correos tienen cuenta.
2. Abre http://localhost:8025. Verás el correo **"Restablecer tu contraseña ·
   Mi negocio"** con un enlace como
   `http://localhost:5173/restablecer-password#token=2ch0BaY5...`.
   La página de la web llega en el B8; de momento copia lo que va detrás de
   `token=`.
3. Despliega **POST /api/v1/auth/restablecer-password** → **Try it out**:

   ```json
   {
     "token": "pega-aquí-el-token",
     "password": "Otra-clave-larga-2026",
     "confirmacionPassword": "Otra-clave-larga-2026"
   }
   ```

   **Execute** → **204**. Ya puedes iniciar sesión con la contraseña nueva; la
   antigua da 401 y las sesiones que Ana tuviera abiertas se han cerrado.
4. Ejecuta lo mismo otra vez → **422** "El enlace no es válido o ha caducado.
   Solicita uno nuevo." Cada enlace sirve **una sola vez** y caduca a los
   **30 minutos**.
5. Si pides el enlace **cuatro veces** en una hora para el mismo correo, la
   cuarta da **429**. Es el límite contra abusos del paso 3.4.

### 7.7 Exportar mis datos y eliminar la cuenta (B3)

Inicia sesión con Ana y pulsa **Authorize** con su token (7.1 y 7.2).

1. **GET /api/v1/me/datos** → **Execute** → **200** con todos sus datos
   personales en JSON (cuenta, fecha de nacimiento, cuándo aceptó la
   privacidad y sesiones abiertas). Swagger muestra un enlace **Download file**:
   en la web será el botón "Descargar mis datos". Nunca incluye la contraseña.
2. **DELETE /api/v1/me** → **Try it out**, con la contraseña como confirmación:

   ```json
   { "password": "Otra-clave-larga-2026" }
   ```

   Con una contraseña incorrecta → **422**. Con la buena → **204**.
3. **GET /api/v1/me** con el mismo token (sigue vivo hasta 15 minutos) muestra
   la cuenta anonimizada: `"nombre": "Usuario"`, `"apellidos": "eliminado"` y
   un correo `eliminado-…@anonimo.invalid`. El login con el correo de Ana da
   401 y ese correo queda libre para registrarse otra vez.
4. Solo los **clientes** pueden eliminar su cuenta: con el token del
   administrador, `DELETE /api/v1/me` da **403**.

### 7.8 Catálogo y empleados (B4)

Inicia sesión como administrador y pulsa **Authorize** con su token (7.1 y 7.2).

1. **POST /api/v1/admin/servicios** → **Try it out**:

   ```json
   { "nombre": "Corte", "descripcion": "Corte y peinado", "duracionMinutos": 30, "precio": 15.00 }
   ```

   **Execute** → **201**. Con `"duracionMinutos": 45` → **422**: la duración
   tiene que ser múltiplo del intervalo de reserva (30 minutos, RN-011). Con el
   mismo nombre en mayúsculas ("CORTE") → **409**.
2. **GET /api/v1/servicios** (sin sesión, puedes pulsar **Logout** en
   Authorize) → el servicio aparece en el catálogo público.
3. **POST /api/v1/admin/empleados** (otra vez con el token del admin), con el
   `id` del servicio del paso 1:

   ```json
   { "nombre": "Luis", "apellidos": "García López", "email": "luis@ejemplo.com",
     "telefono": "611222333", "servicioIds": [1] }
   ```

   **Execute** → **201** con `"passwordTemporal": "..."`. **Apúntala: solo se
   muestra esta vez.** Si se pierde, **POST
   /api/v1/admin/empleados/{id}/password-temporal** genera otra.
4. **GET /api/v1/profesionales?servicioId=1** → Luis aparece como
   `"Luis G."`, sin correo ni teléfono.
5. Inicia sesión con `luis@ejemplo.com` y la contraseña temporal: solo puede
   usar `GET /me` y `PUT /me/password`. Cámbiala con **PUT /api/v1/me/password**
   y, con el token nuevo, **GET /api/v1/empleado/servicios** muestra el Corte.
6. Con el token del admin, **PATCH /api/v1/admin/empleados/{id}/activo** con
   `{ "activo": false }` → Luis deja de salir en `/profesionales` y ya no puede
   entrar. Cuando existan las citas (B6), si tiene citas futuras la respuesta
   será **409** con `citasFuturas`.
7. **GET /api/v1/admin/negocio** muestra la configuración. En **PUT
   /api/v1/admin/negocio**, cambiar el intervalo a 60 con el Corte de 30
   minutos da **422** con la lista de servicios que habría que cambiar antes.

### 7.9 Horarios y disponibilidad (B5)

Necesitas un servicio y un empleado que lo realice (apartado 7.8). Con el
token del administrador:

1. **PUT /api/v1/admin/horario-general** → **Try it out**. De lunes (1) a
   viernes (5) mañana y tarde, y el sábado (6) por la mañana; el domingo, que no
   aparece, queda cerrado:

   ```json
   { "dias": [
     { "diaSemana": 1, "tramos": [ { "inicio": "09:00", "fin": "14:00" }, { "inicio": "16:00", "fin": "20:00" } ] },
     { "diaSemana": 2, "tramos": [ { "inicio": "09:00", "fin": "14:00" }, { "inicio": "16:00", "fin": "20:00" } ] },
     { "diaSemana": 3, "tramos": [ { "inicio": "09:00", "fin": "14:00" }, { "inicio": "16:00", "fin": "20:00" } ] },
     { "diaSemana": 4, "tramos": [ { "inicio": "09:00", "fin": "14:00" }, { "inicio": "16:00", "fin": "20:00" } ] },
     { "diaSemana": 5, "tramos": [ { "inicio": "09:00", "fin": "14:00" }, { "inicio": "16:00", "fin": "20:00" } ] },
     { "diaSemana": 6, "tramos": [ { "inicio": "09:00", "fin": "14:00" } ] }
   ] }
   ```

   **Execute** → **200** con los 7 días y `"citasAfectadas": []`. Si dos tramos
   se solapan (09:00-14:00 y 13:00-16:00) → **422**.
2. **GET /api/v1/disponibilidad** (sin sesión) con `servicioId` y
   `profesionalId` del 7.8 y una `fecha` de un día laborable próximo
   (`2026-11-03`) → la lista de `horas` libres: 09:00, 09:30... Con una fecha
   pasada o a más de 60 días → **422**.
3. **PUT /api/v1/admin/empleados/{id}/horario** con el id del empleado, para
   que ese martes (2) solo trabaje de 10:00 a 12:00 y el lunes (1) libre:

   ```json
   { "dias": [
     { "diaSemana": 1, "tramos": [] },
     { "diaSemana": 2, "tramos": [ { "inicio": "10:00", "fin": "12:00" } ] }
   ] }
   ```

   La respuesta trae `semanaEfectiva`: lo que trabaja cada día. Repite la
   consulta del paso 2 para un martes: solo salen 10:00, 10:30, 11:00 y 11:30.
   Con `{ "dias": [] }` vuelve al horario general.
4. **POST /api/v1/admin/dias-cierre** con `{ "fecha": "2026-12-08", "motivo":
   "Festivo" }` (sin `empleadoId` cierra todo el negocio) → **201**. Repetirlo
   da **409**. La disponibilidad de ese día sale vacía, con
   `siguienteFechaConHuecos` apuntando al siguiente día con horas.
5. Si un cambio de horario o un día de cierre deja citas fuera, se guarda
   igualmente y la respuesta las lista en `citasAfectadas` para que decidas qué
   hacer con ellas.

### 7.10 Arrancar con datos de demostración (B5)

Para ver la aplicación con datos de una peluquería sin crearlos a mano, usa
**otra base de datos vacía** (los datos solo se crean si aún no hay ningún
servicio). Si quieres usar la de siempre y empezar de cero, `docker compose
down -v` la borra entera.

1. Comprueba que tu `.env` tiene `DEMO_PASSWORD` (apartado 3.2).
2. Arranca el backend con el perfil `demo`:

   ```powershell
   cd backend
   $env:SPRING_PROFILES_ACTIVE = "demo"
   .\mvnw.cmd spring-boot:run
   ```

   En el log aparece `Datos de demostración creados: 6 servicios, 3
   profesionales, el cliente cliente@demo.invalid y 2 citas.`
3. Ya puedes entrar con `luis@demo.invalid`, `ana@demo.invalid`,
   `marta@demo.invalid` (empleados) o `cliente@demo.invalid` (cliente), todos
   con tu `DEMO_PASSWORD`. Hay horario de lunes a sábado, Ana libra los lunes,
   Marta entra a las 10:00 los sábados, el negocio cierra en Navidad y Año
   Nuevo y Luis tiene una semana de vacaciones dentro de un mes. La cliente,
   Carmen, tiene una hija menor (Lucía) y una cita para cada una en el primer
   día laborable a partir de pasado mañana.
4. Para volver al modo normal, cierra la ventana de PowerShell o escribe
   `Remove-Item Env:SPRING_PROFILES_ACTIVE`.

### 7.11 Reservar y cancelar citas (B6)

Necesitas el horario y la disponibilidad del 7.9 (o los datos de demo del
7.10). Inicia sesión como **cliente** (7.3, o `cliente@demo.invalid`) y pulsa
**Authorize** con su token.

1. **GET /api/v1/disponibilidad** con el servicio, el profesional y una fecha
   próxima: elige una de las `horas`, por ejemplo `10:00`.
2. **POST /api/v1/citas**. La hora va con la fecha y el desfase de Madrid
   (`+01:00` en invierno, `+02:00` en verano):

   ```json
   { "servicioId": 1, "profesionalId": 2, "inicio": "2026-11-03T10:00:00+01:00" }
   ```

   **201** con `"estado": "CONFIRMADA"`, el `fin` y el `precio` calculados por
   el servidor y `puedeCancelar`. Repite la consulta del paso 1: las 10:00 ya no
   salen. Si repites la reserva (o la hace otro cliente) → **409** "Ese horario
   ya no está disponible". Una hora que no está en la lista también es **409**.
3. **Para un menor a cargo:** **POST /api/v1/me/beneficiarios** con
   `{ "nombre": "Lucía", "apellidos": "Pérez", "fechaNacimiento": "2018-05-10" }`
   → **201** con su `id`. Reserva igual que en el paso 2 añadiendo
   `"beneficiarioId"`. Con 14 años o más → **422** (debe crear su propia
   cuenta). Una misma persona no puede tener dos citas a la vez (**422**).
4. **GET /api/v1/me/citas** → las próximas, en orden. Con `pasadas=true`, el
   historial. **GET /api/v1/citas/{id}** → el detalle.
5. **POST /api/v1/citas/{id}/cancelacion** con `{ "motivo": "No puedo ir" }`
   (opcional) → **200** con `"estado": "CANCELADA"` y la hora vuelve a estar
   libre. Si faltan menos de 24 horas → **422** "Contacta con el negocio".
6. Con el token del administrador, **GET /api/v1/citas/{id}** muestra además el
   correo del cliente y el historial de cambios (`cambios`).

### 7.12 Agenda del empleado y gestión de reservas (B6)

**Como empleado** (con los datos de demo, `ana@demo.invalid`; si no, el
empleado del 7.8 después de cambiar su contraseña temporal):

1. **GET /api/v1/empleado/agenda** → hoy, con sus tramos y sus citas no
   canceladas. Con `desde` y `hasta` (como mucho 31 días) ves varios días; un
   día de cierre sale con `"cierre": true`.
2. **GET /api/v1/empleado/citas** → sus citas de hoy en adelante; se puede
   filtrar con `estado` (`CONFIRMADA`, `COMPLETADA`, `CANCELADA`,
   `NO_PRESENTADO`), `desde` y `hasta`.
3. **PATCH /api/v1/citas/{id}/estado** con `{ "estado": "CANCELADA", "motivo":
   "Baja del profesional" }` → **200**. Sin motivo → **422**. `COMPLETADA` o
   `NO_PRESENTADO` antes de la hora de inicio → **422**. Una cita de otro
   empleado → **404**.

**Como administrador:**

4. **GET /api/v1/admin/clientes?buscar=carmen** → los clientes que coinciden
   por nombre, correo o teléfono, con sus menores a cargo.
5. **POST /api/v1/admin/citas** con el `clienteId` y lo mismo que en el 7.11
   (y `beneficiarioId` si es para un menor) → **201**. Las reglas son las
   mismas que cuando reserva el cliente.
6. **GET /api/v1/admin/citas** con filtros (`desde`, `hasta`, `profesionalId`,
   `estado`, `clienteId`). Para cancelar una cita en cualquier momento, el
   mismo **PATCH /api/v1/citas/{id}/estado** del paso 3, con motivo.

## 8. Ver las tablas (opcional)

Con una segunda ventana de PowerShell:

```powershell
docker exec -it gestion-de-citas-db psql -U gestion_de_citas -d gestion_de_citas -c "\dt"
```

Debe listar `cita`, `cliente`, `empleado`, `negocio`, `refresh_token`,
`servicio`… y `flyway_schema_history` (el historial de migraciones).

Con **DBeaver**: Nueva conexión → PostgreSQL → Host `localhost`, Puerto `5433`,
Base de datos `gestion_de_citas`, Usuario `gestion_de_citas` y la contraseña
de tu `.env`.

## 9. Parar todo

1. En la ventana del backend pulsa **Ctrl+C** (si pregunta, responde `S`).
2. Para la base de datos y Mailpit (los datos se conservan):

   ```powershell
   cd ..
   docker compose stop
   ```

La próxima vez: abre Docker Desktop, `docker compose up -d` desde la raíz del
repositorio y `.\mvnw.cmd spring-boot:run` desde `backend`.

Si quieres **borrar la base de datos y empezar de cero**:
`docker compose down -v` (borra todos los datos).

## 10. Ejecutar las pruebas

Con Docker Desktop abierto (no hace falta tener el backend ni la base de datos
arrancados):

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones\gestion-de-citas\backend
.\mvnw.cmd verify
```

Las pruebas crean su **propio** PostgreSQL temporal con Testcontainers y lo
destruyen al acabar, así que nunca tocan tus datos. `verify` también mide la
**cobertura** con JaCoCo: abre `backend\target\site\jacoco\index.html` en el
navegador para ver qué líneas ejecutan las pruebas. Al final debes ver:

```
Tests run: 173, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Es normal ver en el log un `ERROR ... Error inesperado`: es una prueba que
comprueba a propósito que un fallo interno no muestra detalles al cliente.

GitHub Actions ejecuta estas mismas pruebas en cada PR (pestaña **Checks**).

## 11. Desde IntelliJ IDEA (alternativa)

1. File → Open → elige la carpeta `gestion-de-citas` (o `backend\pom.xml`) →
   "Open as Project".
2. Comprueba el SDK: File → Project Structure → SDK = 21.
3. Abre `backend/src/main/java/com/gestiondecitas/GestionDeCitasApplication.java`
   y pulsa el triángulo verde junto a `main`.
4. Para las pruebas: clic derecho en `backend/src/test/java` → Run 'All Tests'.

La base de datos de Docker tiene que estar arrancada (paso 4) igual que antes.

## 12. Problemas frecuentes

| Mensaje | Causa y solución |
|---|---|
| `JAVA_HOME not found` o `JAVA_HOME is set to an invalid directory` | `JAVA_HOME` no apunta al JDK 21. Repasa el paso 2 de `instalacion-entorno.md` y abre una terminal nueva. |
| `Connection to localhost:5433 refused` | La base de datos no está arrancada: abre Docker Desktop y ejecuta `docker compose up -d` desde la raíz. |
| `password authentication failed for user "gestion_de_citas"` | Cambiaste la contraseña del `.env` después de crear la base de datos (PostgreSQL solo la lee la primera vez). Ejecuta `docker compose down -v` y `docker compose up -d` (borra los datos). |
| `Port 8080 was already in use` | Ya tienes el backend abierto en otra ventana o IntelliJ. Ciérralo, o pulsa Ctrl+C en esa ventana. |
| `Bind for 0.0.0.0:5433 failed: port is already allocated` | Otro programa usa el puerto 5433. Avísame y elegimos otro. |
| `Bind for 0.0.0.0:1025 failed` o `0.0.0.0:8025 failed` | Otro programa (quizá otro Mailpit o MailHog) usa ese puerto. Ciérralo o avísame y elegimos otro. |
| No llega ningún correo a http://localhost:8025 | Mailpit no está arrancado (`docker ps` no muestra `gestion-de-citas-correo`): ejecuta `docker compose up -d`. En la ventana del backend verás `No se pudo enviar el correo de recuperación`. |
| `la autentificación password falló` (mensaje en español) | El backend está hablando con **otro** PostgreSQL instalado en Windows, no con el de Docker. Comprueba que tu `.env` tiene `DB_URL=jdbc:postgresql://localhost:5433/gestion_de_citas` (puerto 5433, como `.env.example`). |
| `Could not find a valid Docker environment` (al ejecutar las pruebas) | Docker Desktop no está abierto o no dice "Engine running". |
| `Binding to target ... app.security ... failed` con `falta JWT_SECRET` o `JWT_SECRET debe tener al menos 32 caracteres` | Falta la clave en el `.env` o es corta. Repite el paso 3.1. |
| `ADMIN_PASSWORD no cumple la política de contraseñas` | La contraseña del administrador del `.env` es corta o muy común. Cámbiala (10 caracteres o más) y vuelve a arrancar. |
| Aviso `ADMIN_EMAIL o ADMIN_PASSWORD están vacíos` | El backend arranca, pero sin administrador. Rellénalos en el `.env` (paso 3.2) y reinicia. |
| Cambié `ADMIN_PASSWORD` en el `.env` y no puedo entrar con la nueva | Es lo esperado: el administrador solo se crea la primera vez. Entra con la contraseña antigua y cámbiala con `PUT /me/password`, o borra la base de datos (`docker compose down -v`). |
| **429** al iniciar sesión | Has hecho más de 5 intentos en un minuto. Espera un minuto. |
| `.\mvnw.cmd` no se reconoce | No estás dentro de la carpeta `backend`. Ejecuta `cd backend`. |
| Error de SSL al descargar Maven o librerías | Mándame el mensaje completo; puede ser el mismo bloqueo de OpenSSL de la sección 1 de `instalacion-entorno.md`. |

Si algo falla y no está en la tabla, copia las **últimas 30 líneas** de la
ventana y mándamelas.
