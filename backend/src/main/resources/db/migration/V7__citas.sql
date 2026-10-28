-- =====================================================================
-- GestionDeCitas · V7 · Citas (B6)
-- =====================================================================

-- Un beneficiario con citas no se borra: se anonimiza (RGPD, datos de menores).
ALTER TABLE beneficiario ADD COLUMN eliminado_en TIMESTAMPTZ;
ALTER TABLE beneficiario ALTER COLUMN fecha_nacimiento DROP NOT NULL;
CREATE INDEX ix_beneficiario_cliente ON beneficiario (cliente_id);

-- Gestión de reservas del administrador (filtro por fechas).
CREATE INDEX ix_cita_inicio ON cita (inicio);
CREATE INDEX ix_cambio_estado_realizado_por ON cambio_estado_cita (realizado_por);
