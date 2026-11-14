# Registro de defectos

> Fase 5, paso 5e. Aquí queda cada fallo que se encuentra en el proyecto, lo
> que lo causó, cómo se arregló y qué aprendimos. No se borra nada: es la
> memoria de los errores para no repetirlos.

## Cómo se usa

Vale para cualquier fallo: una prueba automática que falla (siempre o a veces),
algo que sale en la prueba manual (`pruebas-manuales.md`) o un error que
encuentra alguien usando la aplicación.

1. **Se apunta en cuanto aparece.** Se abre una ficha nueva con el siguiente
   número (D-1, D-2…), la fecha, dónde salió y qué se vio. Estado: *abierto*.
   Si viene de la prueba manual, la ficha enlaza la línea (por ejemplo, M-2 en
   B3).
2. **Se busca la causa** y se reproduce con una prueba que falle. Si no se
   puede automatizar (un texto que confunde, algo que se ve mal), la prueba es
   la línea de la lista manual.
3. **Se arregla en una rama `fix/...`** con su PR. La rama es solo el sitio de
   trabajo: al fusionarla desaparece, y lo que queda es esta ficha y la prueba.
4. **Se completa la ficha**: causa, solución, PR, la prueba que impide que
   vuelva y lo aprendido. Estado: *arreglado*.

Una prueba que falla a veces es un defecto como cualquier otro: no se
reintenta ni se desactiva (plan de pruebas, §4). Relanzar la CI sirve para
confirmar que es intermitente, no para darlo por bueno.

**Defecto o mejora.** Un defecto no cumple un CU, una RN o un RNF, o confunde o
impide hacer algo, y se arregla. Una mejora funciona, pero podría ser mejor:
va en la lista de mejoras de `pruebas-manuales.md` para después de la primera
versión y no tiene ficha aquí.

## Resumen

| Nº | Fecha | Dónde salió | Qué pasaba | Estado |
|---|---|---|---|---|
| [D-1](#d-1) | 31/10/2026 | Pruebas de la web (5a) | Una prueba fallaba 3 de cada 9 veces | arreglado (PR #15) |
| [D-2](#d-2) | 31/10/2026 | `movil.spec.ts` (5d) | Los enlaces del menú del móvil medían 40 px | arreglado (PR #18) |
| [D-3](#d-3) | 31/10/2026 | `accesibilidad.spec.ts` (5d) | Dos `aria-label` no contenían el texto visible | arreglado (PR #18) |
| [D-4](#d-4) | 31/10/2026 | CI del PR #19 (5e) | Tres pruebas de punta a punta fallaron una vez | arreglado (PR #20) |
| [D-5](#d-5) | 05/11/2026 | Prueba manual, M-2 (5e) | Con el backend parado salía "error inesperado" | arreglado (PR #21) |
| [D-6](#d-6) | 05/11/2026 | Prueba manual, M-7 (5e) | Al registrarse se perdía la hora elegida | arreglado (PR #21) |
| [D-7](#d-7) | 05/11/2026 | Prueba manual, M-5 (5e) | "Buscar cliente" del admin no se entendía | arreglado (PR #21) |
| [D-8](#d-8) | 13/11/2026 | Revisión de las capturas del 6c | La captura en modo oscuro salía a mitad de una transición | arreglado (PR #25) |
| [D-9](#d-9) | 14/11/2026 | Revisión del código para la fase 7 (7.0) | Detrás del proxy, todos compartirían el límite de intentos de login | arreglado (7a) |

## Fichas

### D-1

**La prueba "desactiva un servicio tras confirmarlo" fallaba a veces**

- **Dónde salió:** las pruebas de la web, al lanzarlas varias veces seguidas
  (paso 5a). Fallaba 3 de cada 9 veces.
- **Qué se vio:** tras desactivar, la página volvía a mostrar "Desactivar el
  servicio".
- **Causa:** era un defecto de la prueba, no de la web. Tras desactivar, la
  página vuelve a pedir el servicio (`invalidarServicios`). El servidor
  simulado lo devolvía todavía activo, y si esa respuesta llegaba antes de que
  la prueba buscase el título, la página volvía a su estado anterior.
- **Solución:** el servidor simulado recuerda el cambio, como haría el backend.
- **PR:** #15 (5a).
- **Prueba que lo vigila:** la misma prueba, ahora estable.
- **Lo aprendido:** un servidor simulado tiene que comportarse como el de
  verdad también después de un cambio, no solo en la primera respuesta.

### D-2

**Los enlaces del menú del móvil eran demasiado pequeños para el dedo**

- **Dónde salió:** `movil.spec.ts`, la prueba de tamaño de toque (paso 5d).
- **Qué se vio:** los enlaces del menú medían 40 px de alto, por debajo de los
  44 px recomendados.
- **Causa:** `.enlace` de `Marco.module.css` solo tenía el relleno; los botones
  ya tenían `min-height`.
- **Solución:** `min-height: 2.75rem` en los enlaces del menú, como los botones.
- **PR:** #18 (5d).
- **Prueba que lo vigila:** `movil.spec.ts`, "lo que se toca mide al menos 44 × 44 px".
- **Lo aprendido:** lo que se toca necesita un tamaño mínimo explícito; el
  relleno depende del tamaño de la letra y no lo garantiza.

### D-3

**El nombre de dos controles no contenía su texto visible**

- **Dónde salió:** `accesibilidad.spec.ts`, regla de axe
  `label-content-name-mismatch`, grave (paso 5d).
- **Qué se vio:** en "Ver ficha" (lista de empleados) y "Añadir tramo" (editor
  de horarios), quien usa el control por voz diciendo lo que ve no podía
  pulsarlos (WCAG 2.5.3).
- **Causa:** los `aria-label` decían "Ver la ficha de…" y "Añadir un tramo al…".
- **Solución:** `aria-label` "Ver ficha de…" y "Añadir tramo al…".
- **PR:** #18 (5d).
- **Prueba que lo vigila:** `accesibilidad.spec.ts` en las páginas del admin,
  y las pruebas de la web de esas páginas.
- **Lo aprendido:** un `aria-label` tiene que empezar por el texto que se ve.

### D-4

**Pruebas de punta a punta que rellenaban el campo de la página anterior**

- **Dónde salió:** la CI del PR #19, que solo cambiaba documentos. Al
  relanzarla sin cambiar nada pasó entera, y en el PC pasaban las 25. Volvió a
  fallar igual en la CI de `main` al fusionar el PR #19.
- **Qué se vio:** tres pruebas pulsaban el botón y el aviso de éxito no salía:
  - `horarios.spec.ts`, "Añadir día de cierre", en el escritorio;
  - `recuperar-password.spec.ts`, "Enviar enlace", en el escritorio y en el
    móvil.
- **Cómo se encontró la causa:** al frenar Chromium 30 veces con
  `Emulation.setCPUThrottlingRate` (como una CI muy cargada), fallaron 3 de 6
  ejecuciones. La página quedaba con el campo vacío y su error: "Elige el
  día." y "Escribe tu correo electrónico.".
- **Causa:** era un defecto de las pruebas, no de la aplicación. React Router
  cambia de página dentro de una transición, así que la dirección cambia
  antes de que se pinte la página nueva. La prueba rellenaba el campo justo
  después de pulsar el enlace, y la página anterior tenía uno con la misma
  etiqueta:
  - en Horarios libres también hay un campo "Día";
  - en Iniciar sesión también hay un "Correo electrónico".

  Se escribía en ese campo, que desaparecía al llegar la página nueva, y el
  formulario se enviaba vacío. Solo pasaba si el navegador iba lento, por eso
  en el PC no salía.
- **Solución:**
  - `irA` (menú) y el nuevo `pulsarEnlace` (enlaces de la página) esperan a
    que se vea el título h1 de la página nueva antes de seguir. Se usan en
    todas las navegaciones que van seguidas de rellenar algo.
  - Para la próxima vez, si una prueba falla adjunta las peticiones a la API
    con su estado y lo que tardaron (`utilidades/prueba.ts`). La CI guarda
    también el registro del backend (`e2e/registros/backend.log`).
  - Modo `E2E_CPU_LENTA=<n>` para lanzar las pruebas con Chromium n veces más
    lento y sacar a la luz estas prisas en el PC (guía `pruebas-e2e.md`).
- **PR:** #20.
- **Prueba que lo vigila:** las mismas pruebas, que con `E2E_CPU_LENTA=30`
  fallaban 3 de 6 veces y ahora pasan 6 de 6. Además, la batería entera pasa
  con `E2E_CPU_LENTA=10` (25 de 25) y 10 veces seguidas a velocidad normal
  (250 de 250).
- **Lo aprendido:**
  - Después de pulsar un enlace, hay que esperar a algo que **solo** tenga la
    página nueva (su título) antes de tocar nada.
  - Que la dirección haya cambiado no basta.
  - Una prueba que solo falla en la CI suele ser una prisa: frenar el
    navegador la reproduce en el PC.

### D-5

**Con el backend parado, la web decía "Ha ocurrido un error inesperado"**

- **Dónde salió:** la prueba manual de jose, bloque D, en el móvil (M-2).
  Al reiniciar el backend y la web volvió a funcionar.
- **Qué se vio:** "Cargando" y después "Ha ocurrido un error inesperado.
  Inténtalo de nuevo más tarde." en Servicios, Horarios y el resto de
  páginas que piden datos.
- **Causa:** la web no habla directamente con el backend, sino con el proxy
  que tiene delante (Vite en el PC, el servidor web en producción). Con el
  backend parado, el proxy responde él mismo con un 502 y un texto que no es
  Problem Details. `errorDesdeRespuesta` lo trataba como un error 5xx del
  backend y mostraba el mensaje genérico. El aviso de "sin conexión" solo
  salía si fallaba la red entre el navegador y el proxy.
- **Solución:** un 502, 503 o 504 **sin** Problem Details se trata como
  falta de conexión: "No se puede conectar con el servidor. Comprueba tu
  conexión e inténtalo de nuevo.". Si el backend responde un 503 con su
  Problem Details (por ejemplo, el correo no responde), se conserva su
  mensaje.
- **PR:** #21.
- **Prueba que lo vigila:** `cliente.test.ts`, "un 502/503/504 del proxy sin
  Problem Details es un error de red" (fallaban las tres antes del arreglo) y
  "un 503 con Problem Details del backend conserva su mensaje".
- **Lo aprendido:** entre la web y el backend hay un proxy, y cuando el
  backend falta es el proxy quien responde. Las pruebas simulaban la red
  caída, pero no el backend caído detrás del proxy.

### D-6

**Al registrarse desde una reserva se perdía la hora elegida**

- **Dónde salió:** la prueba manual de jose, A3 (M-7).
- **Qué se vio:** sin cuenta, al elegir una hora la web lleva a "Iniciar
  sesión". Desde allí, "Regístrate", crear la cuenta e iniciar sesión ya no
  volvía a la hora elegida, y había que buscarla otra vez.
- **Causa:** el login recibe la hora en el estado de la navegación
  (`desde`), pero el enlace "Regístrate" no lo pasaba al registro, y el
  registro, al terminar, volvía al login sin él. Las pruebas cubrían "login
  → hora" y "registro → login" por separado, nunca el camino entero.
- **Solución:** el enlace "Regístrate" lleva `desde` al registro, y el
  registro lo devuelve al login junto con el aviso de cuenta creada.
- **PR:** #21.
- **Prueba que lo vigila:** `Registro.test.tsx`, "quien llega al login desde
  una hora y se registra vuelve al login con esa hora" (fallaba antes del
  arreglo).
- **Lo aprendido:** un dato que viaja entre páginas hay que seguirlo por
  todos los caminos que puede tomar quien usa la web, no solo por el más
  corto.

### D-7

**"Buscar cliente" en la reserva del admin no se entendía**

- **Dónde salió:** la prueba manual de jose, al reservar desde la zona del
  admin (M-5).
- **Qué se vio:** no se entendía para qué era el campo ni por qué no dejaba
  confirmar la cita sin encontrar al cliente.
- **Causa:** es intencionado: el admin solo reserva para clientes con
  cuenta (CU-019), porque así el cliente ha aceptado la política de
  privacidad. Pero la página no lo decía; la
  ayuda del campo solo explicaba por qué datos buscar.
- **Solución:** la ayuda del campo dice "Solo clientes con cuenta. Búscalo
  por nombre, apellidos, correo o teléfono; si aún no tiene, debe
  registrarse antes.". La ayuda va unida al campo (`aria-describedby`), así
  que también la oye quien usa un lector de pantalla.
- **PR:** #21.
- **Prueba que lo vigila:** `ZonaAdmin.test.tsx`, "antes de buscar explica que
  solo se reserva para clientes con cuenta" (fallaba antes del arreglo).
- **Lo aprendido:** una regla del negocio que limita lo que se puede hacer
  tiene que estar escrita en la pantalla donde se nota el límite.

### D-8

**La captura en modo oscuro sacaba las horas libres con fondo claro**

- **Dónde salió:** jose, al revisar las capturas del 6c (PR #25), el
  13/11/2026: en `horarios-oscuro.png` las horas se veían con fondo claro y
  texto verde azulado sobre la página oscura, y costaba leerlas.
- **Qué se vio:** el fondo de las horas era (206, 221, 219), casi el del modo
  claro (#e6f2f0), cuando en oscuro debe ser #123a35.
- **Causa:** no era un fallo de la web, sino del script de capturas. Primero
  saca la captura en claro y luego cambia a oscuro con `emulateMedia` sin
  recargar. Las horas son enlaces con `transition: background-color 0.15s`, así
  que la captura se hacía a mitad del cambio de color. Quien abre la web en
  modo oscuro la ve bien desde el principio: sus horas tienen fondo
  #123a35 y texto #2dd4bf (contraste 6,7:1), y axe ya lo comprobaba en
  `accesibilidad.spec.ts`.
- **Solución:** `capturar` usa `page.screenshot({ animations: 'disabled' })`,
  que termina las transiciones antes de sacar la imagen. Capturas
  regeneradas.
- **PR:** #25.
- **Prueba que lo vigila:** la propia captura `horarios-oscuro.png`; el
  color de la web en oscuro lo vigila `accesibilidad.spec.ts`.
- **Lo aprendido:** una captura sacada justo después de cambiar algo en la
  página puede pillar una animación a medias; las capturas se hacen siempre
  con las animaciones desactivadas.

### D-9

**Detrás del proxy, todos los usuarios compartirían el límite de intentos**

- **Dónde salió:** al revisar el código para el plan de la fase 7 (7.0), el
  14/11/2026. No llegó a pasar en producción: se encontró antes de desplegar.
- **Qué se vio:** `AuthController` pasa `peticion.getRemoteAddr()` al límite
  de intentos de login (5 por minuto) y de recuperar contraseña (10 por hora
  por IP). Detrás de Caddy, esa IP es siempre la del proxy.
- **Qué habría pasado:** todos los usuarios compartirían el mismo límite;
  con 5 personas equivocándose a la vez, nadie más podría iniciar sesión
  durante un minuto. En local no se notaba porque no hay proxy.
- **Causa:** el límite se diseñó (B2) pensando en el backend expuesto
  directamente; el proxy llegó con el despliegue.
- **Solución:** el perfil `prod` activa `server.forward-headers-strategy:
  native`: Tomcat toma la IP de `X-Forwarded-For`, pero solo cuando la
  petición llega de una red interna (Caddy, en la red de Docker). Caddy no
  reenvía la `X-Forwarded-For` que mande el navegador, así que no se puede
  falsear. El código de `AuthController` no cambia.
- **PR:** el del 7a.
- **Prueba que lo vigila:** `PerfilProduccionTest` (backend en un puerto de
  verdad con el perfil `prod`): una IP agota sus 5 intentos y otra IP sigue
  pudiendo entrar. Falla sin el arreglo. Comprobado también con el Compose de
  producción: una `X-Forwarded-For` inventada por el cliente no cambia nada.
- **Lo aprendido:** todo lo que depende de quién hace la petición (límites,
  registros) hay que revisarlo cuando aparece un proxy delante.

