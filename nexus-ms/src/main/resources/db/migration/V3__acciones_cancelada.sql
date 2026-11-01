-- Permite que el usuario cancele una acción pendiente antes de confirmarla.
-- Se crea V3 y no se edita V2 porque V2 ya está aplicada en algunas bases (Flyway valida el checksum).
ALTER TABLE acciones_pendientes DROP CONSTRAINT IF EXISTS acciones_pendientes_estatus_check;
ALTER TABLE acciones_pendientes ADD CONSTRAINT acciones_pendientes_estatus_check
    CHECK (estatus IN ('PENDIENTE', 'CONFIRMADA', 'EXPIRADA', 'CANCELADA'));
