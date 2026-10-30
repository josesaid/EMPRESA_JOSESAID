-- V1: esquema inicial de documentos-cumplimiento-ms. Los cambios futuros serán nuevas migraciones.

CREATE TABLE documentos (
    id                         UUID PRIMARY KEY,
    tipo                       VARCHAR(20)  NOT NULL CHECK (tipo IN ('CONTRATO', 'PERMISO', 'REGISTRO_LEGAL')),
    titulo                     VARCHAR(150) NOT NULL,
    area                       VARCHAR(100) NOT NULL,
    vence_en                   DATE,
    retencion_hasta            DATE         NOT NULL,
    eliminado                  BOOLEAN      NOT NULL DEFAULT FALSE,
    motivo_eliminacion         VARCHAR(300),
    eliminado_utc              TIMESTAMPTZ,
    revision_retencion         BOOLEAN      NOT NULL DEFAULT FALSE,
    alerta_vencimiento_emitida BOOLEAN      NOT NULL DEFAULT FALSE,
    version                    BIGINT       NOT NULL DEFAULT 0,
    CHECK (NOT eliminado OR motivo_eliminacion IS NOT NULL)
);

CREATE INDEX idx_documentos_vencimiento ON documentos (vence_en) WHERE eliminado = FALSE;

-- Versiones: inmutables. Cada cambio de contenido crea una fila nueva.
CREATE TABLE versiones_documento (
    id                 UUID PRIMARY KEY,
    documento_id       UUID         NOT NULL REFERENCES documentos (id),
    numero             INTEGER      NOT NULL CHECK (numero > 0),
    hash_sha256        VARCHAR(64)  NOT NULL,
    almacenamiento_ref VARCHAR(300) NOT NULL,
    creado_utc         TIMESTAMPTZ  NOT NULL,
    UNIQUE (documento_id, numero)
);

-- Refuerzo en base de datos: una versión publicada no se modifica ni se borra.
CREATE FUNCTION impedir_modificacion_versiones() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Las versiones publicadas no se modifican ni se eliminan';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_versiones_inmutables
    BEFORE UPDATE OR DELETE ON versiones_documento
    FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_versiones();

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
