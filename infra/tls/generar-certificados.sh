#!/usr/bin/env bash
# Genera una CA interna y el certificado TLS del gateway (PKCS#12).
# Uso: bash infra/tls/generar-certificados.sh
# Salida: infra/tls/out/ (ignorada por Git; nunca subir al repositorio).
#   ca.crt               certificado público de la CA: confiar en él en los clientes
#   ca.key               clave privada de la CA: guardarla fuera de la máquina de trabajo
#   gateway.p12          keystore del gateway (clave + certificado)
#   truststore.p12       truststore con la CA (para los clientes que llaman al gateway)
#   gateway-password.txt contraseña del keystore (si no se definió GATEWAY_TLS_PASSWORD)
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")" && pwd)"
SALIDA="$RAIZ/out"
DIAS_CA="${TLS_DIAS_CA:-1825}"
DIAS_GATEWAY="${TLS_DIAS_GATEWAY:-365}"
mkdir -p "$SALIDA"
chmod 700 "$SALIDA"

if [ -f "$SALIDA/ca.key" ]; then
  echo "Ya existe una CA en $SALIDA. Bórrala a mano si de verdad quieres otra; no se sobrescribe." >&2
  exit 1
fi

PASSWORD="${GATEWAY_TLS_PASSWORD:-$(openssl rand -base64 24)}"
if [ -z "${GATEWAY_TLS_PASSWORD:-}" ]; then
  printf '%s\n' "$PASSWORD" > "$SALIDA/gateway-password.txt"
  chmod 600 "$SALIDA/gateway-password.txt"
fi

# 1. CA interna
openssl req -x509 -newkey rsa:4096 -sha256 -nodes -days "$DIAS_CA" \
  -keyout "$SALIDA/ca.key" -out "$SALIDA/ca.crt" \
  -subj "/CN=EMPRESA_JOSESAID Internal CA" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign"
chmod 600 "$SALIDA/ca.key"

# 2. Certificado del gateway firmado por la CA (SAN para localhost y el nombre en la red de Docker)
openssl req -newkey rsa:2048 -sha256 -nodes \
  -keyout "$SALIDA/gateway.key" -out "$SALIDA/gateway.csr" \
  -subj "/CN=gateway-ms"
cat > "$SALIDA/gateway.ext" <<EXT
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:localhost,DNS:gateway-ms,IP:127.0.0.1
EXT
openssl x509 -req -sha256 -days "$DIAS_GATEWAY" -in "$SALIDA/gateway.csr" \
  -CA "$SALIDA/ca.crt" -CAkey "$SALIDA/ca.key" -CAcreateserial \
  -extfile "$SALIDA/gateway.ext" -out "$SALIDA/gateway.crt"

# 3. Keystore PKCS#12 que lee Spring Boot
openssl pkcs12 -export -name gateway -inkey "$SALIDA/gateway.key" -in "$SALIDA/gateway.crt" \
  -certfile "$SALIDA/ca.crt" -out "$SALIDA/gateway.p12" -passout "pass:$PASSWORD"

# 4. Truststore PKCS#12 con la CA: lo usan ui-ms y nexus-ms para confiar en el gateway
keytool -importcert -noprompt -alias empresa-ca -file "$SALIDA/ca.crt" \
  -keystore "$SALIDA/truststore.p12" -storetype PKCS12 -storepass "$PASSWORD" >/dev/null 2>&1

rm -f "$SALIDA/gateway.csr" "$SALIDA/gateway.ext" "$SALIDA/gateway.key" "$SALIDA/gateway.crt" "$SALIDA/ca.srl"
echo "Listo. Archivos en $SALIDA: ca.crt, gateway.p12 (y gateway-password.txt si no definiste GATEWAY_TLS_PASSWORD)."
