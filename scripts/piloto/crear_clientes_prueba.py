#!/usr/bin/env python3
"""Crea clientes FICTICIOS de prueba en clientes-ms a través del gateway (piloto de Nexus).

Todos los registros se llaman "Cliente Prueba NNN" y usan RFC con prefijo PRU y dominio example.com.
Uso: python3 scripts/piloto/crear_clientes_prueba.py
"""
import getpass
import json
import sys
import urllib.error
import urllib.request

GATEWAY = "http://localhost:8080"
LOGIN = GATEWAY + "/usuarios-accesos-ms/v1/auth/login"
ALTA = GATEWAY + "/clientes-ms/v1/clientes"
CORREO_ADMIN = "admin@empresa.mx"
TOTAL = 100
ROLES = ["COMPRAS", "PAGOS", "ASISTENCIA"]
ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"


def post(url, cuerpo, token=None):
    datos = json.dumps(cuerpo).encode()
    req = urllib.request.Request(url, data=datos, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")[:200]


def rfc_ficticio(i):
    # Formato: 3 letras + 6 dígitos + 3 alfanuméricos. Único por índice, prefijo PRU.
    return "PRU000101X" + ALNUM[i // 36] + ALNUM[i % 36]


def cuerpo_cliente(i):
    return {
        "razonSocial": f"Cliente Prueba {i:03d} SA de CV",
        "rfc": rfc_ficticio(i),
        "contactoPrincipal": {
            "nombre": f"Contacto Prueba {i:03d}",
            "correo": f"prueba{i:03d}@example.com",
            "rol": ROLES[i % len(ROLES)],
        },
    }


def main():
    clave = getpass.getpass("Contraseña de admin@empresa.mx: ")
    estado, respuesta = post(LOGIN, {"correo": CORREO_ADMIN, "password": clave})
    del clave
    if estado != 200:
        print(f"Login falló con código {estado}")
        sys.exit(1)
    token = respuesta["accessToken"]

    creados, fallidos, errores = 0, 0, []
    for i in range(1, TOTAL + 1):
        estado, respuesta = post(ALTA, cuerpo_cliente(i), token)
        if estado in (200, 201):
            creados += 1
        else:
            fallidos += 1
            errores.append((i, estado, respuesta))
    print(f"Creados: {creados}  Fallidos: {fallidos}")
    for error in errores[:10]:
        print("  ", error)


if __name__ == "__main__":
    main()
