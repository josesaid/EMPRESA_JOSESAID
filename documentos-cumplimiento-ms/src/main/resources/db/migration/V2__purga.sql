-- V2: purga física. Un documento eliminado y con retención vencida puede purgarse: se borra el contenido,
-- y los metadatos y hashes de cada versión se conservan.

ALTER TABLE documentos
    ADD COLUMN purgado     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN purgado_utc TIMESTAMPTZ,
    ADD CONSTRAINT ck_purga_requiere_eliminacion CHECK (NOT purgado OR eliminado),
    ADD CONSTRAINT ck_purga_con_fecha CHECK (NOT purgado OR purgado_utc IS NOT NULL);
