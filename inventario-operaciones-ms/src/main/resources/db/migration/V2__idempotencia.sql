-- Claves Idempotency-Key de las altas. Vigencia de 24 horas, controlada por la aplicación.
CREATE TABLE idempotencia (
    clave        VARCHAR(128) PRIMARY KEY,
    huella       VARCHAR(64)  NOT NULL,
    estatus_http INTEGER      NOT NULL,
    cuerpo       TEXT,
    completada   BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_utc   TIMESTAMP WITH TIME ZONE NOT NULL
);
