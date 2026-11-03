#!/usr/bin/env python3
"""Ejecuta las pruebas del piloto (clientes y reportes) contra Nexus y muestra una tabla para revisar.

Los periodos y clientes se toman de las bases reales con docker exec (solo lectura).
Uso: python3 scripts/piloto/ejecutar_pruebas_nexus.py
"""
import getpass
import json
import subprocess
import sys
import urllib.error
import urllib.request

GATEWAY = "http://localhost:8080"
LOGIN = GATEWAY + "/usuarios-accesos-ms/v1/auth/login"
NEXUS = GATEWAY + "/nexus/consultas"
CONTENEDOR = "empresa-postgres-1"


def sql(base, consulta):
    r = subprocess.run(
        ["docker", "exec", CONTENEDOR, "psql", "-U", "postgres", "-d", base, "-tAc", consulta],
        capture_output=True, text=True, check=True)
    return [linea for linea in r.stdout.splitlines() if linea.strip()]


def consultar(token, pregunta):
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
    reportes = sql("reportes", "select periodo, estatus from reportes order by periodo desc")
    cerrado = next((l.split("|")[0] for l in reportes if l.endswith("|CERRADO")), None)
    borrador = next((l.split("|")[0] for l in reportes if l.endswith("|BORRADOR")), None)
    return [
        ("R1", f"Dame el resumen ejecutivo de {cerrado}", "Estatus CERRADO e indicadores"),
        ("R2", f"¿Cuál es el estatus del reporte de {borrador}?", "BORRADOR"),
        ("R3", f"¿Cómo le fue a Ventas en {cerrado}?", "Indicadores de VENTAS_ATENCION_CLIENTES"),
        ("R4", "Dame el resumen de 2019-01", "Dice que no existe, sin inventar"),
        ("C1", "Busca clientes que se llamen Cliente Prueba", "Hasta 10 clientes de prueba"),
        ("C2", "Busca el RFC PRU000101XAB", "Cliente Prueba 001"),
        ("C3", "Busca el RFC PRU000", "Hallazgo: no lo encuentra"),
        ("C4", "Busca el cliente ZZZ-000", "Dice que no existe"),
        ("C5", "¿Cuántos clientes activos tenemos?", "Hallazgo: no tiene filtro por estatus"),
    ]


def login(correo, clave):
    datos = json.dumps({"correo": correo, "password": clave}).encode()
    req = urllib.request.Request(LOGIN, data=datos, method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read())["accessToken"]
    except urllib.error.HTTPError as e:
        print(f"Login de {correo} falló con código {e.code}")
        sys.exit(1)


def main():
    clave_admin = getpass.getpass("Contraseña de admin@empresa.mx: ")
    token_admin = login("admin@empresa.mx", clave_admin)
    del clave_admin
    clave_direccion = getpass.getpass("Contraseña de direccion.piloto: ")
    token_direccion = login("direccion.piloto@example.com", clave_direccion)
    del clave_direccion

    antes = int(sql("nexus", "select count(*) from nexus_consultas")[0])
    print(f"{'ID':<4} {'HTTP':<5} {'ESPERADO':<42} RESPUESTA (inicio)")
    print("-" * 140)
    for id_, pregunta, esperado in casos():
        token = token_direccion if id_.startswith("R") else token_admin
        estado, respuesta = consultar(token, pregunta)
        texto = (respuesta or "").replace("\n", " ")[:90]
        print(f"{id_:<4} {estado:<5} {esperado[:40]:<42} {texto}")
    despues = int(sql("nexus", "select count(*) from nexus_consultas")[0])
    print(f"\nFilas nuevas en auditoría: {despues - antes} (esperado: {len(casos())})")


if __name__ == "__main__":
    main()
