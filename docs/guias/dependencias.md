# Dependencias y avisos de seguridad

> Fase 7, paso 7c. Qué hacer cuando llega un PR de Dependabot o cuando la CI
> falla por un aviso de seguridad (CVE) en una librería o en una imagen.
> Propuesta: [7.3-7c-analisis-dependencias.md](../propuestas/7.3-7c-analisis-dependencias.md).

## 1. Qué vigila cada cosa

| Qué | Quién lo mira | Cuándo | Cuándo falla |
|---|---|---|---|
| Versiones nuevas de todo (Maven, npm, GitHub Actions, imágenes base y de los Compose) | **Dependabot** (`.github/dependabot.yml`) | Cada lunes | No falla: abre PR |
| Librerías de la web y de las pruebas | **`npm audit`** en la CI | Cada PR | Avisos altos o críticos |
| Imágenes de producción: Ubuntu y JRE, librerías Java del `.jar`, Alpine y Caddy | **Trivy** en el trabajo "Producción" de la CI | Cada PR | Avisos altos o críticos **con arreglo**, salvo las excepciones |
| Lo mismo, aunque nadie toque el código | **Seguridad semanal** (`.github/workflows/seguridad.yml`) | Cada lunes (y a mano desde *Actions*) | Igual; GitHub avisa por correo |
| Nuestro propio código Java y TypeScript | **CodeQL** (*Code scanning*, en la configuración de GitHub) | Cada PR | Lo que marque como alerta |

La web compilada no lleva sus librerías como archivos dentro de la imagen
(van dentro de los `.js`), así que esas las revisa `npm audit`, no Trivy.

## 2. Un PR de Dependabot

1. Mira el título: `chore(deps): bump ...`. Si es un grupo (`backend-menores`,
   `web-menores`…), son versiones menores y de parche, normalmente seguras.
   Si es una versión **mayor** (de 4.x a 5.x), lee las notas que enlaza
   Dependabot: pueden pedir cambios en el código.
2. Espera a la CI. **En verde, se fusiona.** Toda la aplicación se prueba en
   cada PR (backend, web, punta a punta, imágenes y producción), así que si
   algo se rompe la CI lo para.
3. En rojo, no se fusiona. Si es fácil de arreglar, se arregla en el mismo PR
   o en otro; si no, se cierra con un comentario de por qué (Dependabot no lo
   vuelve a abrir para esa versión).
4. Un PR de Dependabot nunca cambia `docs/api/openapi.json`; si
   `OpenApiActualizadoTest` falla, es que la librería cambió la API: se
   regenera como dice ADR-017.

## 3. La CI falla por un aviso de seguridad

El resumen de la ejecución muestra la tabla de Trivy o la salida de
`npm audit` con el aviso (CVE), la librería, la versión instalada y la que lo
arregla.

1. **Si hay una versión que lo arregla:** se actualiza la librería (o la
   imagen base) en un PR `fix(deps): ...`. Casi siempre Dependabot ya lo
   habrá propuesto.
2. **Si el arreglo no está a nuestro alcance** (por ejemplo, dentro del
   programa Caddy y aún no hay una versión de Caddy que lo lleve): se valora
   el riesgo y, si es bajo para nuestra configuración, se añade una
   excepción a `.trivyignore.yaml` con su motivo y **una caducidad de 30
   días**, en un PR que lo explique.
3. **Nunca** se quita Trivy ni `npm audit` de la CI ni se baja el nivel para
   que pase.

## 4. Las excepciones (`.trivyignore.yaml`)

Cada excepción lleva el CVE, en qué archivo está, el motivo y la fecha en
que caduca (`expired_at`). Cuando caduca, Trivy vuelve a contarla y la CI
falla. Entonces:

- Si ya hay arreglo (por ejemplo, un Caddy nuevo que Dependabot ha
  propuesto), se actualiza y **se borra la excepción**.
- Si todavía no, se renueva otros 30 días explicando por qué en el PR.

La aplicación desplegada no cambia por esto: es un aviso para revisarlo.

| Excepción | Desde | Caduca | Motivo |
|---|---|---|---|
| CVE-2026-78661 (`golang.org/x/net` en Caddy 2.11.9) | 14/11/2026 | 14/12/2026 | Aún no hay un Caddy con la librería arreglada; el fallo está en el analizador de HTML, que nuestro Caddyfile no usa (sin `templates` ni `markdown`) |

CVE-2026-116587 (`go-jose` en Caddy) no necesita excepción: aún no tiene
arreglo, así que no hace fallar la CI, pero sale en el resumen de cada
ejecución.

## 5. En la configuración de GitHub (una sola vez)

En el repositorio, *Settings → Code security* (o *Advanced Security*):

- **Dependabot alerts**: activado. Avisa de librerías con fallos aunque no
  haya PR.
- **Secret scanning** y **Push protection**: activados. Si alguien sube por
  error una contraseña o un token, GitHub lo bloquea o avisa.
- **Code scanning → CodeQL analysis → Set up → Default**: analiza nuestro
  código Java y TypeScript en cada PR.

Las alertas se ven en la pestaña *Security* del repositorio.
