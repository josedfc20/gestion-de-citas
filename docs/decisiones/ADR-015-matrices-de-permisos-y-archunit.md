# ADR-015 · Matrices de permisos y ArchUnit que fallan por olvido

- **Estado:** aceptada el 31/10/2026
- **Origen:** 5b ([huecos y permisos](../propuestas/5.2-5b-huecos-y-permisos.md))

## Contexto

Hasta la fase 5, los permisos se probaban endpoint a endpoint. Con 54
endpoints y cinco perfiles, es fácil que uno nuevo quede abierto porque nadie
escribió su prueba. Lo mismo con las reglas de arquitectura: se cumplen
mientras alguien se acuerde de ellas.

## Decisión

- **`PermisosTest`** recorre todos los endpoints con los cinco perfiles
  (anónimo, cliente, empleado, empleado con la contraseña temporal y
  admin) y comprueba
  la respuesta esperada de cada uno. **Falla si hay un endpoint que no está en
  la tabla.**
- **`Permisos.test.tsx`** hace lo mismo con todas las rutas de la web.
- **`ArquitecturaTest`** (ArchUnit) convierte en pruebas las reglas de los
  ADR-002 y ADR-009.
- Mínimo de cobertura que hace fallar la CI: backend 95 % de líneas y 85 %
  de ramas; web 90 % y 80 %.

## Alternativas descartadas

- **Seguir con pruebas sueltas por endpoint:** depende de la memoria de quien
  añade el endpoint.
- **Revisar las reglas solo en los PR:** una revisión se puede saltar; una
  prueba en la CI, no.

## Consecuencias

- Añadir un endpoint o una ruta obliga a decidir quién puede usarlo.
- Romper una regla de arquitectura deja la CI en rojo con un mensaje que
  dice cuál.
