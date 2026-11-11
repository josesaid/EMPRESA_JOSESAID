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
import urllib.parse
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
.cabecera-in{max-width:1400px;margin:0 auto;min-width:0}
.volver{color:#fff;opacity:.9;font-size:14px;text-decoration:none}
.etiqueta{display:inline-block;font-size:12px;letter-spacing:.08em;text-transform:uppercase;background:rgba(255,255,255,.2);padding:4px 10px;border-radius:999px;margin:14px 0 10px}
h1{margin:0 0 8px;font-size:30px;line-height:1.2;text-wrap:balance;overflow-wrap:anywhere}
.lead{margin:0;opacity:.92;max-width:70ch}
.pildoras{display:flex;flex-wrap:wrap;gap:8px;margin-top:16px}
.pildora{background:rgba(255,255,255,.18);border-radius:999px;padding:4px 12px;font-size:13px}
main{max-width:1400px;margin:-40px auto 0;padding:0 max(16px,env(safe-area-inset-left,0px));min-width:0}
nav.tabs{display:flex;gap:4px;overflow-x:auto;background:var(--superficie);border:1px solid var(--linea);border-radius:14px;padding:6px;box-shadow:0 6px 20px var(--sombra)}
nav.tabs button{flex:1 0 auto;border:0;background:transparent;color:var(--muted);font:600 14px system-ui,sans-serif;padding:10px 14px;border-radius:10px;cursor:pointer}
nav.tabs button[aria-selected="true"]{background:hsl(var(--h) 80% 50% / .14);color:hsl(var(--h) 70% 35%)}
.jd-boton{display:inline-block;margin-top:10px;padding:10px 16px;border-radius:10px;background:hsl(var(--h) 70% 42%);color:#fff;font-weight:600;text-decoration:none}
.jd-boton:hover{filter:brightness(1.08)}
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
th,td{text-align:left;padding:10px 8px;border-bottom:1px solid var(--linea);vertical-align:top;overflow-wrap:break-word}
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
    <button role="tab" data-tab="javadoc" aria-selected="false">Java Docs</button>
  </nav>
  <section class="panel" data-panel="info"></section>
  <section class="panel" data-panel="readme" hidden></section>
  <section class="panel" data-panel="permisos" hidden></section>
  <section class="panel" data-panel="diagramas" hidden></section>
  <section class="panel" data-panel="tecnologias" hidden></section>
  <section class="panel" data-panel="api" hidden><div id="swagger-ui"></div></section>
  <section class="panel" data-panel="javadoc" hidden></section>
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
    dl.append(el('div', {}, [el('dt', { text: 'Roles a usar' }),
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
    panel.append(el('h2', { class: 'subtitulo', text: 'Roles a usar' }));
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
  const javadoc = $('[data-panel="javadoc"]');
  javadoc.append(el('h2', { text: 'Javadoc' }));
  javadoc.append(el('p', { text: 'Documentación técnica generada desde los comentarios /** */ del código. Se abre desde tu equipo después de generarla con build.sh.' }));
  javadoc.append(el('a', { class: 'jd-boton', href: '../../' + D.info.nombre + '/target/reports/apidocs/index.html', text: 'Abrir Javadoc de ' + D.info.nombre }));

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
  activar(['info', 'readme', 'permisos', 'diagramas', 'tecnologias', 'api', 'javadoc'].indexOf(inicial) >= 0 ? inicial : 'info', false);
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
]

CSS_TEC = (
    ":root{--bg:#f5f7fb;--superficie:#fff;--fg:#0f172a;--muted:#64748b;--linea:#e2e8f0;color-scheme:light}"
    "@media (prefers-color-scheme: dark){:root:not([data-theme=\"light\"]){--bg:#0b1020;--superficie:#131a2e;--fg:#e2e8f0;--muted:#94a3b8;--linea:#23304f;color-scheme:dark}}"
    "*{box-sizing:border-box}"
    "body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.55 system-ui,-apple-system,'Segoe UI',sans-serif;padding-bottom:40px}"
    "a{color:hsl(var(--h) 70% 45%)}"
    ".cab{background:linear-gradient(135deg,hsl(var(--h) 72% 40%),hsl(calc(var(--h) + 30) 78% 26%));color:#fff;padding:28px max(16px,env(safe-area-inset-left,0px)) 56px}"
    ".in{max-width:1400px;margin:0 auto;min-width:0}"
    ".volver{color:#fff;opacity:.9;font-size:14px;text-decoration:none}"
    ".etq{display:inline-block;font-size:12px;letter-spacing:.08em;text-transform:uppercase;background:rgba(255,255,255,.2);padding:4px 10px;border-radius:999px;margin:14px 0 10px}"
    "h1{margin:0 0 8px;font-size:28px;line-height:1.2;overflow-wrap:anywhere}"
    ".lead{margin:0;opacity:.92;max-width:70ch}"
    "main{max-width:1400px;margin:-32px auto 0;padding:0 max(16px,env(safe-area-inset-left,0px))}"
    ".panel{background:var(--superficie);border:1px solid var(--linea);border-radius:14px;padding:20px;margin-bottom:16px;min-width:0;overflow-wrap:break-word}"
    "h2{font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:var(--muted);margin:0 0 10px}"
    "pre{overflow-x:auto;background:rgba(127,127,127,.12);padding:12px;border-radius:10px;font-size:13px}"
    "table{width:100%;border-collapse:collapse;font-size:14px}"
    "th,td{text-align:left;padding:8px;border-bottom:1px solid var(--linea);vertical-align:top;overflow-wrap:anywhere}"
    "th{color:var(--muted);font-size:12px;text-transform:uppercase}"
    ".nw{white-space:nowrap}"
    ".badge{display:inline-block;padding:2px 9px;border-radius:999px;font:600 12px system-ui,sans-serif;white-space:nowrap}"
    ".b-rojo{background:rgba(220,38,38,.14);color:#b91c1c}"
    ".b-ambar{background:rgba(217,119,6,.16);color:#b45309}"
    ".b-verde{background:rgba(5,150,105,.16);color:#047857}"
    ".b-azul{background:rgba(37,99,235,.14);color:#1d4ed8}"
    "@media (prefers-color-scheme: dark){:root:not([data-theme=\"light\"]) .b-rojo{color:#fca5a5}:root:not([data-theme=\"light\"]) .b-ambar{color:#fcd34d}:root:not([data-theme=\"light\"]) .b-verde{color:#6ee7b7}:root:not([data-theme=\"light\"]) .b-azul{color:#93c5fd}}"
    ":root[data-theme=\"dark\"] .b-rojo{color:#fca5a5}:root[data-theme=\"dark\"] .b-ambar{color:#fcd34d}:root[data-theme=\"dark\"] .b-verde{color:#6ee7b7}:root[data-theme=\"dark\"] .b-azul{color:#93c5fd}"
    "tbody tr:nth-child(even) td{background:rgba(127,127,127,.06)}"
    "tbody tr:hover td{background:rgba(37,99,235,.08)}"
    "th{background:rgba(127,127,127,.08)}"
    ".chip{display:inline-block;font:600 12px ui-monospace,monospace;background:hsl(var(--h) 80% 50% / .16);color:hsl(var(--h) 70% 35%);border-radius:6px;padding:3px 8px;margin:2px 4px 2px 0}"
)


def bloque_plugin(texto, artefacto):
    for bloque in re.findall(r"<plugin>[\s\S]*?</plugin>", texto):
        if f"<artifactId>{artefacto}</artifactId>" in bloque:
            return bloque
    return None


TONOS_CELDA = {
    "Severidad": {"Error": "rojo", "Warning": "ambar", "Info": "azul"},
    "Resultado": {"Pasa": "verde", "Falla": "rojo", "Error": "rojo", "Omitida": "ambar"},
}


def insignia(columna, valor):
    """Escapa el valor y, si la columna tiene tono, lo muestra como insignia de color."""
    texto = str(valor)
    if columna == "Prioridad":
        tono = "rojo" if texto == "1" else "ambar" if texto == "2" else "azul" if texto.isdigit() else None
    else:
        tono = TONOS_CELDA.get(columna, {}).get(texto)
    esc = html.escape(texto)
    return f"<span class='badge b-{tono}'>{esc}</span>" if tono else esc


COLUMNAS_CORTAS = {"Archivo", "Línea", "Severidad", "Prioridad", "Resultado", "Tiempo", "Categoría", "Tipo", "Líneas", "Ramas", "Métodos"}

ENCABEZADOS_DETALLE = {
    "pruebas": ["Clase", "Prueba", "Resultado", "Tiempo"],
    "checkstyle": ["Archivo", "Línea", "Severidad", "Mensaje"],
    "pmd": ["Archivo", "Línea", "Prioridad", "Regla", "Mensaje"],
    "spotbugs": ["Tipo", "Clase", "Categoría", "Prioridad", "Descripción"],
    "jacoco": ["Paquete", "Clase", "Líneas", "Ramas", "Métodos"],
    "javadoc": ["Paquete", "Clases documentadas"],
}

CSS_NAV = (".tecs{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 18px;background:var(--superficie);"
           "border:1px solid var(--linea);border-radius:14px;padding:8px;box-shadow:0 6px 20px var(--sombra, rgba(15,23,42,.08))}"
           ".tecs a{padding:6px 12px;border:1px solid var(--linea);border-radius:999px;color:var(--muted);"
           "font-size:14px;text-decoration:none}"
           ".tecs a[aria-current='page']{color:hsl(var(--h) 70% 35%);background:hsl(var(--h) 80% 50% / .14);"
           "border-color:transparent;font-weight:700}"
           "@media (prefers-color-scheme: dark){:root:not([data-theme=\"light\"]) .tecs a[aria-current='page']{color:hsl(var(--h) 90% 75%)}}")


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
        fuentes = glob.glob(os.path.join(svc_dir, "src", "test", "java", "**", "*.java"), recursive=True)
        if not rutas or not fuentes:
            return None
        total = fallos = omitidos = 0
        tiempo = 0.0
        casos = []
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
                    estado = "Falla"
                elif c.find("skipped") is not None:
                    estado = "Omitida"
                else:
                    estado = "Pasa"
                casos.append((c.get("classname", ""), c.get("name", ""), estado,
                              f"{float(c.get('time', 0) or 0):.3f} s"))
        filas = [("Pruebas ejecutadas", total), ("Pasan", total - fallos - omitidos),
                 ("Fallan", fallos), ("Omitidas", omitidos), ("Tiempo total", f"{tiempo:.1f} s")]
        return filas, casos, fecha_de(rutas)

    if tid == "jacoco":
        ruta = os.path.join(tgt, "site", "jacoco", "jacoco.xml")
        r = raiz(ruta) if os.path.exists(ruta) else None
        if r is None:
            return None
        nombres = {"LINE": "Líneas", "BRANCH": "Ramas", "INSTRUCTION": "Instrucciones",
                   "METHOD": "Métodos", "CLASS": "Clases"}
        filas = []
        for k in r.findall("counter"):
            tipo = k.get("type")
            if tipo not in nombres:
                continue
            missed, covered = int(k.get("missed")), int(k.get("covered"))
            filas.append((nombres[tipo], f"{pct(covered, missed + covered)} ({covered} de {missed + covered})"))
        detalle = []
        for paquete in r.findall("package"):
            for clase in paquete.findall("class"):
                cont = {}
                for k in clase.findall("counter"):
                    cont[k.get("type")] = (int(k.get("missed")), int(k.get("covered")))

                def cel(tipo):
                    if tipo not in cont:
                        return "—"
                    m, cu = cont[tipo]
                    return pct(cu, m + cu)
                detalle.append((paquete.get("name", "").replace("/", "."),
                                clase.get("name", "").split("/")[-1], cel("LINE"), cel("BRANCH"), cel("METHOD")))
        detalle.sort()
        return filas, detalle, fecha_de([ruta])

    if tid == "checkstyle":
        ruta = os.path.join(tgt, "checkstyle-result.xml")
        r = raiz(ruta) if os.path.exists(ruta) else None
        if r is None:
            return None
        archivos = r.findall("file")
        errores = [(os.path.basename(f.get("name", "")), er) for f in archivos for er in f.findall("error")]
        err = sum(1 for _, er in errores if er.get("severity") == "error")
        adv = sum(1 for _, er in errores if er.get("severity") == "warning")
        con = sum(1 for f in archivos if f.findall("error"))
        filas = [("Archivos revisados", len(archivos)), ("Errores", err),
                 ("Advertencias", adv), ("Archivos con incidencias", con)]
        detalle = [(n, er.get("line", "?"), "Error" if er.get("severity") == "error" else "Advertencia",
                    er.get("message", "")) for n, er in errores]
        return filas, detalle, fecha_de([ruta])

    if tid == "pmd":
        ruta = os.path.join(tgt, "pmd.xml")
        r = raiz(ruta) if os.path.exists(ruta) else None
        if r is None:
            return None
        archivos = r.findall("file")
        viol = [(os.path.basename(f.get("name", "")), v) for f in archivos for v in f.findall("violation")]
        por_prio = {}
        for _, v in viol:
            por_prio[v.get("priority", "?")] = por_prio.get(v.get("priority", "?"), 0) + 1
        # pmd.xml solo lista archivos con incidencias: los revisados son las fuentes de src/main/java
        revisados = glob.glob(os.path.join(svc_dir, "src", "main", "java", "**", "*.java"), recursive=True)
        filas = [("Archivos revisados", len(revisados)), ("Incidencias", len(viol))]
        for p in sorted(por_prio):
            filas.append((f"Prioridad {p}", por_prio[p]))
        detalle = [(n, v.get("beginline", "?"), v.get("priority", "?"), v.get("rule", ""),
                    (v.text or "").strip()) for n, v in viol]
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
            desc = b.find("ShortMessage")
            detalle.append((b.get("type", ""), clase.get("classname", "") if clase is not None else "",
                            b.get("category", "?"), b.get("priority", "?"),
                            (desc.text or "").strip() if desc is not None else ""))
        return filas, detalle, fecha_de([ruta])

    if tid == "javadoc":
        # Maven escribe en target/site/apidocs, o en target/reports/apidocs si el plugin falla a medias
        base = next((b for b in (os.path.join(tgt, "site", "apidocs"), os.path.join(tgt, "reports", "apidocs"))
                     if os.path.isdir(b)), None)
        if base is None:
            return None
        paquetes = [d for d, _, fs in os.walk(base) if "package-summary.html" in fs]
        detalle = []
        clases = 0
        for d, _, fs in os.walk(base):
            n = 0
            for f in fs:
                if f.endswith(".html") and not f.startswith(("package-", "index", "allclasses", "allpackages", "overview", "module-", "constant-values", "serialized-form", "deprecated-list", "help-doc", "search")) and f[:1].isupper() and "-frame" not in f:
                    n += 1
            clases += n
            if "package-summary.html" in fs:
                rel = os.path.relpath(d, base).replace(os.sep, ".")
                detalle.append(("(raíz)" if rel == "." else rel, n))
        detalle.sort()
        fecha = fecha_de([os.path.join(base, "index.html")]) if os.path.exists(os.path.join(base, "index.html")) else fecha_de([base])
        return [("Clases documentadas", clases), ("Paquetes documentados", len(paquetes))], detalle, fecha

    return None


def seccion_resultados(svc_dir, tid):
    e = html.escape
    r = resultados_de(svc_dir, tid)
    if r is None:
        return ("<section class='panel'><h2>Resultados</h2><p class='aviso'>Todavía no hay resultados "
                "en este equipo. Ejecuta el comando de arriba y vuelve a generar la documentación.</p></section>")
    filas, detalle, fecha = r
    tabla = "<table>" + "".join(f"<tr><th>{e(str(k))}</th><td>{e(str(v))}</td></tr>" for k, v in filas) + "</table>"
    if detalle:
        cab = ENCABEZADOS_DETALLE.get(tid, [])
        cortas = {i for i, h in enumerate(cab) if h in COLUMNAS_CORTAS}
        clase = lambda i: " class='nw'" if i in cortas else ""
        svc = os.path.basename(svc_dir)
        mapa = fuentes_java(svc_dir)

        def celda(i, c):
            columna = cab[i] if i < len(cab) else ""
            if columna == "Clase" and c:
                enlace = enlace_clase(svc, mapa, str(c))
                if enlace:
                    return enlace
            if columna == "Archivo" and str(c).endswith(".java"):
                enlace = enlace_archivo(svc, mapa, str(c))
                if enlace:
                    return enlace
            return insignia(columna, c)

        cuerpo = "".join("<tr>" + "".join(f"<td{clase(i)}>{celda(i, c)}</td>" for i, c in enumerate(fila)) + "</tr>" for fila in detalle)
        lista = ("<h2 style='margin-top:16px'>Detalle</h2><div style='overflow-x:auto'><table><tr>"
                 + "".join(f"<th{clase(i)}>{e(h)}</th>" for i, h in enumerate(cab)) + "</tr>" + cuerpo + "</table></div>")
    elif tid in ("pmd", "checkstyle", "spotbugs"):
        lista = "<p class='aviso'>Sin incidencias en el código revisado.</p>"
    else:
        lista = "<p class='aviso'>Sin elementos que mostrar.</p>"
    return f"<section class='panel'><h2>Resultados · {e(fecha)}</h2>{tabla}{lista}</section>"


def pagina_tecnologia(svc, hue, t, bloque, pruebas, libs, svc_dir, navegacion=()):
    e = html.escape
    comando = t["comando"].replace("{svc}", svc)
    if t["id"] == "pruebas":
        secciones = (
            f"<section class='panel'><h2>Clases de prueba ({len(pruebas)})</h2><ul>"
            + "".join(f"<li>{enlace_java(svc, rel, n, codigo=True)}</li>" for n, rel in pruebas) + "</ul></section>"
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
    enlaces = "".join(
        "<a href='" + e(i) + ".html'" + (" aria-current='page'" if i == t["id"] else "") + ">" + e(n) + "</a>"
        for i, n in navegacion)
    nav = f"<nav class='tecs'>{enlaces}</nav>" if enlaces else ""
    cuerpo = (
        f"{nav}"
        f"<section class='panel'><h2>Qué hace</h2><p>{e(t['que'])}</p></section>"
        f"<section class='panel'><h2>Plugin y versión</h2>{config}</section>"
        f"{secciones}"
        f"<section class='panel'><h2>Cómo ejecutarlo</h2><pre><code>{e(comando)}</code></pre></section>"
        f"{seccion_resultados(svc_dir, t['id'])}"
    )
    return (
        "<!doctype html><html lang='es'><head><meta charset='utf-8'>"
        "<meta name='viewport' content='width=device-width, initial-scale=1'>"
        f"<title>{e(t['nombre'])} · {e(svc)}</title><style>{CSS_TEC}{CSS_NAV}{CSS_ARCHIVO}</style></head>"
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
    nombres = [(os.path.basename(c)[:-5], os.path.relpath(c, svc_dir).replace(os.sep, "/")) for c in clases]
    libs = sorted(set(re.findall(
        r"<artifactId>([^<]*(?:junit|mockito|testcontainers|assertj|archunit|rest-assured)[^<]*)</artifactId>", texto)))
    previas = []
    for t in TECNOLOGIAS:
        if t["id"] == "pruebas":
            bloque = None
            aplicado = bool(clases)
        else:
            bloque = bloque_plugin(texto, t["plugin"])
            aplicado = bloque is not None
        previas.append((t, bloque, aplicado))
    navegacion = [(t["id"], t["nombre"]) for t, _, aplicado in previas if aplicado]
    resultado = []
    for t, bloque, aplicado in previas:
        if aplicado:
            os.makedirs(destino, exist_ok=True)
            with open(os.path.join(destino, t["id"] + ".html"), "w", encoding="utf-8") as f:
                f.write(pagina_tecnologia(svc, hue, t, bloque, nombres, libs, svc_dir, navegacion))
        resultado.append({"id": t["id"], "nombre": t["nombre"], "resumen": t["que"],
                          "aplicado": aplicado,
                          "archivo": f"{svc}/tecnologias/{t['id']}.html" if aplicado else None})
    return resultado


CSS_ARCHIVO = (
    ".archivo-java{color:inherit;text-decoration:none;border-bottom:1px dashed hsl(var(--h) 60% 50%)}"
    ".archivo-java:hover{color:hsl(var(--h) 70% 42%);border-bottom-style:solid}"
    ".archivo-java code{font:600 13px ui-monospace,monospace}"
)


def fuentes_java(svc_dir):
    """Mapa nombre completo (paquete.Clase) -> ruta relativa al servicio, de src/main y src/test."""
    mapa = {}
    for sub in ("src/main/java", "src/test/java"):
        base = os.path.join(svc_dir, *sub.split("/"))
        for ruta in glob.glob(os.path.join(base, "**", "*.java"), recursive=True):
            paquete = os.path.relpath(os.path.dirname(ruta), base).replace(os.sep, ".")
            clase = os.path.basename(ruta)[:-5]
            fqn = clase if paquete == "." else paquete + "." + clase
            mapa.setdefault(fqn, os.path.relpath(ruta, svc_dir).replace(os.sep, "/"))
    return mapa


def enlace_java(svc, rel, texto, codigo=False):
    """Enlace al visor de código para un archivo del servicio (las páginas de tecnologías están en docs/api/<svc>/tecnologias/)."""
    href = "../../codigo/visor.html?" + urllib.parse.urlencode({"svc": svc, "f": rel})
    dentro = f"<code>{html.escape(texto)}</code>" if codigo else html.escape(texto)
    return f"<a class='archivo-java' href='{html.escape(href)}' title='Ver código fuente'>{dentro}</a>"


def enlace_archivo(svc, mapa, nombre):
    """Enlace al código de un archivo por su nombre base, solo si el nombre es único en el servicio."""
    rutas = [r for r in mapa.values() if os.path.basename(r) == nombre]
    return enlace_java(svc, rutas[0], nombre) if len(rutas) == 1 else None


def enlace_clase(svc, mapa, nombre):
    """Enlace al código de una clase indicada por su nombre completo (o simple, si es único). None si no existe."""
    fqn = nombre.split("$")[0].strip()
    rel = mapa.get(fqn)
    if rel is None:
        candidatos = [r for f, r in mapa.items() if f == fqn or f.endswith("." + fqn)]
        if len(candidatos) != 1:
            return None
        rel = candidatos[0]
    return enlace_java(svc, rel, nombre)


def generar_codigo(svc, svc_dir):
    """Escribe docs/api/codigo/<svc>.js con el código fuente Java del servicio (funciona abriendo el HTML sin servidor)."""
    archivos = {rel: leer(os.path.join(svc_dir, rel)) for rel in sorted(fuentes_java(svc_dir).values())}
    carpeta = os.path.join(SALIDA, "codigo")
    os.makedirs(carpeta, exist_ok=True)
    datos = json.dumps(archivos, ensure_ascii=False).replace("</", "<\\/")
    with open(os.path.join(carpeta, svc + ".js"), "w", encoding="utf-8") as f:
        f.write(f"window.CODIGO_JAVA = window.CODIGO_JAVA || {{}};\nwindow.CODIGO_JAVA[{json.dumps(svc)}] = {datos};\n")


PAGINA_VISOR = r"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Código fuente · EMPRESA_JOSESAID</title>
<link id="tema-css" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/java.min.js"></script>
<style>
:root{--bg:#f5f7fb;--superficie:#ffffff;--fg:#0f172a;--muted:#64748b;--linea:#e2e8f0;--sombra:rgba(15,23,42,.08);--cabecera-a:#4338ca;--cabecera-b:#0e7490;--code-bg:#0d1117;--code-barra:#161b22;--code-fg:#c9d1d9;--code-num:#484f58;color-scheme:light}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#0b1020;--superficie:#131a2e;--fg:#e2e8f0;--muted:#94a3b8;--linea:#23304f;--sombra:rgba(0,0,0,.35);color-scheme:dark}}
:root[data-theme="dark"]{--bg:#0b1020;--superficie:#131a2e;--fg:#e2e8f0;--muted:#94a3b8;--linea:#23304f;--sombra:rgba(0,0,0,.35);color-scheme:dark}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.55 system-ui,-apple-system,"Segoe UI",sans-serif;padding-bottom:40px}
.cabecera{background:linear-gradient(135deg,var(--cabecera-a),var(--cabecera-b));color:#fff;padding:24px max(16px,env(safe-area-inset-left,0px)) 56px}
.cabecera .in{max-width:1100px;margin:0 auto}
.volver{color:#fff;opacity:.9;font-size:14px;text-decoration:none}
.volver:hover{opacity:1;text-decoration:underline}
.miga{font-size:13px;opacity:.85;margin:14px 0 6px;word-break:break-all}
.cabecera h1{margin:0;font:700 clamp(20px,4vw,28px)/1.2 ui-monospace,SFMono-Regular,Menlo,monospace;letter-spacing:-.01em;overflow-wrap:anywhere}
.chips{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}
.chip{font-size:12.5px;background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.22);border-radius:999px;padding:3px 10px}
main{max-width:1100px;margin:-36px auto 0;padding:0 max(16px,env(safe-area-inset-left,0px)) 0 max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px))}
.visor{background:var(--code-bg);border:1px solid var(--linea);border-radius:14px;box-shadow:0 10px 30px var(--sombra);overflow:hidden;min-width:0}
.barra{display:flex;align-items:center;justify-content:space-between;gap:12px;background:var(--code-barra);color:var(--code-fg);padding:10px 14px;border-bottom:1px solid var(--code-borde);font:13px ui-monospace,SFMono-Regular,Menlo,monospace}
.puntos{display:flex;gap:6px;flex:none}
.puntos i{width:11px;height:11px;border-radius:50%;background:var(--code-borde);display:block}
.puntos i:nth-child(1){background:#ff5f56}.puntos i:nth-child(2){background:#ffbd2e}.puntos i:nth-child(3){background:#27c93f}
.nombre-archivo{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-align:left}
.copiar,.tema-sel{flex:none;font:600 12.5px system-ui,sans-serif;color:var(--code-fg);background:var(--code-borde);border:1px solid var(--code-num);border-radius:8px;padding:5px 10px;cursor:pointer}
.copiar:hover{filter:brightness(1.12)}
.tema-sel{font-weight:500}
.codigo{display:flex;background:var(--code-bg);overflow:auto;max-width:100%}
.nums{flex:none;margin:0;padding:14px 12px 14px 14px;text-align:right;color:var(--code-num);font:13px/1.6 ui-monospace,SFMono-Regular,Menlo,monospace;user-select:none;border-right:1px solid var(--code-borde)}
.codigo .texto{margin:0;padding:14px 16px 14px 14px;flex:1;min-width:0}
.codigo code{font:13px/1.6 ui-monospace,SFMono-Regular,Menlo,monospace;white-space:pre;background:transparent;color:var(--code-fg)}
.codigo code.hljs{background:transparent;padding:0}
.pie{display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;padding:10px 14px;font-size:13px;color:var(--muted);border-top:1px solid var(--linea)}
.aviso{padding:24px;color:var(--muted)}
.aviso a{color:inherit}
.copiado{color:#2da44e}
</style>
</head>
<body>
<header class="cabecera"><div class="in">
  <a class="volver" id="volver" href="#">← Volver a la ficha</a>
  <div class="miga" id="miga"></div>
  <h1 id="titulo">Código fuente</h1>
  <div class="chips" id="chips"></div>
</div></header>
<main>
  <div class="visor" id="visor">
    <div class="barra">
      <div class="puntos" aria-hidden="true"><i></i><i></i><i></i></div>
      <span class="nombre-archivo" id="nombre"></span>
      <select class="tema-sel" id="tema" aria-label="Tema de color del código">
        <option value="github">GitHub Light</option>
        <option value="github-dark">GitHub Dark</option>
        <option value="atom-one-dark">Atom One Dark</option>
        <option value="atom-one-light">Atom One Light</option>
        <option value="monokai">Monokai</option>
      </select>
      <button class="copiar" id="copiar" type="button">Copiar</button>
    </div>
    <div id="contenido" class="aviso">Cargando código…</div>
  </div>
</main>
<script>
(function () {
  var params = new URLSearchParams(location.search);
  var svc = params.get('svc') || '';
  var ruta = params.get('f') || '';
  var contenido = document.getElementById('contenido');

  function mostrarError(mensaje) {
    contenido.className = 'aviso';
    contenido.textContent = mensaje;
    document.getElementById('copiar').hidden = true;
  }

  function escapar(texto) {
    return texto.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function copiar(texto, boton) {
    function listo() { boton.textContent = 'Copiado'; boton.classList.add('copiado'); setTimeout(function () { boton.textContent = 'Copiar'; boton.classList.remove('copiado'); }, 1600); }
    function respaldo() {
      var area = document.createElement('textarea');
      area.value = texto; document.body.appendChild(area); area.select();
      try { document.execCommand('copy'); listo(); } catch (e) { boton.textContent = 'Selecciona y copia'; }
      document.body.removeChild(area);
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(texto).then(listo, respaldo);
    } else {
      respaldo();
    }
  }

  var TEMAS = {
    'github-dark':    { bg: '#0d1117', barra: '#161b22', fg: '#c9d1d9', num: '#6e7681', borde: '#21262d' },
    'github':         { bg: '#ffffff', barra: '#f6f8fa', fg: '#24292f', num: '#8c959f', borde: '#d0d7de' },
    'atom-one-dark':  { bg: '#282c34', barra: '#21252b', fg: '#abb2bf', num: '#5c6370', borde: '#181a1f' },
    'atom-one-light': { bg: '#fafafa', barra: '#eaeaeb', fg: '#383a42', num: '#9d9d9f', borde: '#d4d4d5' },
    'monokai':        { bg: '#272822', barra: '#1e1f1c', fg: '#f8f8f2', num: '#75715e', borde: '#3e3d32' }
  };

  function aplicarTema(id) {
    if (!TEMAS[id]) { id = 'github'; }
    var t = TEMAS[id];
    var visor = document.getElementById('visor');
    visor.style.setProperty('--code-bg', t.bg);
    visor.style.setProperty('--code-barra', t.barra);
    visor.style.setProperty('--code-fg', t.fg);
    visor.style.setProperty('--code-num', t.num);
    visor.style.setProperty('--code-borde', t.borde);
    document.getElementById('tema-css').href =
      'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/' + id + '.min.css';
    document.getElementById('tema').value = id;
    try { localStorage.setItem('visor-tema', id); } catch (e) { /* sin almacenamiento: se usa el tema por defecto */ }
  }

  var selector = document.getElementById('tema');
  selector.addEventListener('change', function () { aplicarTema(this.value); });
  var guardado = null;
  try { guardado = localStorage.getItem('visor-tema'); } catch (e) { guardado = null; }
  aplicarTema(guardado || 'github');

  function pintar(codigo) {
    var nombre = ruta.split('/').pop();
    var paquete = ruta.replace(/^src\/(main|test)\/java\//, '').split('/').slice(0, -1).join('.');
    var lineas = codigo.split('\n');
    if (lineas.length > 1 && lineas[lineas.length - 1] === '') { lineas.pop(); }
    var resaltado = (window.hljs && hljs.getLanguage('java'))
      ? hljs.highlight(codigo, { language: 'java', ignoreIllegals: true }).value
      : escapar(codigo);

    document.title = nombre + ' · ' + svc;
    document.getElementById('volver').href = '../' + svc + '.html';
    document.getElementById('miga').textContent = svc + ' / ' + ruta;
    document.getElementById('titulo').textContent = nombre;
    document.getElementById('nombre').textContent = ruta;

    var tipo = ruta.indexOf('src/test/') === 0 ? 'Prueba' : 'Código principal';
    var chips = [svc, tipo, 'Java'];
    if (paquete) { chips.push(paquete); }
    chips.push(lineas.length + (lineas.length === 1 ? ' línea' : ' líneas'));
    var zona = document.getElementById('chips');
    zona.innerHTML = '';
    chips.forEach(function (texto) {
      var c = document.createElement('span'); c.className = 'chip'; c.textContent = texto; zona.appendChild(c);
    });

    var numeros = [];
    for (var i = 1; i <= lineas.length; i++) { numeros.push(i); }
    contenido.className = '';
    contenido.innerHTML = '';
    var bloque = document.createElement('div');
    bloque.className = 'codigo';
    bloque.innerHTML =
      '<pre class="nums" aria-hidden="true">' + numeros.join('\n') + '</pre>' +
      '<pre class="texto"><code class="hljs language-java">' + resaltado + '</code></pre>';
    contenido.appendChild(bloque);

    var pie = document.createElement('div');
    pie.className = 'pie';
    pie.innerHTML = '<span>Vista de solo lectura</span><span></span>';
    pie.lastChild.textContent = svc;
    contenido.appendChild(pie);

    document.getElementById('copiar').onclick = function () { copiar(codigo, this); };
  }

  if (!/^[a-z0-9-]+$/.test(svc) || !ruta || ruta.indexOf('..') !== -1) {
    mostrarError('Falta el servicio o el archivo que quieres ver.');
    return;
  }
  var datos = document.createElement('script');
  datos.src = svc + '.js';
  datos.onload = function () {
    var archivos = (window.CODIGO_JAVA || {})[svc] || {};
    if (!Object.prototype.hasOwnProperty.call(archivos, ruta)) {
      mostrarError('No encontré este archivo en ' + svc + ': ' + ruta);
      return;
    }
    pintar(archivos[ruta]);
  };
  datos.onerror = function () {
    mostrarError('No se pudo cargar el código de ' + svc + '. Genera de nuevo la documentación.');
  };
  document.head.appendChild(datos);
})();
</script>
</body>
</html>
"""


def main():
    os.makedirs(SALIDA, exist_ok=True)
    os.makedirs(os.path.join(SALIDA, "codigo"), exist_ok=True)
    with open(os.path.join(SALIDA, "codigo", "visor.html"), "w", encoding="utf-8") as f:
        f.write(PAGINA_VISOR)
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
        generar_codigo(svc, svc_dir)
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
    with open(os.path.join(RAIZ, "index.html"), "w", encoding="utf-8") as f:
        f.write(pagina_indice(resumen, desde_raiz=True))
    print(f"🏁 Fichas generadas en {SALIDA}")


def pagina_indice(resumen, desde_raiz=False):
    total = sum(n for _, n in resumen)
    pre_api = "docs/api/" if desde_raiz else ""
    pre_raiz = "" if desde_raiz else "../../"
    pre_docs = "docs/" if desde_raiz else "../"
    tarjetas = "\n".join(
        f'<a class="tarjeta" style="--h:{(i * 137.508) % 360:.0f}" href="{pre_api}{html.escape(svc)}.html">'
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
  <h2>Guías</h2>
  <div class="grid">
<a class="tarjeta" style="--h:200" href="{pre_raiz}instrucciones.html"><span class="nombre">Instrucciones</span><span class="conteo">Arrancar y apagar el proyecto</span><span class="ver">Abrir guía →</span></a>
<a class="tarjeta" style="--h:20" href="{pre_raiz}diagramas.html"><span class="nombre">Diagramas</span><span class="conteo">Arquitectura y flujos del sistema</span><span class="ver">Abrir diagramas →</span></a>
<a class="tarjeta" style="--h:110" href="{pre_raiz}manual.html"><span class="nombre">Manual</span><span class="conteo">Manual completo del proyecto</span><span class="ver">Abrir manual →</span></a>
<a class="tarjeta" style="--h:160" href="{pre_docs}guias/README.html"><span class="nombre">Guías de procesos</span><span class="conteo">Órdenes, pagos, usuarios y más</span><span class="ver">Abrir guías →</span></a>
<a class="tarjeta" style="--h:260" href="{pre_docs}seguridad/inventario-de-claves.md"><span class="nombre">Seguridad</span><span class="conteo">Auditoría de cifrado y rotación de claves</span><span class="ver">Abrir notas →</span></a>
<a class="tarjeta" style="--h:320" href="{pre_docs}registro-prs.md"><span class="nombre">Registro de PRs</span><span class="conteo">Historial de pull requests</span><span class="ver">Abrir registro →</span></a>
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
