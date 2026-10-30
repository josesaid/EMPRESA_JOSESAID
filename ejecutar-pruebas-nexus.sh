#!/usr/bin/env bash
# Ejecuta las pruebas de nexus-comun en tu Mac. No necesita Docker: es una librería, no un servicio.
# Uso: bash ejecutar-pruebas-nexus.sh   (pega la salida completa en el chat)
set -u
RAIZ="$(cd "$(dirname "$0")" && pwd)"
SERVICIO="$RAIZ/nexus-comun"
ok=0; fallo=0
paso() { echo; echo "== $1 =="; }
verificar() { if [ $1 -eq 0 ]; then echo "OK: $2"; ok=$((ok+1)); else echo "FALLO: $2"; fallo=$((fallo+1)); fi; }

paso "1. Java 21 y Maven"
JAVA21=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
if [ -n "$JAVA21" ]; then export JAVA_HOME="$JAVA21"; fi
java -version 2>&1 | head -1
if ! command -v mvn >/dev/null 2>&1; then echo "Maven no instalado (brew install maven)"; exit 1; fi
mvn -v 2>/dev/null | head -1
verificar 0 "Java y Maven disponibles"

paso "2. Pruebas de nexus-comun (filtro, tokens, prompts y evaluaciones)"
(cd "$SERVICIO" && mvn -B verify > "$RAIZ/.verify-nexus.log" 2>&1); verificar $? "mvn verify"
grep -E "Tests run:" "$RAIZ/.verify-nexus.log" | tail -3

echo
echo "== Resumen: $ok OK, $fallo fallos =="
echo "Log: .verify-nexus.log en la raíz del repositorio."
echo "== Fin. Pega esta salida en el chat =="
