-- V1: esquema inicial de reportes-ejecutivos-ms. Los cambios futuros serán nuevas migraciones.

-- Valor vigente de cada indicador. Clave natural: área, nombre y periodo.
CREATE TABLE indicadores (
    id                     UUID PRIMARY KEY,
    area                   VARCHAR(40)  NOT NULL CHECK (area IN ('VENTAS_ATENCION_CLIENTES', 'FINANZAS_CONTABILIDAD',
                                                                 'OPERACIONES', 'COMPRAS_PROVEEDORES',
                                                                 'ADMINISTRACION_LEGAL', 'DIRECCION_GENERAL')),
    nombre                 VARCHAR(120) NOT NULL,
    periodo                VARCHAR(10)  NOT NULL,
    valor                  NUMERIC(18,4) NOT NULL,
    fuente_actualizada_utc TIMESTAMPTZ  NOT NULL,
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_indicador UNIQUE (area, nombre, periodo)
);

CREATE INDEX idx_indicadores_periodo ON indicadores (periodo);

CREATE TABLE reportes (
    id         UUID PRIMARY KEY,
    periodo    VARCHAR(10) NOT NULL UNIQUE,
    estatus    VARCHAR(10) NOT NULL CHECK (estatus IN ('BORRADOR', 'CERRADO')),
    creado_utc TIMESTAMPTZ NOT NULL,
    cerrado_utc TIMESTAMPTZ,
    version    BIGINT      NOT NULL DEFAULT 0,
    CHECK (estatus <> 'CERRADO' OR cerrado_utc IS NOT NULL)
);

-- Foto del reporte al cerrarlo. Inmutable.
CREATE TABLE reporte_indicadores (
    id                     UUID PRIMARY KEY,
    reporte_id             UUID         NOT NULL REFERENCES reportes (id),
    posicion               INTEGER      NOT NULL,
    area                   VARCHAR(40)  NOT NULL,
    nombre                 VARCHAR(120) NOT NULL,
    valor                  NUMERIC(18,4) NOT NULL,
    fuente_actualizada_utc TIMESTAMPTZ  NOT NULL,
    desactualizado         BOOLEAN      NOT NULL,
    UNIQUE (reporte_id, posicion)
);

-- Refuerzo en base de datos: un reporte cerrado y su foto no se modifican ni se borran.
CREATE FUNCTION impedir_cambio_reporte_cerrado() RETURNS trigger AS $$
BEGIN
    IF OLD.estatus = 'CERRADO' THEN
        RAISE EXCEPTION 'El reporte cerrado no admite cambios';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reporte_cerrado
    BEFORE UPDATE OR DELETE ON reportes
    FOR EACH ROW EXECUTE FUNCTION impedir_cambio_reporte_cerrado();

CREATE FUNCTION impedir_cambio_foto() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La foto de un reporte cerrado no se modifica ni se elimina';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_foto_inmutable
    BEFORE UPDATE OR DELETE ON reporte_indicadores
    FOR EACH ROW EXECUTE FUNCTION impedir_cambio_foto();

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
