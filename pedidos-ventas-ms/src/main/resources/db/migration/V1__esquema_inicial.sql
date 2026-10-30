-- V1: esquema inicial de pedidos-ventas-ms. Los cambios futuros serán nuevas migraciones.

-- Cotización: BORRADOR, ENVIADA, ACEPTADA, RECHAZADA o VENCIDA. pedido_id se llena al convertirla (una sola vez).
CREATE TABLE cotizaciones (
    id             UUID PRIMARY KEY,
    cliente_id     UUID        NOT NULL,
    canal          VARCHAR(10) NOT NULL CHECK (canal IN ('WEB', 'TIENDA', 'TELEFONO')),
    moneda         VARCHAR(3)  NOT NULL,
    tasa           VARCHAR(20) NOT NULL CHECK (tasa IN ('IVA_GENERAL', 'IVA_FRONTERIZO', 'IVA_EXENTO')),
    vigente_hasta  DATE        NOT NULL,
    estatus        VARCHAR(12) NOT NULL CHECK (estatus IN ('BORRADOR', 'ENVIADA', 'ACEPTADA', 'RECHAZADA', 'VENCIDA')),
    pedido_id      UUID        UNIQUE,
    version        BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE lineas_cotizacion (
    cotizacion_id   UUID          NOT NULL REFERENCES cotizaciones (id) ON DELETE CASCADE,
    posicion        INTEGER       NOT NULL,
    producto_id     UUID          NOT NULL,
    descripcion     VARCHAR(200)  NOT NULL,
    cantidad        INTEGER       NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(14,2) NOT NULL CHECK (precio_unitario >= 0),
    PRIMARY KEY (cotizacion_id, posicion)
);

-- Pedido: BORRADOR, CONFIRMADO, ENTREGADO o CANCELADO. Nunca se borra; la devolución lo deja CANCELADO.
CREATE TABLE pedidos (
    id                  UUID PRIMARY KEY,
    cliente_id          UUID        NOT NULL,
    cotizacion_id       UUID        UNIQUE REFERENCES cotizaciones (id),
    canal               VARCHAR(10) NOT NULL CHECK (canal IN ('WEB', 'TIENDA', 'TELEFONO')),
    moneda              VARCHAR(3)  NOT NULL,
    tasa                VARCHAR(20) NOT NULL CHECK (tasa IN ('IVA_GENERAL', 'IVA_FRONTERIZO', 'IVA_EXENTO')),
    estatus             VARCHAR(12) NOT NULL CHECK (estatus IN ('BORRADOR', 'CONFIRMADO', 'ENTREGADO', 'CANCELADO')),
    fecha_confirmacion  TIMESTAMPTZ,
    fecha_entrega       TIMESTAMPTZ,
    motivo_cancelacion  VARCHAR(25) CHECK (motivo_cancelacion IN ('CANCELACION_CLIENTE', 'ERROR_CAPTURA', 'DEVOLUCION_TOTAL')),
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_pedidos_cliente ON pedidos (cliente_id);
CREATE INDEX idx_pedidos_confirmacion ON pedidos (fecha_confirmacion);

CREATE TABLE lineas_pedido (
    pedido_id       UUID          NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    posicion        INTEGER       NOT NULL,
    producto_id     UUID          NOT NULL,
    descripcion     VARCHAR(200)  NOT NULL,
    cantidad        INTEGER       NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(14,2) NOT NULL CHECK (precio_unitario >= 0),
    PRIMARY KEY (pedido_id, posicion)
);

-- Outbox: el evento se escribe en la misma transacción que el cambio de negocio.
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
