-- Eventos de pedidos ya aplicados en inventario. La clave evita reservar o descontar dos veces
-- si Kafka reentrega un mensaje.
CREATE TABLE pedidos_procesados (
    pedido_id     UUID        NOT NULL,
    evento        VARCHAR(40) NOT NULL,
    procesado_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (pedido_id, evento)
);

-- Piezas reservadas por cada pedido confirmado. Se consumen al entregar el pedido.
CREATE TABLE reservas_pedido (
    id          BIGSERIAL PRIMARY KEY,
    pedido_id   UUID          NOT NULL,
    articulo_id UUID          NOT NULL,
    almacen_id  UUID          NOT NULL,
    cantidad    NUMERIC(14,3) NOT NULL CHECK (cantidad > 0)
);
CREATE INDEX idx_reservas_pedido ON reservas_pedido (pedido_id);
