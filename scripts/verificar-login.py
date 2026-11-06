#!/usr/bin/env python3
"""Prueba el login de la UI (127.0.0.1:8098) y revisa la IP que registra el gateway.

Pide el correo y la contraseña en la terminal; la contraseña no se muestra.
No guarda credenciales ni imprime el token de sesión.
"""
import getpass
import http.cookiejar
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

UI = "http://127.0.0.1:8098"
GATEWAY_CONTENEDOR = "gateway-ms-gateway-ms-1"


class SinRedireccion(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def main():
    correo = input("Correo: ").strip()
    password = getpass.getpass("Contraseña: ")

    jar = http.cookiejar.CookieJar()
    abrir = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(jar), SinRedireccion)

    # 1. Página de login para obtener el token CSRF.
    with abrir.open(f"{UI}/login") as r:
        html = r.read().decode("utf-8", "replace")
    m = re.search(r'name="_csrf"\s+value="([^"]+)"', html) or \
        re.search(r'value="([^"]+)"\s+name="_csrf"', html)
    if not m:
        print("No encontré el token CSRF en /login. La UI no responde como se esperaba.")
        sys.exit(1)

    # 2. Envío del formulario.
    datos = urllib.parse.urlencode({
        "correo": correo, "password": password, "_csrf": m.group(1)}).encode()
    peticion = urllib.request.Request(f"{UI}/login", data=datos, method="POST")
    try:
        respuesta = abrir.open(peticion)
        codigo, destino = respuesta.status, respuesta.headers.get("Location", "")
    except urllib.error.HTTPError as e:
        codigo, destino = e.code, e.headers.get("Location", "")

    # Login correcto: redirige a "/" (o fuera de /login?error).
    if codigo in (301, 302, 303) and "error" not in destino:
        print(f"LOGIN OK: la UI redirigió a {destino or '/'}.")
    elif codigo in (301, 302, 303):
        print(f"LOGIN FALLIDO: redirigió a {destino}. Revisa correo y contraseña.")
    else:
        print(f"LOGIN FALLIDO: código HTTP {codigo}.")

    # 3. Línea de log del gateway (solo sale si el nivel DEBUG está activo).
    try:
        log = subprocess.run(
            ["docker", "logs", GATEWAY_CONTENEDOR],
            capture_output=True, text=True, timeout=30)
        lineas = [l for l in (log.stdout + log.stderr).splitlines()
                  if "Intento de inicio" in l]
        if lineas:
            print("Última línea del gateway:")
            print(lineas[-1])
        else:
            print("Sin línea 'Intento de inicio' en el gateway (el nivel DEBUG no está activo).")
    except (OSError, subprocess.SubprocessError) as e:
        print(f"No pude leer los logs de Docker: {type(e).__name__}")


if __name__ == "__main__":
    main()
