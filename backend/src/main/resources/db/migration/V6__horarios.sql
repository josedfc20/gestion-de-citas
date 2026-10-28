-- =====================================================================
-- GestionDeCitas · V6 · Horarios y disponibilidad (B5)
-- =====================================================================

-- Días en que un empleado no sigue el horario general (RN-002).
-- Un día ajustado sin tramos = ese día no trabaja.
CREATE TABLE ajuste_horario (
    empleado_id  BIGINT   NOT NULL REFERENCES empleado(usuario_id),
    dia_semana   SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7), -- 1 = lunes
    PRIMARY KEY (empleado_id, dia_semana)
);

-- Un mismo cierre no se repite (el del negocio tiene empleado_id NULL).
CREATE UNIQUE INDEX ux_dia_cierre ON dia_cierre (negocio_id, COALESCE(empleado_id, 0), fecha);

-- La disponibilidad busca los tramos de un empleado (o los generales) por día.
CREATE INDEX ix_tramo_horario_empleado_dia ON tramo_horario (empleado_id, dia_semana);
