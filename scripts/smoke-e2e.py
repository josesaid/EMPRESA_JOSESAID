#!/usr/bin/env python3
"""Prueba de humo del stack en tu Mac.

Hace login por el gateway (127.0.0.1:8080), usa el mismo token en un endpoint
de lectura de cada servicio y confirma que todos lo aceptan y responden.

Pide correo y contraseña en la terminal; no los guarda ni los imprime.
Solo lectura: no crea ni modifica datos.
"""
import getpass
import json
import sys
import urllib.error
import urllib.request

GATEWAY = "http://127.0.0.1:8080"

# (nombre, ruta, códigos aceptables). 403 = el servicio respondió y aplicó el rol; el usuario de prueba puede no tener ese rol.
SERVICIOS = [
    ("clientes", "/clientes-ms/v1/metricas/conversion?mes=2026-11", {200, 403}),
    ("compras", "/compras-proveedores-ms/v1/proveedores?razonSocial=prueba", {200, 403}),
    ("contabilidad", "/contabilidad-ms/v1/cuentas", {200, 403}),
    ("documentos", "/documentos-cumplimiento-ms/v1/catalogos/tipos-documento", {200, 403}),
    ("facturacion", "/facturacion-pagos-ms/v1/comprobantes?folio=PRUEBA", {200, 403}),
    ("inventario", "/inventario-operaciones-ms/v1/movimientos?articulo=00000000-0000-0000-0000-000000000000", {200, 403}),
    ("pedidos", "/pedidos-ventas-ms/v1/pedidos", {200, 403}),
    ("reportes", "/reportes-ejecutivos-ms/v1/indicadores?periodo=2026-10", {200, 403}),
]

def peticion(metodo, ruta, cuerpo=None, token=None):
    cabeceras = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        cabeceras["Authorization"] = f"Bearer {token}"
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(GATEWAY + ruta, data=datos, headers=cabeceras, method=metodo)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def main():
    correo = input("Correo: ").strip()
    password = getpass.getpass("Contraseña: ")

    codigo, cuerpo = peticion("POST", "/usuarios-accesos-ms/v1/auth/login",
                              {"correo": correo, "password": password})
    if codigo != 200:
        print(f"LOGIN FALLIDO: código {codigo}. No se probaron los servicios.")
        sys.exit(1)
    token = json.loads(cuerpo)["accessToken"]
    print("LOGIN OK")

    fallos = 0
    for nombre, ruta, aceptables in SERVICIOS:
        codigo, cuerpo = peticion("GET", ruta, token=token)
        # 404 cuenta como respuesta válida solo si lo emite el servicio (su tipo de problema termina en "no-encontrado"),
        # no si el gateway no tiene la ruta.
        ok = codigo in aceptables or (codigo == 404 and b"no-encontrado" in cuerpo)
        fallos += 0 if ok else 1
        print(f"{'OK   ' if ok else 'FALLO'} {nombre:<13} {codigo}")

    print(f"\n{len(SERVICIOS) - fallos} de {len(SERVICIOS)} servicios respondieron como se esperaba.")
    sys.exit(1 if fallos else 0)


if __name__ == "__main__":
    main()
