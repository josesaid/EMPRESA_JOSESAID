-- V1: esquema inicial de facturacion-pagos-ms.
-- Compatible hacia atrás: los cambios posteriores serán nuevas migraciones (V2, V3...).

CREATE TABLE comprobantes (
    id                 UUID PRIMARY KEY,
    orden_id           UUID          NOT NULL,
    folio              VARCHAR(20)   UNIQUE,
    moneda             VARCHAR(3)    NOT NULL CHECK (moneda IN ('MXN', 'USD')),
    subtotal           NUMERIC(14,2) NOT NULL CHECK (subtotal > 0),
    iva                NUMERIC(14,2) NOT NULL CHECK (iva >= 0),
    total              NUMERIC(14,2) NOT NULL CHECK (total > 0),
    tasa_iva           NUMERIC(5,4)  NOT NULL CHECK (tasa_iva >= 0 AND tasa_iva <= 1),
    estatus            VARCHAR(20)   NOT NULL CHECK (estatus IN ('BORRADOR', 'EMITIDO', 'CANCELADO')),
    motivo_cancelacion VARCHAR(200),
    version            BIGINT        NOT NULL DEFAULT 0,
    creado_utc         TIMESTAMPTZ   NOT NULL,
    actualizado_utc    TIMESTAMPTZ   NOT NULL
);

-- Pagos aplicados a un comprobante. Un pago no se borra; las correcciones son compensaciones.
CREATE TABLE pagos (
    id              UUID PRIMARY KEY,
    comprobante_id  UUID          NOT NULL REFERENCES comprobantes (id) ON DELETE RESTRICT,
    monto           NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    fecha_pago      DATE          NOT NULL,
    referencia      VARCHAR(100),
    creado_utc      TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_pagos_comprobante ON pagos (comprobante_id);

-- Patrón outbox
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
