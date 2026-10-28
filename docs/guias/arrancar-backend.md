# Guía · Arrancar y probar el backend en tu PC

> Para Windows, con el entorno ya instalado (`instalacion-entorno.md`).
> Todos los comandos se escriben en **PowerShell** (no hace falta administrador):
> menú Inicio → escribe "PowerShell" → Enter. Copia cada comando, pégalo con
> clic derecho y pulsa Enter.

Al terminar habrás comprobado tres cosas: que la base de datos arranca en
Docker, que el backend crea las tablas y responde, y que las pruebas pasan.

## 1. Traer el código a tu PC

Ve a la carpeta del repositorio:

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones\gestion-de-citas
```

- **Si el PR aún no está fusionado** (para probarlo antes de aprobarlo):

  ```powershell
  git fetch origin
  git switch feature/esqueleto-backend
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

## 3. Crear tu archivo `.env` (solo la primera vez)

El `.env` guarda las contraseñas de tu PC. **Nunca se sube a GitHub** (está en
`.gitignore`); en el repositorio solo está la plantilla `.env.example`.

```powershell
Copy-Item .env.example .env
notepad .env
```

En el Bloc de notas cambia `POSTGRES_PASSWORD=cambia-esta-contrasena` por una
contraseña tuya. Usa **solo letras y números** (sin espacios, `\`, `#` ni
comillas) para evitar problemas. Guarda (Ctrl+S) y cierra. El resto de valores
pueden quedarse como están.

## 4. Arrancar la base de datos

```powershell
docker compose up -d
```

La primera vez descarga la imagen de PostgreSQL 16 (un minuto). Comprueba que
está en marcha:

```powershell
docker ps
```

Debe aparecer `gestion-de-citas-db` con el estado **Up ... (healthy)**. Si dice
`(health: starting)`, espera unos segundos y repite.

La base de datos se queda funcionando en segundo plano; no hace falta tener
esta ventana abierta.

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
- Un poco más arriba verás `Successfully applied 2 migrations ... now at version v2`:
  es Flyway creando las tablas (V1) y el registro del negocio (V2). Solo lo hace
  la primera vez; después dirá que el esquema está al día.

**No cierres esta ventana**: el backend funciona mientras está abierta.

## 6. Comprobarlo en el navegador

Abre estas tres direcciones:

| Dirección | Qué debes ver | Qué significa |
|---|---|---|
| http://localhost:8080/actuator/health | `{"groups":["liveness","readiness"],"status":"UP"}` | El backend está vivo y conectado a la base de datos. |
| http://localhost:8080/swagger-ui.html | La página de Swagger "GestionDeCitas API" | La documentación de la API. Aún no hay endpoints: llegan desde B2. |
| http://localhost:8080/api/v1/servicios | `{"detail":"Hay que iniciar sesión.", ... "status":401, ...}` | Correcto por ahora: la seguridad provisional cierra toda la API hasta B2, y el error sale en formato Problem Details. |

## 7. Ver las tablas (opcional)

Con una segunda ventana de PowerShell:

```powershell
docker exec -it gestion-de-citas-db psql -U gestion_de_citas -d gestion_de_citas -c "\dt"
```

Debe listar `cita`, `cliente`, `empleado`, `negocio`, `refresh_token`,
`servicio`… y `flyway_schema_history` (el historial de migraciones).

Con **DBeaver**: Nueva conexión → PostgreSQL → Host `localhost`, Puerto `5432`,
Base de datos `gestion_de_citas`, Usuario `gestion_de_citas` y la contraseña
de tu `.env`.

## 8. Parar todo

1. En la ventana del backend pulsa **Ctrl+C** (si pregunta, responde `S`).
2. Para la base de datos (los datos se conservan):

   ```powershell
   cd ..
   docker compose stop
   ```

La próxima vez: abre Docker Desktop, `docker compose up -d` desde la raíz del
repositorio y `.\mvnw.cmd spring-boot:run` desde `backend`.

Si quieres **borrar la base de datos y empezar de cero**:
`docker compose down -v` (borra todos los datos).

## 9. Ejecutar las pruebas

Con Docker Desktop abierto (no hace falta tener el backend ni la base de datos
arrancados):

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones\gestion-de-citas\backend
.\mvnw.cmd verify
```

Las pruebas crean su **propio** PostgreSQL temporal con Testcontainers y lo
destruyen al acabar, así que nunca tocan tus datos. Al final debes ver:

```
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Es normal ver en el log un `ERROR ... Error inesperado`: es una prueba que
comprueba a propósito que un fallo interno no muestra detalles al cliente.

GitHub Actions ejecuta estas mismas pruebas en cada PR (pestaña **Checks**).

## 10. Desde IntelliJ IDEA (alternativa)

1. File → Open → elige la carpeta `gestion-de-citas` (o `backend\pom.xml`) →
   "Open as Project".
2. Comprueba el SDK: File → Project Structure → SDK = 21.
3. Abre `backend/src/main/java/com/gestiondecitas/GestionDeCitasApplication.java`
   y pulsa el triángulo verde junto a `main`.
4. Para las pruebas: clic derecho en `backend/src/test/java` → Run 'All Tests'.

La base de datos de Docker tiene que estar arrancada (paso 4) igual que antes.

## 11. Problemas frecuentes

| Mensaje | Causa y solución |
|---|---|
| `JAVA_HOME not found` o `JAVA_HOME is set to an invalid directory` | `JAVA_HOME` no apunta al JDK 21. Repasa el paso 2 de `instalacion-entorno.md` y abre una terminal nueva. |
| `Connection to localhost:5432 refused` | La base de datos no está arrancada: abre Docker Desktop y ejecuta `docker compose up -d` desde la raíz. |
| `password authentication failed for user "gestion_de_citas"` | Cambiaste la contraseña del `.env` después de crear la base de datos (PostgreSQL solo la lee la primera vez). Ejecuta `docker compose down -v` y `docker compose up -d` (borra los datos). |
| `Port 8080 was already in use` | Ya tienes el backend abierto en otra ventana o IntelliJ. Ciérralo, o pulsa Ctrl+C en esa ventana. |
| `Bind for 0.0.0.0:5432 failed: port is already allocated` | Tienes otro PostgreSQL instalado en Windows usando el puerto 5432. Páralo desde "Servicios" de Windows, o avísame y cambiamos el puerto. |
| `Could not find a valid Docker environment` (al ejecutar las pruebas) | Docker Desktop no está abierto o no dice "Engine running". |
| `.\mvnw.cmd` no se reconoce | No estás dentro de la carpeta `backend`. Ejecuta `cd backend`. |
| Error de SSL al descargar Maven o librerías | Mándame el mensaje completo; puede ser el mismo bloqueo de OpenSSL de la sección 1 de `instalacion-entorno.md`. |

Si algo falla y no está en la tabla, copia las **últimas 30 líneas** de la
ventana y mándamelas.
