-- Cuentas por pagar a proveedores: una por orden aprobada en compras-proveedores-ms. Los pagos no se modifican ni se borran.
CREATE TABLE cuentas_por_pagar (
    id            UUID PRIMARY KEY,
    orden_id      UUID          NOT NULL UNIQUE,
    proveedor_id  UUID          NOT NULL,
    moneda        VARCHAR(3)    NOT NULL,
    monto         NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    version       BIGINT        NOT NULL DEFAULT 0,
    creado_utc    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE pagos_proveedor (
    id          UUID PRIMARY KEY,
    cuenta_id   UUID          NOT NULL REFERENCES cuentas_por_pagar (id) ON DELETE RESTRICT,
    monto       NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    fecha_pago  DATE          NOT NULL,
    referencia  VARCHAR(100),
    creado_utc  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_pagos_proveedor_cuenta ON pagos_proveedor (cuenta_id);
