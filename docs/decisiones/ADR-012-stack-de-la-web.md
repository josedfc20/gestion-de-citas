# ADR-012 · Web con React, Vite y TanStack Query, sin librerías de componentes

- **Estado:** aceptada el 25/10/2026 (paso 3.1); ajustada en el B7
  (31/10/2026)
- **Origen:** paso 3.1 y B7 ([web base](../propuestas/4.7-B7-web-base.md))

## Contexto

La web es para clientes (sobre todo en el móvil), empleados y el
administrador. Casi todo lo que muestra viene de la API, así que el problema
principal es pedir, guardar en caché y refrescar datos del servidor, no
manejar un estado complejo en el navegador.

## Decisión

- **React 19 + TypeScript (estricto) con Vite**, React Router para las
  rutas.
- **TanStack Query** para los datos del servidor (caché, invalidar tras
  reservar o cancelar; sin reintentos en los 4xx).
- **React Hook Form + Zod** para los formularios. Zod repite solo las reglas
  de formato; las de negocio las decide el servidor.
- **CSS Modules con variables propias** (modo oscuro, móvil primero), sin
  Tailwind ni librerías de componentes.
- **TypeScript 7 y oxlint**, que son los que trae la plantilla actual de Vite.
- En desarrollo, **Vite reenvía `/api` al backend**: la web y la API
  comparten dirección.

## Alternativas descartadas

- **Redux u otro estado global:** casi no hay estado que no venga del
  servidor.
- **Tailwind o MUI:** más dependencias y menos control del CSS, que en este
  proyecto también se quería aprender.
- **ESLint:** al empezar la web todavía no admitía TypeScript 7.
- **Llamar a `localhost:8080` con CORS:** complica la cookie del refresh
  (ADR-007) y no se parece a cómo se despliega.

## Consecuencias

- Algo más de CSS a mano.
- La web se prueba sin backend con Vitest, Testing Library y MSW.
