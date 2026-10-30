-- V2: área por usuario y rol JEFE_AREA (lectura de indicadores de su área en reportes-ejecutivos-ms).
ALTER TABLE usuarios ADD COLUMN area VARCHAR(40);

ALTER TABLE usuario_roles DROP CONSTRAINT usuario_roles_rol_check;
ALTER TABLE usuario_roles ADD CONSTRAINT usuario_roles_rol_check
    CHECK (rol IN ('ADMIN', 'FINANZAS', 'CONTADOR', 'VENTAS', 'COMPRAS',
                   'ALMACEN', 'OPERACIONES', 'CALIDAD', 'DIRECCION', 'LEGAL', 'JEFE_AREA'));
