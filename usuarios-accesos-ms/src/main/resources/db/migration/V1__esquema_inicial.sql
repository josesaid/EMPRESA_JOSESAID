-- V1: esquema inicial de usuarios-accesos-ms.
-- Compatible hacia atrás: cualquier cambio posterior será una nueva migración V2, V3...

CREATE TABLE usuarios (
    id               UUID PRIMARY KEY,
    correo           VARCHAR(254) NOT NULL UNIQUE,
    nombre_completo  VARCHAR(200) NOT NULL,
    password_hash    VARCHAR(100) NOT NULL,
    estatus          VARCHAR(20)  NOT NULL CHECK (estatus IN ('ACTIVO', 'BLOQUEADO', 'BAJA')),
    intentos_fallidos INTEGER     NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
    bloqueado_hasta  TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    creado_utc       TIMESTAMPTZ  NOT NULL,
    actualizado_utc  TIMESTAMPTZ  NOT NULL
);

-- Roles asignados a cada usuario (un usuario puede tener varios)
CREATE TABLE usuario_roles (
    usuario_id UUID        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    rol        VARCHAR(30) NOT NULL CHECK (rol IN ('ADMIN', 'FINANZAS', 'CONTADOR', 'VENTAS', 'COMPRAS',
                                                  'ALMACEN', 'OPERACIONES', 'CALIDAD', 'DIRECCION', 'LEGAL')),
    PRIMARY KEY (usuario_id, rol)
);

-- Patrón outbox: los eventos se escriben en la misma transacción que el cambio de negocio
CREATE TABLE outbox_eventos (
    id            UUID PRIMARY KEY,
    tipo          VARCHAR(100) NOT NULL,
    agregado_id   VARCHAR(100) NOT NULL,
    payload       TEXT         NOT NULL,
    creado_utc    TIMESTAMPTZ  NOT NULL,
    publicado_utc TIMESTAMPTZ,
    intentos      INTEGER      NOT NULL DEFAULT 0
);

-- Índice parcial: solo se consultan los eventos pendientes de publicar
CREATE INDEX idx_outbox_pendientes ON outbox_eventos (creado_utc) WHERE publicado_utc IS NULL;
