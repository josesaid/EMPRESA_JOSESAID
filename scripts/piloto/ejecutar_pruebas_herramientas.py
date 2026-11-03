#!/usr/bin/env python3
"""Prueba real de las herramientas de Nexus por área, contra los servicios levantados con docker compose.

Cada caso se pregunta con un usuario del rol que sí puede usar la herramienta (creados con
crear_datos_prueba_por_rol.py y guardados en ~/piloto-roles-passwords.txt). Además hay casos
negativos: un rol que NO debe usar esa herramienta, para comprobar que el asistente no la usa.

Toma IDs de las bases reales con docker exec (solo lectura). Los casos sin datos se omiten.

Uso:
  python3 scripts/piloto/ejecutar_pruebas_herramientas.py
"""
import getpass
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

GATEWAY = "http://localhost:8080"
LOGIN = GATEWAY + "/usuarios-accesos-ms/v1/auth/login"
NEXUS = GATEWAY + "/nexus/consultas"
CONTENEDOR = "empresa-postgres-1"
ARCHIVO_ROLES = os.path.expanduser("~/piloto-roles-passwords.txt")


def sql(base, consulta):
    r = subprocess.run(
        ["docker", "exec", CONTENEDOR, "psql", "-U", "postgres", "-d", base, "-tAc", consulta],
        capture_output=True, text=True, check=True)
    return [linea for linea in r.stdout.splitlines() if linea.strip()]


def primero(base, consulta):
    filas = sql(base, consulta)
    return filas[0] if filas else None


def cargar_usuarios_rol():
    usuarios = {}
    with open(ARCHIVO_ROLES) as f:
        for linea in f:
            partes = linea.rstrip("\n").split("\t")
            if len(partes) == 3:
                usuarios[partes[0]] = (partes[1], partes[2])
    return usuarios


def login(correo, clave):
    datos = json.dumps({"correo": correo, "password": clave}).encode()
    req = urllib.request.Request(LOGIN, data=datos, method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read())["accessToken"]
    except urllib.error.HTTPError as e:
        print(f"  Login de {correo} falló con código {e.code}")
        return None


def preguntar(token, pregunta):
    datos = json.dumps({"pregunta": pregunta}).encode()
    req = urllib.request.Request(NEXUS, data=datos, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, json.loads(r.read()).get("respuesta", "")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")[:160]


def casos():
    periodo = primero("reportes", "select periodo from reportes order by periodo desc") or "2026-09"
    proveedor = primero("compras", "select id from proveedores limit 1")
    orden = primero("compras", "select id from ordenes_compra limit 1")
    comprobante = primero("facturacion_pagos", "select id from comprobantes limit 1")
    poliza = primero("contabilidad", "select id from polizas limit 1")
    articulo = primero("inventario", "select id from articulos limit 1")
    lote = primero("inventario", "select lote from movimientos_inventario where lote is not null limit 1")
    documento = primero("documentos", "select id from documentos limit 1")
    usuario = primero("usuarios_accesos", "select id from usuarios limit 1")
    # (código, pregunta, rol con permiso, roles que deberían poder usar la herramienta)
    return [
        ("V1", "¿Qué pedidos están confirmados?", "VENTAS", "VENTAS, DIRECCION"),
        ("V2", f"Dame el resumen de ventas de {periodo}", "FINANZAS", "DIRECCION, FINANZAS"),
        ("P1", f"Dame los datos del proveedor {proveedor}", "COMPRAS", "COMPRAS, FINANZAS"),
        ("O1", f"Dame la orden de compra {orden}", "COMPRAS", "COMPRAS, ALMACEN, DIRECCION"),
        ("F1", f"Dame el comprobante {comprobante}", "FINANZAS", "FINANZAS, VENTAS"),
        ("K1", f"Dame la póliza {poliza}", "CONTADOR", "CONTADOR, DIRECCION"),
        ("K2", f"Dame la balanza del periodo {periodo}", "CONTADOR", "CONTADOR, DIRECCION"),
        ("I1", f"¿Cuáles son las existencias del artículo {articulo}?", "ALMACEN", "ALMACEN, COMPRAS, VENTAS"),
        ("I2", f"Dame la trazabilidad del lote {lote}", "OPERACIONES", "OPERACIONES, CALIDAD"),
        ("D1", f"Dame los datos del documento {documento}", "LEGAL", "LEGAL, DIRECCION"),
        ("D2", "¿Qué documentos vencen en los próximos 30 días?", "LEGAL", "LEGAL, DIRECCION"),
        ("U1", f"Dame los datos del usuario {usuario}", "ADMIN", "ADMIN"),
        ("X1", "pedidos confirmados", "VENTAS", "Caso de control, sin herramienta"),
    ]


# Negativos: un rol que NO debe usar la herramienta del caso.
NEGATIVOS = [
    ("V1-neg", "¿Qué pedidos están confirmados?", "COMPRAS"),
    ("K1-neg", "Dame la póliza {poliza}", "COMPRAS"),
    ("P1-neg", "Dame los datos del proveedor {proveedor}", "CONTADOR"),
]


def main():
    usuarios = cargar_usuarios_rol()
    if not usuarios:
        print(f"No hay usuarios de rol en {ARCHIVO_ROLES}. Corre primero crear_datos_prueba_por_rol.py.")
        sys.exit(1)
    clave_admin = getpass.getpass("Contraseña de admin@empresa.mx: ")
    token_admin = login("admin@empresa.mx", clave_admin)
    del clave_admin
    if not token_admin:
        sys.exit(1)
    tokens = {"ADMIN": token_admin}

    def token_de(rol):
        if rol not in tokens:
            if rol not in usuarios:
                return None
            correo, clave = usuarios[rol]
            tokens[rol] = login(correo, clave)
        return tokens[rol]

    for codigo, pregunta, rol, roles in casos():
        if "None" in pregunta:
            print(f"{codigo}  OMITIDO  roles: {roles}\n    No hay datos en la base local para este caso.\n")
            continue
        token = token_de(rol)
        if not token:
            print(f"{codigo}  SIN TOKEN para {rol}  roles: {roles}\n")
            continue
        estatus, texto = preguntar(token, pregunta)
        resumen = " ".join(str(texto).split())[:220]
        print(f"{codigo}  [{estatus}]  como: {rol}  roles: {roles}\n    P: {pregunta}\n    R: {resumen}\n")

    print("== Negativos (el rol no debe usar la herramienta) ==")
    for codigo, plantilla, rol in NEGATIVOS:
        pregunta = plantilla.format(poliza=primero("contabilidad", "select id from polizas limit 1"),
                                    proveedor=primero("compras", "select id from proveedores limit 1"))
        if "None" in pregunta:
            print(f"{codigo}  OMITIDO\n")
            continue
        token = token_de(rol)
        if not token:
            print(f"{codigo}  SIN TOKEN para {rol}\n")
            continue
        estatus, texto = preguntar(token, pregunta)
        resumen = " ".join(str(texto).split())[:220]
        print(f"{codigo}  [{estatus}]  como: {rol}\n    P: {pregunta}\n    R: {resumen}\n")


if __name__ == "__main__":
    main()
