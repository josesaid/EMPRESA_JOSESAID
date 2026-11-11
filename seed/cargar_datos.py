#!/usr/bin/env python3
"""Carga datos de demo en las bases de EMPRESA_JOSESAID.

Uso (desde la raíz del repo, con Docker corriendo y el stack levantado):
    python3 seed/cargar_datos.py            # carga; se niega si ya hay datos
    python3 seed/cargar_datos.py --reset    # borra los datos de negocio y carga de nuevo

Requisitos: Python 3 y el paquete `cryptography` (pip3 install cryptography).
Lee las claves de cifrado y de índice desde el archivo .env de la raíz.

Qué hace:
- Inserta solo en las tablas de negocio (no toca outbox, idempotencia ni refresh_tokens).
- Cifra los datos personales con AES-256-GCM, igual que los servicios, y calcula los índices HMAC.
- Usa identificadores fijos (UUID determinísticos) para que las referencias entre servicios coincidan.
- Cubre todas las combinaciones de los estatus y tipos; si las combinaciones superan 100, agrega filas.
- Usuarios demo: <rol>@demo.empresa.mx con contraseña Demo2026!; admin@empresa.mx tiene todos los roles, sin restricciones
"""
import argparse
import base64
import datetime as dt
import hashlib
import hmac
import os
import random
import subprocess
import sys
import unicodedata
import uuid
from pathlib import Path

from cryptography.hazmat.primitives.ciphers.aead import AESGCM

RAIZ = Path(__file__).resolve().parent.parent
NS = uuid.UUID("6f1c2b8e-4d5a-4e7b-9a10-454d50524553")
N = 100  # filas por tabla de negocio (sube solo si las combinaciones lo exigen)
CLAVE_DEMO_HASH = "$2a$10$BWhRyHT12F/nUc5MU7VWZO3P13tT1VJN6fQA2jBfDx/GsZ6A9f1o6"  # BCrypt de Demo2026!
ROLES = ["ADMIN", "FINANZAS", "CONTADOR", "VENTAS", "COMPRAS", "ALMACEN",
         "OPERACIONES", "CALIDAD", "DIRECCION", "LEGAL", "JEFE_AREA"]
AREAS_USUARIO = ["VENTAS_ATENCION_CLIENTES", "FINANZAS_CONTABILIDAD", "OPERACIONES",
                 "COMPRAS_PROVEEDORES", "ADMINISTRACION_LEGAL", "DIRECCION_GENERAL"]
NOMBRES = ["Ana", "Luis", "María", "Jorge", "Lucía", "Carlos", "Sofía", "Miguel", "Valeria", "Diego",
           "Fernanda", "Javier", "Paola", "Ricardo", "Daniela", "Andrés", "Gabriela", "Héctor", "Renata", "Mauricio"]
APELLIDOS = ["García", "Hernández", "López", "Martínez", "Pérez", "Ramírez", "Sánchez", "Torres", "Flores",
             "Rivera", "Gómez", "Díaz", "Cruz", "Morales", "Reyes", "Ortiz", "Vargas", "Castillo", "Ruiz", "Mendoza"]
PREFIJOS_EMPRESA = ["Distribuidora", "Comercializadora", "Servicios", "Industrias", "Grupo", "Suministros",
                    "Logística", "Constructora"]
SUFIJOS_EMPRESA = ["del Norte", "del Bajío", "Occidente", "Pacífico", "Central", "Integral", "Express", "Premier"]
FINALIDADES = ["Marketing", "Facturación", "Cobranza", "Atención al cliente", "Encuestas de satisfacción"]
ARTICULOS = [("Tornillo hexagonal 1/4", "PZA"), ("Cable calibre 12", "M"), ("Aceite industrial 19L", "LT"),
             ("Caja de cartón mediana", "PZA"), ("Resina epóxica 1kg", "KG"), ("Rodamiento 6204", "PZA"),
             ("Tubo PVC 2 pulg", "M"), ("Pintura base agua 4L", "LT")]
CUENTAS = [("1101", "Caja", "DEUDORA"), ("1102", "Bancos", "DEUDORA"), ("1201", "Clientes", "DEUDORA"),
           ("1301", "Inventario", "DEUDORA"), ("1401", "IVA acreditable", "DEUDORA"),
           ("2101", "Proveedores", "ACREEDORA"), ("2201", "IVA trasladado", "ACREEDORA"),
           ("2301", "Impuestos por pagar", "ACREEDORA"), ("4101", "Ingresos por ventas", "ACREEDORA"),
           ("5101", "Costo de ventas", "DEUDORA"), ("6101", "Gastos de administración", "DEUDORA"),
           ("6201", "Gastos de venta", "DEUDORA")]
PERIODOS = [f"2026-{m:02d}" for m in range(1, 13)]
INDICADORES = ["Ventas del mes", "Cuentas por cobrar", "Órdenes pendientes", "Rotación de inventario"]


# ---------- utilidades de datos ----------

def uid(tipo, i):
    """Identificador fijo: el mismo tipo e índice dan siempre el mismo UUID en todas las bases."""
    return str(uuid.uuid5(NS, f"{tipo}:{i}"))


def mixto(i, tamanos):
    """Combinación mixta: la fila i recorre todas las combinaciones de varios catálogos antes de repetir."""
    valores, resto = [], i
    for t in tamanos:
        valores.append(resto % t)
        resto //= t
    return valores


def combinaciones(*tamanos):
    """Cuántas combinaciones hay; si supera N, la tabla crece para cubrirlas todas."""
    total = 1
    for t in tamanos:
        total *= t
    return max(N, min(total, 2000))


def sql(v):
    """Literal SQL seguro para un valor Python."""
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, dt.datetime):
        return f"'{v.isoformat()}'"
    if isinstance(v, dt.date):
        return f"'{v.isoformat()}'"
    return "'" + str(v).replace("'", "''") + "'"


def insertar(tabla, columnas, filas):
    """Devuelve el INSERT de una tabla con todas sus filas en un solo bloque."""
    if not filas:
        return ""
    valores = ",\n".join("(" + ", ".join(sql(v) for v in f) + ")" for f in filas)
    return f"INSERT INTO {tabla} ({', '.join(columnas)}) VALUES\n{valores};\n"


def sin_acentos(texto):
    return "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn")


# ---------- cifrado e índices (mismo formato que los servicios Java) ----------

class Cripto:
    """Cifrado y índices de un servicio. Cada servicio tiene su propia pareja de claves en .env."""

    def __init__(self, env, nombre_cifrado, nombre_indice):
        self.version = int(env.get("CRYPTO_VERSION_ACTIVA", "1")) if nombre_cifrado.startswith("CRYPTO") else 1
        self.aes = AESGCM(base64.b64decode(env[nombre_cifrado].strip()))
        self.clave_indice = base64.b64decode(env[nombre_indice].strip())

    def cifrar(self, texto):
        """Formato del servicio: v<versión>:Base64(IV de 12 bytes || texto cifrado + tag)."""
        if texto is None:
            return None
        iv = os.urandom(12)
        cifrado = self.aes.encrypt(iv, texto.encode("utf-8"), None)
        return f"v{self.version}:" + base64.b64encode(iv + cifrado).decode("ascii")

    def _hmac(self, entrada):
        return hmac.new(self.clave_indice, entrada.encode("utf-8"), hashlib.sha256).hexdigest()

    def indice_rfc(self, rfc):
        return self._hmac("rfc:" + rfc.strip().upper())

    def indice_correo(self, correo):
        return self._hmac("correo:" + correo.strip().lower())

    def indice_rfc_b64(self, rfc):
        """Índice de compras: HMAC del RFC en mayúsculas, en Base64."""
        return base64.b64encode(hmac.new(self.clave_indice, rfc.strip().upper().encode("utf-8"),
                                         hashlib.sha256).digest()).decode("ascii")

    def indice_nombre(self, nombre):
        normal = " ".join(sin_acentos(nombre.strip()).split()).lower()
        return self._hmac("nombre:" + normal)


def leer_env(ruta):
    env = {}
    for linea in Path(ruta).read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if linea and not linea.startswith("#") and "=" in linea:
            clave, valor = linea.split("=", 1)
            env[clave.strip()] = valor.strip().strip('"').strip("'")
    return env


# ---------- generación de filas por base ----------

def generar(cr_cli, cr_usr, cr_cmp, rnd):
    """Devuelve {base: [(tabla, columnas, filas), ...]} en el orden en que deben insertarse."""
    tiempo0 = dt.datetime(2026, 1, 5, 9, 0, tzinfo=dt.timezone.utc)
    cuando = lambda i, dias=7: tiempo0 + dt.timedelta(days=(i * dias) % 330, hours=i % 10)
    n_usuarios = max(N, len(ROLES))
    bases = {}

    cr = cr_usr  # usuarios-accesos-ms

    # --- usuarios_accesos ---
    usuarios, roles = [], []
    for i in range(n_usuarios):
        rol_principal = ROLES[i % len(ROLES)]
        estatus = ["ACTIVO", "BLOQUEADO", "BAJA"][(i // len(ROLES)) % 3]
        debe_cambiar = (i // (len(ROLES) * 3)) % 2 == 1
        if i == 0:
            correo = "admin@empresa.mx"  # administrador con todos los roles
        elif i < len(ROLES):
            correo = f"{rol_principal.lower()}@demo.empresa.mx"
        else:
            correo = f"usuario{i}@demo.empresa.mx"
        nombre = f"{NOMBRES[i % len(NOMBRES)]} {APELLIDOS[i % len(APELLIDOS)]}"
        bloqueado = cuando(i, 3) if estatus == "BLOQUEADO" else None
        usuarios.append((uid("usuario", i), cr.cifrar(correo), cr.indice_correo(correo), cr.cifrar(nombre),
                         CLAVE_DEMO_HASH, estatus, 5 if estatus == "BLOQUEADO" else 0, bloqueado, 0,
                         cuando(i, 2), cuando(i, 2), AREAS_USUARIO[i % len(AREAS_USUARIO)], debe_cambiar))
        roles.append((uid("usuario", i), rol_principal))
        if i == 0:
            roles.extend((uid("usuario", i), r) for r in ROLES if r != rol_principal)
        elif i % 3 == 0:
            secundario = ROLES[(i * 3 + 1) % len(ROLES)]
            if secundario != rol_principal:
                roles.append((uid("usuario", i), secundario))
    bases["usuarios_accesos"] = [
        ("usuarios", ["id", "correo", "correo_indice", "nombre_completo", "password_hash", "estatus",
                      "intentos_fallidos", "bloqueado_hasta", "version", "creado_utc", "actualizado_utc",
                      "area", "debe_cambiar_password"], usuarios),
        ("usuario_roles", ["usuario_id", "rol"], roles),
    ]

    # --- clientes ---
    cr = cr_cli  # clientes-ms
    prospectos, clientes, contactos, consentimientos, interacciones = [], [], [], [], []
    nprosp = combinaciones(5, 3, 2)
    for i in range(nprosp):
        estatus_p, fuente, con_rfc = mixto(i, [5, 3, 2])
        estatus_p = ["NUEVO", "CONTACTADO", "CALIFICADO", "CONVERTIDO", "PERDIDO"][estatus_p]
        fuente = ["CAMPANA", "REFERIDO", "EVENTO"][fuente]
        nombre = f"{NOMBRES[i % 20]} {APELLIDOS[(i * 3) % 20]}"
        correo = f"prospecto{i}@demo.invalid"
        rfc = None
        if con_rfc:
            rfc = f"PRO{i:03d}{NOMBRES[i % 20][0].upper()}{i % 7}{chr(65 + i % 26)}"[:13]
        conversion = cuando(i, 4) if estatus_p == "CONVERTIDO" else None
        prospectos.append((uid("prospecto", i), cr.cifrar(nombre), cr.indice_nombre(nombre), cr.cifrar(correo),
                           cr.indice_correo(correo), cr.cifrar(rfc) if rfc else None,
                           cr.indice_rfc(rfc) if rfc else None, fuente, estatus_p,
                           cuando(i, 1).date(), conversion, cuando(i, 1), cuando(i, 1), 0))
    for i in range(N):
        estatus = ["ACTIVO", "SUSPENDIDO", "BAJA"][i % 3]
        razon = f"{PREFIJOS_EMPRESA[i % 8]} {SUFIJOS_EMPRESA[(i // 8) % 8]}" + (f" {i}" if i >= 64 else "") + " SA de CV"
        rfc = f"{chr(65 + i % 26)}{chr(65 + (i * 7) % 26)}{chr(65 + (i * 11) % 26)}{2000 + i % 25:04d}{(i % 12) + 1:02d}{(i % 28) + 1:02d}{i:03d}"[:12]
        prospecto = uid("prospecto", i) if i % 3 == 0 and i < nprosp else None
        clientes.append((uid("cliente", i), razon, cr.cifrar(rfc), cr.indice_rfc(rfc), estatus, prospecto,
                         cuando(i, 3).date(), cuando(i, 3), cuando(i, 3), 0))
        nombre_c = f"{NOMBRES[(i * 7) % 20]} {APELLIDOS[(i * 5) % 20]}"
        correo_c = f"contacto{i}@demo.invalid"
        rol = ["COMPRAS", "PAGOS", "ASISTENCIA"][i % 3]
        contactos.append((uid("contacto", i), uid("cliente", i), cr.cifrar(nombre_c), cr.cifrar(correo_c),
                          cr.indice_correo(correo_c), rol, i % 3 == 0, cuando(i, 3), cuando(i, 3), 0))
        medio = ["FORMULARIO", "CORREO", "TELEFONO", "PRESENCIAL"][i % 4]
        consentimientos.append((uid("consentimiento", i), uid("contacto", i), FINALIDADES[i % len(FINALIDADES)],
                                cuando(i, 2), medio, (cuando(i, 2) + dt.timedelta(days=365)).date(),
                                cuando(i, 1) if i % 5 == 0 else None, cuando(i, 2)))
        tipo = ["LLAMADA", "CORREO", "VISITA"][i % 3]
        interacciones.append((uid("interaccion", i), uid("cliente", i), tipo, cuando(i, 2),
                              uid("usuario", i % n_usuarios),
                              cr.cifrar(f"Seguimiento {tipo.lower()} con {razon}. Acuerdos registrados."),
                              cuando(i, 2)))
    bases["clientes"] = [
        ("prospecto", ["id", "nombre_cifrado", "nombre_hash", "correo_cifrado", "correo_hash", "rfc_cifrado",
                       "rfc_hash", "fuente", "estatus", "fecha_registro", "fecha_conversion", "created_at",
                       "updated_at", "version"], prospectos),
        ("cliente", ["id", "razon_social", "rfc_cifrado", "rfc_hash", "estatus", "prospecto_id", "fecha_alta",
                     "created_at", "updated_at", "version"], clientes),
        ("contacto", ["id", "cliente_id", "nombre_cifrado", "correo_cifrado", "correo_hash", "rol", "principal",
                      "created_at", "updated_at", "version"], contactos),
        ("consentimiento", ["id", "contacto_id", "finalidad", "fecha_otorgado", "medio", "vigencia",
                            "revocado_at", "created_at"], consentimientos),
        ("interaccion", ["id", "cliente_id", "tipo", "fecha", "responsable_id", "resumen_cifrado", "created_at"],
         interacciones),
    ]

    # --- inventario ---
    almacenes = [(uid("almacen", k), f"Almacén {k + 1}", f"Nave {k + 1}, zona industrial") for k in range(5)]
    articulos, existencias, movimientos_inv, ordenes_op = [], [], [], []
    for i in range(N):
        nombre_a, unidad = ARTICULOS[i % len(ARTICULOS)]
        sku = f"SKU-{i:04d}"
        lote = (i // len(ARTICULOS)) % 2 == 0
        articulos.append((uid("articulo", i), sku, f"{nombre_a} {i:03d}", unidad, lote, (i % 4) * 5))
        cantidad = 50 + (i * 13) % 200
        reservado = (i * 7) % (cantidad // 3)
        existencias.append((uid("existencia", i), uid("articulo", i), uid("almacen", i % 5), cantidad, reservado, 0))
    tipos_mov = ["ENTRADA", "SALIDA", "AJUSTE", "RESERVA"]
    for i in range(N):
        tipo = tipos_mov[i % 4]
        cantidad = {"ENTRADA": 10, "SALIDA": -4, "AJUSTE": -2 if i % 2 else 3, "RESERVA": 5}[tipo]
        lote = f"L{i:04d}" if articulos[i][4] else None
        movimientos_inv.append((uid("movimiento", i), uid("articulo", i), uid("almacen", i % 5), tipo, cantidad,
                                lote, f"REF-{i:05d}", cuando(i, 1)))
    for i in range(combinaciones(2, 2)):
        tipo, estatus = mixto(i, [2, 2])
        tipo = ["PRODUCCION", "SERVICIO"][tipo]
        estatus = ["ABIERTA", "CERRADA"][estatus]
        objetivo = 100 + i * 5
        real = objetivo - 3 if estatus == "CERRADA" else None
        ordenes_op.append((uid("orden_operacion", i), tipo, estatus, objetivo, real, 0))
    bases["inventario"] = [
        ("almacenes", ["id", "nombre", "ubicacion"], almacenes),
        ("articulos", ["id", "sku", "nombre", "unidad", "lote_obligatorio", "minimo"], articulos),
        ("existencias", ["id", "articulo_id", "almacen_id", "cantidad", "reservado", "version"], existencias),
        ("movimientos_inventario", ["id", "articulo_id", "almacen_id", "tipo", "cantidad", "lote", "referencia",
                                    "creado_utc"], movimientos_inv),
        ("ordenes_operacion", ["id", "tipo", "estatus", "cantidad_objetivo", "cantidad_real", "version"],
         ordenes_op),
    ]

    # --- compras ---
    cr = cr_cmp  # compras-proveedores-ms
    proveedores, ordenes_c, lineas_oc, recepciones = [], [], [], []
    for i in range(N):
        razon = f"Proveedor {PREFIJOS_EMPRESA[(i * 3) % 8]} {i:03d}"
        rfc = f"PRV{i:03d}{chr(65 + i % 26)}{chr(65 + (i * 3) % 26)}"[:12]
        correo = f"proveedor{i}@demo.invalid"
        estatus = ["ACTIVO", "INACTIVO", "BLOQUEADO"][i % 3]
        proveedores.append((uid("proveedor", i), razon, cr.cifrar(rfc), cr.indice_rfc_b64(rfc), cr.cifrar(correo),
                            estatus, 0))
    estatus_oc = ["BORRADOR", "APROBADA", "RECIBIDA_PARCIAL", "RECIBIDA", "CANCELADA"]
    for i in range(N):
        estatus = estatus_oc[i % 5]
        moneda = "USD" if i % 4 == 0 else "MXN"
        motivo = "Proveedor sin stock" if estatus == "CANCELADA" else None
        ordenes_c.append((uid("orden_compra", i), uid("proveedor", i), moneda, estatus, motivo, 0,
                          cuando(i, 2), f"comprador{i % 5}@demo.empresa.mx"))
        for k in range(2):
            sku = f"SKU-{(i * 2 + k) % N:04d}"
            pedida = 10 + (i + k) % 20
            recibida = 0 if estatus in ("BORRADOR", "APROBADA") else (pedida if estatus == "RECIBIDA" else pedida // 2)
            lineas_oc.append((uid("linea_oc", i * 2 + k), uid("orden_compra", i), sku, pedida, recibida,
                              round(50 + (i + k) * 3.5, 2)))
            if recibida > 0:
                recepciones.append((uid("recepcion", i * 2 + k), uid("orden_compra", i), sku, recibida,
                                    (cuando(i, 2) + dt.timedelta(days=5)).date()))
    bases["compras"] = [
        ("proveedores", ["id", "razon_social", "rfc_cifrado", "rfc_indice", "correo_cifrado", "estatus", "version"],
         proveedores),
        ("ordenes_compra", ["id", "proveedor_id", "moneda", "estatus", "motivo_cancelacion", "version",
                            "creado_utc", "creada_por"], ordenes_c),
        ("lineas_orden", ["id", "orden_id", "articulo_sku", "cantidad_ordenada", "cantidad_recibida",
                          "precio_unitario"], lineas_oc),
        ("recepciones", ["id", "orden_id", "articulo_sku", "cantidad", "fecha"], recepciones),
    ]

    # --- pedidos ---
    cotizaciones, lin_cot, pedidos, lin_ped = [], [], [], []
    canales = ["WEB", "TIENDA", "TELEFONO"]
    tasas = ["IVA_GENERAL", "IVA_FRONTERIZO", "IVA_EXENTO"]
    est_cot = ["BORRADOR", "ENVIADA", "ACEPTADA", "RECHAZADA", "VENCIDA"]
    est_ped = ["BORRADOR", "CONFIRMADO", "ENTREGADO", "CANCELADO"]
    motivos = ["CANCELACION_CLIENTE", "ERROR_CAPTURA", "DEVOLUCION_TOTAL"]
    for i in range(N):
        canal = canales[i % 3]
        tasa = tasas[(i // 3) % 3]
        moneda = "USD" if i % 5 == 0 else "MXN"
        estatus_c = est_cot[i % 5]
        estatus_p = est_ped[(i // 5) % 4]
        motivo = motivos[(i // 20) % 3] if estatus_p == "CANCELADO" else None
        cotizaciones.append((uid("cotizacion", i), uid("cliente", i % N), canal, moneda, tasa,
                             (cuando(i, 2) + dt.timedelta(days=30)).date(), estatus_c, uid("pedido", i)))
        pedidos.append((uid("pedido", i), uid("cliente", i % N), uid("cotizacion", i), canal, moneda, tasa,
                        estatus_p, cuando(i, 2) if estatus_p != "BORRADOR" else None,
                        cuando(i, 5) if estatus_p == "ENTREGADO" else None, motivo, 0))
        for k in range(2):
            producto = uid("articulo", (i * 2 + k) % N)
            desc = ARTICULOS[(i * 2 + k) % len(ARTICULOS)][0]
            lin_cot.append((uid("cotizacion", i), k + 1, producto, desc, 1 + (i + k) % 9,
                            round(80 + (i + k) * 2.25, 2)))
            lin_ped.append((uid("pedido", i), k + 1, producto, desc, 1 + (i + k) % 9,
                            round(80 + (i + k) * 2.25, 2)))
    bases["pedidos"] = [
        ("cotizaciones", ["id", "cliente_id", "canal", "moneda", "tasa", "vigente_hasta", "estatus", "pedido_id"],
         cotizaciones),
        ("lineas_cotizacion", ["cotizacion_id", "posicion", "producto_id", "descripcion", "cantidad",
                               "precio_unitario"], lin_cot),
        ("pedidos", ["id", "cliente_id", "cotizacion_id", "canal", "moneda", "tasa", "estatus",
                     "fecha_confirmacion", "fecha_entrega", "motivo_cancelacion", "version"], pedidos),
        ("lineas_pedido", ["pedido_id", "posicion", "producto_id", "descripcion", "cantidad",
                           "precio_unitario"], lin_ped),
    ]

    # --- facturacion_pagos ---
    tasa_valor = {"IVA_GENERAL": 0.16, "IVA_FRONTERIZO": 0.08, "IVA_EXENTO": 0.0}
    comprobantes, pagos, cuentas, pagos_prov = [], [], [], []
    for i in range(N):
        subtotal = round(500 + i * 37.5, 2)
        tasa = tasa_valor[tasas[(i // 3) % 3]]
        iva = round(subtotal * tasa, 2)
        total = round(subtotal + iva, 2)
        estatus = ["BORRADOR", "EMITIDO", "CANCELADO"][i % 3]
        moneda = "USD" if i % 5 == 0 else "MXN"
        motivo = "Error en datos fiscales" if estatus == "CANCELADO" else None
        comprobantes.append((uid("comprobante", i), uid("pedido", i), f"FAC-{i:05d}", moneda, subtotal, iva,
                             total, tasa, estatus, motivo, 0, cuando(i, 2), cuando(i, 2)))
        if estatus == "EMITIDO":
            pagos.append((uid("pago", i), uid("comprobante", i), round(total / 2, 2),
                          (cuando(i, 2) + dt.timedelta(days=10)).date(), f"TRX-{i:06d}", cuando(i, 2)))
        monto = round(300 + i * 21.1, 2)
        cuentas.append((uid("cuenta_pagar", i), uid("orden_compra", i), uid("proveedor", i), moneda, monto, 0,
                        cuando(i, 2)))
        if i % 2 == 0:
            pagos_prov.append((uid("pago_proveedor", i), uid("cuenta_pagar", i), round(monto / 3, 2),
                               (cuando(i, 2) + dt.timedelta(days=12)).date(), f"SPEI-{i:06d}", cuando(i, 2)))
    bases["facturacion_pagos"] = [
        ("comprobantes", ["id", "orden_id", "folio", "moneda", "subtotal", "iva", "total", "tasa_iva", "estatus",
                          "motivo_cancelacion", "version", "creado_utc", "actualizado_utc"], comprobantes),
        ("pagos", ["id", "comprobante_id", "monto", "fecha_pago", "referencia", "creado_utc"], pagos),
        ("cuentas_por_pagar", ["id", "orden_id", "proveedor_id", "moneda", "monto", "version", "creado_utc"],
         cuentas),
        ("pagos_proveedor", ["id", "cuenta_id", "monto", "fecha_pago", "referencia", "creado_utc"], pagos_prov),
    ]

    # --- contabilidad ---
    cuentas_c = [(uid("cuenta_contable", k), codigo, nombre, nat, k != 11)
                 for k, (codigo, nombre, nat) in enumerate(CUENTAS)]
    periodos = [(p, "CERRADO" if k < 9 else "ABIERTO", 0) for k, p in enumerate(PERIODOS)]
    polizas, movs = [], []
    tipos_p = ["INGRESO", "EGRESO", "DIARIO"]
    est_p = ["BORRADOR", "APLICADA", "REVERSADA"]
    for i in range(N):
        mes = PERIODOS[i % 12]
        reversa = uid("poliza", i - 1) if est_p[i % 3] == "REVERSADA" and i > 0 else None
        polizas.append((uid("poliza", i), mes, tipos_p[i % 3], cuando(i, 2).date(), est_p[i % 3], reversa, 0))
        monto = round(100 + i * 12.5, 2)
        cargo_c = CUENTAS[(i * 2) % len(CUENTAS)]
        abono_c = CUENTAS[(i * 2 + 1) % len(CUENTAS)]
        movs.append((uid("movimiento_contable", 2 * i), uid("poliza", i), uid("cuenta_contable",
                     CUENTAS.index(cargo_c)), monto, 0))
        movs.append((uid("movimiento_contable", 2 * i + 1), uid("poliza", i), uid("cuenta_contable",
                     CUENTAS.index(abono_c)), 0, monto))
    bases["contabilidad"] = [
        ("periodos", ["anio_mes", "estatus", "version"], periodos),
        ("cuentas_contables", ["id", "codigo", "nombre", "naturaleza", "activa"], cuentas_c),
        ("polizas", ["id", "anio_mes", "tipo", "fecha", "estatus", "reversa_de", "version"], polizas),
        ("movimientos", ["id", "poliza_id", "cuenta_id", "cargo", "abono"], movs),
    ]

    # --- documentos ---
    documentos, versiones = [], []
    tipos_d = ["CONTRATO", "PERMISO", "REGISTRO_LEGAL"]
    for i in range(N):
        tipo = tipos_d[i % 3]
        eliminado = i % 7 == 0
        motivo = "Solicitud del área" if eliminado else None
        eliminado_utc = cuando(i, 1) if eliminado else None
        documentos.append((uid("documento", i), tipo, f"Documento demo {i:03d}", AREAS_USUARIO[i % 6],
                           (cuando(i, 2) + dt.timedelta(days=200)).date(),
                           (cuando(i, 2) + dt.timedelta(days=900)).date(), eliminado, motivo, eliminado_utc,
                           i % 4 == 0, i % 5 == 0, 0, False, None))
        for num in range(1, 1 + (i % 3) + 1):
            versiones.append((uid("version_documento", i * 10 + num), uid("documento", i), num,
                              hashlib.sha256(f"doc-{i}-v{num}".encode()).hexdigest(),
                              f"almacenamiento-demo/doc-{i:03d}-v{num}.pdf", cuando(i, 1)))
    bases["documentos"] = [
        ("documentos", ["id", "tipo", "titulo", "area", "vence_en", "retencion_hasta", "eliminado",
                        "motivo_eliminacion", "eliminado_utc", "revision_retencion",
                        "alerta_vencimiento_emitida", "version", "purgado", "purgado_utc"], documentos),
        ("versiones_documento", ["id", "documento_id", "numero", "hash_sha256", "almacenamiento_ref",
                                 "creado_utc"], versiones),
    ]

    # --- reportes ---
    indicadores, reportes, reporte_ind = [], [], []
    for i in range(N):
        area = AREAS_USUARIO[i % 6]
        nombre = INDICADORES[(i // 6) % 4]
        periodo = PERIODOS[(i // 24) % 12]
        indicadores.append((uid("indicador", i), area, nombre, periodo, round(1000 + i * 17.3, 4), cuando(i, 1),
                            0))
    # Un índice único por (área, nombre, periodo): se evita repetir la combinación.
    vistos, depurados = set(), []
    for fila in indicadores:
        clave = (fila[1], fila[2], fila[3])
        if clave not in vistos:
            vistos.add(clave)
            depurados.append(fila)
    indicadores = depurados
    for k, periodo in enumerate(PERIODOS):
        estatus = "CERRADO" if k < 9 else "BORRADOR"
        cerrado = cuando(k, 20) if estatus == "CERRADO" else None
        reportes.append((uid("reporte", k), periodo, estatus, cuando(k, 20), cerrado, 0))
        for pos, ind in enumerate(indicadores[k * 4: k * 4 + 4]):
            reporte_ind.append((uid("reporte_indicador", k * 10 + pos), uid("reporte", k), pos + 1,
                                ind[1], ind[2], ind[4], ind[5],
                                (k % 3 == 0 and pos == 0)))
    bases["reportes"] = [
        ("indicadores", ["id", "area", "nombre", "periodo", "valor", "fuente_actualizada_utc", "version"],
         indicadores),
        ("reportes", ["id", "periodo", "estatus", "creado_utc", "cerrado_utc", "version"], reportes),
        ("reporte_indicadores", ["id", "reporte_id", "posicion", "area", "nombre", "valor",
                                 "fuente_actualizada_utc", "desactualizado"], reporte_ind),
    ]
    return bases


# ---------- ejecución ----------

def psql_cmd(base, opciones):
    if opciones.psql:
        return opciones.psql.split() + ["-d", base, "-v", "ON_ERROR_STOP=1", "-q"]
    return ["docker", "compose", "-f", "docker-compose.yml", "exec", "-T", "postgres",
            "psql", "-U", "postgres", "-d", base, "-v", "ON_ERROR_STOP=1", "-q"]


def correr(base, sql_texto, opciones, salida=False):
    proc = subprocess.run(psql_cmd(base, opciones) + (["-Atc", sql_texto] if salida else ["-f", "-"]),
                          input=None if salida else sql_texto.encode("utf-8"),
                          capture_output=True, cwd=RAIZ)
    if proc.returncode != 0:
        raise RuntimeError(f"[{base}] {proc.stderr.decode('utf-8', 'replace').strip()}")
    return proc.stdout.decode("utf-8").strip()


def main():
    ap = argparse.ArgumentParser(description="Carga datos de demo en las bases de EMPRESA_JOSESAID.")
    ap.add_argument("--reset", action="store_true", help="borra los datos de negocio antes de cargar")
    ap.add_argument("--psql", default="", help="comando psql alternativo (pruebas), p. ej. 'psql -h 127.0.0.1 -p 5432 -U postgres'")
    opciones = ap.parse_args()

    env_ruta = RAIZ / ".env"
    if not env_ruta.exists():
        sys.exit("Falta el archivo .env en la raíz del repo.")
    env = leer_env(env_ruta)
    pares = [("CRYPTO_CLAVE_CIFRADO", "CRYPTO_CLAVE_INDICE"),          # clientes-ms
             ("USUARIOS_CIFRADO_CLAVE", "USUARIOS_INDICE_CLAVE"),      # usuarios-accesos-ms
             ("COMPRAS_CIFRADO_CLAVE", "COMPRAS_INDICE_CLAVE")]        # compras-proveedores-ms
    for cifrado, indice in pares:
        for clave in (cifrado, indice):
            if not env.get(clave):
                sys.exit(f"Falta {clave} en .env (sin este valor no se puede cifrar el servicio correspondiente)")
    bases = generar(Cripto(env, *pares[0]), Cripto(env, *pares[1]), Cripto(env, *pares[2]), random.Random(42))

    for base, tablas in bases.items():
        nombres = [t[0] for t in tablas]
        if not opciones.reset:
            for nombre in nombres:
                cuenta = correr(base, f"SELECT count(*) FROM {nombre}", opciones, salida=True)
                if int(cuenta) > 0:
                    sys.exit(f"La tabla {base}.{nombre} ya tiene datos. Usa --reset para reemplazarlos.")
        print(f"Cargando {base}...", flush=True)
        bloque = ["BEGIN;"]
        if opciones.reset:
            bloque.append("TRUNCATE " + ", ".join(nombres) + " CASCADE;")
        for nombre, columnas, filas in tablas:
            bloque.append(insertar(nombre, columnas, filas))
        bloque.append("COMMIT;")
        correr(base, "\n".join(bloque), opciones)
        print(f"  {base}: " + ", ".join(f"{n}={len(f)}" for n, _, f in tablas))
    print("Listo. Usuarios demo: <rol>@demo.empresa.mx; admin@empresa.mx con todos los roles. Contraseña Demo2026!")


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as e:
        sys.exit(str(e))
