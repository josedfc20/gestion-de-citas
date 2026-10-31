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
