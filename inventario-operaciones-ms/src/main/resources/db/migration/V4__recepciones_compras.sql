-- Recepciones de compras ya aplicadas en inventario. Evita sumar dos veces una misma recepción si Kafka la reentrega.
CREATE TABLE recepciones_procesadas (
    evento_id     UUID PRIMARY KEY,
    procesado_utc TIMESTAMP WITH TIME ZONE NOT NULL
);
