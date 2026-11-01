-- Acciones de escritura preparadas por Nexus. Solo se ejecutan cuando el usuario las confirma.
CREATE TABLE acciones_pendientes (
    id          UUID PRIMARY KEY,
    usuario     VARCHAR(100)  NOT NULL,
    tipo        VARCHAR(40)   NOT NULL,
    payload     TEXT          NOT NULL,
    resumen     VARCHAR(1000) NOT NULL,
    estatus     VARCHAR(12)   NOT NULL CHECK (estatus IN ('PENDIENTE', 'CONFIRMADA', 'EXPIRADA')),
    creada_utc  TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_utc  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_acciones_pendientes_usuario ON acciones_pendientes (usuario, estatus);
