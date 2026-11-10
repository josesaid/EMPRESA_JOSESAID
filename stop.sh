#!/usr/bin/env bash
# Apaga todo lo que levanta el docker-compose.yml de la raíz (servicios, infraestructura y monitoreo).
# No borra volúmenes: los datos de PostgreSQL, Redis y Kafka se conservan.
# Uso: bash stop.sh

set -u
cd "$(dirname "$0")" || exit 1

# Colores solo si la salida es una terminal
if [ -t 1 ]; then
  RST=$'\033[0m'; NEG=$'\033[1m'; TENUE=$'\033[2m'
  ROJO=$'\033[1;31m'; VERDE=$'\033[1;32m'; AMARILLO=$'\033[1;33m'
  AZUL=$'\033[1;34m'; MAGENTA=$'\033[1;35m'; CIAN=$'\033[1;36m'
else
  RST=""; NEG=""; TENUE=""; ROJO=""; VERDE=""; AMARILLO=""; AZUL=""; MAGENTA=""; CIAN=""
fi

inicio=$(date +%s)

formatear() {
  local s=$1
  printf "%02d:%02d:%02d" $((s/3600)) $(((s%3600)/60)) $((s%60))
}
transcurrido() { formatear $(( $(date +%s) - inicio )); }

linea() { echo "${MAGENTA}────────────────────────────────────────────────────────────${RST}"; }

paso() {
  echo ""
  linea
  echo "${NEG}${CIAN}$1${RST}"
  linea
}

echo ""
echo "${NEG}${ROJO}🛑 Iniciando apagado de EMPRESA_JOSESAID${RST}"

if ! docker info >/dev/null 2>&1; then
  paso "🐳 Docker no está corriendo"
  echo "   ${AMARILLO}ℹ️  El daemon de Docker no responde: no hay contenedores que detener${RST}"
  echo "   ${TENUE}Si Docker Desktop está abierto, revísalo y vuelve a intentar.${RST}"
  duracion=$(( $(date +%s) - inicio ))
  echo ""
  linea
  echo "${NEG}📋 Resumen${RST}"
  echo "   ${AZUL}ℹ️  Docker ya estaba apagado: nada que detener${RST}"
  echo "   ${AZUL}⏱️  Tiempo total: ${NEG}$(formatear "$duracion")${RST}${AZUL}  ($duracion s)${RST}"
  echo "${NEG}${ROJO}🏁 Apagado terminado${RST}"
  linea
  exit 0
fi

paso "🔎 Paso 1/3 · Revisando contenedores activos"
activos=$(docker compose ps -q 2>/dev/null | wc -l | tr -d ' ')
if [ "$activos" = "0" ]; then
  echo "   ${TENUE}ℹ️  No hay contenedores activos del proyecto${RST}"
else
  echo "   ${AMARILLO}📦 Contenedores activos: ${NEG}$activos${RST}"
fi

paso "⏹️  Paso 2/3 · Deteniendo y quitando contenedores y redes (los volúmenes se conservan)"
hecho_servicios=0
if docker compose down --remove-orphans; then
  echo "   ${VERDE}✅ Servicios detenidos${RST}"
  hecho_servicios=1
else
  echo "   ${ROJO}❌ docker compose down falló. Revisa el mensaje anterior${RST}"
fi

paso "🧪 Paso 3/3 · Verificando que no quede nada del proyecto"
restantes=$(docker ps -q --filter "label=com.docker.compose.project" 2>/dev/null | wc -l | tr -d ' ')
if [ "$restantes" = "0" ]; then
  echo "   ${VERDE}✅ No quedan contenedores de Compose corriendo${RST}"
else
  echo "   ${AMARILLO}⚠️  Quedan ${NEG}$restantes${RST}${AMARILLO} contenedores de Compose${RST}"
  echo "   ${TENUE}Revisa con: docker ps${RST}"
fi

duracion=$(( $(date +%s) - inicio ))
echo ""
linea
echo "${NEG}📋 Resumen${RST}"
if [ "$hecho_servicios" = "1" ] && [ "$restantes" = "0" ]; then
  echo "   ${VERDE}✅ Todo apagado${RST}"
else
  echo "   ${AMARILLO}⚠️  Hubo pendientes, revisa los pasos anteriores${RST}"
fi
echo "   ${AZUL}💾 Volúmenes conservados (datos intactos)${RST}"
echo "   ${AZUL}⏱️  Tiempo total: ${NEG}$(formatear "$duracion")${RST}${AZUL}  ($duracion s)${RST}"
echo "${NEG}${ROJO}🏁 Apagado terminado${RST}"
linea
