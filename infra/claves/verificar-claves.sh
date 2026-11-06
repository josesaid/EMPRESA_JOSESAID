#!/usr/bin/env bash
# Revisa un archivo .env antes de levantar el ecosistema: que existan todas las variables
# obligatorias, que las claves tengan el formato correcto y que el .env no esté en Git.
# Uso: bash infra/claves/verificar-claves.sh [ruta/al/.env]   (por defecto: .env de la raíz)
# No imprime valores secretos: solo nombres de variables y resultados.
set -u
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="${1:-$RAIZ/.env}"

# Debe coincidir con las variables obligatorias (${VAR:?...}) de los docker-compose.yml.
# La prueba de infra/claves/prueba-verificar-claves.sh verifica que la lista siga siendo la correcta.
OBLIGATORIAS="POSTGRES_PASSWORD CLIENTES_DB_PASSWORD USUARIOS_ACCESOS_DB_PASSWORD USUARIOS_JWT_CLAVE_PRIVADA
FACTURACION_DB_PASSWORD CONTABILIDAD_DB_PASSWORD INVENTARIO_DB_PASSWORD COMPRAS_DB_PASSWORD PEDIDOS_DB_PASSWORD
DOCUMENTOS_DB_PASSWORD REPORTES_DB_PASSWORD NEXUS_DB_PASSWORD USUARIOS_CIFRADO_CLAVE USUARIOS_INDICE_CLAVE
COMPRAS_CIFRADO_CLAVE COMPRAS_INDICE_CLAVE DOCUMENTOS_CIFRADO_CLAVE CRYPTO_CLAVE_CIFRADO CRYPTO_CLAVE_INDICE
EUREKA_USUARIO EUREKA_PASSWORD ANTHROPIC_API_KEY GRAFANA_ADMIN_PASSWORD"

ok=0; fallo=0
verificar() { if [ "$1" -eq 0 ]; then echo "OK: $2"; ok=$((ok+1)); else echo "FALLO: $2"; fallo=$((fallo+1)); fi; }

if [ ! -f "$ENV_FILE" ]; then
  echo "FALLO: no existe $ENV_FILE"; exit 1
fi

# Lee una variable sin ejecutar el archivo (no se hace source: el .env no es código).
valor() { grep -E "^$1=" "$ENV_FILE" | tail -n 1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//"; }

for v in $OBLIGATORIAS; do
  x="$(valor "$v")"
  if [ -z "$x" ]; then verificar 1 "$v definida y no vacía"; else verificar 0 "$v definida"; fi
done

# Claves AES-256 y claves HMAC: base64 de 32 bytes, y distintas entre sí.
bytes_base64() { printf '%s' "$1" | base64 -d 2>/dev/null | wc -c | tr -d ' '; }
for v in USUARIOS_CIFRADO_CLAVE USUARIOS_INDICE_CLAVE COMPRAS_CIFRADO_CLAVE COMPRAS_INDICE_CLAVE DOCUMENTOS_CIFRADO_CLAVE CRYPTO_CLAVE_CIFRADO CRYPTO_CLAVE_INDICE; do
  x="$(valor "$v")"
  [ -z "$x" ] && continue
  verificar $([ "$(bytes_base64 "$x")" = "32" ]; echo $?) "$v decodifica a 32 bytes"
done
c1="$(valor COMPRAS_CIFRADO_CLAVE)"; c2="$(valor COMPRAS_INDICE_CLAVE)"
if [ -n "$c1" ] && [ -n "$c2" ]; then [ "$c1" != "$c2" ]; verificar $? "COMPRAS_CIFRADO_CLAVE y COMPRAS_INDICE_CLAVE son distintas"; fi
c1="$(valor USUARIOS_CIFRADO_CLAVE)"; c2="$(valor USUARIOS_INDICE_CLAVE)"
if [ -n "$c1" ] && [ -n "$c2" ]; then [ "$c1" != "$c2" ]; verificar $? "USUARIOS_CIFRADO_CLAVE y USUARIOS_INDICE_CLAVE son distintas"; fi

# Clave RSA de JWT: PKCS#8 DER en base64 (la misma que usa usuarios-accesos-ms)
jwt="$(valor USUARIOS_JWT_CLAVE_PRIVADA)"
if [ -n "$jwt" ]; then
  printf '%s' "$jwt" | base64 -d 2>/dev/null | openssl pkey -inform DER -noout >/dev/null 2>&1
  verificar $? "USUARIOS_JWT_CLAVE_PRIVADA es PKCS#8 DER válido"
fi

# Contraseñas de base de datos: al menos 16 caracteres (openssl rand -base64 24 da 32)
for v in POSTGRES_PASSWORD CLIENTES_DB_PASSWORD USUARIOS_ACCESOS_DB_PASSWORD FACTURACION_DB_PASSWORD; do
  x="$(valor "$v")"
  [ -z "$x" ] && continue
  verificar $([ "${#x}" -ge 16 ]; echo $?) "$v tiene al menos 16 caracteres"
done

# El .env no debe estar versionado en Git
if git -C "$RAIZ" rev-parse --git-dir >/dev/null 2>&1; then
  if git -C "$RAIZ" ls-files --error-unmatch "$ENV_FILE" >/dev/null 2>&1; then
    verificar 1 "el archivo .env no está en Git"
  else
    verificar 0 "el archivo .env no está en Git"
  fi
fi

echo
echo "Resumen: $ok OK, $fallo FALLO"
exit $fallo
