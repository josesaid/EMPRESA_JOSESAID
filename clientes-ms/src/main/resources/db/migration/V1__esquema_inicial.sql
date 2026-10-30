-- Esquema inicial del microservicio de Clientes.
-- Fechas de negocio sin hora: DATE. Instantes y auditoría: TIMESTAMPTZ (UTC).
-- Datos personales (RFC, correo, nombre, resumen) se guardan cifrados; los campos *_hash
-- permiten búsquedas exactas y detección de duplicados sin exponer el valor en claro.

CREATE TABLE prospecto (
    id               UUID PRIMARY KEY,
    nombre_cifrado   TEXT         NOT NULL,
    nombre_hash      VARCHAR(64)  NOT NULL,
    correo_cifrado   TEXT         NOT NULL,
    correo_hash      VARCHAR(64)  NOT NULL,
    rfc_cifrado      TEXT,
    rfc_hash         VARCHAR(64),
    fuente           VARCHAR(20)  NOT NULL,
    estatus          VARCHAR(20)  NOT NULL,
    fecha_registro   DATE         NOT NULL,
    fecha_conversion TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_prospecto_estatus CHECK (estatus IN ('NUEVO', 'CONTACTADO', 'CALIFICADO', 'CONVERTIDO', 'PERDIDO')),
    CONSTRAINT ck_prospecto_fuente CHECK (fuente IN ('CAMPANA', 'REFERIDO', 'EVENTO'))
);

CREATE INDEX ix_prospecto_correo_hash ON prospecto (correo_hash);
CREATE INDEX ix_prospecto_nombre_hash ON prospecto (nombre_hash);
CREATE INDEX ix_prospecto_rfc_hash ON prospecto (rfc_hash);

CREATE TABLE cliente (
    id            UUID PRIMARY KEY,
    razon_social  VARCHAR(250) NOT NULL,
    rfc_cifrado   TEXT         NOT NULL,
    rfc_hash      VARCHAR(64)  NOT NULL,
    estatus       VARCHAR(20)  NOT NULL,
    prospecto_id  UUID REFERENCES prospecto (id),
    fecha_alta    DATE         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_cliente_estatus CHECK (estatus IN ('ACTIVO', 'SUSPENDIDO', 'BAJA'))
);

-- Regla de negocio: no pueden existir dos clientes no dados de baja con el mismo RFC.
CREATE UNIQUE INDEX ux_cliente_rfc_activo ON cliente (rfc_hash) WHERE estatus <> 'BAJA';
CREATE INDEX ix_cliente_razon_social ON cliente (UPPER(razon_social));

CREATE TABLE contacto (
    id              UUID PRIMARY KEY,
    cliente_id      UUID         NOT NULL REFERENCES cliente (id),
    nombre_cifrado  TEXT         NOT NULL,
    correo_cifrado  TEXT         NOT NULL,
    correo_hash     VARCHAR(64)  NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    principal       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_contacto_rol CHECK (rol IN ('COMPRAS', 'PAGOS', 'ASISTENCIA'))
);

-- Como máximo un contacto principal por cliente (la aplicación garantiza que siempre exista uno).
CREATE UNIQUE INDEX ux_contacto_principal ON contacto (cliente_id) WHERE principal = TRUE;
CREATE INDEX ix_contacto_cliente ON contacto (cliente_id);
CREATE INDEX ix_contacto_correo_hash ON contacto (correo_hash);

CREATE TABLE interaccion (
    id               UUID PRIMARY KEY,
    cliente_id       UUID        NOT NULL REFERENCES cliente (id),
    tipo             VARCHAR(20) NOT NULL,
    fecha            TIMESTAMPTZ NOT NULL,
    responsable_id   UUID        NOT NULL,
    resumen_cifrado  TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_interaccion_tipo CHECK (tipo IN ('LLAMADA', 'CORREO', 'VISITA'))
);

CREATE INDEX ix_interaccion_cliente_fecha ON interaccion (cliente_id, fecha DESC);

CREATE TABLE consentimiento (
    id              UUID PRIMARY KEY,
    contacto_id     UUID         NOT NULL REFERENCES contacto (id),
    finalidad       VARCHAR(100) NOT NULL,
    fecha_otorgado  TIMESTAMPTZ  NOT NULL,
    medio           VARCHAR(20)  NOT NULL,
    vigencia        DATE,
    revocado_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_consentimiento_medio CHECK (medio IN ('FORMULARIO', 'CORREO', 'TELEFONO', 'PRESENCIAL'))
);

CREATE INDEX ix_consentimiento_contacto ON consentimiento (contacto_id);

-- Patrón outbox: el evento se escribe en la misma transacción que el cambio de estado.
CREATE TABLE outbox_evento (
    id              UUID PRIMARY KEY,
    tipo_agregado   VARCHAR(50)  NOT NULL,
    agregado_id     UUID         NOT NULL,
    tipo_evento     VARCHAR(80)  NOT NULL,
    payload         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    enviado_at      TIMESTAMPTZ
);

CREATE INDEX ix_outbox_pendiente ON outbox_evento (created_at) WHERE enviado_at IS NULL;
