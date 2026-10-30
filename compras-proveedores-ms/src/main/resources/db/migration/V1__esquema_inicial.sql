-- V1: esquema inicial de compras-proveedores-ms. Los datos fiscales se guardan cifrados (AES-256-GCM).

CREATE TABLE proveedores (
    id              UUID PRIMARY KEY,
    razon_social    VARCHAR(200) NOT NULL,
    rfc_cifrado     TEXT         NOT NULL,
    rfc_indice      VARCHAR(64)  NOT NULL UNIQUE,   -- HMAC-SHA256 del RFC normalizado; permite buscar sin descifrar
    correo_cifrado  TEXT         NOT NULL,
    estatus         VARCHAR(12)  NOT NULL CHECK (estatus IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE ordenes_compra (
    id                 UUID PRIMARY KEY,
    proveedor_id       UUID        NOT NULL REFERENCES proveedores (id),
    moneda             VARCHAR(3)  NOT NULL CHECK (moneda IN ('MXN', 'USD')),
    estatus            VARCHAR(20) NOT NULL CHECK (estatus IN ('BORRADOR', 'APROBADA', 'RECIBIDA_PARCIAL', 'RECIBIDA', 'CANCELADA')),
    motivo_cancelacion VARCHAR(200),
    version            BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE lineas_orden (
    id                 UUID PRIMARY KEY,
    orden_id           UUID          NOT NULL REFERENCES ordenes_compra (id) ON DELETE RESTRICT,
    articulo_sku       VARCHAR(40)   NOT NULL,
    cantidad_ordenada  NUMERIC(14,3) NOT NULL CHECK (cantidad_ordenada > 0),
    cantidad_recibida  NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (cantidad_recibida >= 0),
    precio_unitario    NUMERIC(14,2) NOT NULL CHECK (precio_unitario >= 0),
    CONSTRAINT uq_linea_orden_sku UNIQUE (orden_id, articulo_sku),
    CHECK (cantidad_recibida <= cantidad_ordenada)
);

-- Historial de recepciones: no se modifica ni se borra.
CREATE TABLE recepciones (
    id           UUID PRIMARY KEY,
    orden_id     UUID          NOT NULL REFERENCES ordenes_compra (id),
    articulo_sku VARCHAR(40)   NOT NULL,
    cantidad     NUMERIC(14,3) NOT NULL CHECK (cantidad > 0),
    fecha        DATE          NOT NULL
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
