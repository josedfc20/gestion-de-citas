# ADR-005 · Herencia por tablas y menores como beneficiarios sin cuenta

- **Estado:** aceptada el 25/10/2026
- **Origen:** pasos 2.4 ([clases](../propuestas/2.4-explicacion-diagrama-clases.md))
  y 2.6 ([base de datos](../propuestas/2.6-diseno-base-de-datos.md))

## Contexto

Cliente, empleado y administrador comparten el login (correo, contraseña,
rol), pero cada uno guarda datos distintos. Además, los menores de 14 años no
pueden tener cuenta (RN-006) y aun así necesitan citas.

## Decisión

- Tabla común **`usuario`** (correo único sin distinguir mayúsculas, hash,
  rol) más una tabla por subtipo, **`cliente`** y **`empleado`**, cuya clave
  primaria es a la vez clave foránea a `usuario` (`@Inheritance(JOINED)`).
  El administrador no tiene tabla: basta `rol = 'ADMIN'`.
- Los menores son **`beneficiario`**: pertenecen a un cliente adulto, no
  tienen cuenta y la cita indica para quién es. La edad límite es
  `edadMinimaCuenta` del negocio.

## Alternativas descartadas

- **Una sola tabla con todo:** muchas columnas vacías según el rol.
- **Tres tablas sin la común:** el login buscaría en tres sitios y el correo
  único sería difícil de garantizar.
- **Cuentas para los menores:** contra RN-006 y con más datos personales de
  menores de los necesarios.

## Consecuencias

- El login y la seguridad solo miran `usuario`.
- Un beneficiario con citas no se borra: se anonimiza y se cancelan sus
  citas futuras (ver ADR-010).
