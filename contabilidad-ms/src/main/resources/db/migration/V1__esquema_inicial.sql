-- V1: esquema inicial de contabilidad-ms. Los cambios futuros serán nuevas migraciones (V2, V3...).

CREATE TABLE cuentas_contables (
    id          UUID PRIMARY KEY,
    codigo      VARCHAR(20)  NOT NULL UNIQUE,
    nombre      VARCHAR(150) NOT NULL,
    naturaleza  VARCHAR(10)  NOT NULL CHECK (naturaleza IN ('DEUDORA', 'ACREEDORA')),
    activa      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Periodo contable en formato AAAA-MM. Un periodo cerrado no admite movimientos.
CREATE TABLE periodos (
    anio_mes  VARCHAR(7)     PRIMARY KEY,
    estatus   VARCHAR(10) NOT NULL CHECK (estatus IN ('ABIERTO', 'CERRADO')),
    version   BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE polizas (
    id          UUID PRIMARY KEY,
    anio_mes    VARCHAR(7)     NOT NULL REFERENCES periodos (anio_mes),
    tipo        VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO', 'EGRESO', 'DIARIO')),
    fecha       DATE        NOT NULL,
    estatus     VARCHAR(12) NOT NULL CHECK (estatus IN ('BORRADOR', 'APLICADA', 'REVERSADA')),
    reversa_de  UUID        REFERENCES polizas (id),
    version     BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_polizas_periodo ON polizas (anio_mes);

-- Movimientos: un cargo o un abono por fila. Son inmutables una vez aplicada la póliza.
CREATE TABLE movimientos (
    id          UUID PRIMARY KEY,
    poliza_id   UUID          NOT NULL REFERENCES polizas (id) ON DELETE RESTRICT,
    cuenta_id   UUID          NOT NULL REFERENCES cuentas_contables (id),
    cargo       NUMERIC(14,2) NOT NULL CHECK (cargo >= 0),
    abono       NUMERIC(14,2) NOT NULL CHECK (abono >= 0),
    CHECK ((cargo > 0 AND abono = 0) OR (abono > 0 AND cargo = 0))
);

CREATE INDEX idx_movimientos_poliza ON movimientos (poliza_id);

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
