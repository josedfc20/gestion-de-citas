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

1. Trae lo último: `git switch main` y `git pull`.
2. Con Docker Desktop abierto: `docker compose up -d`.
3. **Base de datos vacía**, para que se creen los datos de demostración (solo
   se crean si no hay ningún servicio). Si tu base de datos ya tiene datos de
   pruebas anteriores: `docker compose down -v` y otra vez `docker compose up -d`.
4. Comprueba que tu `.env` tiene `DEMO_PASSWORD` (apartado 7.10 de
   `arrancar-backend.md`).
5. Backend con la demo, en una ventana de PowerShell:

   ```powershell
   cd backend
   $env:SPRING_PROFILES_ACTIVE = "demo"
   .\mvnw spring-boot:run
   ```

6. Web, en otra ventana: `cd frontend`, `npm install` y `npm run dev`. Abre
   **http://localhost:5173**.

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

| Nº | Línea | Qué pasa | Tipo | Ficha | Estado |
|---|---|---|---|---|---|
| | | | | | |

**Tipo:**
- *Defecto*: no cumple un caso de uso, una regla o un requisito, o confunde o
  impide hacer algo. Tiene su ficha (D-n) en `registro-de-defectos.md`, con la
  causa, la solución y lo aprendido. Se arregla en el 5e en una rama `fix/...`,
  con una prueba que lo reproduzca.
- *Mejora*: funciona, pero podría ser mejor. Se apunta para después de la
  primera versión.
