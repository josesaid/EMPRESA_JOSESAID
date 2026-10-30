-- V1: esquema inicial de inventario-operaciones-ms. Los cambios futuros serán nuevas migraciones.

CREATE TABLE articulos (
    id                UUID PRIMARY KEY,
    sku               VARCHAR(40)  NOT NULL UNIQUE,
    nombre            VARCHAR(150) NOT NULL,
    unidad            VARCHAR(10)  NOT NULL,
    lote_obligatorio  BOOLEAN      NOT NULL DEFAULT FALSE,
    minimo            NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (minimo >= 0)
);

CREATE TABLE almacenes (
    id         UUID PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    ubicacion  VARCHAR(200) NOT NULL
);

-- Una fila por artículo y almacén. Id propio y restricción única para simplificar el control optimista.
CREATE TABLE existencias (
    id          UUID PRIMARY KEY,
    articulo_id UUID          NOT NULL REFERENCES articulos (id),
    almacen_id  UUID          NOT NULL REFERENCES almacenes (id),
    cantidad    NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    reservado   NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (reservado >= 0),
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_existencia_articulo_almacen UNIQUE (articulo_id, almacen_id),
    CHECK (reservado <= cantidad)
);

-- Movimientos inmutables. AJUSTE puede ser negativo; los demás tipos son positivos.
CREATE TABLE movimientos_inventario (
    id          UUID PRIMARY KEY,
    articulo_id UUID          NOT NULL REFERENCES articulos (id),
    almacen_id  UUID          NOT NULL REFERENCES almacenes (id),
    tipo        VARCHAR(10)   NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE', 'RESERVA')),
    cantidad    NUMERIC(14,3) NOT NULL CHECK (cantidad <> 0),
    lote        VARCHAR(60),
    referencia  VARCHAR(100),
    creado_utc  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_movimientos_lote ON movimientos_inventario (lote);

CREATE TABLE ordenes_operacion (
    id                UUID PRIMARY KEY,
    tipo              VARCHAR(12)   NOT NULL CHECK (tipo IN ('PRODUCCION', 'SERVICIO')),
    estatus           VARCHAR(10)   NOT NULL CHECK (estatus IN ('ABIERTA', 'CERRADA')),
    cantidad_objetivo NUMERIC(14,3) NOT NULL CHECK (cantidad_objetivo > 0),
    cantidad_real     NUMERIC(14,3),
    version           BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE outbox_eventos (
    id            UUID PRIMARY KEY,
    tipo          VARCHAR(100) NOT NULL,
    agregado_id   VARCHAR(100) NOT NULL,
    payload       TEXT         NOT NULL,
    creado_utc    TIMESTAMPTZ  NOT NULL,
    publicado_utc TIMESTAMPTZ,
    intentos      INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pendientes ON outbox_eventos (creado_utc) WHERE publicado_utc IS NULL;
