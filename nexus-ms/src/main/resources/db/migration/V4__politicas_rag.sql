-- Fragmentos de políticas de la empresa con su embedding (RAG).
-- CREATE EXTENSION IF NOT EXISTS no falla si ya existe, aunque nexus no sea superusuario.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE fragmentos_politica (
    id          UUID PRIMARY KEY,
    politica_id VARCHAR(100)  NOT NULL,
    indice      INTEGER       NOT NULL,
    titulo      VARCHAR(200)  NOT NULL,
    vigencia    DATE          NOT NULL,
    roles       TEXT[]        NOT NULL,
    texto       TEXT          NOT NULL,
    -- 1024 dimensiones: dimensión del modelo voyage-3
    embedding   vector(1024)  NOT NULL,
    UNIQUE (politica_id, indice)
);

-- Búsqueda por similitud coseno
CREATE INDEX idx_fragmentos_politica_embedding ON fragmentos_politica USING hnsw (embedding vector_cosine_ops);
-- Filtro por rol antes de comparar vectores
CREATE INDEX idx_fragmentos_politica_roles ON fragmentos_politica USING GIN (roles);
