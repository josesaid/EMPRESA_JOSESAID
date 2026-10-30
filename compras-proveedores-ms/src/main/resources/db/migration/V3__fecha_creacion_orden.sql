-- Fecha de creación de la orden para el indicador mensual "órdenes del mes".
-- Las órdenes anteriores a esta migración quedan sin fecha (NULL) y no cuentan en ningún mes.
-- Las nuevas reciben now() por DEFAULT sin cambiar la entidad JPA.
ALTER TABLE ordenes_compra ADD COLUMN creado_utc TIMESTAMPTZ;
ALTER TABLE ordenes_compra ALTER COLUMN creado_utc SET DEFAULT now();
