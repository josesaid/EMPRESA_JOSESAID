#!/usr/bin/env bash
# Ejecuta las pruebas completas de usuarios-accesos-ms en tu Mac (requiere Docker Desktop abierto).
# Uso: bash ejecutar-pruebas-usuarios.sh   (pega la salida completa en el chat)
set -u
RAIZ="$(cd "$(dirname "$0")" && pwd)"
SERVICIO="$RAIZ/usuarios-accesos-ms"
ok=0; fallo=0
paso() { echo; echo "== $1 =="; }
verificar() { if [ $1 -eq 0 ]; then echo "OK: $2"; ok=$((ok+1)); else echo "FALLO: $2"; fallo=$((fallo+1)); fi; }

paso "1. Java 21 y Maven"
JAVA21=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
if [ -z "$JAVA21" ]; then echo "No encuentro JDK 21. Instálalo (por ejemplo: brew install --cask temurin@21) y vuelve a ejecutar."; exit 1; fi
export JAVA_HOME="$JAVA21"
java -version 2>&1 | head -1
mvn -v 2>/dev/null | head -1 || { echo "Maven no instalado (brew install maven)"; exit 1; }

paso "2. Docker"
docker info >/dev/null 2>&1; verificar $? "Docker responde"

paso "3. Validar archivos de Compose"
POSTGRES_PASSWORD=x CLIENTES_DB_PASSWORD=x USUARIOS_ACCESOS_DB_PASSWORD=x FACTURACION_DB_PASSWORD=x USUARIOS_JWT_SECRET=x USUARIOS_ADMIN_INICIAL_CORREO=admin@test.local USUARIOS_ADMIN_INICIAL_PASSWORD=x docker compose -f "$RAIZ/docker-compose.yml" config -q 2>"$RAIZ/.cfg-raiz.err"; verificar $? "Compose de la raíz válido (valores de prueba; no crea nada)"
(cd "$SERVICIO" && USUARIOS_ACCESOS_DB_PASSWORD=x USUARIOS_JWT_SECRET=x USUARIOS_ADMIN_INICIAL_CORREO=admin@test.local USUARIOS_ADMIN_INICIAL_PASSWORD=x POSTGRES_PASSWORD=x docker compose config -q); verificar $? "Compose de usuarios-accesos-ms válido"

paso "4. Pruebas completas (unitarias + integración con Testcontainers)"
(cd "$SERVICIO" && mvn -B verify > "$RAIZ/.verify-usuarios.log" 2>&1); verificar $? "mvn verify"
grep -E "Tests run:" "$RAIZ/.verify-usuarios.log" | tail -3

paso "5. Construir la imagen"
(cd "$SERVICIO" && docker build -t usuarios-accesos-ms:0.1.0 . > "$RAIZ/.build-usuarios.log" 2>&1); verificar $? "docker build"

echo
echo "== Resumen: $ok OK, $fallo fallos =="
echo "Logs: .verify-usuarios.log y .build-usuarios.log en la raíz del repositorio."
echo "== Fin. Pega esta salida en el chat =="
