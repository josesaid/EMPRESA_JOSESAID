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


def pagina_swagger(svc, spec):
    json_txt = json.dumps(spec, ensure_ascii=False, indent=2).replace("</", "<\\/")
    return f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>API {html.escape(svc)} · Swagger</title>
<link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@{SWAGGER_VERSION}/swagger-ui.css">
<style>body{{margin:0}} .topbar{{display:none}}
.volver{{font:14px system-ui,sans-serif;padding:10px 16px;background:#1b1f24;color:#fff}}
.volver a{{color:#9ecbff}}</style>
</head>
<body>
<div class="volver"><a href="index.html">← Índice de servicios</a> · {html.escape(svc)}</div>
<div id="swagger-ui"></div>
<script type="application/json" id="spec">
{json_txt}
</script>
<script src="https://unpkg.com/swagger-ui-dist@{SWAGGER_VERSION}/swagger-ui-bundle.js"></script>
<script>
window.onload = function () {{
  window.ui = SwaggerUIBundle({{
    spec: JSON.parse(document.getElementById("spec").textContent),
    dom_id: "#swagger-ui",
    deepLinking: true,
    persistAuthorization: true
  }});
}};
</script>
</body>
</html>
"""


def pagina_indice(resumen):
    total = sum(n for _, n in resumen)
    tarjetas = "\n".join(
        f'<a class="tarjeta" style="--h:{(i * 137.508) % 360:.0f}" href="{html.escape(svc)}.html">'
        f'<span class="nombre">{html.escape(svc)}</span>'
        f'<span class="conteo">{n} endpoint{"s" if n != 1 else ""}</span>'
        f'<span class="ver">Abrir Swagger →</span></a>'
        for i, (svc, n) in enumerate(resumen))
    fecha = datetime.date.today().isoformat()
    return f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Documentación de API · EMPRESA_JOSESAID</title>
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
      <span class="etiqueta">Documentación de API</span>
      <h1>EMPRESA_JOSESAID</h1>
      <p class="lead">Endpoints REST de cada microservicio, con su especificación OpenAPI 3.0 y Swagger UI.</p>
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


def main():
    os.makedirs(SALIDA, exist_ok=True)
    servicios = sorted(
        d for d in os.listdir(RAIZ)
        if os.path.isdir(os.path.join(RAIZ, d)) and d not in EXCLUIDOS
        and controladores(os.path.join(RAIZ, d)))
    resumen = []
    for svc in servicios:
        spec, n = construir_spec(svc)
        with open(os.path.join(SALIDA, svc + ".json"), "w", encoding="utf-8") as f:
            json.dump(spec, f, ensure_ascii=False, indent=2)
        with open(os.path.join(SALIDA, svc + ".html"), "w", encoding="utf-8") as f:
            f.write(pagina_swagger(svc, spec))
        resumen.append((svc, n))
        print(f"✅ {svc}: {n} endpoints")
    with open(os.path.join(SALIDA, "index.html"), "w", encoding="utf-8") as f:
        f.write(pagina_indice(resumen))
    print(f"🏁 Documentación generada en {SALIDA}")


if __name__ == "__main__":
    main()
