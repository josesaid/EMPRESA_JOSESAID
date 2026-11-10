#!/usr/bin/env bash
# Apaga todo lo que levanta el docker-compose.yml de la raíz (servicios, infraestructura y monitoreo).
# No borra volúmenes: los datos de PostgreSQL, Redis y Kafka se conservan.
# Uso: bash stop.sh

set -u
cd "$(dirname "$0")" || exit 1

inicio=$(date +%s)
formatear() {
  local s=$1
  printf "%02d:%02d:%02d" $((s/3600)) $(((s%3600)/60)) $((s%60))
}

hecho=()
pendientes=()

echo "🛑 Iniciando apagado de EMPRESA_JOSESAID..."

echo "🔎 1/3 Revisando contenedores activos..."
activos=$(docker compose ps -q 2>/dev/null | wc -l | tr -d ' ')
if [ "$activos" = "0" ]; then
  echo "   ℹ️  No hay contenedores activos del proyecto."
else
  echo "   📦 Contenedores activos: $activos"
fi
hecho+=("Revisados $activos contenedores activos")

echo "⏹️  2/3 Deteniendo y quitando contenedores y redes (los volúmenes se conservan)..."
if docker compose down --remove-orphans; then
  echo "   ✅ Servicios detenidos."
  hecho+=("Servicios, infraestructura y monitoreo detenidos")
else
  echo "   ❌ docker compose down falló. Revisa el mensaje anterior."
  pendientes+=("docker compose down falló")
fi

echo "🧪 3/3 Verificando que no quede nada del proyecto..."
restantes_proyecto=$(docker ps -q --filter "label=com.docker.compose.project" 2>/dev/null | wc -l | tr -d ' ')
if [ "$restantes_proyecto" = "0" ]; then
  echo "   ✅ No quedan contenedores de Compose corriendo."
  hecho+=("Verificado: no quedan contenedores de Compose")
else
  echo "   ⚠️  Quedan $restantes_proyecto contenedores de Compose. Revisa con: docker ps"
  pendientes+=("Quedan $restantes_proyecto contenedores de Compose")
fi

echo ""
echo "📋 Resumen"
for item in "${hecho[@]}"; do echo "   ✅ $item"; done
for item in "${pendientes[@]:-}"; do [ -n "$item" ] && echo "   ⚠️  Pendiente: $item"; done
echo "   💾 Volúmenes conservados (datos intactos)."
duracion=$(( $(date +%s) - inicio ))
echo "⏱️  Tiempo total de ejecución: $(formatear "$duracion") ($duracion s)"
echo "🏁 Apagado terminado."
