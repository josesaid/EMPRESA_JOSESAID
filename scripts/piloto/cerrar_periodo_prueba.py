#!/usr/bin/env python3
"""Asigna valores FICTICIOS a los indicadores de 2026-09 (borrador) y cierra el periodo.

Usa el usuario DIRECCION de piloto. Registrar un indicador existente lo actualiza (clave: área, nombre y periodo).
Aborta si el periodo no existe o ya está CERRADO.
Uso: python3 scripts/piloto/cerrar_periodo_prueba.py
"""
import getpass
import json
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

GATEWAY = "http://localhost:8080"
LOGIN = GATEWAY + "/usuarios-accesos-ms/v1/auth/login"
INDICADORES = GATEWAY + "/reportes-ejecutivos-ms/v1/indicadores"
CIERRE = GATEWAY + "/reportes-ejecutivos-ms/v1/reportes/{periodo}/cierre"
CORREO = "direccion.piloto@example.com"
PERIODO = "2026-09"
VALORES_PRUEBA = [
    ("VENTAS_ATENCION_CLIENTES", "ventas_mes", 1250000.50),
    ("VENTAS_ATENCION_CLIENTES", "clientes_nuevos_mes", 27),
    ("FINANZAS_CONTABILIDAD", "cobrado_mes", 980000),
    ("FINANZAS_CONTABILIDAD", "polizas_mes", 210),
    ("COMPRAS_PROVEEDORES", "ordenes_mes", 64),
    ("COMPRAS_PROVEEDORES", "ordenes_canceladas_mes", 3),
    ("OPERACIONES", "movimientos_mes", 540),
]


def llamar(url, cuerpo=None, token=None):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(url, data=datos, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.read().decode(errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")


def estatus_periodo():
    r = subprocess.run(
        ["docker", "exec", "empresa-postgres-1", "psql", "-U", "postgres", "-d", "reportes",
         "-tAc", f"select estatus from reportes where periodo = '{PERIODO}'"],
        capture_output=True, text=True, check=True)
    return r.stdout.strip()


def main():
    estatus = estatus_periodo()
    if estatus != "BORRADOR":
        print(f"El periodo {PERIODO} está en estatus '{estatus or 'no existe'}'. Solo se cierra si es BORRADOR.")
        sys.exit(1)

    clave = getpass.getpass("Contraseña de direccion.piloto: ")
    estado, cuerpo = llamar(LOGIN, {"correo": CORREO, "password": clave})
    del clave
    if estado != 200:
        print(f"Login falló con código {estado}")
        sys.exit(1)
    token = json.loads(cuerpo)["accessToken"]

    ahora = datetime.now(timezone.utc).isoformat()
    actualizados = 0
    for area, nombre, valor in VALORES_PRUEBA:
        estado, cuerpo = llamar(INDICADORES, {
            "area": area, "nombre": nombre, "periodo": PERIODO,
            "valor": valor, "fuenteActualizadaUtc": ahora,
        }, token)
        if estado in (200, 201):
            actualizados += 1
        else:
            print(f"Indicador {nombre} falló con código {estado}: {cuerpo[:150]}")

    estado, cuerpo = llamar(CIERRE.format(periodo=PERIODO), None, token)
    print(f"Indicadores actualizados: {actualizados} de {len(VALORES_PRUEBA)}")
    print(f"Cierre de {PERIODO}: código {estado}")
    if estado not in (200, 201):
        print(cuerpo[:300])


if __name__ == "__main__":
    main()
