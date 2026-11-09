# ADR-009 · Un reloj inyectable (`Clock`) para todo lo que depende de la fecha

- **Estado:** aceptada el 26/10/2026
- **Origen:** paso 3.2 ([arquitectura](../propuestas/3.2-arquitectura-backend.md)),
  vigilada con ArchUnit desde el 5b

## Contexto

Muchas reglas dependen de "ahora": cancelar hasta 24 h antes (RN-004),
reservar como mucho con 60 días de antelación, la edad mínima, las horas ya
empezadas que no se ofrecen. Con `LocalDateTime.now()` repartido por el
código, probar "faltan 23 horas" obliga a esperar o a trucos frágiles.

## Decisión

Se inyecta un **`java.time.Clock`** y todo el código pide la hora a ese
reloj. En las pruebas se usa un reloj fijo. "Hoy" se calcula siempre en la
**zona horaria del negocio**, no en la del servidor.

## Alternativas descartadas

- **`now()` directo** y pruebas con fechas relativas a hoy: pruebas que
  fallan según el día o la hora a la que se ejecutan.
- **Una librería para "congelar" el tiempo:** en Java no hace falta, `Clock`
  ya viene en el JDK.

## Consecuencias

- Las reglas de fechas se prueban de forma exacta, incluido el cambio de
  hora de verano.
- `ArquitecturaTest` hace fallar la CI si alguien llama a `now()` sin el
  `Clock`.
