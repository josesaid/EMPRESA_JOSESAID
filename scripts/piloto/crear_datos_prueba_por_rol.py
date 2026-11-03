#!/usr/bin/env python3
"""Crea usuarios de PRUEBA por rol y algunos registros ficticios por área (piloto de Nexus).

- Usuarios: uno por rol. Si ya existen, se reutilizan las contraseñas de ~/piloto-roles-passwords.txt.
  Si el usuario tiene contraseña temporal, el script la cambia (cambiar-password-inicial) y actualiza
  el archivo. Las contraseñas nunca se imprimen.
- Datos: cada registro lo crea un usuario de su propio rol, así también se prueban los permisos.
- Áreas sin seed aquí: FINANZAS y VENTAS (requieren IDs que este script no genera), documentos (multipart),
  CALIDAD y LEGAL (sin registros propios). Esos casos siguen saliendo como OMITIDO.

Uso: python3 scripts/piloto/crear_datos_prueba_por_rol.py
"""
import getpass
import json
import os
import secrets
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

GATEWAY = "http://localhost:8080"
LOGIN = GATEWAY + "/usuarios-accesos-ms/v1/auth/login"
CAMBIO = GATEWAY + "/usuarios-accesos-ms/v1/auth/cambiar-password-inicial"
USUARIOS = GATEWAY + "/usuarios-accesos-ms/v1/usuarios"
CORREO_ADMIN = "admin@empresa.mx"
ARCHIVO = os.path.expanduser("~/piloto-roles-passwords.txt")
ESTADO = os.path.expanduser("~/piloto-seed-estado.json")
ROLES = ["COMPRAS", "FINANZAS", "VENTAS", "CONTADOR", "ALMACEN", "OPERACIONES", "CALIDAD", "LEGAL"]


def llamada(metodo, url, cuerpo=None, token=None):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(url, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            cuerpo_r = r.read()
            return r.status, json.loads(cuerpo_r) if cuerpo_r else {}
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")[:200]


def llamada_multipart(url, datos, nombre_archivo, contenido, token):
    """POST multipart con una parte JSON ('datos') y una parte de archivo ('archivo')."""
    frontera = "----piloto" + secrets.token_hex(8)
    cuerpo = (
        f"--{frontera}\r\n"
        'Content-Disposition: form-data; name="datos"\r\n'
        "Content-Type: application/json\r\n\r\n"
    ).encode() + json.dumps(datos).encode() + (
        f"\r\n--{frontera}\r\n"
        f'Content-Disposition: form-data; name="archivo"; filename="{nombre_archivo}"\r\n'
        "Content-Type: text/plain\r\n\r\n"
    ).encode() + contenido + f"\r\n--{frontera}--\r\n".encode()
    req = urllib.request.Request(url, data=cuerpo, method="POST")
    req.add_header("Content-Type", "multipart/form-data; boundary=" + frontera)
    req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")[:200]


def nueva_clave():
    return "Piloto-" + secrets.token_hex(8) + "1!"


def login(correo, clave):
    estado, respuesta = llamada("POST", LOGIN, {"correo": correo, "password": clave})
    return respuesta.get("accessToken") if estado == 200 else None


def activar(correo, clave_actual):
    """Cambia la contraseña temporal. Devuelve la nueva contraseña o None."""
    clave = nueva_clave()
    estado, respuesta = llamada("POST", CAMBIO, {
        "correo": correo, "passwordActual": clave_actual, "passwordNueva": clave})
    if estado in (200, 204):
        return clave
    print(f"  Cambio de contraseña de {correo} falló con código {estado}: {respuesta}")
    return None


def guardar(claves):
    fd = os.open(ARCHIVO, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w") as f:
        for rol, (correo, clave) in claves.items():
            f.write(f"{rol}\t{correo}\t{clave}\n")


def leer():
    claves = {}
    if os.path.exists(ARCHIVO):
        with open(ARCHIVO) as f:
            for linea in f:
                partes = linea.rstrip("\n").split("\t")
                if len(partes) == 3:
                    claves[partes[0]] = (partes[1], partes[2])
    return claves


def crear_usuarios(token_admin, claves):
    for rol in ROLES:
        correo = f"{rol.lower()}.piloto@example.com"
        if rol in claves:
            continue
        clave = nueva_clave()
        estado, respuesta = llamada("POST", USUARIOS, {
            "correo": correo,
            "nombreCompleto": f"Piloto {rol.capitalize()}",
            "password": clave,
            "roles": [rol],
        }, token_admin)
        if estado in (200, 201):
            claves[rol] = (correo, clave)
            print(f"Usuario {rol}: creado")
        else:
            print(f"Usuario {rol}: FALLÓ código {estado} {respuesta}")
    guardar(claves)
    return claves


def token_de(claves, rol):
    if rol not in claves:
        return None
    correo, clave = claves[rol]
    token = login(correo, clave)
    if token:
        return token
    # Contraseña temporal o vencida: se cambia y se guarda la nueva.
    nueva = activar(correo, clave)
    if not nueva:
        return None
    claves[rol] = (correo, nueva)
    guardar(claves)
    return login(correo, nueva)


def id_sql(base, consulta):
    r = subprocess.run(
        ["docker", "exec", "empresa-postgres-1", "psql", "-U", "postgres", "-d", base, "-tAc", consulta],
        capture_output=True, text=True)
    filas = [linea.strip() for linea in r.stdout.splitlines() if linea.strip()]
    return filas[0] if filas else None


def leer_estado():
    if os.path.exists(ESTADO):
        with open(ESTADO) as f:
            return json.load(f)
    return {}


def marcar_estado(clave):
    estado = leer_estado()
    estado[clave] = True
    with open(ESTADO, "w") as f:
        json.dump(estado, f)


def buscar_cliente(token, nombre):
    """Busca el ID de un cliente por razón social, con el API de clientes-ms (rol VENTAS)."""
    from urllib.parse import quote
    estado, cuerpo = llamada("GET", GATEWAY + "/clientes-ms/v1/clientes?nombre=" + quote(nombre) + "&size=5",
                             None, token)
    if estado != 200:
        print(f"  búsqueda de cliente {nombre}: falló con código {estado}: {cuerpo}")
        return None

    def encontrar(x):
        if isinstance(x, dict):
            if x.get("razonSocial") == nombre and "id" in x:
                return x["id"]
            for v in x.values():
                r = encontrar(v)
                if r:
                    return r
        elif isinstance(x, list):
            for v in x:
                r = encontrar(v)
                if r:
                    return r
        return None

    return encontrar(cuerpo)


def paso(nombre, estado, respuesta):
    ok = estado in (200, 201)
    print(f"  {nombre}: {'OK' if ok else 'FALLÓ ' + str(estado) + ' ' + str(respuesta)}")
    return respuesta if ok else None


def sembrar(claves, token_admin):
    print("Datos de prueba:")
    G = GATEWAY

    t = token_de(claves, "COMPRAS")
    if t:
        paso("proveedor PRU", *llamada("POST", G + "/compras-proveedores-ms/v1/proveedores", {
            "razonSocial": "Proveedor Prueba Piloto SA de CV",
            "rfc": "PRU010101AA1",
            "correo": "proveedor.prueba@example.com",
        }, t))

    t_op = token_de(claves, "OPERACIONES")
    articulo = None
    if t_op:
        articulo = paso("artículo PRU-ART-001", *llamada("POST", G + "/inventario-operaciones-ms/v1/articulos", {
            "sku": "PRU-ART-001", "nombre": "Artículo Prueba Piloto", "unidad": "PZA",
            "loteObligatorio": False, "minimo": 10,
        }, t_op))

    t_al = token_de(claves, "ALMACEN")
    if t_al:
        almacen = paso("almacén Prueba", *llamada("POST", G + "/inventario-operaciones-ms/v1/almacenes", {
            "nombre": "Almacén Prueba Piloto", "ubicacion": "Zona de pruebas",
        }, t_al))
        if articulo and almacen:
            paso("entrada de 50 PZA", *llamada("POST", G + "/inventario-operaciones-ms/v1/movimientos", {
                "articuloId": articulo["id"], "almacenId": almacen["id"],
                "tipo": "ENTRADA", "cantidad": 50, "referencia": "PRU-ENTRADA-001",
            }, t_al))

    t_co = token_de(claves, "CONTADOR")
    if t_co:
        caja = paso("cuenta 1102 Bancos", *llamada("POST", G + "/contabilidad-ms/v1/cuentas", {
            "codigo": "1102", "nombre": "Bancos", "naturaleza": "DEUDORA"}, t_co))
        ventas = paso("cuenta 4102 Ingresos", *llamada("POST", G + "/contabilidad-ms/v1/cuentas", {
            "codigo": "4102", "nombre": "Ingresos", "naturaleza": "ACREEDORA"}, t_co))
        if caja and ventas:
            paso("póliza de ingreso 250", *llamada("POST", G + "/contabilidad-ms/v1/polizas", {
                "tipo": "INGRESO",
                "fecha": date.today().replace(day=1).isoformat(),
                "movimientos": [
                    {"cuentaId": caja["id"], "cargo": 250},
                    {"cuentaId": ventas["id"], "abono": 250},
                ],
            }, t_co))

    # Orden de compra (O1) y entrada con lote (I2): se buscan IDs en las bases locales.
    estado = leer_estado()
    if t and claves.get("COMPRAS") and estado.get("orden_compra"):
        print("  orden de compra PRU: ya creada en una corrida anterior; se omite")
    elif t and claves.get("COMPRAS"):
        proveedor_id = id_sql("compras", "select id from proveedores where razon_social='Proveedor Prueba Piloto SA de CV'")
        if proveedor_id:
            if paso("orden de compra PRU", *llamada("POST", G + "/compras-proveedores-ms/v1/ordenes-compra", {
                "proveedorId": proveedor_id,
                "moneda": "MXN",
                "lineas": [{"articuloSku": "PRU-ART-001", "cantidad": 10, "precioUnitario": 100}],
            }, t)):
                marcar_estado("orden_compra")
        else:
            print("  orden de compra PRU: omitida, no se encontró el proveedor PRU")
    if t_al and estado.get("entrada_lote"):
        print("  entrada con lote PRU-LOTE-001: ya creada en una corrida anterior; se omite")
    elif t_al:
        articulo_id = id_sql("inventario", "select id from articulos where sku='PRU-ART-001'")
        almacen_id = id_sql("inventario", "select id from almacenes where nombre='Almacén Prueba Piloto'")
        if articulo_id and almacen_id:
            if paso("entrada con lote PRU-LOTE-001", *llamada("POST", G + "/inventario-operaciones-ms/v1/movimientos", {
                "articuloId": articulo_id, "almacenId": almacen_id,
                "tipo": "ENTRADA", "cantidad": 10, "lote": "PRU-LOTE-001",
                "referencia": "PRU-LOTE-ENTRADA-001",
            }, t_al)):
                marcar_estado("entrada_lote")
        else:
            print("  entrada con lote: omitida, no se encontró el artículo o el almacén")

    # Pedido confirmado (VENTAS) y comprobante sobre ese pedido (FINANZAS): para V1 y F1.
    t_v = token_de(claves, "VENTAS")
    t_f = token_de(claves, "FINANZAS")
    if t_v and t_f and leer_estado().get("pedido_confirmado"):
        print("  pedido confirmado y comprobante: ya creados en una corrida anterior; se omiten")
    elif t_v and t_f:
        cliente_id = buscar_cliente(t_v, "Cliente Prueba 001 SA de CV")
        if not cliente_id:
            # La base de clientes está vacía: se crea el cliente de prueba con ADMIN (solo ADMIN puede crear clientes).
            estado, cuerpo = llamada("POST", G + "/clientes-ms/v1/clientes", {
                "razonSocial": "Cliente Prueba 001 SA de CV",
                "rfc": "PRU000101XAB",
                "contactoPrincipal": {"nombre": "Contacto Prueba 001", "correo": "prueba001@example.com",
                                      "rol": "PAGOS"},
            }, token_admin)
            if estado in (200, 201):
                print("  cliente Cliente Prueba 001: creado")
                cliente_id = buscar_cliente(t_v, "Cliente Prueba 001 SA de CV")
            else:
                print(f"  cliente Cliente Prueba 001: FALLÓ {estado} {cuerpo}")
        producto_id = id_sql("inventario", "select id from articulos where sku='PRU-ART-001'")
        if not cliente_id:
            print("  pedido PRU: no se encontró el cliente 'Cliente Prueba 001 SA de CV' en clientes-ms")
        if cliente_id and producto_id:
            pedido = paso("pedido PRU", *llamada("POST", G + "/pedidos-ventas-ms/v1/pedidos", {
                "clienteId": cliente_id,
                "canal": "WEB",
                "moneda": "MXN",
                "tasaIva": "IVA_GENERAL",
                "lineas": [{"productoId": producto_id, "descripcion": "Artículo Prueba Piloto",
                            "cantidad": 2, "precioUnitario": 500}],
            }, t_v))
            if pedido:
                confirmado = paso("confirmación del pedido", *llamada(
                    "POST", G + "/pedidos-ventas-ms/v1/pedidos/" + pedido["id"] + "/confirmacion", None, t_v))
                if confirmado:
                    if paso("comprobante PRU sobre el pedido", *llamada(
                            "POST", G + "/facturacion-pagos-ms/v1/comprobantes", {
                                "ordenId": pedido["id"], "moneda": "MXN",
                                "subtotal": 1000, "tasaIva": 0.16}, t_f)):
                        marcar_estado("pedido_confirmado")
        else:
            print("  pedido PRU: omitido, no se encontró el cliente de prueba o el artículo")

    # Documento de LEGAL (D1, D2): carga multipart con vencimiento a 15 días.
    t_legal = token_de(claves, "LEGAL")
    if t_legal and leer_estado().get("documento"):
        print("  documento PRU: ya creado en una corrida anterior; se omite")
    elif t_legal:
        if paso("documento PRU (contrato, vence en 15 días)", *llamada_multipart(
                G + "/documentos-cumplimiento-ms/v1/documentos",
                {"tipo": "CONTRATO", "titulo": "Contrato Prueba Piloto", "area": "Legal",
                 "venceEn": (date.today() + timedelta(days=15)).isoformat()},
                "contrato-prueba.txt", b"Contrato de prueba del piloto de Nexus.", t_legal)):
            marcar_estado("documento")

    print("Omitidos por diseño: CALIDAD (sin registros propios).")


def main():
    clave = getpass.getpass("Contraseña de admin@empresa.mx: ")
    token = login(CORREO_ADMIN, clave)
    del clave
    if not token:
        print("Login de admin falló")
        sys.exit(1)
    claves = crear_usuarios(token, leer())
    # Activa la contraseña de todos los roles (también los que no tienen datos de seed).
    for rol in ROLES:
        token_de(claves, rol)
    sembrar(claves, token)
    print(f"Contraseñas actualizadas en {ARCHIVO}. Guárdalas en tu gestor y bórralas después.")


if __name__ == "__main__":
    main()
