-- Asigna fecha a las órdenes creadas antes de V3, que no tienen creado_utc.
-- Se usa el momento de esta migración: esas órdenes cuentan en el mes en que se aplicó.
UPDATE ordenes_compra SET creado_utc = now() WHERE creado_utc IS NULL;
