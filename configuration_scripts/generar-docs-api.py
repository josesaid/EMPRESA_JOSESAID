#!/usr/bin/env python3
"""Genera la documentación OpenAPI (Swagger) de cada microservicio con API REST.

Lee los controllers de cada servicio y produce, en docs/api/:
  - <servicio>.json   especificación OpenAPI 3.0
  - <servicio>.html   página de Swagger UI con la especificación incrustada
  - index.html        índice de todos los servicios

Uso (desde la raíz del proyecto):  python3 configuration_scripts/generar-docs-api.py
"""
import glob
import html
import json
import os
import re
import datetime

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SALIDA = os.path.join(RAIZ, "docs", "api")
EXCLUIDOS = {"nexus-comun", "ui-ms"}  # librería, y UI con páginas MVC (no API REST)
SWAGGER_VERSION = "5.17.14"

PRIMITIVOS = {
    "String": {"type": "string"},
    "char": {"type": "string"}, "Character": {"type": "string"},
    "UUID": {"type": "string", "format": "uuid"},
    "int": {"type": "integer", "format": "int32"}, "Integer": {"type": "integer", "format": "int32"},
    "long": {"type": "integer", "format": "int64"}, "Long": {"type": "integer", "format": "int64"},
    "short": {"type": "integer"}, "Short": {"type": "integer"},
    "double": {"type": "number", "format": "double"}, "Double": {"type": "number", "format": "double"},
    "float": {"type": "number", "format": "float"}, "Float": {"type": "number", "format": "float"},
    "BigDecimal": {"type": "number"},
    "boolean": {"type": "boolean"}, "Boolean": {"type": "boolean"},
    "LocalDate": {"type": "string", "format": "date"},
    "LocalDateTime": {"type": "string", "format": "date-time"},
    "OffsetDateTime": {"type": "string", "format": "date-time"},
    "ZonedDateTime": {"type": "string", "format": "date-time"},
    "Instant": {"type": "string", "format": "date-time"},
    "Object": {"type": "object"},
}
# Tipos de Spring/seguridad que no son parámetros de la API
IGNORAR_PARAMETROS = {"Authentication", "JwtAuthenticationToken", "Jwt", "Principal",
                      "HttpServletRequest", "HttpServletResponse", "HttpSession", "Model",
                      "BindingResult", "Errors", "WebRequest", "HttpHeaders"}

MAPEO = re.compile(
    r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(\s*(?:value\s*=\s*|path\s*=\s*)?(?:"([^"]*)")?[^)]*\))?')
ANOTACION = r'@[\w.]+(?:\((?:[^()"]|"[^"]*")*\))?'
FIRMA = re.compile(
    r'((?:\s*' + ANOTACION + r')*)\s*(?:public\s+|protected\s+|private\s+)?'
    r'([\w<>\[\],.?\s]+?)\s+(\w+)\s*\(([\s\S]*?)\)\s*(?:throws[\w\s,.]+)?\{')


def leer(ruta):
    with open(ruta, encoding="utf-8") as f:
        return f.read()


def dividir_nivel_superior(texto):
    """Separa por comas que no estén dentro de <>, () ni []."""
    partes, nivel, actual = [], 0, ""
    for c in texto:
        if c in "<([":
            nivel += 1
        elif c in ">)]":
            nivel -= 1
        if c == "," and nivel == 0:
            partes.append(actual.strip())
            actual = ""
        else:
            actual += c
    if actual.strip():
        partes.append(actual.strip())
    return [p for p in partes if p]


def tipo_simple(tipo):
    """Quita 'final', anotaciones y genéricos de tipo: 'List<Foo>' -> ('List', 'Foo')."""
    tipo = re.sub(ANOTACION, "", tipo).replace("final ", "").strip()
    m = re.match(r"([\w.]+)\s*<\s*(.+)\s*>$", tipo)
    if m:
        interno = dividir_nivel_superior(m.group(2))[-1]
        return m.group(1).split(".")[-1], tipo_simple(interno)[0]
    return tipo.split(".")[-1], None


def buscar_dto(svc_dir, nombre):
    """Busca el archivo del tipo: primero por nombre, luego un record o clase con ese nombre."""
    for ruta in glob.glob(os.path.join(svc_dir, "src", "main", "java", "**", nombre + ".java"), recursive=True):
        return leer(ruta)
    patron = re.compile(r"\b(record|class)\s+" + re.escape(nombre) + r"\b")
    for ruta in glob.glob(os.path.join(svc_dir, "src", "main", "java", "**", "*.java"), recursive=True):
        texto = leer(ruta)
        if patron.search(texto):
            return texto
    return None


def campos_de_dto(texto, nombre):
    """Devuelve [(tipo, nombre)] de un record o de una clase con campos private."""
    m = re.search(r"record\s+" + re.escape(nombre) + r"\s*\(", texto)
    if m:
        inicio, nivel, i = m.end(), 1, m.end()
        while nivel and i < len(texto):
            nivel += {"(": 1, ")": -1}.get(texto[i], 0)
            i += 1
        campos = []
        for parte in dividir_nivel_superior(texto[inicio:i - 1]):
            parte = re.sub(ANOTACION, "", parte).strip()
            tipo, _, campo = parte.rpartition(" ")
            if tipo and campo:
                campos.append((tipo.strip(), campo.strip()))
        return campos
    return [(t.strip(), c) for t, c in re.findall(
        r"^\s*private\s+(?:final\s+)?([\w<>\[\],.?\s]+?)\s+(\w+)\s*(?:=[^;]*)?;", texto, re.M)]


def esquema_de(tipo, svc_dir, componentes, profundidad=0):
    base, interno = tipo_simple(tipo)
    if base in ("List", "Set", "Collection", "Iterable"):
        return {"type": "array", "items": esquema_de(interno or "Object", svc_dir, componentes, profundidad)}
    if base in ("Optional", "ResponseEntity", "Page", "Slice") and interno:
        return esquema_de(interno, svc_dir, componentes, profundidad)
    if base in PRIMITIVOS:
        return dict(PRIMITIVOS[base])
    if base.endswith("Map") or base == "Map":
        return {"type": "object", "additionalProperties": True}
    if base in componentes or profundidad > 3:
        return {"$ref": "#/components/schemas/" + base} if base in componentes else {"type": "object"}
    texto = buscar_dto(svc_dir, base)
    if texto is None:  # enum u otro tipo fuera del servicio
        return {"type": "string", "description": base}
    componentes[base] = {"type": "object", "properties": {}}
    props = {}
    for ctipo, cnombre in campos_de_dto(texto, base):
        props[cnombre] = esquema_de(ctipo, svc_dir, componentes, profundidad + 1)
    componentes[base]["properties"] = props
    return {"$ref": "#/components/schemas/" + base}


def nombre_legible(metodo):
    palabras = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", metodo).lower().split()
    return (" ".join(palabras)).capitalize() if palabras else metodo


def unir_ruta(base, sub):
    ruta = "/" + "/".join(p for p in (base.strip("/"), sub.strip("/")) if p)
    return ruta or "/"


def controladores(svc_dir):
    """Archivos Java de API REST: los que tienen @RestController (o @Controller con mapeos)."""
    rutas = []
    for ruta in sorted(glob.glob(os.path.join(svc_dir, "src", "main", "java", "**", "*.java"), recursive=True)):
        texto = leer(ruta)
        if "@RestController" in texto or ("@Controller" in texto and MAPEO.search(texto)):
            rutas.append(ruta)
    return rutas


def extraer_endpoints(svc_dir, svc, componentes):
    endpoints = []
    for ruta in controladores(svc_dir):
        texto = leer(ruta)
        clase = os.path.basename(ruta)[:-5]
        cabecera = texto[:texto.index("public class")] if "public class" in texto else texto
        prefijos = re.findall(r'@RequestMapping\(\s*(?:value\s*=\s*|path\s*=\s*)?"([^"]*)"', cabecera)
        prefijo = prefijos[-1] if prefijos else ""
        for m in MAPEO.finditer(texto):
            verbo = m.group(1).lower()
            ruta_met = m.group(2) or ""
            firma = FIRMA.match(texto, m.end())
            if not firma:
                continue
            anotaciones, tipo_retorno, metodo, params_txt = firma.group(1), firma.group(2).strip(), firma.group(3), firma.group(4)
            if metodo in ("class", "record"):
                continue
            roles = re.search(r'@PreAuthorize\("([^"]*)"\)', anotaciones)
            parametros, cuerpo = [], None
            for p in dividir_nivel_superior(params_txt):
                tipo, _, nombre = re.sub(ANOTACION, "", p).strip().rpartition(" ")
                if not tipo or not nombre:
                    continue
                base, _ = tipo_simple(tipo)
                if base in IGNORAR_PARAMETROS:
                    continue
                if "@RequestBody" in p:
                    cuerpo = esquema_de(tipo, svc_dir, componentes)
                    continue
                pv = re.search(r'@PathVariable(?:\(\s*(?:value\s*=\s*)?"([^"]*)")?', p)
                rp = re.search(r'@RequestParam(?:\(\s*(?:value\s*=\s*|name\s*=\s*)?"([^"]*)")?', p)
                if pv:
                    parametros.append({"name": pv.group(1) or nombre, "in": "path", "required": True,
                                       "schema": esquema_de(tipo, svc_dir, componentes)})
                elif rp:
                    parametros.append({"name": rp.group(1) or nombre, "in": "query",
                                       "required": "required=false" not in p and "defaultValue" not in p,
                                       "schema": esquema_de(tipo, svc_dir, componentes)})
                elif base == "Pageable":
                    for qn, qs in (("page", {"type": "integer", "default": 0}),
                                   ("size", {"type": "integer", "default": 20}),
                                   ("sort", {"type": "string"})):
                        parametros.append({"name": qn, "in": "query", "required": False, "schema": qs})
            ruta_completa = unir_ruta(prefijo, ruta_met)
            # Los parámetros de ruta deben aparecer en el path
            for nombre_ruta in re.findall(r"\{(\w+)\}", ruta_completa):
                if not any(x["in"] == "path" and x["name"] == nombre_ruta for x in parametros):
                    parametros.append({"name": nombre_ruta, "in": "path", "required": True, "schema": {"type": "string"}})
            respuesta_tipo = esquema_de(tipo_retorno, svc_dir, componentes) if tipo_retorno not in ("void", "") else None
            ok = {"description": "Respuesta exitosa"}
            if respuesta_tipo:
                ok["content"] = {"application/json": {"schema": respuesta_tipo}}
            operacion = {
                "operationId": f"{svc}.{clase}.{metodo}",
                "summary": nombre_legible(metodo),
                "tags": [clase.replace("Controller", "")],
                "security": [{"bearerAuth": []}],
                "responses": {
                    ("201" if verbo == "post" and not ruta_completa.endswith(("/estatus", "/cancelar")) else "200"): ok,
                    "400": {"description": "Solicitud inválida"},
                    "401": {"description": "Token ausente o inválido"},
                    "403": {"description": "Sin permiso para esta operación"},
                },
            }
            if roles:
                operacion["description"] = "Roles requeridos: " + roles.group(1)
            if parametros:
                operacion["parameters"] = parametros
            if cuerpo is not None:
                operacion["requestBody"] = {"required": True, "content": {"application/json": {"schema": cuerpo}}}
            endpoints.append((ruta_completa, verbo, operacion))
    return endpoints


def construir_spec(svc):
    svc_dir = os.path.join(RAIZ, svc)
    componentes = {}
    endpoints = extraer_endpoints(svc_dir, svc, componentes)
    paths = {}
    for ruta, verbo, op in endpoints:
        paths.setdefault(ruta, {})[verbo] = op
    descripcion = "Documentación generada desde los controllers del servicio " + svc + "."
    readme = os.path.join(svc_dir, "README.md")
    if os.path.exists(readme):
        for linea in leer(readme).splitlines():
            if linea.strip() and not linea.startswith("#"):
                descripcion = linea.strip()
                break
    return {
        "openapi": "3.0.3",
        "info": {"title": "API " + svc, "version": "0.1.0", "description": descripcion},
        "servers": [{"url": "/", "description": "Ruta relativa del servicio"}],
        "tags": sorted({op["tags"][0] for _, _, op in endpoints}) and [
            {"name": t} for t in sorted({op["tags"][0] for _, _, op in endpoints})],
        "paths": dict(sorted(paths.items())),
        "components": {
            "securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}},
            "schemas": componentes,
        },
    }, len(endpoints)


# ---------- Fichas por microservicio ----------

GITHUB_BLOB = "https://github.com/josesaid/EMPRESA_JOSESAID/blob/main"
GITHUB_RAW = "https://raw.githubusercontent.com/josesaid/EMPRESA_JOSESAID/main"
EXTENSIONES_IMAGEN = (".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp")
EXTENSIONES_DIAGRAMA = (".mmd", ".png", ".svg")


def servicios_documentados():
    return sorted(
        d for d in os.listdir(RAIZ)
        if os.path.isdir(os.path.join(RAIZ, d)) and d not in EXCLUIDOS
        and os.path.exists(os.path.join(RAIZ, d, "pom.xml")))


def texto_conteo(n):
    if not n:
        return "Sin API REST"
    return f"{n} endpoint{'s' if n != 1 else ''}"


def leer_pom(svc_dir):
    texto = leer(os.path.join(svc_dir, "pom.xml"))

    def etiqueta(nombre):
        m = re.search(rf"<{nombre}>([^<]*)</{nombre}>", texto)
        return m.group(1).strip() if m else ""

    java = re.search(r"<java\.version>([^<]+)</java\.version>", texto)
    padre = re.search(r"<parent>[\s\S]*?<version>([^<]+)</version>", texto)
    return {
        "descripcion": etiqueta("description"),
        "java": java.group(1).strip() if java else "",
        "spring_boot": padre.group(1).strip() if padre else "",
    }


def puerto_de(svc_dir):
    for ruta in sorted(glob.glob(os.path.join(svc_dir, "src", "main", "resources", "application*.y*ml"))):
        dentro = False
        for linea in leer(ruta).splitlines():
            if re.match(r"^server:\s*$", linea):
                dentro = True
                continue
            if dentro and linea and not linea.startswith((" ", "\t")):
                dentro = False
            if dentro:
                m = re.match(r"^\s+port:\s*(.+)$", linea)
                if m:
                    valor = m.group(1).strip()
                    d = re.search(r":(\d+)\}", valor)
                    return d.group(1) if d else valor
    return ""


def filas_permisos(spec):
    filas = []
    if not spec:
        return filas
    for ruta, ops in spec["paths"].items():
        for verbo, op in ops.items():
            desc = op.get("description", "")
            roles = desc.replace("Roles requeridos: ", "") if desc.startswith("Roles requeridos:") else ""
            filas.append({"verbo": verbo.upper(), "ruta": ruta,
                          "operacion": op.get("summary", ""), "roles": roles})
    return filas


def reescribir_readme(svc, texto):
    import posixpath

    def cambiar(m):
        ruta = m.group(1)
        if re.match(r"^(https?:|mailto:|#|/)", ruta):
            return m.group(0)
        if ruta.startswith("docs/diagramas/"):
            return "](" + svc + "/diagramas/" + ruta[len("docs/diagramas/"):] + ")"
        repo = posixpath.normpath(posixpath.join(svc, ruta))
        base = GITHUB_RAW if ruta.lower().endswith(EXTENSIONES_IMAGEN) else GITHUB_BLOB
        return "](" + base + "/" + repo + ")"

    return re.sub(r"\]\(([^)\s]+)\)", cambiar, texto)


def copiar_diagramas(svc):
    import shutil
    origen = os.path.join(RAIZ, svc, "docs", "diagramas")
    destino = os.path.join(SALIDA, svc, "diagramas")
    if os.path.isdir(destino):
        shutil.rmtree(destino)
    if not os.path.isdir(origen):
        return []
    os.makedirs(destino, exist_ok=True)
    grupos = {}
    for nombre in sorted(os.listdir(origen)):
        base, ext = os.path.splitext(nombre)
        ext = ext.lower()
        if ext not in EXTENSIONES_DIAGRAMA:
            continue
        shutil.copy2(os.path.join(origen, nombre), os.path.join(destino, nombre))
        g = grupos.setdefault(base, {"titulo": base.replace("-", " ").replace("_", " "),
                                     "archivos": [], "imagen": None, "codigo": None})
        ruta_web = f"{svc}/diagramas/{nombre}"
        g["archivos"].append(ruta_web)
        if ext == ".mmd":
            g["codigo"] = leer(os.path.join(origen, nombre))
        else:
            g["imagen"] = ruta_web
    return list(grupos.values())


PLANTILLA_FICHA = r"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>__SVC__ · EMPRESA_JOSESAID</title>
<link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@__SWV__/swagger-ui.css">
<style>
:root{--bg:#f5f7fb;--superficie:#ffffff;--fg:#0f172a;--muted:#64748b;--linea:#e2e8f0;--sombra:rgba(15,23,42,.08);color-scheme:light}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#0b1020;--superficie:#131a2e;--fg:#e2e8f0;--muted:#94a3b8;--linea:#23304f;--sombra:rgba(0,0,0,.4);color-scheme:dark}}
:root[data-theme="dark"]{--bg:#0b1020;--superficie:#131a2e;--fg:#e2e8f0;--muted:#94a3b8;--linea:#23304f;--sombra:rgba(0,0,0,.4);color-scheme:dark}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.55 system-ui,-apple-system,"Segoe UI",sans-serif;padding-bottom:40px}
a{color:hsl(var(--h) 70% 45%)}
.cabecera{background:linear-gradient(135deg,hsl(var(--h) 72% 40%),hsl(calc(var(--h) + 30) 78% 26%));color:#fff;padding:28px max(16px,env(safe-area-inset-left,0px)) 64px}
.cabecera-in{max-width:1000px;margin:0 auto;min-width:0}
.volver{color:#fff;opacity:.9;font-size:14px;text-decoration:none}
.etiqueta{display:inline-block;font-size:12px;letter-spacing:.08em;text-transform:uppercase;background:rgba(255,255,255,.2);padding:4px 10px;border-radius:999px;margin:14px 0 10px}
h1{margin:0 0 8px;font-size:30px;line-height:1.2;text-wrap:balance;overflow-wrap:anywhere}
.lead{margin:0;opacity:.92;max-width:70ch}
.pildoras{display:flex;flex-wrap:wrap;gap:8px;margin-top:16px}
.pildora{background:rgba(255,255,255,.18);border-radius:999px;padding:4px 12px;font-size:13px}
main{max-width:1000px;margin:-40px auto 0;padding:0 max(16px,env(safe-area-inset-left,0px));min-width:0}
nav.tabs{display:flex;gap:4px;overflow-x:auto;background:var(--superficie);border:1px solid var(--linea);border-radius:14px;padding:6px;box-shadow:0 6px 20px var(--sombra)}
nav.tabs button{flex:1 0 auto;border:0;background:transparent;color:var(--muted);font:600 14px system-ui,sans-serif;padding:10px 14px;border-radius:10px;cursor:pointer}
nav.tabs button[aria-selected="true"]{background:hsl(var(--h) 80% 50% / .14);color:hsl(var(--h) 70% 35%)}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]) nav.tabs button[aria-selected="true"]{color:hsl(var(--h) 90% 75%)}}
:root[data-theme="dark"] nav.tabs button[aria-selected="true"]{color:hsl(var(--h) 90% 75%)}
.panel{margin-top:18px;background:var(--superficie);border:1px solid var(--linea);border-radius:14px;padding:20px;box-shadow:0 6px 20px var(--sombra);min-width:0}
[hidden]{display:none!important}
dl.datos{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:16px 24px;margin:0}
dl.datos dt{font-size:12px;letter-spacing:.06em;text-transform:uppercase;color:var(--muted)}
dl.datos dd{margin:2px 0 0;font-weight:600;overflow-wrap:anywhere}
.chip{display:inline-block;font:600 12px ui-monospace,monospace;background:hsl(var(--h) 80% 50% / .16);color:hsl(var(--h) 70% 35%);border-radius:6px;padding:3px 8px;margin:2px 4px 2px 0}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]) .chip{color:hsl(var(--h) 90% 78%)}}
:root[data-theme="dark"] .chip{color:hsl(var(--h) 90% 78%)}
.subtitulo{font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:var(--muted);margin:0 0 10px}
.tabla-wrap{overflow-x:auto;max-width:100%}
table{width:100%;border-collapse:collapse;font-size:14px}
th,td{text-align:left;padding:10px 8px;border-bottom:1px solid var(--linea);vertical-align:top;overflow-wrap:anywhere}
th{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.06em}
.verbo{font:700 12px ui-monospace,monospace;border-radius:6px;padding:3px 7px;background:hsl(var(--h) 80% 50% / .18);color:hsl(var(--h) 70% 35%)}
.md{overflow-wrap:anywhere;max-width:100%}
.md img{max-width:100%;height:auto}
.md pre{overflow-x:auto;background:rgba(127,127,127,.12);padding:12px;border-radius:10px}
.md table{display:block;overflow-x:auto}
.md h1,.md h2,.md h3{line-height:1.25}
.grid-diag{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(300px,100%),1fr));gap:16px}
.diag{border:1px solid var(--linea);border-radius:12px;padding:12px;background:var(--bg);min-width:0}
.diag h3{font-size:15px;margin:0 0 8px;overflow-wrap:anywhere}
.diag a.abrir{display:block;text-align:center}
.diag img{max-width:100%;height:auto;border-radius:8px;background:#fff}
.diag .mermaid{overflow-x:auto;text-align:center}
.diag .pie{font-size:13px;margin-top:8px;overflow-wrap:anywhere}
.aviso{color:var(--muted)}
#swagger-ui{max-width:100%;overflow-x:auto}
.swagger-ui .topbar{display:none}
</style>
</head>
<body style="--h:__HUE__">
<header class="cabecera">
  <div class="cabecera-in">
    <a class="volver" href="index.html">← Todos los microservicios</a>
    <div><span class="etiqueta">Microservicio</span></div>
    <h1 id="titulo">__SVC__</h1>
    <p class="lead" id="descr"></p>
    <div class="pildoras" id="pildoras"></div>
  </div>
</header>
<main>
  <nav class="tabs" role="tablist" aria-label="Secciones">
    <button role="tab" data-tab="info" aria-selected="true">Información</button>
    <button role="tab" data-tab="readme" aria-selected="false">README</button>
    <button role="tab" data-tab="permisos" aria-selected="false">Roles y permisos</button>
    <button role="tab" data-tab="diagramas" aria-selected="false">Diagramas</button>
    <button role="tab" data-tab="tecnologias" aria-selected="false">Calidad y tecnologías</button>
    <button role="tab" data-tab="api" aria-selected="false">API (Swagger)</button>
  </nav>
  <section class="panel" data-panel="info"></section>
  <section class="panel" data-panel="readme" hidden></section>
  <section class="panel" data-panel="permisos" hidden></section>
  <section class="panel" data-panel="diagramas" hidden></section>
  <section class="panel" data-panel="tecnologias" hidden></section>
  <section class="panel" data-panel="api" hidden><div id="swagger-ui"></div></section>
</main>
<script type="application/json" id="datos">__DATOS__</script>
<script type="application/json" id="spec">__SPEC__</script>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>
<script src="https://unpkg.com/swagger-ui-dist@__SWV__/swagger-ui-bundle.js"></script>
<script>
(function () {
  const D = JSON.parse(document.getElementById('datos').textContent);
  const SPEC = JSON.parse(document.getElementById('spec').textContent);
  const $ = (s) => document.querySelector(s);

  function el(tag, attrs, hijos) {
    const n = document.createElement(tag);
    attrs = attrs || {};
    Object.keys(attrs).forEach(function (k) {
      if (k === 'text') n.textContent = attrs[k];
      else if (k === 'class') n.className = attrs[k];
      else n.setAttribute(k, attrs[k]);
    });
    (hijos || []).forEach(function (h) {
      if (h === null || h === undefined || h === false) return;
      n.append(h.nodeType ? h : document.createTextNode(String(h)));
    });
    return n;
  }

  function chips(lista, vacio) {
    if (!lista || !lista.length) return [el('span', { class: 'aviso', text: vacio })];
    return lista.map(function (r) { return el('span', { class: 'chip', text: r }); });
  }

  if (window.mermaid) {
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'loose',
      theme: window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'default'
    });
  }

  function pintarInfo() {
    const i = D.info;
    $('#titulo').textContent = i.nombre;
    $('#descr').textContent = i.descripcion || '';
    const pild = $('#pildoras');
    [
      'Puerto ' + (i.puerto || '—'),
      'Java ' + (i.java || '—'),
      'Spring Boot ' + (i.spring_boot || '—'),
      i.endpoints ? i.endpoints + ' endpoints' : 'Sin API REST'
    ].forEach(function (t) { pild.append(el('span', { class: 'pildora', text: t })); });
    const campos = [
      ['Servicio', i.nombre],
      ['Descripción', i.descripcion || '—'],
      ['Puerto de la aplicación', i.puerto || '—'],
      ['Java', i.java || '—'],
      ['Spring Boot', i.spring_boot || '—'],
      ['Endpoints REST', i.endpoints || 'Ninguno']
    ];
    const dl = el('dl', { class: 'datos' });
    campos.forEach(function (c) {
      dl.append(el('div', {}, [el('dt', { text: c[0] }), el('dd', { text: String(c[1]) })]));
    });
    dl.append(el('div', {}, [el('dt', { text: 'Roles detectados' }),
      el('dd', {}, chips(i.roles, 'Ninguno detectado'))]));
    $('[data-panel="info"]').append(dl);
  }

  function pintarReadme() {
    const panel = $('[data-panel="readme"]');
    if (!D.readme) {
      panel.append(el('p', { class: 'aviso', text: 'Este servicio no tiene README.md.' }));
      return;
    }
    const art = el('article', { class: 'md' });
    if (window.marked) {
      art.innerHTML = marked.parse(D.readme);
    } else {
      art.append(el('pre', { text: D.readme }));
    }
    art.querySelectorAll('pre > code.language-mermaid').forEach(function (code) {
      const m = el('div', { class: 'mermaid' });
      m.dataset.src = code.textContent;
      code.parentElement.replaceWith(m);
    });
    panel.append(art);
  }

  function pintarPermisos() {
    const panel = $('[data-panel="permisos"]');
    panel.append(el('h2', { class: 'subtitulo', text: 'Roles detectados' }));
    panel.append(el('p', {}, chips(D.info.roles, 'Ninguno detectado')));
    if (!D.permisos.length) {
      panel.append(el('p', { class: 'aviso', text: 'Este servicio no expone endpoints REST, así que no tiene reglas de acceso por endpoint.' }));
      return;
    }
    const cabeza = el('tr', {}, ['Método', 'Ruta', 'Operación', 'Regla de acceso'].map(function (t) {
      return el('th', { text: t });
    }));
    const cuerpo = el('tbody');
    D.permisos.forEach(function (p) {
      cuerpo.append(el('tr', {}, [
        el('td', {}, [el('span', { class: 'verbo', text: p.verbo })]),
        el('td', { text: p.ruta }),
        el('td', { text: p.operacion }),
        el('td', { text: p.roles || 'Solo autenticación (sin @PreAuthorize)' })
      ]));
    });
    const tabla = el('table', {}, [el('thead', {}, [cabeza]), cuerpo]);
    panel.append(el('div', { class: 'tabla-wrap' }, [tabla]));
  }

  function pintarDiagramas() {
    const panel = $('[data-panel="diagramas"]');
    if (!D.diagramas.length) {
      panel.append(el('p', { class: 'aviso', text: 'Este servicio no tiene carpeta docs/diagramas.' }));
      return;
    }
    const grid = el('div', { class: 'grid-diag' });
    D.diagramas.forEach(function (d) {
      const tarjeta = el('article', { class: 'diag' });
      tarjeta.append(el('h3', { text: d.titulo }));
      if (d.imagen) {
        const img = el('img', { src: d.imagen, alt: d.titulo, loading: 'lazy' });
        img.onerror = function () {
          img.replaceWith(el('p', { class: 'aviso', text: 'No se pudo mostrar la imagen. Usa el enlace de abajo para abrir el archivo.' }));
        };
        tarjeta.append(el('a', { href: d.imagen, target: '_blank', rel: 'noopener', class: 'abrir', title: 'Abrir archivo' }, [img]));
      } else if (d.codigo) {
        const m = el('div', { class: 'mermaid' });
        m.dataset.src = d.codigo;
        tarjeta.append(m);
      }
      const pie = el('div', { class: 'pie' });
      d.archivos.forEach(function (a, idx) {
        if (idx) pie.append(' · ');
        pie.append(el('a', { href: a, target: '_blank', rel: 'noopener', text: a.split('/').pop() }));
      });
      tarjeta.append(pie);
      grid.append(tarjeta);
    });
    panel.append(grid);
  }

  async function renderMermaidIn(raiz) {
    if (!raiz) return;
    for (const nodo of raiz.querySelectorAll('.mermaid')) {
      if (nodo.dataset.ok) continue;
      const src = nodo.dataset.src || '';
      if (!window.mermaid) {
        nodo.replaceChildren(el('pre', { text: src }));
        continue;
      }
      try {
        const salida = await mermaid.render('m' + Math.random().toString(36).slice(2), src);
        nodo.innerHTML = salida.svg;
        nodo.dataset.ok = '1';
      } catch (e) {
        nodo.replaceChildren(
          el('p', { class: 'aviso', text: 'No se pudo renderizar este diagrama. Se muestra el código fuente:' }),
          el('pre', { text: src }));
      }
    }
  }

  function pintarTecnologias() {
    const panel = $('[data-panel="tecnologias"]');
    panel.append(el('p', { class: 'aviso', text: 'Herramientas de calidad y pruebas que el pom.xml de este servicio aplica. Las marcadas como no aplicadas no están configuradas.' }));
    const grid = el('div', { class: 'grid-diag' });
    D.tecnologias.forEach(function (t) {
      const tarjeta = el('article', { class: 'diag' });
      tarjeta.append(el('h3', { text: t.nombre }));
      tarjeta.append(el('p', { class: 'aviso', text: t.resumen }));
      if (t.aplicado) {
        tarjeta.append(el('a', { href: t.archivo, class: 'chip', text: 'Ver detalle →' }));
      } else {
        tarjeta.append(el('span', { class: 'chip', text: 'No aplicado' }));
      }
      grid.append(tarjeta);
    });
    panel.append(grid);
  }

  let swaggerListo = false;
  function iniciarSwagger() {
    if (swaggerListo || !SPEC || !window.SwaggerUIBundle) return;
    swaggerListo = true;
    window.ui = SwaggerUIBundle({ spec: SPEC, dom_id: '#swagger-ui', deepLinking: false, persistAuthorization: true });
  }

  function pintarApi() {
    if (SPEC) return;
    $('[data-panel="api"]').replaceChildren(el('p', { class: 'aviso', text: 'Este servicio no expone endpoints REST, así que no tiene Swagger.' }));
    $('[data-tab="api"]').hidden = true;
  }

  const botones = Array.from(document.querySelectorAll('[role="tab"]'));
  function activar(nombre, cambiarHash) {
    if (nombre === 'api' && !SPEC) nombre = 'info';
    botones.forEach(function (b) { b.setAttribute('aria-selected', String(b.dataset.tab === nombre)); });
    document.querySelectorAll('[data-panel]').forEach(function (p) { p.hidden = p.dataset.panel !== nombre; });
    if (nombre === 'readme' || nombre === 'diagramas') renderMermaidIn(document.querySelector('[data-panel="' + nombre + '"]'));
    if (nombre === 'api') iniciarSwagger();
    if (cambiarHash) { try { history.replaceState(null, '', '#' + nombre); } catch (e) {} }
  }
  botones.forEach(function (b) { b.addEventListener('click', function () { activar(b.dataset.tab, true); }); });

  pintarInfo();
  pintarReadme();
  pintarPermisos();
  pintarDiagramas();
  pintarApi();
  pintarTecnologias();
  const inicial = location.hash.slice(1);
  activar(['info', 'readme', 'permisos', 'diagramas', 'tecnologias', 'api'].indexOf(inicial) >= 0 ? inicial : 'info', false);
})();
</script>
</body>
</html>
"""


def pagina_ficha(svc, datos, spec, hue):
    json_datos = json.dumps(datos, ensure_ascii=False).replace("</", "<\\/")
    json_spec = json.dumps(spec, ensure_ascii=False).replace("</", "<\\/") if spec else "null"
    return (PLANTILLA_FICHA
            .replace("__SVC__", html.escape(svc))
            .replace("__HUE__", f"{hue:.0f}")
            .replace("__SWV__", SWAGGER_VERSION)
            .replace("__DATOS__", json_datos)
            .replace("__SPEC__", json_spec))


TECNOLOGIAS = [
    {"id": "pruebas", "nombre": "Pruebas automatizadas", "plugin": None,
     "que": "Pruebas unitarias y de integración con JUnit 5. Surefire ejecuta las unitarias y Failsafe las de integración.",
     "comando": "mvn -f {svc}/pom.xml verify",
     "reportes": "target/surefire-reports y target/failsafe-reports"},
    {"id": "checkstyle", "nombre": "Checkstyle", "plugin": "maven-checkstyle-plugin",
     "que": "Revisa convenciones de estilo del código Java (nombres, tamaños de métodos, imports) contra un conjunto de reglas.",
     "comando": "mvn -f {svc}/pom.xml checkstyle:check",
     "reportes": "target/checkstyle-result.xml"},
    {"id": "pmd", "nombre": "PMD", "plugin": "maven-pmd-plugin",
     "que": "Busca patrones problemáticos en el código: variables sin uso, bloques vacíos y complejidad excesiva.",
     "comando": "mvn -f {svc}/pom.xml pmd:check",
     "reportes": "target/pmd.xml"},
    {"id": "spotbugs", "nombre": "SpotBugs", "plugin": "spotbugs-maven-plugin",
     "que": "Analiza el bytecode para detectar errores probables: referencias nulas, recursos sin cerrar y problemas de concurrencia.",
     "comando": "mvn -f {svc}/pom.xml spotbugs:check",
     "reportes": "target/spotbugsXml.xml"},
    {"id": "jacoco", "nombre": "JaCoCo", "plugin": "jacoco-maven-plugin",
     "que": "Mide la cobertura de pruebas (líneas y ramas) y puede fallar el build si no se alcanza el mínimo configurado.",
     "comando": "mvn -f {svc}/pom.xml verify",
     "reportes": "target/site/jacoco/index.html"},
    {"id": "javadoc", "nombre": "Javadoc", "plugin": "maven-javadoc-plugin",
     "que": "Genera documentación técnica a partir de los comentarios /** */.",
     "comando": "mvn -f {svc}/pom.xml javadoc:javadoc",
     "reportes": "target/site/apidocs/index.html"},
    {"id": "cobertura", "nombre": "Cobertura", "plugin": "cobertura-maven-plugin",
     "que": "Alternativa a JaCoCo para medir la cobertura de pruebas.",
     "comando": "mvn -f {svc}/pom.xml cobertura:cobertura",
     "reportes": "target/site/cobertura/index.html"},
]

CSS_TEC = (
    ":root{--bg:#f5f7fb;--superficie:#fff;--fg:#0f172a;--muted:#64748b;--linea:#e2e8f0;color-scheme:light}"
    "@media (prefers-color-scheme: dark){:root:not([data-theme=\"light\"]){--bg:#0b1020;--superficie:#131a2e;--fg:#e2e8f0;--muted:#94a3b8;--linea:#23304f;color-scheme:dark}}"
    "*{box-sizing:border-box}"
    "body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.55 system-ui,-apple-system,'Segoe UI',sans-serif;padding-bottom:40px}"
    "a{color:hsl(var(--h) 70% 45%)}"
    ".cab{background:linear-gradient(135deg,hsl(var(--h) 72% 40%),hsl(calc(var(--h) + 30) 78% 26%));color:#fff;padding:28px max(16px,env(safe-area-inset-left,0px)) 56px}"
    ".in{max-width:900px;margin:0 auto;min-width:0}"
    ".volver{color:#fff;opacity:.9;font-size:14px;text-decoration:none}"
    ".etq{display:inline-block;font-size:12px;letter-spacing:.08em;text-transform:uppercase;background:rgba(255,255,255,.2);padding:4px 10px;border-radius:999px;margin:14px 0 10px}"
    "h1{margin:0 0 8px;font-size:28px;line-height:1.2;overflow-wrap:anywhere}"
    ".lead{margin:0;opacity:.92;max-width:70ch}"
    "main{max-width:900px;margin:-32px auto 0;padding:0 max(16px,env(safe-area-inset-left,0px))}"
    ".panel{background:var(--superficie);border:1px solid var(--linea);border-radius:14px;padding:20px;margin-bottom:16px;min-width:0;overflow-wrap:anywhere}"
    "h2{font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:var(--muted);margin:0 0 10px}"
    "pre{overflow-x:auto;background:rgba(127,127,127,.12);padding:12px;border-radius:10px;font-size:13px}"
    "table{width:100%;border-collapse:collapse;font-size:14px}"
    "th,td{text-align:left;padding:8px;border-bottom:1px solid var(--linea);vertical-align:top}"
    "th{color:var(--muted);font-size:12px;text-transform:uppercase}"
    ".chip{display:inline-block;font:600 12px ui-monospace,monospace;background:hsl(var(--h) 80% 50% / .16);color:hsl(var(--h) 70% 35%);border-radius:6px;padding:3px 8px;margin:2px 4px 2px 0}"
)


def bloque_plugin(texto, artefacto):
    for bloque in re.findall(r"<plugin>[\s\S]*?</plugin>", texto):
        if f"<artifactId>{artefacto}</artifactId>" in bloque:
            return bloque
    return None


def resultados_de(svc_dir, tid):
    import xml.etree.ElementTree as ET
    tgt = os.path.join(svc_dir, "target")

    def raiz(ruta):
        try:
            return ET.parse(ruta).getroot()
        except Exception:
            return None

    def fecha_de(rutas):
        mt = max(os.path.getmtime(r) for r in rutas)
        return "Actualizado " + datetime.datetime.fromtimestamp(mt).strftime("%Y-%m-%d %H:%M")

    def pct(cubiertos, total):
        return f"{(100.0 * cubiertos / total):.1f} %" if total else "Sin datos"

    if tid == "pruebas":
        rutas = (glob.glob(os.path.join(tgt, "surefire-reports", "TEST-*.xml"))
                 + glob.glob(os.path.join(tgt, "failsafe-reports", "TEST-*.xml")))
        if not rutas:
            return None
        total = fallos = omitidos = 0
        tiempo = 0.0
        fallidas = []
        for ruta in rutas:
            r = raiz(ruta)
            if r is None:
                continue
            total += int(r.get("tests", 0))
            fallos += int(r.get("failures", 0)) + int(r.get("errors", 0))
            omitidos += int(r.get("skipped", 0))
            tiempo += float(r.get("time", 0) or 0)
            for c in r.iter("testcase"):
                if c.find("failure") is not None or c.find("error") is not None:
                    fallidas.append(f"{c.get('classname', '')}.{c.get('name', '')}")
        filas = [("Pruebas ejecutadas", total), ("Pasan", total - fallos - omitidos),
                 ("Fallan", fallos), ("Omitidas", omitidos), ("Tiempo total", f"{tiempo:.1f} s")]
        return filas, fallidas, fecha_de(rutas)

    if tid == "jacoco":
        ruta = os.path.join(tgt, "site", "jacoco", "jacoco.xml")
        r = raiz(ruta) if os.path.exists(ruta) else None
        if r is None:
            return None
        filas = []
        nombres = {"LINE": "Líneas", "BRANCH": "Ramas", "INSTRUCTION": "Instrucciones",
                   "METHOD": "Métodos", "CLASS": "Clases"}
        for c in r.findall("counter"):
            tipo = c.get("type")
            if tipo not in nombres:
                continue
            missed, covered = int(c.get("missed")), int(c.get("covered"))
            filas.append((nombres[tipo], f"{pct(covered, missed + covered)} ({covered} de {missed + covered})"))
        return filas, [], fecha_de([ruta])

    if tid == "checkstyle":
        ruta = os.path.join(tgt, "checkstyle-result.xml")
        r = raiz(ruta) if os.path.exists(ruta) else None
        if r is None:
            return None
        archivos = r.findall("file")
        errores = [(f.get("name", ""), er) for f in archivos for er in f.findall("error")]
        err = sum(1 for _, er in errores if er.get("severity") == "error")
        adv = sum(1 for _, er in errores if er.get("severity") == "warning")
        con = sum(1 for f in archivos if f.findall("error"))
        filas = [("Archivos revisados", len(archivos)), ("Errores", err),
                 ("Advertencias", adv), ("Archivos con incidencias", con)]
        detalle = [f"{os.path.basename(n)}:{er.get('line', '?')} · {er.get('message', '')}" for n, er in errores]
        return filas, detalle, fecha_de([ruta])

    if tid == "pmd":
        ruta = os.path.join(tgt, "pmd.xml")
        r = raiz(ruta) if os.path.exists(ruta) else None
        if r is None:
            return None
        archivos = r.findall("file")
        viol = [(f.get("name", ""), v) for f in archivos for v in f.findall("violation")]
        por_prio = {}
        for _, v in viol:
            por_prio[v.get("priority", "?")] = por_prio.get(v.get("priority", "?"), 0) + 1
        filas = [("Archivos revisados", len(archivos)), ("Incidencias", len(viol))]
        for p in sorted(por_prio):
            filas.append((f"Prioridad {p}", por_prio[p]))
        detalle = [f"{os.path.basename(n)}:{v.get('beginline', '?')} · {v.get('rule', '')}: {(v.text or '').strip()}"
                   for n, v in viol]
        return filas, detalle, fecha_de([ruta])

    if tid == "spotbugs":
        ruta = os.path.join(tgt, "spotbugsXml.xml")
        r = raiz(ruta) if os.path.exists(ruta) else None
        if r is None:
            return None
        bugs = list(r.iter("BugInstance"))
        por_cat = {}
        for b in bugs:
            por_cat[b.get("category", "?")] = por_cat.get(b.get("category", "?"), 0) + 1
        filas = [("Hallazgos", len(bugs))]
        for cat in sorted(por_cat):
            filas.append((f"Categoría {cat.capitalize()}", por_cat[cat]))
        detalle = []
        for b in bugs:
            clase = b.find("Class")
            detalle.append(f"{b.get('type', '')} · {clase.get('classname', '') if clase is not None else ''}")
        return filas, detalle, fecha_de([ruta])

    if tid == "javadoc":
        base = os.path.join(tgt, "site", "apidocs")
        if not os.path.isdir(base):
            return None
        paquetes = [d for d, _, fs in os.walk(base) if "package-summary.html" in fs]
        clases = 0
        for d, _, fs in os.walk(base):
            for f in fs:
                if f.endswith(".html") and not f.startswith(("package-", "index", "allclasses", "allpackages", "overview", "module-", "constant-values", "serialized-form", "deprecated-list", "help-doc", "search")) and f[:1].isupper() and "-frame" not in f:
                    clases += 1
        fecha = fecha_de([os.path.join(base, "index.html")]) if os.path.exists(os.path.join(base, "index.html")) else fecha_de([base])
        return [("Clases documentadas", clases), ("Paquetes documentados", len(paquetes))], [], fecha

    if tid == "cobertura":
        ruta = os.path.join(tgt, "site", "cobertura", "coverage.xml")
        if not os.path.exists(ruta):
            ruta = os.path.join(tgt, "coverage.xml")
        r = raiz(ruta) if os.path.exists(ruta) else None
        if r is None:
            return None
        lr = float(r.get("line-rate", 0)) * 100
        br = float(r.get("branch-rate", 0)) * 100
        return [("Líneas", f"{lr:.1f} %"), ("Ramas", f"{br:.1f} %")], [], fecha_de([ruta])

    return None


def seccion_resultados(svc_dir, tid):
    e = html.escape
    r = resultados_de(svc_dir, tid)
    if r is None:
        return ("<section class='panel'><h2>Resultados</h2><p class='aviso'>Todavía no hay resultados "
                "en este equipo. Ejecuta el comando de arriba y vuelve a generar la documentación.</p></section>")
    filas, detalle, fecha = r
    tabla = "<table>" + "".join(f"<tr><th>{e(str(k))}</th><td>{e(str(v))}</td></tr>" for k, v in filas) + "</table>"
    lista = ""
    if detalle:
        lista = ("<h2 style='margin-top:16px'>Detalle</h2><ul>"
                 + "".join(f"<li><code>{e(x)}</code></li>" for x in detalle[:30]) + "</ul>")
        if len(detalle) > 30:
            lista += f"<p class='aviso'>y {len(detalle) - 30} más</p>"
    return f"<section class='panel'><h2>Resultados · {e(fecha)}</h2>{tabla}{lista}</section>"


def pagina_tecnologia(svc, hue, t, bloque, pruebas, libs, svc_dir):
    e = html.escape
    comando = t["comando"].replace("{svc}", svc)
    if t["id"] == "pruebas":
        secciones = (
            f"<section class='panel'><h2>Clases de prueba ({len(pruebas)})</h2><ul>"
            + "".join(f"<li><code>{e(n)}</code></li>" for n in pruebas) + "</ul></section>"
            + ("<section class='panel'><h2>Librerías de prueba</h2><ul>"
               + "".join(f"<li><code>{e(l)}</code></li>" for l in libs) + "</ul></section>" if libs else "")
        )
        config = "<p>Surefire y Failsafe vienen configurados por el parent de Spring Boot; el pom del servicio solo declara Failsafe.</p>"
    else:
        version = re.search(r"<version>([^<]+)</version>", bloque)
        goals = re.findall(r"<goal>([^<]+)</goal>", bloque)
        conf = re.search(r"<configuration>[\s\S]*?</configuration>", bloque)
        filas = (
            f"<tr><th>Plugin</th><td><code>{e(t['plugin'])}</code></td></tr>"
            f"<tr><th>Versión</th><td>{e(version.group(1)) if version else 'Heredada del parent'}</td></tr>"
            f"<tr><th>Goals</th><td>{e(', '.join(goals)) if goals else 'Por defecto del plugin'}</td></tr>"
        )
        config = f"<table>{filas}</table>"
        secciones = ""
        if conf:
            secciones = (f"<section class='panel'><h2>Configuración en pom.xml</h2><pre><code>"
                         f"{e(conf.group(0))}</code></pre></section>")
    cuerpo = (
        f"<section class='panel'><h2>Qué hace</h2><p>{e(t['que'])}</p></section>"
        f"<section class='panel'><h2>Plugin y versión</h2>{config}</section>"
        f"{secciones}"
        f"<section class='panel'><h2>Cómo ejecutarlo</h2><pre><code>{e(comando)}</code></pre></section>"
        f"{seccion_resultados(svc_dir, t['id'])}"
    )
    return (
        "<!doctype html><html lang='es'><head><meta charset='utf-8'>"
        "<meta name='viewport' content='width=device-width, initial-scale=1'>"
        f"<title>{e(t['nombre'])} · {e(svc)}</title><style>{CSS_TEC}</style></head>"
        f"<body style='--h:{hue:.0f}'>"
        f"<header class='cab'><div class='in'><a class='volver' href='../../{e(svc)}.html#tecnologias'>← Ficha de {e(svc)}</a>"
        f"<div><span class='etq'>Tecnología</span></div><h1>{e(t['nombre'])}</h1>"
        f"<p class='lead'>{e(svc)}</p></div></header>"
        f"<main>{cuerpo}</main></body></html>"
    )


def generar_tecnologias(svc, svc_dir, hue):
    import shutil
    texto = leer(os.path.join(svc_dir, "pom.xml"))
    destino = os.path.join(SALIDA, svc, "tecnologias")
    if os.path.isdir(destino):
        shutil.rmtree(destino)
    clases = sorted(glob.glob(os.path.join(svc_dir, "src", "test", "java", "**", "*.java"), recursive=True))
    nombres = [os.path.basename(c)[:-5] for c in clases]
    libs = sorted(set(re.findall(
        r"<artifactId>([^<]*(?:junit|mockito|testcontainers|assertj|archunit|rest-assured)[^<]*)</artifactId>", texto)))
    resultado = []
    for t in TECNOLOGIAS:
        if t["id"] == "pruebas":
            bloque = None
            aplicado = bool(clases)
        else:
            bloque = bloque_plugin(texto, t["plugin"])
            aplicado = bloque is not None
        if aplicado:
            os.makedirs(destino, exist_ok=True)
            with open(os.path.join(destino, t["id"] + ".html"), "w", encoding="utf-8") as f:
                f.write(pagina_tecnologia(svc, hue, t, bloque, nombres, libs, svc_dir))
        resultado.append({"id": t["id"], "nombre": t["nombre"], "resumen": t["que"],
                          "aplicado": aplicado,
                          "archivo": f"{svc}/tecnologias/{t['id']}.html" if aplicado else None})
    return resultado




def main():
    os.makedirs(SALIDA, exist_ok=True)
    servicios = servicios_documentados()
    resumen = []
    for svc in servicios:
        svc_dir = os.path.join(RAIZ, svc)
        spec, n = construir_spec(svc)
        pom = leer_pom(svc_dir)
        ruta_readme = os.path.join(svc_dir, "README.md")
        readme = reescribir_readme(svc, leer(ruta_readme)) if os.path.exists(ruta_readme) else ""
        descripcion = pom["descripcion"]
        if not descripcion and readme:
            descripcion = next((l.strip() for l in readme.splitlines()
                                if l.strip() and not l.startswith("#")), "")
        filas = filas_permisos(spec if n else None)
        roles = sorted({r for f in filas for r in re.findall(r"'([A-Z_]+)'", f["roles"])})
        hue = (len(resumen) * 137.508) % 360
        diagramas = copiar_diagramas(svc)
        tecnologias = generar_tecnologias(svc, svc_dir, hue)
        datos = {
            "svc": svc,
            "info": {"nombre": svc, "descripcion": descripcion, "puerto": puerto_de(svc_dir),
                     "java": pom["java"], "spring_boot": pom["spring_boot"],
                     "endpoints": n, "roles": roles},
            "readme": readme,
            "permisos": filas,
            "diagramas": diagramas,
            "api": bool(n),
            "tecnologias": tecnologias,
        }
        ruta_json = os.path.join(SALIDA, svc + ".json")
        if n:
            with open(ruta_json, "w", encoding="utf-8") as f:
                json.dump(spec, f, ensure_ascii=False, indent=2)
        elif os.path.exists(ruta_json):
            os.remove(ruta_json)
        with open(os.path.join(SALIDA, svc + ".html"), "w", encoding="utf-8") as f:
            f.write(pagina_ficha(svc, datos, spec if n else None, hue))
        resumen.append((svc, n))
        print(f"✅ {svc}: {n} endpoints · {len(diagramas)} diagramas · README {'sí' if readme else 'no'}")
    with open(os.path.join(SALIDA, "index.html"), "w", encoding="utf-8") as f:
        f.write(pagina_indice(resumen))
    print(f"🏁 Fichas generadas en {SALIDA}")


def pagina_indice(resumen):
    total = sum(n for _, n in resumen)
    tarjetas = "\n".join(
        f'<a class="tarjeta" style="--h:{(i * 137.508) % 360:.0f}" href="{html.escape(svc)}.html">'
        f'<span class="nombre">{html.escape(svc)}</span>'
        f'<span class="conteo">{texto_conteo(n)}</span>'
        f'<span class="ver">Abrir ficha →</span></a>'
        for i, (svc, n) in enumerate(resumen))
    fecha = datetime.date.today().isoformat()
    return f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Microservicios · EMPRESA_JOSESAID</title>
<style>
:root{{
  --bg:#f5f7fb; --superficie:#ffffff; --fg:#0f172a; --muted:#64748b; --linea:#e2e8f0;
  --acento:#6366f1; --acento2:#06b6d4; --cabecera-a:#4338ca; --cabecera-b:#0e7490; --sombra:rgba(15,23,42,.08);
  color-scheme:light;
}}
@media (prefers-color-scheme: dark){{
  :root:not([data-theme="light"]){{
    --bg:#0b1020; --superficie:#131a2e; --fg:#e2e8f0; --muted:#94a3b8; --linea:#23304f;
    --acento:#818cf8; --acento2:#22d3ee; --cabecera-a:#312e81; --cabecera-b:#155e75; --sombra:rgba(0,0,0,.4);
    color-scheme:dark;
  }}
}}
:root[data-theme="dark"]{{
  --bg:#0b1020; --superficie:#131a2e; --fg:#e2e8f0; --muted:#94a3b8; --linea:#23304f;
  --acento:#818cf8; --acento2:#22d3ee; --cabecera-a:#312e81; --cabecera-b:#155e75; --sombra:rgba(0,0,0,.4);
  color-scheme:dark;
}}
*{{box-sizing:border-box}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.55 system-ui,-apple-system,"Segoe UI",sans-serif}}
.cabecera{{background:linear-gradient(135deg,var(--cabecera-a),var(--cabecera-b));color:#fff;
  padding:40px max(16px,env(safe-area-inset-left,0px)) 56px}}
.cabecera-in{{max-width:960px;margin:0 auto;display:flex;align-items:center;justify-content:space-between;gap:32px}}
.cabecera-texto{{min-width:0;flex:1 1 auto}}
.logo{{width:340px;max-width:44%;height:auto;flex:none;filter:drop-shadow(0 10px 24px rgba(0,0,0,.25));animation:flota 6s ease-in-out infinite}}
@keyframes flota{{0%,100%{{transform:translateY(0)}}50%{{transform:translateY(-6px)}}}}
@media (max-width:640px){{.cabecera-in{{flex-direction:column;align-items:flex-start}}.logo{{width:260px;max-width:100%}}}}
@media (prefers-reduced-motion: reduce){{.logo{{animation:none}}}}
.etiqueta{{display:inline-block;font-size:12px;letter-spacing:.08em;text-transform:uppercase;
  background:rgba(255,255,255,.18);padding:4px 10px;border-radius:999px;margin-bottom:12px}}
h1{{margin:0 0 8px;font-size:30px;line-height:1.2;text-wrap:balance}}
.lead{{margin:0;opacity:.9;max-width:62ch}}
.autor{{margin-top:22px;display:flex;flex-wrap:wrap;gap:8px 18px;font-size:14px}}
.autor a{{color:#fff;text-decoration:underline;text-underline-offset:3px}}
.autor strong{{font-weight:650}}
main{{max-width:960px;margin:-32px auto 0;padding:0 max(16px,env(safe-area-inset-left,0px)) 48px}}
.stats{{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:24px}}
.stat{{flex:1 1 180px;background:var(--superficie);border:1px solid var(--linea);border-radius:14px;
  padding:16px 18px;box-shadow:0 6px 20px var(--sombra)}}
.stat b{{display:block;font-size:26px;color:var(--acento)}}
.stat span{{color:var(--muted);font-size:13px}}
h2{{font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:var(--muted);margin:28px 0 12px}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:12px}}
.tarjeta{{display:flex;flex-direction:column;gap:6px;text-decoration:none;color:inherit;
  background:var(--superficie);border:1px solid var(--linea);border-radius:14px;padding:16px 18px;
  border-left:5px solid hsl(var(--h) 80% 52%);transition:transform .15s,box-shadow .15s;
  background:linear-gradient(160deg,hsl(var(--h) 90% 97% / 1),var(--superficie) 55%)}}
.tarjeta .ver{{color:hsl(var(--h) 70% 42%)}}
.tarjeta .conteo{{color:hsl(var(--h) 35% 40%)}}
.tarjeta:hover,.tarjeta:focus-visible{{box-shadow:0 12px 26px hsl(var(--h) 80% 50% / .22)}}
.tarjeta:hover,.tarjeta:focus-visible{{transform:translateY(-2px);box-shadow:0 10px 24px var(--sombra);outline:none}}
.nombre{{font-weight:650;word-break:break-word}}
.conteo{{color:var(--muted);font-size:13px}}
.ver{{margin-top:6px;color:var(--acento);font-size:13px;font-weight:600}}
footer{{margin-top:32px;color:var(--muted);font-size:13px}}
@media (prefers-color-scheme: dark){{:root:not([data-theme="light"]) .tarjeta{{background:linear-gradient(160deg,hsl(var(--h) 45% 16%),var(--superficie) 60%)}}
:root:not([data-theme="light"]) .tarjeta{{border-left-color:hsl(var(--h) 85% 66%)}}
:root:not([data-theme="light"]) .tarjeta .ver{{color:hsl(var(--h) 90% 75%)}}
:root:not([data-theme="light"]) .tarjeta .conteo{{color:hsl(var(--h) 40% 72%)}}}}
:root[data-theme="dark"] .tarjeta{{background:linear-gradient(160deg,hsl(var(--h) 45% 16%),var(--superficie) 60%);border-left-color:hsl(var(--h) 85% 66%)}}
:root[data-theme="dark"] .tarjeta .ver{{color:hsl(var(--h) 90% 75%)}}
:root[data-theme="dark"] .tarjeta .conteo{{color:hsl(var(--h) 40% 72%)}}
</style>
</head>
<body>
<header class="cabecera">
  <div class="cabecera-in">
    <div class="cabecera-texto">
      <span class="etiqueta">Fichas de microservicios</span>
      <h1>EMPRESA_JOSESAID</h1>
      <p class="lead">Ficha de cada microservicio: información, README, roles y permisos, diagramas y Swagger.</p>
      <div class="autor">
        <span><strong>Jose Said Olano Garcia</strong> · Senior Engineering Manager</span>
        <a href="mailto:josesaid@gmail.com">josesaid@gmail.com</a>
        <a href="https://josesaid.github.io/">josesaid.github.io</a>
      </div>
    </div>
    <svg class="logo" viewBox="0 0 500 120" role="img" aria-label="EMPRESA_JOSESAID">
      <defs>
        <linearGradient id="gl" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#ffffff"/>
          <stop offset="1" stop-color="#67e8f9"/>
        </linearGradient>
        <linearGradient id="gt" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stop-color="#ffffff"/>
          <stop offset="1" stop-color="#a5f3fc"/>
        </linearGradient>
        <filter id="brillo" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="3" result="b"/>
          <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>
      <g transform="translate(4,4) scale(0.52)" filter="url(#brillo)">
        <g fill="none" stroke="url(#gl)" stroke-width="5" stroke-linejoin="round">
          <polygon points="100,18 172,59 172,141 100,182 28,141 28,59"/>
          <polygon points="100,46 146,72 146,128 100,154 54,128 54,72" stroke-opacity=".5" stroke-width="3"/>
        </g>
        <g stroke="#ffffff" stroke-opacity=".55" stroke-width="3">
          <line x1="100" y1="100" x2="100" y2="18"/><line x1="100" y1="100" x2="172" y2="59"/>
          <line x1="100" y1="100" x2="172" y2="141"/><line x1="100" y1="100" x2="100" y2="182"/>
          <line x1="100" y1="100" x2="28" y2="141"/><line x1="100" y1="100" x2="28" y2="59"/>
        </g>
        <g fill="#ffffff">
          <circle cx="100" cy="18" r="8"/><circle cx="172" cy="59" r="8"/><circle cx="172" cy="141" r="8"/>
          <circle cx="100" cy="182" r="8"/><circle cx="28" cy="141" r="8"/><circle cx="28" cy="59" r="8"/>
        </g>
        <circle cx="100" cy="100" r="26" fill="#ffffff"/>
        <text x="100" y="109" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="24" font-weight="800" fill="#312e81">JS</text>
      </g>
      <text x="128" y="62" font-family="system-ui,-apple-system,'Segoe UI',sans-serif" font-size="27" font-weight="800" letter-spacing="0.3" fill="url(#gt)">EMPRESA_JOSESAID</text>
      <text x="130" y="90" font-family="system-ui,-apple-system,sans-serif" font-size="12" font-weight="700" letter-spacing="4" fill="#67e8f9">API · MICROSERVICIOS</text>
      <line x1="130" y1="100" x2="330" y2="100" stroke="#67e8f9" stroke-opacity=".6" stroke-width="2"/>
    </svg>
  </div>
</header>
<main>
  <div class="stats">
    <div class="stat"><b>{len(resumen)}</b><span>Servicios documentados</span></div>
    <div class="stat"><b>{total}</b><span>Endpoints</span></div>
    <div class="stat"><b>{fecha}</b><span>Fecha de generación</span></div>
  </div>
  <h2>Servicios</h2>
  <div class="grid">
{tarjetas}
  </div>
</main>
</body>
</html>
"""


if __name__ == "__main__":
    main()
