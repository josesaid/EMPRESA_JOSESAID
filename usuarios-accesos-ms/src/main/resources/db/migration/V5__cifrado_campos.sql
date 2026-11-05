-- Cifrado de columnas personales (AES-256-GCM) y su índice HMAC para buscar por correo.
-- Los valores existentes en texto plano los cifra MigracionCifradoUsuarios al arrancar.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_correo_key;
ALTER TABLE usuarios ALTER COLUMN correo TYPE VARCHAR(400);
ALTER TABLE usuarios ALTER COLUMN nombre_completo TYPE VARCHAR(400);
ALTER TABLE usuarios ADD COLUMN correo_indice VARCHAR(64);
CREATE UNIQUE INDEX ux_usuarios_correo_indice ON usuarios (correo_indice);
