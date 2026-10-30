-- V4: contraseña temporal. Las cuentas dadas de alta con contraseña inicial deben cambiarla antes de iniciar sesión.
ALTER TABLE usuarios ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;
