-- V3: sesiones con refresh token. Solo se guarda el hash del token, nunca el token.
-- Una familia agrupa las rotaciones de una misma sesión; reusar un token rotado revoca la familia.
CREATE TABLE refresh_tokens (
    id            UUID PRIMARY KEY,
    usuario_id    UUID        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    familia_id    UUID        NOT NULL,
    hash          VARCHAR(64) NOT NULL UNIQUE,
    creado_utc    TIMESTAMPTZ NOT NULL,
    expira_utc    TIMESTAMPTZ NOT NULL,
    usado_utc     TIMESTAMPTZ,
    revocado_utc  TIMESTAMPTZ
);
CREATE INDEX ix_refresh_tokens_familia ON refresh_tokens (familia_id);
CREATE INDEX ix_refresh_tokens_usuario ON refresh_tokens (usuario_id);
