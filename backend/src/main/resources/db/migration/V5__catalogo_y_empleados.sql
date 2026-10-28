-- =====================================================================
-- GestionDeCitas · V5 · Catálogo y empleados (B4)
-- =====================================================================

-- Nombre de servicio único sin distinguir mayúsculas (como el email de usuario).
ALTER TABLE servicio DROP CONSTRAINT servicio_nombre_key;
CREATE UNIQUE INDEX ux_servicio_nombre ON servicio (lower(nombre));

-- /profesionales?servicioId= busca por servicio; la clave primaria empieza por empleado.
CREATE INDEX ix_empleado_servicio_servicio ON empleado_servicio (servicio_id);
