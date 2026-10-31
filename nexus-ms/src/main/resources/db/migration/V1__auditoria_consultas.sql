-- Auditoría de consultas de Nexus. Guarda la pregunta ya redactada, nunca el texto sin filtrar.
CREATE TABLE nexus_consultas (
    id                 UUID PRIMARY KEY,
    usuario            VARCHAR(100) NOT NULL,
    pregunta_redactada TEXT NOT NULL,
    respuesta_resumen  VARCHAR(500),
    herramientas       VARCHAR(500),
    tokens_entrada     INTEGER,
    tokens_salida      INTEGER,
    creado_utc         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_nexus_consultas_usuario_creado ON nexus_consultas (usuario, creado_utc);
