-- Usuario que creó la orden, para la regla de segregación: nadie aprueba una orden que creó.
-- Las órdenes anteriores a esta migración quedan sin creador (NULL) y solo DIRECCION puede aprobarlas.
ALTER TABLE ordenes_compra ADD COLUMN creada_por VARCHAR(255);
