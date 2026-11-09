# ADR-016 · Playwright para las pruebas de punta a punta

- **Estado:** aceptada el 31/10/2026
- **Origen:** 5.0 ([plan](../propuestas/5.0-plan-de-pruebas.md), decisiones 1 y
  2) y 5c ([E2E](../propuestas/5.3-5c-pruebas-e2e.md))

## Contexto

Las pruebas del backend no ven la web, y las de la web usan una API
simulada. Hace falta probar los flujos principales con todo junto: navegador,
web, backend y base de datos. También la reserva simultánea desde dos
navegadores (RNF-003) y la vista móvil (RNF-008).

## Decisión

- **Playwright** en una carpeta propia **`e2e/`** del monorepo, con su
  `package.json` y la versión fijada. Solo Chromium, con un proyecto `movil`
  a 390 px.
- `playwright.config.ts` **arranca siempre su propio backend y su propia
  web**, con su base de datos (`gestion_de_citas_e2e`) y sus variables;
  nunca reutiliza los de desarrollo ni el perfil `demo`.
- Cada prueba **crea sus datos por la API** con nombres únicos y usa la web
  solo para el flujo que prueba, así se pueden ejecutar en paralelo.
- Selectores por rol y texto visible, sin `data-testid`. axe comprueba la
  accesibilidad de todas las páginas en modo claro y oscuro.

## Alternativas descartadas

- **Cypress:** más lento en la CI y peor con varias pestañas, que hacen falta
  para la reserva simultánea.
- **Todo dentro de Docker con una imagen del backend:** más parecido a
  producción, pero más lento de montar; se verá en la fase 7.

## Consecuencias

- Una prueba intermitente se trata como un defecto (D-4): se arregla, nunca
  se reintenta ni se desactiva.
- Firefox y Safari solo se prueban a mano.
