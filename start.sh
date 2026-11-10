#!/usr/bin/env bash
# Levanta todo lo que define el docker-compose.yml de la raíz y espera a que cada servicio esté listo.
# Uso: bash start.sh            usa las imágenes ya construidas
#      bash start.sh --build    reconstruye las imágenes antes de levantar (tarda más)
# Requiere Docker Desktop corriendo y un archivo .env en la raíz.
# Tiempo máximo de espera por servicios: TIMEOUT_SEG (300 s por defecto).

set -u
cd "$(dirname "$0")" || exit 1

TIMEOUT_SEG=${TIMEOUT_SEG:-300}
INTERVALO=5

BUILD=""
if [ "${1:-}" = "--build" ]; then BUILD="--build"; fi

inicio=$(date +%s)
formatear() {
  local s=$1
  printf "%02d:%02d:%02d" $((s/3600)) $(((s%3600)/60)) $((s%60))
}
transcurrido() { formatear $(( $(date +%s) - inicio )); }
fallo() {
  echo "❌ $1"
  echo "⏱️  Tiempo transcurrido: $(transcurrido)"
  exit 1
}

echo "🚀 Iniciando EMPRESA_JOSESAID..."

echo "🐳 1/6 Verificando Docker..."
if ! docker info >/dev/null 2>&1; then
  fallo "El daemon de Docker no responde. Abre Docker Desktop y vuelve a intentar."
fi
echo "   ✅ Docker responde."

echo "📄 2/6 Verificando archivo .env..."
if [ ! -f .env ]; then
  fallo "No existe .env en la raíz del proyecto. Crea el archivo con las variables requeridas."
fi
echo "   ✅ .env encontrado."

echo "🌐 3/6 Verificando la red empresa-net..."
if docker network inspect empresa-net >/dev/null 2>&1; then
  echo "   ✅ La red ya existe."
else
  echo "   🔧 La red no existe. Creándola..."
  docker network create empresa-net >/dev/null || fallo "No se pudo crear la red empresa-net."
  echo "   ✅ Red creada."
fi

echo "⏸️  Pausa de 5 s antes de levantar los contenedores..."
sleep 5

if [ -n "$BUILD" ]; then
  echo "🔨 4/6 Construyendo imágenes y levantando contenedores (puede tardar varios minutos)..."
else
  echo "⬆️  4/6 Levantando contenedores..."
fi
docker compose up -d $BUILD || fallo "docker compose up falló. Revisa el mensaje anterior."
echo "   ✅ Contenedores creados. Arrancando..."

echo "⏸️  Pausa de 10 s para que PostgreSQL, Kafka y Redis empiecen a arrancar..."
sleep 10

echo "⏳ 5/6 Esperando a que los servicios estén listos (máximo $(formatear "$TIMEOUT_SEG"))..."
espera_inicio=$(date +%s)
while true; do
  total=0
  listos=0
  pend=""
  while IFS='|' read -r servicio estado salud; do
    [ -z "$servicio" ] && continue
    total=$((total + 1))
    if [ "$estado" = "running" ] && { [ -z "$salud" ] || [ "$salud" = "healthy" ]; }; then
      listos=$((listos + 1))
    else
      pend="$pend $servicio($estado${salud:+/$salud})"
    fi
  done < <(docker compose ps -a --format '{{.Service}}|{{.State}}|{{.Health}}' 2>/dev/null)

  if [ "$total" -gt 0 ] && [ -z "$pend" ]; then
    echo "   ✅ $listos/$total servicios listos."
    break
  fi

  esperado=$(( $(date +%s) - espera_inicio ))
  if [ "$esperado" -ge "$TIMEOUT_SEG" ]; then
    echo "   ⚠️  Se agotó el tiempo de espera: $listos/$total listos."
    echo "   ⏳ Pendientes:${pend:- ninguno}"
    fallo "Algunos servicios no quedaron listos. Revisa con: docker compose ps  y  docker compose logs <servicio>"
  fi

  echo "   🔄 [$(formatear "$esperado")] Listos: $listos/$total. Esperando:${pend:- ninguno}"
  sleep "$INTERVALO"
done

echo "🔍 6/6 Estado final de los contenedores:"
docker compose ps --format '   {{.Service}}: {{.State}}{{if .Health}} ({{.Health}}){{end}}' 2>/dev/null

duracion=$(( $(date +%s) - inicio ))
echo ""
echo "⏱️  Tiempo total de ejecución: $(formatear "$duracion") ($duracion s)"
echo "🏁 Inicio terminado. EMPRESA_JOSESAID está listo."
