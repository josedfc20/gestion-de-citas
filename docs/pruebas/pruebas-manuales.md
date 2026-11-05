# Pruebas manuales

> Fase 5, paso 5e. Propuesta aprobada en `docs/propuestas/5.5-5e-pruebas-manuales.md`.
> Una hora, en tu PC, con los datos de demostración.

Las pruebas automáticas comprueban que la aplicación **hace lo que debe**. Esta
lista es para lo que una máquina no juzga bien: si **se entiende, se ve bien y
resulta cómoda**. Hazla como lo haría alguien de la peluquería o un cliente que
nunca la ha visto.

**Cómo apuntar lo que veas:** cuéntalo en el hilo del proyecto con el número de
la línea (por ejemplo, "C3: el precio sale cortado") y, si puedes, una captura.
Yo lo apunto en la tabla del final como M-1, M-2… y te digo si es un **defecto**
(se arregla en esta fase) o una **mejora** (se apunta para después de la primera
versión). Ante la duda, cuéntalo: es mejor apuntar de más.

## 0. Preparar la demo

**Todo se prueba en la web, http://localhost:5173**, como lo haría un cliente
o alguien de la peluquería. Swagger (`/swagger-ui`) es una herramienta para
programadores: lo que hagas allí no cuenta como prueba de la web y puede
dejar datos que estropeen la demo (M-1).

### La primera vez

1. Trae lo último: `git switch main` y `git pull`.
2. **Revisa tu `.env`** comparándolo con `.env.example`: cada línea que haya
   en `.env.example` tiene que estar también en el tuyo, con tu valor. Si tu
   `.env` es de una fase anterior, seguramente le faltan estas:
   - `COOKIE_SECURE=true` (vale también en tu PC: los navegadores aceptan
     estas cookies en `localhost`);
   - `ADMIN_EMAIL`, `ADMIN_PASSWORD` y `ADMIN_NOMBRE`;
   - `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM`;
   - `APP_WEB_URL=http://localhost:5173`;
   - `DEMO_PASSWORD`.

   Las contraseñas (`ADMIN_PASSWORD` y `DEMO_PASSWORD`) deben tener **entre 10
   y 64 caracteres** y no ser una contraseña común; si no, el backend no
   arranca y lo dice en el error.
3. Abre Docker Desktop y espera a que diga que está en marcha. Después, en
   una ventana **nueva** de PowerShell: `docker compose up -d`.
4. **Base de datos vacía**: los datos de demostración solo se crean si no hay
   ningún servicio. Si ya has usado la aplicación (o Swagger) antes:
   `docker compose down -v` y otra vez `docker compose up -d`.
5. Backend con la demo, en una ventana de PowerShell (las dos líneas en la
   **misma** ventana: el perfil solo vale para esa ventana):

   ```powershell
   cd backend
   $env:SPRING_PROFILES_ACTIVE = "demo"
   .\mvnw spring-boot:run
   ```

6. **Comprueba que la demo se ha cargado.** En lo que escribe el backend al
   arrancar tiene que salir una línea que empieza por `Datos de demostración
   creados: 6 servicios, 3 profesionales…`. Si no sale, el backend arrancó
   sin el perfil `demo` o la base de datos no estaba vacía: páralo
   (`Ctrl+C`) y repite los pasos 4 y 5.
7. Web, en otra ventana: `cd frontend`, `npm install` (solo la primera vez o
   tras un `git pull`) y `npm run dev`. Abre **http://localhost:5173**: en
   "Profesionales" tienen que salir Luis, Ana y Marta.

### Cada vez que vuelvas a probar

Tres cosas, en este orden:

1. Docker Desktop abierto y `docker compose up -d`.
2. El backend con el perfil `demo` (paso 5). Los datos de la demo siguen ahí;
   no hace falta vaciar la base de datos.
3. La web con `npm run dev` (paso 7, sin `npm install`).

Si la web dice "No se puede conectar con el servidor", es que el backend no
está en marcha: mira su ventana.

**Quién es quién en la demo** (todos con la contraseña `DEMO_PASSWORD` de tu
`.env`, salvo el admin):

| Cuenta | Correo | Qué tiene |
|---|---|---|
| Cliente | `cliente@demo.invalid` | Carmen Pérez Gil, con su hija Lucía (8 años) y dos citas pasado mañana o el siguiente día laborable: Lavar y peinar con Marta a las 10:00 y Corte infantil para Lucía con Ana a las 11:00 |
| Empleados | `luis@demo.invalid`, `ana@demo.invalid`, `marta@demo.invalid` | Luis tiene una semana de vacaciones dentro de 30 días; Ana libra los lunes; Marta entra a las 10:00 los sábados |
| Admin | El `ADMIN_EMAIL` de tu `.env` | Con su `ADMIN_PASSWORD` |

El negocio abre de lunes a viernes de 09:00 a 14:00 y de 16:00 a 20:00, y los
sábados de 09:00 a 14:00. Los domingos, Navidad y Año Nuevo está cerrado.

## A. Primera impresión (5 min)

Antes de leer nada más de esta lista, abre la web como si fueras un cliente
nuevo.

| # | Qué hacer | Qué deberías ver | Bien / Defecto |
|---|---|---|---|
| A1 | Mira la página de inicio 10 segundos | Sabes de qué negocio es y qué puedes hacer (ver servicios, reservar) | |
| A2 | Intenta reservar un corte de pelo para el próximo jueves **sin cuenta** | Llegas a elegir la hora y la web te pide registrarte o iniciar sesión, sin perderte | |
| A3 | Regístrate con tus datos y termina la reserva | Al iniciar sesión vuelves a la hora que elegiste | |
| A4 | Cuenta cuántos pasos y pantallas te ha llevado | Te parece razonable para una peluquería | |

## B. Textos (10 min)

| # | Qué hacer | Qué deberías ver | Bien / Defecto |
|---|---|---|---|
| B1 | En el registro, pulsa "Crear cuenta" sin rellenar nada | Cada campo dice qué falta, con frases cortas y claras | |
| B2 | Inicia sesión con una contraseña mal | "Correo o contraseña no válidos.", sin decir cuál de los dos falla | |
| B3 | Como Carmen, reserva la primera hora libre que haya dentro de las próximas 24 horas y abre su detalle | Un mensaje que explica que ya no se puede cancelar online y qué hacer (contactar con el negocio) | |
| B4 | Elige un domingo en "Horarios libres" | Dice que no hay horas ese día y ofrece el siguiente con horas | |
| B5 | Lee las páginas de "Mis citas", "Menores a mi cargo" y "Mi perfil" | Siempre se trata de **tú**, sin "usted"; sin palabras técnicas (token, API, error 500…); sin faltas | |
| B6 | Lee la política de privacidad | Se entiende qué datos se guardan y para qué | |
| B7 | Como admin, desactiva un servicio que tenga citas futuras (y vuelve a activarlo) | El aviso dice cuántas citas hay y qué pasa con ellas | |

## C. Aspecto en el PC (10 min)

Mira cada página primero en modo claro y luego en oscuro (en Windows:
Configuración → Personalización → Colores → "Elige tu modo").

| # | Qué hacer | Qué deberías ver | Bien / Defecto |
|---|---|---|---|
| C1 | Inicio, Servicios, Profesionales y Horarios libres | Todo alineado, sin textos cortados ni solapados, con espacio suficiente | |
| C2 | Los precios y las fechas en todas las páginas | Formato español: "15,00 €", "lunes, 2 de noviembre", horas de 24 h | |
| C3 | Como Carmen, "Mis citas" y el detalle de una cita | Se distingue bien el estado (Confirmada, Cancelada…) y para quién es | |
| C4 | Como Luis, la agenda de la semana | Se leen bien los días, los horarios y las citas; el día de hoy se distingue | |
| C5 | Como admin, Reservas, Servicios, Empleados, Horarios y Negocio | Las listas y los formularios se ven ordenados; los botones de peligro (desactivar, cancelar) se distinguen | |
| C6 | En modo oscuro, repasa C1 a C5 por encima | Nada queda ilegible ni con colores raros | |
| C7 | Haz la ventana del navegador estrecha poco a poco | La página se adapta sin que nada se salga; a partir de cierto ancho sale el botón ☰ | |

## D. Tu móvil (10 min)

Por defecto el login no funciona desde el móvil en local, porque la cookie de
la sesión solo viaja por HTTPS (apartado 4.10 de `arrancar-web.md`). **Solo
para esta prueba:**

1. En tu `.env`, cambia `COOKIE_SECURE=true` por `COOKIE_SECURE=false` y
   reinicia el backend.
2. Para la web (Ctrl+C) y arráncala visible en la red: `npm run dev -- --host`.
3. Abre en el móvil la dirección `Network: http://192.168.x.x:5173` que muestra
   Vite (el móvil en la misma wifi que el PC).
4. **Al terminar, vuelve a poner `COOKIE_SECURE=true`** y reinicia el backend.

| # | Qué hacer | Qué deberías ver | Bien / Defecto |
|---|---|---|---|
| D1 | Abre el menú ☰, recorre las páginas públicas y ciérralo | El menú se abre y se cierra bien y nada se sale de la pantalla | |
| D2 | Inicia sesión como Carmen | Al escribir el correo sale el teclado con @ | |
| D3 | Reserva una hora para Lucía | El selector de fecha es el del móvil y es cómodo; los botones de las horas se pulsan sin fallar | |
| D4 | Cancela la cita que acabas de reservar | El mensaje de confirmación se ve entero y los botones se distinguen | |
| D5 | En "Mi perfil", toca el campo del teléfono | Sale el teclado numérico | |
| D6 | Gira el móvil en horizontal | La página se adapta | |

## E. Solo con el teclado (5 min)

En el PC, sin tocar el ratón.

| # | Qué hacer | Qué deberías ver | Bien / Defecto |
|---|---|---|---|
| E1 | Carga el inicio y pulsa Tab una vez | Aparece "Saltar al contenido"; con Intro lleva al contenido | |
| E2 | Sigue con Tab por toda la página | Siempre se ve dónde está el foco (un borde o un cambio de color) y el orden tiene sentido: de arriba abajo y de izquierda a derecha | |
| E3 | Inicia sesión como Carmen y reserva una hora solo con Tab, flechas, Espacio e Intro | Se puede hacer de principio a fin | |
| E4 | En el detalle de la cita, pulsa "Cancelar cita" y luego "Volver" | El foco no se pierde al abrir y cerrar la confirmación | |

## F. Correos (5 min)

| # | Qué hacer | Qué deberías ver | Bien / Defecto |
|---|---|---|---|
| F1 | Cierra sesión, ve a "¿Has olvidado la contraseña?" y pídelo para `cliente@demo.invalid` | El mensaje no dice si el correo existe | |
| F2 | Abre Mailpit (http://localhost:8025) y el correo que ha llegado | Remitente y asunto claros; el texto saluda a Carmen, explica qué hacer y cuánto dura el enlace; sin faltas | |
| F3 | Pulsa el enlace del correo | Se abre "Elegir contraseña nueva" en la web | |
| F4 | Elige una contraseña nueva e inicia sesión con ella | Funciona. **Después, vuelve a poner la de `DEMO_PASSWORD`** desde "Mi perfil" → "Cambiar contraseña" para seguir con la lista | |

## G. Otro navegador (5 min)

| # | Qué hacer | Qué deberías ver | Bien / Defecto |
|---|---|---|---|
| G1 | En **Firefox**, regístrate con otro correo, reserva una hora y cancélala | Funciona igual que en Chrome o Edge y se ve igual | |
| G2 | En el navegador de tu móvil (Safari si es un iPhone), repasa D1 a D3 | Lo mismo que en el apartado D | |

## H. Situaciones raras (10 min)

| # | Qué hacer | Qué deberías ver | Bien / Defecto |
|---|---|---|---|
| H1 | Con la web abierta como Carmen, para el backend (Ctrl+C) y pulsa en "Mis citas" | Un mensaje de que no se ha podido conectar, con "Reintentar"; nada de pantallas en blanco | |
| H2 | Vuelve a arrancar el backend y pulsa "Reintentar" | La página carga sin tener que recargar ni volver a iniciar sesión | |
| H3 | Deja la pestaña abierta 20 minutos sin tocarla y luego reserva una hora | Funciona sin pedir el login otra vez (la sesión se renueva sola a los 15 minutos) | |
| H4 | Abre la web en dos pestañas con la misma sesión; cierra sesión en una y sigue usando la otra | La segunda deja de funcionar como mucho a los 15 minutos (cuando caduca su acceso) y lleva al login, sin errores raros ni pantallas en blanco | |
| H5 | Reserva una hora y pulsa el botón "Atrás" del navegador | No se vuelve a reservar la misma hora; como mucho vuelves a la página de confirmar, que avisa de que la hora ya no está libre | |
| H6 | En "Horarios libres", copia la dirección con servicio, profesional y día y ábrela en otra pestaña | Sale la misma búsqueda | |
| H7 | Escribe una dirección que no existe (`/no-existe`) | Una página "no encontrada" con un enlace para volver | |

## Defectos y mejoras encontrados

Resultado de la pasada de jose del 05/11/2026 (Chrome en Windows y Chrome
en un iPhone 15 Pro Max).

| Nº | Línea | Qué pasa | Tipo | Ficha | Estado |
|---|---|---|---|---|---|
| M-1 | 0 | Los pasos de preparación no decían que se prueba en la web y no en Swagger, ni qué faltaba en un `.env` antiguo, ni cómo saber si la demo se había cargado | Defecto | — (documento) | arreglado en esta lista |
| M-2 | D | Con el backend parado, la web decía "Ha ocurrido un error inesperado" en vez de "No se puede conectar con el servidor" | Defecto | [D-5](registro-de-defectos.md#d-5) | arreglado |
| M-3 | A1 | La página de inicio y los servicios no tienen fotos | Mejora | — | después de la v1 |
| M-4 | A1 | Ver los servicios en un carrusel. Propuesta: mantener la cuadrícula con una foto en cada tarjeta, que en el móvil se lee mejor que un carrusel | Mejora | — | después de la v1 |
| M-5 | A1–A2 (admin) | En "Nueva reserva" del admin no se entendía para qué es "Buscar cliente" ni por qué no deja confirmar sin encontrarlo | Defecto | [D-7](registro-de-defectos.md#d-7) | arreglado |
| M-6 | A3 | Parecía que se podía crear la cuenta sin marcar la privacidad | No era un defecto | — | repetido en la web, funciona: la web y el backend lo rechazan |
| M-7 | A3 | Tras registrarse e iniciar sesión no se volvía a la hora elegida | Defecto | [D-6](registro-de-defectos.md#d-6) | arreglado |
| M-8 | A3 | Un botón con un ojo para ver la contraseña que se escribe | Mejora | — | después de la v1 |
| M-9 | E1–E4 | El teclado no parecía recorrer la página | No era un defecto | — | se probó con la barra espaciadora; con el tabulador todo el bloque E funciona |

**Tipo:**
- *Defecto*: no cumple un caso de uso, una regla o un requisito, o confunde o
  impide hacer algo. Tiene su ficha (D-n) en `registro-de-defectos.md`, con la
  causa, la solución y lo aprendido. Se arregla en el 5e en una rama `fix/...`,
  con una prueba que lo reproduzca.
- *Mejora*: funciona, pero podría ser mejor. Se apunta para después de la
  primera versión.
- *No era un defecto*: al repetirlo, la aplicación hace lo que debe. Se
  deja en la tabla para que conste que se revisó.

## Mejoras para después de la primera versión

Ninguna se hace en la fase 5 (decisión del 5e). Quedan aquí para planificarlas.

- **Fotos** en la página de inicio y en cada servicio (M-3). Necesita decidir
  de dónde salen las fotos y cómo las sube el admin.
- **Tarjetas de servicio con foto** en lugar de un carrusel (M-4): en un
  carrusel solo se ve un servicio a la vez y en el móvil cuesta compararlos.
- **Ver la contraseña** con un botón de ojo en los campos de contraseña (M-8).
