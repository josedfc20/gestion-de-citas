-- =====================================================================
-- GestionDeCitas · V4 · Cuenta y RGPD (B3)
-- Eliminar la cuenta anonimiza los datos en lugar de borrar la fila (paso 2.6),
-- porque las citas son historial del negocio.
-- =====================================================================

-- Distingue una cuenta eliminada (no se reactiva nunca) de una desactivada.
ALTER TABLE usuario ADD COLUMN eliminado_en TIMESTAMPTZ;

-- La fecha de nacimiento es un dato personal: se vacía al anonimizar.
ALTER TABLE cliente ALTER COLUMN fecha_nacimiento DROP NOT NULL;

-- Para invalidar los enlaces de recuperación pendientes de un usuario (CU-020).
CREATE INDEX ix_token_recuperacion_usuario ON token_recuperacion (usuario_id);
