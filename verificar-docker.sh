#!/usr/bin/env bash
# Verifica Docker y las herramientas de build en tu Mac.
# Uso: bash verificar-docker.sh   (pega la salida completa en el chat)
set -u

echo "== 1. Docker =="
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker no está en el PATH"; exit 1
fi
docker version --format 'cliente={{.Client.Version}} servidor={{.Server.Version}}' || { echo "ERROR: el daemon no responde. Abre Docker Desktop."; exit 1; }
docker info --format 'driver={{.Driver}} contenedores={{.Containers}}'

echo "== 2. Descarga y ejecución (hello-world) =="
docker run --rm hello-world | head -3

echo "== 3. PostgreSQL 17 (smoke test) =="
docker rm -f pg-check >/dev/null 2>&1
docker run -d --name pg-check -e POSTGRES_PASSWORD=check postgres:17-alpine >/dev/null
for i in $(seq 1 30); do docker exec pg-check pg_isready -U postgres >/dev/null 2>&1 && break; sleep 2; done
docker exec pg-check pg_isready -U postgres
docker rm -f pg-check >/dev/null

echo "== 4. Herramientas de build =="
java -version 2>&1 | head -1
mvn -v 2>/dev/null | head -1 || echo "mvn no instalado"
docker compose version 2>/dev/null || echo "docker compose no disponible"

echo "== Fin. Pega esta salida en el chat =="
