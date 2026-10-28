-- =====================================================================
-- GestionDeCitas · Paso 2.6 · Esquema de base de datos (BORRADOR)
-- PostgreSQL 16. Propuesta de diseño pendiente de revisión: no ejecutar
-- todavía. En la fase 4 se convertirá en migraciones (Flyway).
-- =====================================================================

-- Necesaria para la restricción que impide solapes (RNF-003).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------- Configuración del negocio ----------
CREATE TABLE negocio (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre                 VARCHAR(120) NOT NULL,
    zona_horaria           VARCHAR(50)  NOT NULL DEFAULT 'Europe/Madrid',
    intervalo_minutos      INT NOT NULL DEFAULT 30 CHECK (intervalo_minutos > 0),       -- RN-010
    antelacion_max_dias    INT NOT NULL DEFAULT 60 CHECK (antelacion_max_dias > 0),     -- RN-009
    horas_min_cancelacion  INT NOT NULL DEFAULT 24 CHECK (horas_min_cancelacion >= 0),  -- RN-004
    edad_minima_cuenta     INT NOT NULL DEFAULT 14 CHECK (edad_minima_cuenta > 0)       -- RN-006
);

-- ---------- Usuarios (herencia: una tabla común + una por subtipo) ----------
CREATE TABLE usuario (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rol            VARCHAR(20)  NOT NULL CHECK (rol IN ('CLIENTE','EMPLEADO','ADMIN')),
    nombre         VARCHAR(80)  NOT NULL,
    apellidos      VARCHAR(120) NOT NULL,
    email          VARCHAR(254) NOT NULL,
    telefono       VARCHAR(20),
    password_hash  VARCHAR(100) NOT NULL,                       -- RNF-001
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_usuario_email ON usuario (lower(email)); -- email único sin distinguir mayúsculas

CREATE TABLE cliente (
    usuario_id              BIGINT PRIMARY KEY REFERENCES usuario(id),
    fecha_nacimiento        DATE NOT NULL,                     -- RN-006
    privacidad_aceptada_en  TIMESTAMPTZ NOT NULL               -- RNF-007
);

CREATE TABLE empleado (
    usuario_id             BIGINT PRIMARY KEY REFERENCES usuario(id),
    debe_cambiar_password  BOOLEAN NOT NULL DEFAULT TRUE       -- RN-005
);

CREATE TABLE token_recuperacion (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id),
    token_hash  VARCHAR(100) NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ NOT NULL,                          -- 30 min (CU-020)
    usado       BOOLEAN NOT NULL DEFAULT FALSE
);

-- Añadida en el paso 3.4 (seguridad): tokens de refresco con rotación
CREATE TABLE refresh_token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id),
    token_hash  VARCHAR(100) NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ NOT NULL,
    revocado    BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE beneficiario (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id        BIGINT NOT NULL REFERENCES cliente(usuario_id),
    nombre            VARCHAR(80)  NOT NULL,
    apellidos         VARCHAR(120) NOT NULL,
    fecha_nacimiento  DATE NOT NULL                             -- RN-007 (< 14 años, se valida en la aplicación)
);

-- ---------- Catálogo ----------
CREATE TABLE servicio (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre            VARCHAR(120) NOT NULL UNIQUE,
    descripcion       TEXT,
    duracion_minutos  INT NOT NULL CHECK (duracion_minutos > 0), -- múltiplo del intervalo: RN-011 (aplicación)
    precio            NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    activo            BOOLEAN NOT NULL DEFAULT TRUE
);

-- Relación muchos a muchos Empleado–Servicio (RF-013, RF-019)
CREATE TABLE empleado_servicio (
    empleado_id  BIGINT NOT NULL REFERENCES empleado(usuario_id),
    servicio_id  BIGINT NOT NULL REFERENCES servicio(id),
    PRIMARY KEY (empleado_id, servicio_id)
);

-- ---------- Horarios ----------
CREATE TABLE tramo_horario (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id   BIGINT NOT NULL REFERENCES negocio(id),
    empleado_id  BIGINT REFERENCES empleado(usuario_id),          -- NULL = horario general (RN-002)
    dia_semana   SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7), -- 1 = lunes
    hora_inicio  TIME NOT NULL,
    hora_fin     TIME NOT NULL,
    CHECK (hora_inicio < hora_fin)
);

CREATE TABLE dia_cierre (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id   BIGINT NOT NULL REFERENCES negocio(id),
    empleado_id  BIGINT REFERENCES empleado(usuario_id),          -- NULL = cierra todo el negocio
    fecha        DATE NOT NULL,
    motivo       VARCHAR(200)
);

-- ---------- Citas ----------
CREATE TABLE cita (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id          BIGINT NOT NULL REFERENCES cliente(usuario_id),
    empleado_id         BIGINT NOT NULL REFERENCES empleado(usuario_id),
    servicio_id         BIGINT NOT NULL REFERENCES servicio(id),
    beneficiario_id     BIGINT REFERENCES beneficiario(id),       -- NULL = la cita es para el cliente
    inicio              TIMESTAMPTZ NOT NULL,
    fin                 TIMESTAMPTZ NOT NULL,
    precio              NUMERIC(10,2) NOT NULL CHECK (precio >= 0), -- copia del precio al reservar
    estado              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA'
                        CHECK (estado IN ('CONFIRMADA','COMPLETADA','CANCELADA','NO_PRESENTADO')), -- RN-008
    motivo_cancelacion  VARCHAR(300),
    creada_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (inicio < fin),
    -- RNF-003: la base de datos impide dos citas activas solapadas del mismo profesional,
    -- aunque dos clientes reserven exactamente a la vez.
    CONSTRAINT no_solape_profesional EXCLUDE USING gist (
        empleado_id WITH =,
        tstzrange(inicio, fin, '[)') WITH &&
    ) WHERE (estado IN ('CONFIRMADA','COMPLETADA'))
);
CREATE INDEX ix_cita_empleado_inicio ON cita (empleado_id, inicio); -- agenda y disponibilidad
CREATE INDEX ix_cita_cliente_inicio  ON cita (cliente_id, inicio);  -- "Mis citas"

CREATE TABLE cambio_estado_cita (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cita_id          BIGINT NOT NULL REFERENCES cita(id) ON DELETE CASCADE, -- composición
    estado_anterior  VARCHAR(20),                                  -- NULL al crear la cita
    estado_nuevo     VARCHAR(20) NOT NULL,
    fecha            TIMESTAMPTZ NOT NULL DEFAULT now(),
    realizado_por    BIGINT NOT NULL REFERENCES usuario(id)
);
CREATE INDEX ix_cambio_cita ON cambio_estado_cita (cita_id);
