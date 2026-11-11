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

ayuda() {
cat <<EOF
${NEG}${AZUL}Uso:${RST} bash configuration_scripts/start.sh ${CIAN}[opciones]${RST}

Levanta los servicios de ${NEG}EMPRESA_JOSESAID${RST} (incluye ui-ms) y espera a que estén listos.

  ${NEG}Opción                 Qué hace${RST}
  ${TENUE}---------------------  ----------------------------------------------------------${RST}
  🟢 ${VERDE}(sin opciones)${RST}      Levanta los contenedores con las imágenes ya construidas
  🏗️  ${AMARILLO}--build${RST}             Reconstruye las imágenes antes de levantar ${TENUE}(tarda varios minutos)${RST}
  ❓ ${CIAN}-h, --help${RST}          Muestra esta ayuda

${NEG}${MAGENTA}Variables de entorno opcionales:${RST}
  ⏱️  ${AMARILLO}TIMEOUT_SEG${RST}          Espera máxima por los servicios ${TENUE}(300 s)${RST}
  ⏱️  ${AMARILLO}TIMEOUT_UI_SEG${RST}       Espera máxima por ui-ms ${TENUE}(120 s)${RST}
  ⏱️  ${AMARILLO}TIMEOUT_DOCKER_SEG${RST}   Espera máxima por Docker Desktop ${TENUE}(180 s)${RST}
  🌐 ${AMARILLO}LOGIN_URL${RST}            URL que se abre al terminar ${TENUE}(http://localhost:8098/login)${RST}

${NEG}${MAGENTA}Requisitos:${RST}
  🐳 Docker Desktop corriendo
  📄 Archivo ${NEG}.env${RST} en la raíz del proyecto

${NEG}${MAGENTA}Ejemplos:${RST}
  ${VERDE}bash configuration_scripts/start.sh${RST}
  ${VERDE}bash configuration_scripts/start.sh --build${RST}
EOF
}

BUILD=""
for arg in "$@"; do
  case "$arg" in
    --build)   BUILD="--build" ;;
    -h|--help) ayuda; exit 0 ;;
    *)         echo "Argumento desconocido: $arg"
               echo "Uso: bash configuration_scripts/start.sh [--build] [--help]"
               exit 1 ;;
  esac
done

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

paso "🐳 Paso 1/7 · Verificando Docker"
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

paso "📄 Paso 2/7 · Verificando archivo .env"
if [ ! -f .env ]; then
  fallo "No existe .env en la raíz del proyecto. Crea el archivo con las variables requeridas."
fi
echo "   ${VERDE}✅ .env encontrado${RST}"

paso "🌐 Paso 3/7 · Verificando la red empresa-net"
if docker network inspect empresa-net >/dev/null 2>&1; then
  echo "   ${VERDE}✅ La red ya existe${RST}"
else
  echo "   ${AMARILLO}🔧 La red no existe. Creándola...${RST}"
  docker network create empresa-net >/dev/null || fallo "No se pudo crear la red empresa-net."
  echo "   ${VERDE}✅ Red creada${RST}"
fi

if [ -n "$BUILD" ]; then
  paso "🔨 Paso 4/7 · Construyendo imágenes y levantando contenedores (puede tardar varios minutos)"
else
  paso "⬆️  Paso 4/7 · Levantando contenedores"
fi
# Descarga las imágenes que falten localmente (compose usa --pull never y no las bajaría).
# Muestra el avance de cada una (porcentaje por capas). Si una no está en el registro
# (imagen construida en local), se avisa y compose usa la local.
pendientes=()
while IFS= read -r img; do
  [ -z "$img" ] && continue
  docker image inspect "$img" >/dev/null 2>&1 || pendientes+=("$img")
done < <(docker compose config --images 2>/dev/null | sort -u)

if [ "${#pendientes[@]}" -gt 0 ]; then
  n=${#pendientes[@]}
  echo "   ${TENUE}⬇️  Imágenes por descargar: $n${RST}"
  logdir=$(mktemp -d)
  pids=(); logs=(); codigos=()
  for i in "${!pendientes[@]}"; do
    img=${pendientes[$i]}
    log="$logdir/$i.log"
    logs+=("$log")
    docker pull "$img" > "$log" 2>&1 &
    pids+=("$!")
    codigos+=("")
  done

  primera=1
  while :; do
    activos=0
    for i in "${!pendientes[@]}"; do
      if kill -0 "${pids[$i]}" 2>/dev/null; then
        activos=$((activos + 1))
      elif [ -z "${codigos[$i]}" ]; then
        if wait "${pids[$i]}"; then codigos[$i]=0; else codigos[$i]=1; fi
      fi
    done

    # Redibuja el bloque en la misma posición si la salida es una terminal
    if [ -t 1 ] && [ "$primera" -eq 0 ]; then printf "\033[%dA" "$n"; fi
    primera=0

    for i in "${!pendientes[@]}"; do
      img=${pendientes[$i]}; log=${logs[$i]}
      total=$(grep -c "Pulling fs layer" "$log" 2>/dev/null || true); total=${total:-0}
      hechas=$(grep -c -E "Pull complete|Already exists" "$log" 2>/dev/null || true); hechas=${hechas:-0}
      if [ "${codigos[$i]}" = "0" ]; then
        hechas=$total; pct=100; estado="${VERDE}✅ listo${RST}"
      else
        if [ "$total" -gt 0 ]; then pct=$(( hechas * 100 / total )); else pct=0; fi
        if [ "${codigos[$i]}" = "1" ]; then
          estado="${TENUE}⚠️  no está en el registro (se usará la local)${RST}"
        else
          estado="${AMARILLO}⬇️  $hechas/$total capas${RST}"
        fi
      fi
      printf "\033[2K   %-32s [%s] %3d%%  %s\n" "$img" "$(barra "$hechas" "$total")" "$pct" "$estado"
    done

    [ "$activos" -eq 0 ] && break
    sleep 2
  done
  rm -rf "$logdir"
  echo ""
fi
docker compose up -d --pull never $BUILD || fallo "docker compose up falló. Revisa el mensaje anterior."
echo "   ${VERDE}✅ Contenedores creados${RST}"
echo "   ${TENUE}⏸️  Pausa de 10 s para que PostgreSQL, Kafka y Redis arranquen...${RST}"
sleep 10

max_espera=$(formatear "$TIMEOUT_SEG")
paso "⏳ Paso 5/7 · Esperando a que los servicios estén listos (máximo $max_espera)"
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

paso "🔍 Paso 6/7 · Estado final"
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
docker compose --env-file .env -f ui-ms/docker-compose.yml up -d --pull never || fallo "No se pudo levantar ui-ms. Revisa el mensaje anterior."
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

# Después del login, abre también el índice de documentación de la raíz del proyecto
INDICE="$(pwd)/index.html"
sleep 1
if [ "$(uname -s)" = "Darwin" ] && [ -f "$INDICE" ] && open "$INDICE" 2>/dev/null; then
  echo "   ${VERDE}✅ Índice de documentación abierto${RST}"
elif [ -f "$INDICE" ]; then
  echo "   ${AMARILLO}⚠️  No se pudo abrir el índice. Ábrelo manualmente: $INDICE${RST}"
fi
