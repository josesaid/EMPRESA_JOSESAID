#!/usr/bin/env bash
# Levanta todo lo que define el docker-compose.yml de la raíz y espera a que cada servicio esté listo.
# Uso: bash configuration_scripts/start.sh            usa las imágenes ya construidas
#      bash configuration_scripts/start.sh --build    reconstruye las imágenes antes de levantar (tarda más)
# Requiere Docker Desktop corriendo y un archivo .env en la raíz.
# Tiempo máximo de espera por servicios: TIMEOUT_SEG (300 s por defecto).

set -u
cd "$(dirname "$0")/.." || exit 1

TIMEOUT_SEG=${TIMEOUT_SEG:-300}
TIMEOUT_UI_SEG=${TIMEOUT_UI_SEG:-120}
TIMEOUT_DOCKER_SEG=${TIMEOUT_DOCKER_SEG:-180}
INTERVALO=5
ANCHO=30

# Colores solo si la salida es una terminal
if [ -t 1 ]; then
  RST=$'\033[0m'; NEG=$'\033[1m'; TENUE=$'\033[2m'
  ROJO=$'\033[1;31m'; VERDE=$'\033[1;32m'; AMARILLO=$'\033[1;33m'
  AZUL=$'\033[1;34m'; MAGENTA=$'\033[1;35m'; CIAN=$'\033[1;36m'
else
  RST=""; NEG=""; TENUE=""; ROJO=""; VERDE=""; AMARILLO=""; AZUL=""; MAGENTA=""; CIAN=""
fi

BUILD=""
if [ "${1:-}" = "--build" ]; then BUILD="--build"; fi

inicio=$(date +%s)

formatear() {
  local s=$1
  printf "%02d:%02d:%02d" $((s/3600)) $(((s%3600)/60)) $((s%60))
}
transcurrido() { formatear $(( $(date +%s) - inicio )); }

linea() { echo "${MAGENTA}────────────────────────────────────────────────────────────${RST}"; }

barra() {
  local listos=$1 total=$2
  local lleno=0
  if [ "$total" -gt 0 ]; then lleno=$(( listos * ANCHO / total )); fi
  local vacio=$(( ANCHO - lleno ))
  local b="" i
  for ((i = 0; i < lleno; i++)); do b="${b}█"; done
  local v=""
  for ((i = 0; i < vacio; i++)); do v="${v}░"; done
  printf "%s%s%s%s%s" "$VERDE" "$b" "$TENUE" "$v" "$RST"
}

fallo() {
  echo ""
  linea
  echo "${ROJO}❌ $1${RST}"
  echo "${AMARILLO}⏱️  Tiempo transcurrido: $(transcurrido)${RST}"
  linea
  exit 1
}

paso() {
  echo ""
  linea
  echo "${NEG}${CIAN}$1${RST}"
  linea
}

echo ""
echo "${NEG}${AZUL}🚀 Iniciando EMPRESA_JOSESAID${RST}"

paso "🐳 Paso 1/6 · Verificando Docker"
if docker info >/dev/null 2>&1; then
  echo "   ${VERDE}✅ Docker responde${RST}"
elif [ "$(uname -s)" = "Darwin" ]; then
  echo "   ${AMARILLO}🔧 Docker no está corriendo. Abriendo Docker Desktop...${RST}"
  open -a Docker 2>/dev/null || fallo "No se pudo abrir Docker Desktop. Ábrelo manualmente y vuelve a intentar."
  espera_docker=$(date +%s)
  while ! docker info >/dev/null 2>&1; do
    esperado_docker=$(( $(date +%s) - espera_docker ))
    if [ "$esperado_docker" -ge "$TIMEOUT_DOCKER_SEG" ]; then
      fallo "Docker Desktop no respondió en $(formatear "$TIMEOUT_DOCKER_SEG"). Revísalo manualmente."
    fi
    echo "   ${TENUE}⏳ [$(formatear "$esperado_docker")] Esperando a que Docker Desktop esté listo...${RST}"
    sleep 5
  done
  echo "   ${VERDE}✅ Docker Desktop listo (tardó $(formatear $(( $(date +%s) - espera_docker ))))${RST}"
else
  fallo "El daemon de Docker no responde. Abre Docker Desktop y vuelve a intentar."
fi

paso "📄 Paso 2/6 · Verificando archivo .env"
if [ ! -f .env ]; then
  fallo "No existe .env en la raíz del proyecto. Crea el archivo con las variables requeridas."
fi
echo "   ${VERDE}✅ .env encontrado${RST}"

paso "🌐 Paso 3/6 · Verificando la red empresa-net"
if docker network inspect empresa-net >/dev/null 2>&1; then
  echo "   ${VERDE}✅ La red ya existe${RST}"
else
  echo "   ${AMARILLO}🔧 La red no existe. Creándola...${RST}"
  docker network create empresa-net >/dev/null || fallo "No se pudo crear la red empresa-net."
  echo "   ${VERDE}✅ Red creada${RST}"
fi

if [ -n "$BUILD" ]; then
  paso "🔨 Paso 4/6 · Construyendo imágenes y levantando contenedores (puede tardar varios minutos)"
else
  paso "⬆️  Paso 4/6 · Levantando contenedores"
fi
docker compose up -d $BUILD || fallo "docker compose up falló. Revisa el mensaje anterior."
echo "   ${VERDE}✅ Contenedores creados${RST}"
echo "   ${TENUE}⏸️  Pausa de 10 s para que PostgreSQL, Kafka y Redis arranquen...${RST}"
sleep 10

paso "⏳ Paso 5/6 · Esperando a que los servicios estén listos (máximo $(formatear "$TIMEOUT_SEG"))"
espera_inicio=$(date +%s)
anterior=""
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
      pend="${pend}   ${AMARILLO}· ${servicio}${RST}  ${TENUE}(${estado}${salud:+ / $salud})${RST}"$'\n'
    fi
  done < <(docker compose ps -a --format '{{.Service}}|{{.State}}|{{.Health}}' 2>/dev/null)

  esperado=$(( $(date +%s) - espera_inicio ))

  if [ "$total" -gt 0 ] && [ -z "$pend" ]; then
    echo "   [$(barra "$listos" "$total")] ${NEG}$listos/$total${RST}  ⏱️  $(formatear "$esperado")"
    echo ""
    echo "   ${VERDE}${NEG}✅ Todos los servicios están listos${RST}"
    break
  fi

  if [ "$esperado" -ge "$TIMEOUT_SEG" ]; then
    echo ""
    echo "   ${ROJO}⚠️  Se agotó el tiempo de espera: $listos/$total listos${RST}"
    echo "   ${AMARILLO}⏳ Pendientes:${RST}"
    printf "%s" "$pend"
    fallo "Revisa con: docker compose ps   y   docker compose logs <servicio>"
  fi

  echo "   [$(barra "$listos" "$total")] ${NEG}$listos/$total${RST}  ⏱️  $(formatear "$esperado")"
  if [ "$pend" != "$anterior" ]; then
    echo "   ${AMARILLO}⏳ Esperando a:${RST}"
    printf "%s" "$pend"
    anterior="$pend"
  fi
  sleep "$INTERVALO"
done

paso "🔍 Paso 6/6 · Estado final"
docker compose ps -a --format '{{.Service}}|{{.State}}|{{.Health}}' 2>/dev/null | sort | while IFS='|' read -r servicio estado salud; do
  if [ -n "$salud" ]; then
    if [ "$salud" = "healthy" ] && [ "$estado" = "running" ]; then
      printf "   ${VERDE}✅ %-30s %s (%s)${RST}\n" "$servicio" "$estado" "$salud"
    else
      printf "   ${AMARILLO}⚠️  %-30s %s (%s)${RST}\n" "$servicio" "$estado" "$salud"
    fi
  else
    if [ "$estado" = "running" ]; then
      printf "   ${VERDE}🟢 %-30s %s${RST}\n" "$servicio" "$estado"
    else
      printf "   ${AMARILLO}⚠️  %-30s %s${RST}\n" "$servicio" "$estado"
    fi
  fi
done

paso "🖥️  Paso 7/7 · Levantando la UI (ui-ms)"
LOGIN_URL=${LOGIN_URL:-http://localhost:8098/login}
SALUD_UI=${SALUD_UI:-http://localhost:8098/actuator/health}
echo "   ⬆️  Levantando el contenedor de ui-ms..."
docker compose --env-file .env -f ui-ms/docker-compose.yml up -d || fallo "No se pudo levantar ui-ms. Revisa el mensaje anterior."
echo "   ✅ Contenedor de ui-ms creado"
espera_ui=$(date +%s)
while ! curl -fsS "$SALUD_UI" >/dev/null 2>&1; do
  esperado_ui=$(( $(date +%s) - espera_ui ))
  if [ "$esperado_ui" -ge "$TIMEOUT_UI_SEG" ]; then
    fallo "ui-ms no respondió en $(formatear "$TIMEOUT_UI_SEG"). Revisa con: docker compose --env-file .env -f ui-ms/docker-compose.yml logs"
  fi
  echo "   ⏳ [$(formatear "$esperado_ui")] Esperando a que la UI responda..."
  sleep 5
done
echo "   ✅ UI lista"

duracion=$(( $(date +%s) - inicio ))
echo ""
linea
echo "${AZUL}⏱️  Tiempo total: ${NEG}$(formatear "$duracion")${RST}${AZUL}  ($duracion s)${RST}"
echo "${NEG}${VERDE}🏁 EMPRESA_JOSESAID está listo${RST}"
linea

echo ""
echo "${AZUL}🌐 Abriendo el login en 3 segundos: ${NEG}$LOGIN_URL${RST}"
sleep 3
if [ "$(uname -s)" = "Darwin" ] && open "$LOGIN_URL" 2>/dev/null; then
  echo "   ${VERDE}✅ Navegador abierto en el login${RST}"
else
  echo "   ${AMARILLO}⚠️  No se pudo abrir el navegador. Abre manualmente: $LOGIN_URL${RST}"
fi
