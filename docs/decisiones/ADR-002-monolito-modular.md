# ADR-002 · Monolito modular por funcionalidad, en tres capas

- **Estado:** aceptada el 26/10/2026
- **Origen:** paso 3.2 ([propuesta](../propuestas/3.2-arquitectura-backend.md)),
  comprobada con ArchUnit en el 5b

## Contexto

Un solo negocio, pocos usuarios a la vez y un único desarrollador. El código
tiene que ser fácil de encontrar y de ampliar (RNF-006) sin la carga de
operar varios servicios.

## Decisión

- **Un solo backend** Spring Boot con un único despliegue.
- **Paquetes por funcionalidad** (`auth`, `usuario`, `beneficiario`,
  `servicio`, `empleado`, `horario`, `disponibilidad`, `cita`, `negocio` y
  `shared`), no por tipo de clase.
- Dentro de cada uno, **Controller → Service → Repository**, en un solo
  sentido. El controller valida el formato, el servicio aplica las reglas y
  abre la transacción, el repository solo lee y guarda.
- La API recibe y devuelve **DTOs**, nunca entidades (si se devolviera
  `Usuario`, saldría el hash de la contraseña).
- `usuario` y `beneficiario` no dependen de `cita`: definen interfaces
  (`AportaDatosPersonales`, `CitasDeBeneficiario`) que `cita` implementa.

## Alternativas descartadas

- **Microservicios:** despliegues, red y datos repartidos sin ninguna ventaja
  para este tamaño.
- **Paquetes por capa** (`controllers/`, `services/`…): todo lo de las citas
  queda repartido por el proyecto.
- **Arquitectura hexagonal completa** (puertos y adaptadores): más
  interfaces y clases de las que el problema pide.

## Consecuencias

- `ArquitecturaTest` (ArchUnit) hace fallar la CI si un controller usa un
  repository, devuelve una entidad o si `usuario`/`beneficiario` dependen de
  `cita`.
- Si algún día hiciera falta separar un módulo, ya tiene sus límites
  marcados.
