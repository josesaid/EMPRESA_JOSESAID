#!/usr/bin/env python3
"""Crea un usuario de PRUEBA con rol DIRECCION para el piloto de Nexus (reportes).

La contraseña se genera aquí, se guarda en ~/piloto-direccion-password.txt (permisos 600)
y nunca se imprime. Uso: python3 scripts/piloto/crear_usuario_direccion.py
"""
import getpass
import json
import os
import secrets
import sys
import urllib.error
import urllib.request

GATEWAY = "http://localhost:8080"
LOGIN = GATEWAY + "/usuarios-accesos-ms/v1/auth/login"
USUARIOS = GATEWAY + "/usuarios-accesos-ms/v1/usuarios"
CORREO_ADMIN = "admin@empresa.mx"
CORREO_PILOTO = "direccion.piloto@example.com"
ARCHIVO = os.path.expanduser("~/piloto-direccion-password.txt")


def post(url, cuerpo, token=None):
    req = urllib.request.Request(url, data=json.dumps(cuerpo).encode(), method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")[:200]


def main():
    clave_admin = getpass.getpass("Contraseña de admin@empresa.mx: ")
    estado, respuesta = post(LOGIN, {"correo": CORREO_ADMIN, "password": clave_admin})
    del clave_admin
    if estado != 200:
        print(f"Login falló con código {estado}")
        sys.exit(1)
    token = respuesta["accessToken"]

    clave_piloto = "Piloto-" + secrets.token_hex(8) + "1!"
    estado, respuesta = post(USUARIOS, {
        "correo": CORREO_PILOTO,
        "nombreCompleto": "Dirección Piloto",
        "password": clave_piloto,
        "roles": ["DIRECCION"],
    }, token)
    if estado in (200, 201):
        fd = os.open(ARCHIVO, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        with os.fdopen(fd, "w") as f:
            f.write(f"{CORREO_PILOTO}\n{clave_piloto}\n")
        print(f"Usuario creado: {CORREO_PILOTO} (rol DIRECCION).")
        print(f"Contraseña guardada en {ARCHIVO}. Guárdala en tu gestor y bórrala después.")
    else:
        print(f"No se creó el usuario. Código {estado}: {respuesta}")


if __name__ == "__main__":
    main()
