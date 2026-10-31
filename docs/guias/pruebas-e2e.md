# Guía · Pruebas de punta a punta (Playwright) en tu PC

> Fase 5, pasos 5c y 5d. Para Windows, con el entorno instalado (`instalacion-entorno.md`)
> y el backend y la web funcionando (`arrancar-backend.md`, `arrancar-web.md`).
> Los comandos van en **PowerShell**.

Las pruebas de `e2e/` abren Chromium y usan la web como un usuario: se
registran, reservan, cancelan, cambian horarios… contra el backend y
PostgreSQL de verdad. Comprueban que la web y el backend encajan, algo que las
pruebas de `backend/` (sin web) y de `frontend/` (con la API simulada) no ven.

## 1. Qué arranca cada uno

| Qué | Quién lo arranca |
|---|---|
| PostgreSQL y Mailpit | Tú, con `docker compose up -d` (los mismos de siempre) |
| Backend (puerto 8080) | Playwright, con el `.jar` que compilas en el paso 3 |
| Web (puerto 4173) | Playwright: `npm run build` y `vite preview` |

Las pruebas usan **otra base de datos**, `gestion_de_citas_e2e`, en el mismo
contenedor que la de desarrollo, para no llenar la tuya de clientes y servicios
de prueba. Tienen su propio administrador (`admin-e2e@pruebas.local`) y nunca
cargan los datos de demostración.

> **Antes de ejecutarlas, para el backend y la web de desarrollo** si los
> tienes en marcha (Ctrl+C en sus ventanas). Playwright arranca los suyos y
> avisa si el puerto 8080 o el 4173 está ocupado.

## 2. Preparar la base de datos (solo la primera vez)

Con Docker Desktop abierto:

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones\gestion-de-citas
docker compose up -d
docker exec gestion-de-citas-db createdb -U gestion_de_citas gestion_de_citas_e2e
```

Si en tu `.env` el usuario (`POSTGRES_USER`) no es `gestion_de_citas`, pon el
tuyo detrás de `-U`. Las tablas las crea Flyway al arrancar el backend. El
usuario y la contraseña los leen las pruebas de tu `.env`.

## 3. Compilar el backend

Cada vez que cambie el código del backend:

```powershell
cd backend
.\mvnw -DskipTests package
cd ..
```

Deja el archivo `backend\target\gestion-de-citas-backend-0.1.0-SNAPSHOT.jar`.

## 4. Instalar Playwright (solo la primera vez)

```powershell
cd frontend
npm install
cd ..\e2e
npm ci
npx playwright install chromium
```

`npx playwright install chromium` descarga el navegador que usan las pruebas
(unos 150 MB), aparte del Chrome que ya tengas.

## 5. Ejecutar las pruebas

Desde `e2e`:

```powershell
npm test
```

Tarda alrededor de un minuto y medio: unos 20 segundos en arrancar el backend
y la web, y el resto en las pruebas. Cada prueba sale con su proyecto delante:
`[chromium]` es el navegador de escritorio y `[movil]`, el mismo Chromium con
una pantalla de móvil de 390 px (los flujos del cliente y `movil.spec.ts`). Al terminar sale una línea por prueba. Si
alguna falla, se abre en el navegador el **informe de Playwright** con la
captura de pantalla, el vídeo y la traza de la prueba que falló; también se
abre después con `npm run informe`.

Para ejecutar solo un archivo: `npx playwright test cliente`. Solo el móvil:
`npx playwright test --project=movil`.

### Ver las pruebas paso a paso

```powershell
npm run test:ui
```

Abre la interfaz de Playwright: eliges una prueba, la ejecutas y ves la página
en cada paso (con lo que la prueba ha pulsado y escrito). Es la forma más
cómoda de revisar qué hace cada una.

## 6. Qué prueba cada archivo

| Archivo | Flujo |
|---|---|
| `cliente.spec.ts` | Registro, login, servicios, profesionales, reserva en `/horarios`, "Mis citas", cancelar (la hora vuelve a estar libre) y cerrar sesión |
| `menor.spec.ts` | Alta de un menor y una reserva para él; en "Mis citas" sale "Para Lucas" |
| `empleado.spec.ts` | El admin crea un servicio y un empleado; el empleado cambia la contraseña temporal, ve "Mis servicios" y cancela con motivo la cita que le reserva un cliente |
| `horarios.spec.ts` | Horario propio de un empleado y un día de cierre: cambian las horas libres y se ofrece el siguiente día |
| `reserva-admin.spec.ts` | El admin reserva para un cliente desde `/horarios`; historial "por Administrador" |
| `recuperar-password.spec.ts` | Enlace por correo (lo lee de Mailpit), contraseña nueva, la antigua ya no sirve y el enlace no se puede reutilizar |
| `rgpd.spec.ts` | Descarga de datos (JSON sin contraseñas) y eliminación de la cuenta |
| `movil.spec.ts` | Solo en `[movil]`: ninguna página se sale de la pantalla y lo que se toca mide al menos 44 × 44 px |
| `accesibilidad.spec.ts` | axe en 33 páginas de los cuatro perfiles, en modo claro y oscuro; falla con los fallos graves y críticos |
| `concurrencia.spec.ts` | Dos navegadores confirman la misma hora a la vez: solo uno la consigue |
| `seguridad.spec.ts` | La cookie de la sesión, que el token no se guarde en el navegador, las cabeceras del backend y que ninguna respuesta lleve hashes |

Si `accesibilidad` falla, el informe dice qué regla de axe no se cumple, en
qué página y en qué elemento, con un enlace a la explicación de la regla.

Cada prueba crea sus propios datos con nombres únicos, así que pueden ir en
paralelo y repetirse sin limpiar la base de datos. Si algún día quieres
vaciarla:

```powershell
docker exec gestion-de-citas-db dropdb -U gestion_de_citas gestion_de_citas_e2e
docker exec gestion-de-citas-db createdb -U gestion_de_citas gestion_de_citas_e2e
```

## 7. En la CI

El trabajo **Punta a punta (Playwright)** de GitHub Actions hace lo mismo
después de que pasen los del backend y la web, con PostgreSQL y Mailpit como
servicios. Si falla, en la página de la ejecución hay un artefacto
`informe-playwright`: descárgalo, descomprímelo y ábrelo con
`npx playwright show-report playwright-report` desde `e2e`.

## 8. Si algo falla

| Síntoma | Causa probable |
|---|---|
| `http://localhost:8080 is already used` | Tienes el backend de desarrollo en marcha: páralo |
| `Unable to access jarfile` | Falta compilar el backend (paso 3) |
| `database "gestion_de_citas_e2e" does not exist` | Falta el paso 2 |
| `password authentication failed` | El `POSTGRES_PASSWORD` de tu `.env` no es el del contenedor |
| `recuperar-password` no encuentra el correo | Mailpit no está en marcha: `docker compose up -d` |
| `Executable doesn't exist` | Falta `npx playwright install chromium` |
