#!/usr/bin/env bash
# Prueba de verificar-claves.sh: un .env bueno pasa; uno con errores falla.
# Uso: bash infra/claves/prueba-verificar-claves.sh
set -u
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
VERIFICADOR="$RAIZ/infra/claves/verificar-claves.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
pruebas=0; fallos=0
afirmar() { pruebas=$((pruebas+1)); if [ "$1" -eq 0 ]; then echo "OK: $2"; else echo "FALLO: $2"; fallos=$((fallos+1)); fi; }

aes() { openssl rand -base64 32; }
pwd_() { openssl rand -base64 24; }
generar_env() {
  local f="$1"
  {
    echo "POSTGRES_PASSWORD=$(pwd_)"
    for v in CLIENTES_DB_PASSWORD USUARIOS_ACCESOS_DB_PASSWORD FACTURACION_DB_PASSWORD CONTABILIDAD_DB_PASSWORD INVENTARIO_DB_PASSWORD COMPRAS_DB_PASSWORD PEDIDOS_DB_PASSWORD DOCUMENTOS_DB_PASSWORD REPORTES_DB_PASSWORD NEXUS_DB_PASSWORD; do echo "$v=$(pwd_)"; done
    echo "USUARIOS_JWT_CLAVE_PRIVADA=$(openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -outform DER 2>/dev/null | base64 | tr -d '\n')"
    for v in USUARIOS_CIFRADO_CLAVE USUARIOS_INDICE_CLAVE COMPRAS_CIFRADO_CLAVE COMPRAS_INDICE_CLAVE DOCUMENTOS_CIFRADO_CLAVE CRYPTO_CLAVE_CIFRADO CRYPTO_CLAVE_INDICE; do echo "$v=$(aes)"; done
    echo "EUREKA_USUARIO=eureka"
    echo "EUREKA_PASSWORD=$(pwd_)"
    echo "ANTHROPIC_API_KEY=sk-ant-prueba"
    echo "GRAFANA_ADMIN_PASSWORD=$(pwd_)"
  } > "$f"
}

# 1. Un .env completo y correcto pasa
generar_env "$TMP/bueno.env"
bash "$VERIFICADOR" "$TMP/bueno.env" >/dev/null 2>&1
afirmar $? "un .env correcto pasa la verificación"

# 2. Falta una variable obligatoria: falla
grep -v "^NEXUS_DB_PASSWORD=" "$TMP/bueno.env" > "$TMP/falta.env"
bash "$VERIFICADOR" "$TMP/falta.env" >/dev/null 2>&1
afirmar $([ $? -ne 0 ]; echo $?) "una variable obligatoria faltante falla"

# 3. Clave AES de 16 bytes en lugar de 32: falla
sed "s|^USUARIOS_CIFRADO_CLAVE=.*|USUARIOS_CIFRADO_CLAVE=$(openssl rand -base64 16)|" "$TMP/bueno.env" > "$TMP/aes16.env"
bash "$VERIFICADOR" "$TMP/aes16.env" >/dev/null 2>&1
afirmar $([ $? -ne 0 ]; echo $?) "una clave AES de 16 bytes falla"

# 4. Clave de cifrado igual a la de índice: falla
val="$(grep '^COMPRAS_CIFRADO_CLAVE=' "$TMP/bueno.env" | cut -d= -f2-)"
sed "s|^COMPRAS_INDICE_CLAVE=.*|COMPRAS_INDICE_CLAVE=$val|" "$TMP/bueno.env" > "$TMP/igual.env"
bash "$VERIFICADOR" "$TMP/igual.env" >/dev/null 2>&1
afirmar $([ $? -ne 0 ]; echo $?) "cifrado e índice iguales fallan"

# 5. Clave JWT que no es PKCS#8 DER válido: falla
sed "s|^USUARIOS_JWT_CLAVE_PRIVADA=.*|USUARIOS_JWT_CLAVE_PRIVADA=$(printf 'no-es-una-clave' | base64)|" "$TMP/bueno.env" > "$TMP/jwt.env"
bash "$VERIFICADOR" "$TMP/jwt.env" >/dev/null 2>&1
afirmar $([ $? -ne 0 ]; echo $?) "una clave JWT inválida falla"

# 6. Contraseña de base de datos corta: falla
sed "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=corta|" "$TMP/bueno.env" > "$TMP/corta.env"
bash "$VERIFICADOR" "$TMP/corta.env" >/dev/null 2>&1
afirmar $([ $? -ne 0 ]; echo $?) "una contraseña corta falla"

# 7. La lista de obligatorias coincide con las variables ${X:?...} de los docker-compose.yml
cd "$RAIZ" || exit 1
en_compose="$(git grep -ho '\${[A-Z_]*:?' -- '*.yml' 2>/dev/null | sed 's/\${//; s/:?//' | sort -u)"
en_script="$(sed -n '/^OBLIGATORIAS=/,/"$/p' "$VERIFICADOR" | tr -d '"' | sed 's/OBLIGATORIAS=//' | tr ' ' '\n' | grep -E '^[A-Z_]+$' | sort -u)"
comunes_faltan="$(comm -23 <(echo "$en_compose" | grep -v '^$') <(echo "$en_script" | grep -v '^$') | grep -v GATEWAY_TLS_PASSWORD | grep -v '^$')"
afirmar $([ -z "$comunes_faltan" ]; echo $?) "la lista del verificador cubre las variables obligatorias de los Compose (sin GATEWAY_TLS_PASSWORD)"
[ -n "$comunes_faltan" ] && echo "  faltan en el verificador: $comunes_faltan"

echo; echo "Pruebas: $pruebas, fallos: $fallos"
exit $fallos
