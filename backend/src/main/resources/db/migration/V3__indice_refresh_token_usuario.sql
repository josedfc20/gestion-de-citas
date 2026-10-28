-- =====================================================================
-- GestionDeCitas · V3 · Índice para cerrar todas las sesiones de un usuario
-- (al cambiar la contraseña o al detectar un refresh token reutilizado, B2).
-- =====================================================================
CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id);
